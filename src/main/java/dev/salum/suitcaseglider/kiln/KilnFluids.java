package dev.salum.suitcaseglider.kiln;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.MachineContent;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.*;
import net.minecraftforge.registries.*;

public final class KilnFluids {
    public static final DeferredRegister<FluidType> TYPES=DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES,SuitcaseGlider.ID);
    public static final DeferredRegister<Fluid> FLUIDS=DeferredRegister.create(ForgeRegistries.FLUIDS,SuitcaseGlider.ID);
    public static final RegistryObject<FluidType> TYPE=TYPES.register("creosote",()->new FluidType(FluidType.Properties.create().density(1100).viscosity(3000)){
        @Override public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer){consumer.accept(new IClientFluidTypeExtensions(){
            public ResourceLocation getStillTexture(){return new ResourceLocation("minecraft:block/water_still");}
            public ResourceLocation getFlowingTexture(){return new ResourceLocation("minecraft:block/water_flow");}
            public int getTintColor(){return 0xff79461d;}
        });}
    });
    public static final RegistryObject<ForgeFlowingFluid.Source> SOURCE=FLUIDS.register("creosote",()->new ForgeFlowingFluid.Source(properties()));
    public static final RegistryObject<ForgeFlowingFluid.Flowing> FLOWING=FLUIDS.register("flowing_creosote",()->new ForgeFlowingFluid.Flowing(properties()));
    public static final RegistryObject<LiquidBlock> BLOCK=MachineContent.BLOCKS.register("creosote",()->new LiquidBlock(SOURCE,Block.Properties.copy(Blocks.WATER)));
    public static final RegistryObject<BucketItem> BUCKET=SuitcaseGlider.ITEMS.register("creosote_bucket",()->new BucketItem(SOURCE,new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
    private static ForgeFlowingFluid.Properties properties(){return new ForgeFlowingFluid.Properties(TYPE,SOURCE,FLOWING).bucket(BUCKET).block(BLOCK).tickRate(10).slopeFindDistance(2).levelDecreasePerBlock(2);}
    public static void register(IEventBus bus){TYPES.register(bus);FLUIDS.register(bus);}
}
