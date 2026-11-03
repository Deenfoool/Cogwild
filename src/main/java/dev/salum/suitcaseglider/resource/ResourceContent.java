package dev.salum.suitcaseglider.resource;

import dev.salum.suitcaseglider.SuitcaseGlider;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

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
    public static final List<RegistryObject<Item>> ALL=List.of(UNFIRED_BRICK,REFRACTORY_BRICK,COKE);
    public static void init(){}
}
