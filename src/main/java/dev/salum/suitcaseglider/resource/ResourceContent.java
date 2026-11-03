package dev.salum.suitcaseglider.resource;

import dev.salum.suitcaseglider.SuitcaseGlider;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.level.block.Block;
import dev.salum.suitcaseglider.machine.MachineContent;

/** First material tier; later production machines consume these same registry IDs. */
public final class ResourceContent {
    private static class Material extends Item {
        private final String description;
        Material(String name){super(new Properties());description="tooltip.suitcaseglider."+name;}
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable(description).withStyle(ChatFormatting.GRAY));}
    }
    public static final RegistryObject<Item> UNFIRED_BRICK=SuitcaseGlider.ITEMS.register("unfired_refractory_brick",()->new Material("unfired_refractory_brick"));
    public static final RegistryObject<Item> REFRACTORY_BRICK=SuitcaseGlider.ITEMS.register("refractory_brick",()->new Material("refractory_brick"));
    public static final RegistryObject<Item> COKE=SuitcaseGlider.ITEMS.register("coke",()->new Material("coke"){
        @Override public int getBurnTime(ItemStack stack,RecipeType<?> type){return 3200;}
    });
    public static final RegistryObject<Block> BRICKS=MachineContent.BLOCKS.register("refractory_bricks",()->new Block(Block.Properties.of().strength(7,12).sound(net.minecraft.world.level.block.SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<KilnComponentBlock> CONTROLLER=MachineContent.BLOCKS.register("coke_oven_controller",KilnComponentBlock::new);
    public static final RegistryObject<KilnComponentBlock> INPUT=MachineContent.BLOCKS.register("coke_oven_input",KilnComponentBlock::new);
    public static final RegistryObject<KilnComponentBlock> OUTPUT=MachineContent.BLOCKS.register("coke_oven_output",KilnComponentBlock::new);
    private static RegistryObject<Item> component(String name,RegistryObject<? extends Block> block){return SuitcaseGlider.ITEMS.register(name,()->new BlockItem(block.get(),new Item.Properties()){
        @Override public void appendHoverText(ItemStack stack,Level level,List<Component> lines,TooltipFlag flag){lines.add(Component.translatable("tooltip.suitcaseglider."+name).withStyle(ChatFormatting.GRAY));}
    });}
    public static final RegistryObject<Item> BRICKS_ITEM=component("refractory_bricks",BRICKS),CONTROLLER_ITEM=component("coke_oven_controller",CONTROLLER),INPUT_ITEM=component("coke_oven_input",INPUT),OUTPUT_ITEM=component("coke_oven_output",OUTPUT);
    public static final List<RegistryObject<Item>> ALL=List.of(UNFIRED_BRICK,REFRACTORY_BRICK,COKE,BRICKS_ITEM,CONTROLLER_ITEM,INPUT_ITEM,OUTPUT_ITEM);
    public static void init(){}
}
