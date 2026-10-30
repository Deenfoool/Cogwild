package dev.salum.suitcaseglider.advancement;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.MachineContent;
import dev.salum.suitcaseglider.mech.MechContent;
import java.nio.file.*;
import java.io.*;
import java.util.*;
import net.minecraft.advancements.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Synthetic server integration checks. The fixture datapack exists only in this isolated profile. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class AdvancementDevelopment {
    private static MinecraftServer stop;private static int countdown;private static final List<String> report=new ArrayList<>();
    private static void check(boolean pass,String label){report.add(label+"="+pass);if(!pass)throw new IllegalStateException("ADVANCEMENT_SMOKE_FAILED: "+label);}
    private static Advancement get(MinecraftServer server,String id){return Objects.requireNonNull(server.getAdvancements().getAdvancement(new ResourceLocation("cogwild",id)),id);}
    private static boolean done(ServerPlayer player,String id){return player.getAdvancements().getOrStartProgress(get(player.server,id)).isDone();}
    private static void inventory(ServerPlayer player,ItemStack item){player.getInventory().setItem(0,item);CriteriaTriggers.INVENTORY_CHANGED.trigger(player,player.getInventory(),item);}
    private static ServerPlayer player(ServerLevel level,String suffix){
        // Forge deliberately blocks advancement awards to FakePlayer. Use an unconnected real server player.
        var server=level.getServer();var player=new ServerPlayer(server,level,new GameProfile(UUID.nameUUIDFromBytes(("cogwild-advancement-"+suffix).getBytes()),"CogwildChecks"));
        player.connection=new ServerGamePacketListenerImpl(server,new Connection(PacketFlow.SERVERBOUND),player);
        for(var entry:AdvancementCatalog.entries()){var advancement=get(server,entry.id().split(":")[1]);for(String criterion:advancement.getCriteria().keySet())player.getAdvancements().revoke(advancement,criterion);}
        player.totalExperience=0;return player;
    }
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("cogwild.advancementSmoke"))return;
        var server=event.getServer();var level=server.overworld();var p=player(level,"main");
        for(var entry:AdvancementCatalog.entries()){
            var advancement=get(server,entry.id().split(":")[1]);
            check(!advancement.getDisplay().getIcon().isEmpty(),"registered icon "+entry.id());check(advancement.getDisplay().isHidden()==entry.hidden(),"hidden flag "+entry.id());
            for(String criterion:advancement.getCriteria().keySet())p.getAdvancements().revoke(advancement,criterion);
        }
        p.totalExperience=0;check(AdvancementCatalog.entries().size()==24,"all 24 definitions loaded by Minecraft");
        inventory(p,new ItemStack(Items.COPPER_INGOT));check(!done(p,"root"),"unrelated metal does not satisfy steel condition");
        // The test datapack tags iron as steel, gold ore as tungsten, copper ore as zinc, and paper as a blueprint.
        inventory(p,new ItemStack(Items.IRON_INGOT));check(done(p,"root"),"root responds to steel tag");
        inventory(p,new ItemStack(MachineContent.CRUSHER_ITEM.get()));check(done(p,"first_machine"),"existing crusher unlocks mechanization");check(!done(p,"crusher"),"owning crusher is not processing ore");
        IndustrialProgress.record(p,IndustrialProgress.Action.ORE_CRUSHED,1);check(done(p,"crusher"),"crusher ownership plus successful ore processing");
        inventory(p,new ItemStack(Items.GOLD_ORE));check(!done(p,"rare_ores"),"one rare ore is insufficient");
        inventory(p,new ItemStack(Items.COPPER_ORE));check(done(p,"rare_ores"),"both historical ore criteria required");
        inventory(p,new ItemStack(Items.PAPER));check(!done(p,"first_blueprint"),"possession alone is not blueprint crafting or trading");
        MinecraftForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(p,new ItemStack(Items.PAPER),new SimpleContainer(9)));check(done(p,"first_blueprint"),"crafting branch of blueprint OR condition");
        var buyer=player(level,"buyer");CriteriaTriggers.TRADE.trigger(buyer,new net.minecraft.world.entity.npc.Villager(EntityType.VILLAGER,level),new ItemStack(Items.PAPER));check(done(buyer,"first_blueprint"),"trade branch of blueprint OR condition");
        IndustrialProgress.record(p,IndustrialProgress.Action.DIESEL_PRODUCED,0);IndustrialProgress.record(p,IndustrialProgress.Action.DIESEL_PRODUCED,-10);check(!done(p,"diesel"),"nonpositive production ignored");
        for(int i=0;i<3;i++)IndustrialProgress.record(p,IndustrialProgress.Action.DIESEL_PRODUCED,250);
        check(!done(p,"diesel")&&IndustrialProgress.total(p,IndustrialProgress.Action.DIESEL_PRODUCED)==750,"partial diesel batches accumulate below one bucket");
        var clone=player(level,"clone");CompoundTag unrelated=new CompoundTag();unrelated.putString("OtherModMarker","preserved");clone.getPersistentData().put(Player.PERSISTED_NBT_TAG,unrelated);
        IndustrialProgress.clone(new PlayerEvent.Clone(clone,p,true));check(IndustrialProgress.total(clone,IndustrialProgress.Action.DIESEL_PRODUCED)==750,"partial production survives respawn clone");check(clone.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getString("OtherModMarker").equals("preserved"),"clone preserves unrelated persisted data");
        int before=clone.totalExperience;IndustrialProgress.record(clone,IndustrialProgress.Action.DIESEL_PRODUCED,250);check(done(clone,"diesel")&&clone.totalExperience-before==20,"one bucket grants specified XP");
        int rewarded=clone.totalExperience;IndustrialProgress.record(clone,IndustrialProgress.Action.DIESEL_PRODUCED,1000);check(clone.totalExperience==rewarded,"repeated production cannot repeat XP reward");
        IndustrialProgress.record(clone,IndustrialProgress.Action.DIESEL_PRODUCED,Long.MAX_VALUE);IndustrialProgress.record(clone,IndustrialProgress.Action.DIESEL_PRODUCED,1);check(IndustrialProgress.total(clone,IndustrialProgress.Action.DIESEL_PRODUCED)==Long.MAX_VALUE,"counter saturates without overflow");
        var ant=MechContent.ANT.get().create(level);ant.tameWithName(p);check(!done(p,"first_mech")&&!done(p,"mech_ant"),"taming is not shipyard assembly");
        IndustrialProgress.mechAssembled(p,new ResourceLocation("cogwild","mech_ant"));check(done(p,"first_mech")&&done(p,"mech_ant")&&!done(p,"mechanical_menagerie"),"ant assembly advances only matching achievements");
        IndustrialProgress.mechAssembled(p,new ResourceLocation("cogwild","robobuffalo"));check(!done(p,"mechanical_menagerie"),"two mech types do not complete menagerie");
        IndustrialProgress.mechAssembled(p,new ResourceLocation("cogwild","mech_bee"));check(done(p,"mechanical_menagerie"),"all three mech types complete menagerie");
        // Future machinery is not simulated: these calls verify only its documented server event contract.
        for(var action:IndustrialProgress.Action.values())IndustrialProgress.record(p,action,1000);
        for(var entry:AdvancementCatalog.entries())check(done(p,entry.id().split(":")[1]),"synthetic criteria completion "+entry.id());
        check(p.totalExperience==535,"exactly 535 total XP for all 24 advancements");
        p.getAdvancements().save();var file=server.getWorldPath(LevelResource.PLAYER_ADVANCEMENTS_DIR).resolve(p.getUUID()+".json");
        var reloaded=new PlayerAdvancements(server.getFixerUpper(),server.getPlayerList(),server.getAdvancements(),file,p);
        for(var entry:AdvancementCatalog.entries())check(reloaded.getOrStartProgress(get(server,entry.id().split(":")[1])).isDone(),"saved progress reload "+entry.id());
        reloaded.stopListening();check(p.totalExperience==535,"loading completed progress does not repeat rewards");
        Files.write(Path.of("advancement-smoke-report.txt"),report);SuitcaseGlider.LOG.info("ADVANCEMENT_SMOKE_OK {} checks",report.size());stop=server;countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(stop!=null&&event.phase==TickEvent.Phase.END&&--countdown<=0){var server=stop;stop=null;server.halt(false);}}
}
