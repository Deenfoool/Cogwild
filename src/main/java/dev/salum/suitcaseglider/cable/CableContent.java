package dev.salum.suitcaseglider.cable;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.MachineContent;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

public final class CableContent {
    public static final RegistryObject<EnergyNodeBlock> RELAY=MachineContent.BLOCKS.register("cable_relay",()->new EnergyNodeBlock(false));
    public static final RegistryObject<EnergyNodeBlock> GENERATOR=MachineContent.BLOCKS.register("fuel_generator",()->new EnergyNodeBlock(true));
    public static final RegistryObject<Item> CABLE=SuitcaseGlider.ITEMS.register("power_cable",()->new CableItem(new Item.Properties()));
    public static final RegistryObject<Item> RELAY_ITEM=SuitcaseGlider.ITEMS.register("cable_relay",()->new BlockItem(RELAY.get(),new Item.Properties()));
    public static final RegistryObject<Item> GENERATOR_ITEM=SuitcaseGlider.ITEMS.register("fuel_generator",()->new BlockItem(GENERATOR.get(),new Item.Properties()));
    public static final RegistryObject<BlockEntityType<EnergyNodeEntity>> TYPE=MachineContent.ENTITIES.register("energy_node",()->BlockEntityType.Builder.of(EnergyNodeEntity::new,RELAY.get(),GENERATOR.get()).build(null));
    public static void init(){}
}
