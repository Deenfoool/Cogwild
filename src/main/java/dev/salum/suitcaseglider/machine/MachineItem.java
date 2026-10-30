package dev.salum.suitcaseglider.machine;

import java.util.function.Consumer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class MachineItem extends BlockItem {
    public MachineItem(MachineBlock block){super(block,new Properties());}
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer){consumer.accept(new IClientItemExtensions(){@Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){return dev.salum.suitcaseglider.client.MachineRenderer.itemRenderer();}});}
}
