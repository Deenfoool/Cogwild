package dev.salum.suitcaseglider.mech;

import dev.salum.suitcaseglider.SuitcaseGlider;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class MechContent {
    public static final DeferredRegister<EntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.ENTITY_TYPES,SuitcaseGlider.ID);
    public static final RegistryObject<EntityType<MechAntEntity>> ANT=ENTITIES.register("mech_ant",()->EntityType.Builder.of(MechAntEntity::new,MobCategory.CREATURE).sized(1.8f,2.35f).clientTrackingRange(12).updateInterval(1).build("suitcaseglider:mech_ant"));
    public static final RegistryObject<Item> EGG=SuitcaseGlider.ITEMS.register("mech_ant_spawn_egg",()->new ForgeSpawnEggItem(ANT,0x41484a,0xb18b40,new Item.Properties()));
    public static void register(IEventBus bus){ENTITIES.register(bus);bus.addListener(MechContent::attributes);}
    private static void attributes(EntityAttributeCreationEvent event){event.put(ANT.get(),MechAntEntity.createBaseHorseAttributes().build());}
}
