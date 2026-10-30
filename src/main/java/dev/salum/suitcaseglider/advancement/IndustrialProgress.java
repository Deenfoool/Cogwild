package dev.salum.suitcaseglider.advancement;

import dev.salum.suitcaseglider.SuitcaseGlider;
import java.util.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Server-side integration points. Producers must call them after a real successful operation. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class IndustrialProgress {
    public enum Action {
        STEEL_SMELTED,BRASS_SMELTED,ORE_CRUSHED,COMPRESSED_AIR_TANK_FILLED,CONSTRUCTION_YARD_FORMED,BLUEPRINT_CRAFTED,
        OIL_EXTRACTED,DIESEL_PRODUCED,BIODIESEL_PRODUCED,NITROGEN_PRODUCED,DISTILLATION_CYCLE_COMPLETED,
        LUBRICANT_PRODUCED,DIESEL_GENERATOR_STARTED,AVIATION_FUEL_PRODUCED,BOOST_FUEL_PRODUCED,HARDENED_STEEL_PRODUCED,
        MECH_ASSEMBLED,MECH_ANT_ASSEMBLED,MECH_BEE_ASSEMBLED,ROBOBUFFALO_ASSEMBLED;
        public String key(){return name().toLowerCase(Locale.ROOT);}
        public static boolean valid(String key){return Arrays.stream(values()).anyMatch(a->a.key().equals(key));}
    }
    public static final IndustrialProgressTrigger TRIGGER=new IndustrialProgressTrigger();
    public static final TagKey<Item> MECH_BLUEPRINTS=TagKey.create(Registries.ITEM,new ResourceLocation("cogwild","mech_blueprints"));
    private static final String COUNTERS="CogwildIndustrialProgress";
    public static void register(IEventBus bus){
        for(var entry:AdvancementCatalog.entries()){
            String name=entry.id().substring(entry.id().indexOf(':')+1);
            SuitcaseGlider.ITEMS.register("advancement_icon_"+name,()->new Item(new Item.Properties()));
        }
        bus.addListener((FMLCommonSetupEvent event)->event.enqueueWork(()->CriteriaTriggers.register(TRIGGER)));
    }
    public static void record(ServerPlayer player,Action action,long units){
        if(units<=0)return;
        if(!player.server.isSameThread())throw new IllegalStateException("Advancements must be recorded on the server thread");
        CompoundTag persisted=player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG),counters=persisted.getCompound(COUNTERS);
        long old=Math.max(0,counters.getLong(action.key())),total=units>Long.MAX_VALUE-old?Long.MAX_VALUE:old+units;
        counters.putLong(action.key(),total);persisted.put(COUNTERS,counters);player.getPersistentData().put(Player.PERSISTED_NBT_TAG,persisted);
        TRIGGER.trigger(player,action.key(),total);
    }
    public static long total(ServerPlayer player,Action action){return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getCompound(COUNTERS).getLong(action.key());}
    public static void mechAssembled(ServerPlayer player,ResourceLocation mech){
        if(!Set.of("cogwild","suitcaseglider").contains(mech.getNamespace()))return;
        Action specific=switch(mech.getPath()){case "mech_ant"->Action.MECH_ANT_ASSEMBLED;case "mech_bee"->Action.MECH_BEE_ASSEMBLED;case "robobuffalo"->Action.ROBOBUFFALO_ASSEMBLED;default->null;};
        if(specific==null)return;record(player,Action.MECH_ASSEMBLED,1);record(player,specific,1);
    }
    @SubscribeEvent public static void crafted(PlayerEvent.ItemCraftedEvent event){if(event.getEntity() instanceof ServerPlayer player&&event.getCrafting().is(MECH_BLUEPRINTS))record(player,Action.BLUEPRINT_CRAFTED,1);}
    @SubscribeEvent public static void clone(PlayerEvent.Clone event){
        var old=event.getOriginal().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if(old.contains(COUNTERS)){var next=event.getEntity().getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);next.put(COUNTERS,old.getCompound(COUNTERS).copy());event.getEntity().getPersistentData().put(Player.PERSISTED_NBT_TAG,next);}
    }
}
