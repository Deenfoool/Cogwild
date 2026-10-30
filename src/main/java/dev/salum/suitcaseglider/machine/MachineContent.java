package dev.salum.suitcaseglider.machine;

import dev.salum.suitcaseglider.SuitcaseGlider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class MachineContent {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,SuitcaseGlider.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,SuitcaseGlider.ID);
    public static final RegistryObject<MachineBlock> LATHE=BLOCKS.register("lathe",()->new MachineBlock("lathe"));
    public static final RegistryObject<MachineBlock> CRUSHER=BLOCKS.register("crusher",()->new MachineBlock("crusher"));
    public static final RegistryObject<Item> LATHE_ITEM=SuitcaseGlider.ITEMS.register("lathe",()->new MachineItem(LATHE.get()));
    public static final RegistryObject<Item> CRUSHER_ITEM=SuitcaseGlider.ITEMS.register("crusher",()->new MachineItem(CRUSHER.get()));
    public static final RegistryObject<BlockEntityType<MachineBlockEntity>> TYPE=ENTITIES.register("machine",()->BlockEntityType.Builder.of(MachineBlockEntity::new,LATHE.get(),CRUSHER.get()).build(null));
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);}
}
