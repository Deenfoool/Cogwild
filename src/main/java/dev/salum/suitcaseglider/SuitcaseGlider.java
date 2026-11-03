package dev.salum.suitcaseglider;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.*;
import org.slf4j.Logger;

@Mod(SuitcaseGlider.ID)
public final class SuitcaseGlider {
    public static final String ID = "suitcaseglider";
    public static final Logger LOG = LogUtils.getLogger();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ID);
    public static final DeferredRegister<Enchantment> ENCHANTMENTS = DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ID);
    public static final RegistryObject<Item> GLIDER = ITEMS.register("glider", () -> new GliderItem(new Item.Properties().durability(192)));
    public static final EnchantmentCategory CATEGORY = EnchantmentCategory.create("SUITCASE_GLIDER", i -> i instanceof GliderItem);
    public static final RegistryObject<Enchantment> SOARING = ENCHANTMENTS.register("soaring", () -> new Enchantment(Enchantment.Rarity.RARE, CATEGORY, new EquipmentSlot[]{EquipmentSlot.MAINHAND}) {
        @Override public int getMaxLevel() { return 3; }
        @Override public int getMinCost(int level) { return 12 + level * 7; }
        @Override public int getMaxCost(int level) { return getMinCost(level) + 20; }
        @Override public boolean isTreasureOnly() { return true; }
    });
    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("gliders", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.suitcaseglider")).icon(() -> new ItemStack(GLIDER.get()))
            .displayItems((parameters, output) -> {
                output.accept(GLIDER.get());
                output.accept(dev.salum.suitcaseglider.kiln.KilnContent.KEY.get());
                output.accept(dev.salum.suitcaseglider.kiln.KilnContent.CHARCOAL_ITEM.get());
                output.accept(dev.salum.suitcaseglider.kiln.KilnFluids.BUCKET.get());
                dev.salum.suitcaseglider.resource.ResourceContent.ALL.forEach(item -> output.accept(item.get()));
                dev.salum.suitcaseglider.blast.BlastContent.ITEMS.forEach(item -> output.accept(item.get()));
                output.accept(dev.salum.suitcaseglider.mech.MechContent.EGG.get());
                dev.salum.suitcaseglider.machine.MachineContent.MACHINE_ITEMS.values().forEach(item -> output.accept(item.get()));
                output.accept(dev.salum.suitcaseglider.cable.CableContent.CABLE.get());
                output.accept(dev.salum.suitcaseglider.cable.CableContent.RELAY_ITEM.get());
                output.accept(dev.salum.suitcaseglider.cable.CableContent.GENERATOR_ITEM.get());
                for (int level = 1; level <= 3; level++) output.accept(EnchantedBookItem.createForEnchantment(new EnchantmentInstance(SOARING.get(), level)));
            }).build());
    public SuitcaseGlider() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        dev.salum.suitcaseglider.mech.MechContent.register(bus);
        dev.salum.suitcaseglider.machine.MachineContent.register(bus);
        dev.salum.suitcaseglider.cable.CableContent.init();
        dev.salum.suitcaseglider.advancement.IndustrialProgress.register(bus);
        dev.salum.suitcaseglider.resource.ResourceContent.init();
        dev.salum.suitcaseglider.kiln.KilnContent.register(bus);
        dev.salum.suitcaseglider.blast.BlastContent.register(bus);
        ITEMS.register(bus); ENCHANTMENTS.register(bus); TABS.register(bus);
        GliderNetwork.register();
        dev.salum.suitcaseglider.mech.MechNetwork.register();
        dev.salum.suitcaseglider.cable.CableNetwork.register();
        MinecraftForge.EVENT_BUS.register(new GliderController());
        MinecraftForge.EVENT_BUS.register(new SmokeChecks());
    }
    public static ResourceLocation id(String path) { return new ResourceLocation(ID, path); }
}
