package ru.deenfoool.cogwild.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.deenfoool.cogwild.Cogwild;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, Cogwild.MOD_ID);

    public static final RegistryObject<Block> MACHINE_CASING = BLOCKS.register(
            "machine_casing",
            () -> new Block(BlockBehaviour.Properties.of()
                    .strength(4.0F, 8.0F)
                    .sound(SoundType.METAL)
                    .requiresCorrectToolForDrops())
    );

    public static final RegistryObject<Item> MACHINE_CASING_ITEM = ModItems.ITEMS.register(
            "machine_casing",
            () -> new BlockItem(MACHINE_CASING.get(), new Item.Properties())
    );

    private ModBlocks() {
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
