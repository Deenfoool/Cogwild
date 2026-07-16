package ru.deenfoool.cogwild;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import ru.deenfoool.cogwild.registry.ModBlocks;
import ru.deenfoool.cogwild.registry.ModCreativeTabs;
import ru.deenfoool.cogwild.registry.ModItems;

@Mod(Cogwild.MOD_ID)
public final class Cogwild {
    public static final String MOD_ID = "cogwild";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Cogwild(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModCreativeTabs.register(modEventBus);

        LOGGER.info("Cogwild: Mechanical Fauna initialized");
    }
}
