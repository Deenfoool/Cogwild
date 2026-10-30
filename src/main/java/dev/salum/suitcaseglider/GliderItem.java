package dev.salum.suitcaseglider;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

public final class GliderItem extends Item implements DyeableLeatherItem {
    public GliderItem(Properties properties) { super(properties); }
    @Override public int getEnchantmentValue() { return 8; }
    @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return ingredient.is(Items.LEATHER); }
    @Override public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment == Enchantments.UNBREAKING || enchantment == SuitcaseGlider.SOARING.get();
    }
    public static int fabricColor(ItemStack stack) {
        return stack.getItem() instanceof DyeableLeatherItem dye && dye.hasCustomColor(stack) ? dye.getColor(stack) : 0xdcd6be;
    }
    @Override public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.suitcaseglider.auto").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.suitcaseglider.hands").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.suitcaseglider.dye").withStyle(ChatFormatting.DARK_GRAY));
    }
    @Override public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override public net.minecraft.client.model.HumanoidModel.ArmPose getArmPose(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.InteractionHand hand, ItemStack stack) {
                return hand==net.minecraft.world.InteractionHand.MAIN_HAND ? dev.salum.suitcaseglider.client.GliderClient.ARM_POSE : null;
            }
            @Override public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return dev.salum.suitcaseglider.client.GliderItemRenderer.instance();
            }
        });
    }
}
