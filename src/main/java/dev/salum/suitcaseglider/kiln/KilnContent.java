package dev.salum.suitcaseglider.kiln;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.MachineContent;
import dev.salum.suitcaseglider.resource.ResourceContent;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;

public final class KilnContent {
    public static final RegistryObject<net.minecraft.world.level.block.Block> CHARCOAL_BLOCK=MachineContent.BLOCKS.register("charcoal_block",()->new net.minecraft.world.level.block.Block(net.minecraft.world.level.block.Block.Properties.of().strength(5,6).sound(net.minecraft.world.level.block.SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> CHARCOAL_ITEM=SuitcaseGlider.ITEMS.register("charcoal_block",()->new BlockItem(CHARCOAL_BLOCK.get(),new Item.Properties()){
        @Override public int getBurnTime(ItemStack s,net.minecraft.world.item.crafting.RecipeType<?> type){return 14400;}
    });
    public static final RegistryObject<Item> KEY=SuitcaseGlider.ITEMS.register("guild_assembly_key",()->new Item(new Item.Properties().stacksTo(1)){
        @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){lines.add(Component.translatable("tooltip.suitcaseglider.guild_assembly_key").withStyle(net.minecraft.ChatFormatting.GRAY));}
    });
    public static final RegistryObject<BlockEntityType<KilnBlockEntity>> TYPE=MachineContent.ENTITIES.register("coke_oven",()->BlockEntityType.Builder.of(KilnBlockEntity::new,ResourceContent.CONTROLLER.get(),ResourceContent.INPUT.get(),ResourceContent.OUTPUT.get()).build(null));
    public static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,SuitcaseGlider.ID);
    public static final RegistryObject<MenuType<KilnMenu>> MENU=MENUS.register("coke_oven",()->IForgeMenuType.create(KilnMenu::new));
    public static void register(IEventBus bus){MENUS.register(bus);KilnFluids.register(bus);}
}
