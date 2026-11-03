package dev.salum.suitcaseglider.kiln;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.resource.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class CreosoteDevelopment {
    private static MinecraftServer stop;private static int countdown;private static final List<String> report=new ArrayList<>();
    private static void check(boolean ok,String label){if(!ok)throw new IllegalStateException("CREOSOTE_SMOKE_FAILED "+label);report.add(label+"=true");}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("cogwild.creosoteSmoke"))return;
        var server=event.getServer();var l=server.overworld();var p=FakePlayerFactory.getMinecraft(l);p.setGameMode(GameType.SURVIVAL);p.getInventory().clearContent();
        var grid=new TransientCraftingContainer(p.inventoryMenu,3,3);for(int i=0;i<9;i++)grid.setItem(i,new ItemStack(Items.CHARCOAL));
        var compressed=server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l).orElseThrow().assemble(grid,server.registryAccess());check(compressed.is(KilnContent.CHARCOAL_ITEM.get())&&compressed.getCount()==1,"nine charcoal compress to one block");
        grid.clearContent();grid.setItem(0,compressed);var unpacked=server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l).orElseThrow().assemble(grid,server.registryAccess());check(unpacked.is(Items.CHARCOAL)&&unpacked.getCount()==9,"charcoal block reverses without resource gain");
        check(net.minecraftforge.common.ForgeHooks.getBurnTime(compressed,RecipeType.SMELTING)==14400,"charcoal block fuel equals nine charcoal");
        var at=new BlockPos(0,80,40);var k=KilnDevelopment.build(l,at,Direction.NORTH);p.setPos(.5,80,38);k.form(p);
        check(k.inventory.getSlots()==3,"three inventory slots");
        check(!k.inventory.isItemValid(2,new ItemStack(Items.COAL))&&!k.inventory.isItemValid(2,new ItemStack(Items.CHARCOAL))&&k.inventory.isItemValid(2,compressed),"only fuel blocks fit lower slot");
        check(!KilnBlockEntity.accepts(compressed),"charcoal block is fuel not coke feedstock");
        k.inventory.setStackInSlot(0,new ItemStack(Items.COAL));k.tick();check(k.data.get(2)==KilnBlockEntity.NO_FUEL&&k.inventory.getStackInSlot(0).getCount()==1&&k.data.get(0)==0,"coal cannot self-ignite");
        k.inventory.setStackInSlot(2,new ItemStack(KilnContent.CHARCOAL_ITEM.get(),2));k.tick();check(k.data.get(0)==1&&k.inventory.getStackInSlot(2).getCount()==1&&k.data.get(6)==14399,"one charcoal fuel block starts and heats cycle");
        check(k.tank.isEmpty(),"creosote is not awarded early");for(int i=1;i<1200;i++)k.tick();
        check(k.inventory.getStackInSlot(1).getCount()==1&&k.tank.getFluidAmount()==250,"one coal yields one coke and 250 mB");check(k.data.get(6)==13200,"heat consumed exactly per processing tick");
        k.inventory.setStackInSlot(0,new ItemStack(Items.COAL_BLOCK));for(int i=0;i<10800;i++)k.tick();
        check(k.inventory.getStackInSlot(1).getCount()==10&&k.tank.getFluidAmount()==2500,"coal block yields nine coke and 2250 mB");check(k.data.get(6)==2400&&k.inventory.getStackInSlot(2).getCount()==1,"remaining heat used before next fuel block");
        k.tank.setFluid(new FluidStack(KilnFluids.SOURCE.get(),16000));k.inventory.setStackInSlot(0,new ItemStack(Items.COAL,3));k.tick();check(k.inventory.getStackInSlot(0).getCount()==3&&k.data.get(6)==2400,"full tank consumes neither raw material nor heat");
        k.tank.setFluid(new FluidStack(KilnFluids.SOURCE.get(),15800));k.tick();check(k.inventory.getStackInSlot(0).getCount()==3,"250 mB free space required");
        k.inventory.setStackInSlot(0,new ItemStack(Items.COAL_BLOCK));k.tank.setFluid(new FluidStack(KilnFluids.SOURCE.get(),13800));k.tick();check(k.inventory.getStackInSlot(0).is(Items.COAL_BLOCK),"entire block creosote volume reserved before start");
        k.tank.setFluid(new FluidStack(KilnFluids.SOURCE.get(),13750));k.tick();check(k.data.get(0)==1,"exact 2250 mB free space permits block cycle");
        k.tank.setFluid(new FluidStack(KilnFluids.SOURCE.get(),16000));int heat=k.data.get(6);k.tick();check(k.data.get(0)==1&&k.data.get(6)==heat,"blocked active cycle preserves progress and heat");
        var fluid=l.getBlockEntity(at.below()).getCapability(ForgeCapabilities.FLUID_HANDLER).orElseThrow(()->new IllegalStateException("No fluid port"));
        check(fluid.getTankCapacity(0)==16000,"output drain exposes sixteen-bucket tank");
        check(fluid.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER,1000),FluidAction.EXECUTE)==0,"drain rejects incoming foreign fluid");
        check(fluid.fill(new FluidStack(KilnFluids.SOURCE.get(),1000),FluidAction.EXECUTE)==0,"drain rejects filling even with creosote");
        check(fluid.drain(1000,FluidAction.SIMULATE).getAmount()==1000&&k.tank.getFluidAmount()==16000,"simulated drain is side-effect free");
        check(fluid.drain(2250,FluidAction.EXECUTE).getAmount()==2250&&k.tank.getFluidAmount()==13750,"pipe drains exact requested volume");k.tick();check(k.data.get(0)==2,"draining tank automatically resumes progress");
        var out=l.getBlockEntity(at.below()).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("No output item port"));check(out.getSlots()==2&&out.isItemValid(1,compressed)&&out.extractItem(1,1,false).isEmpty(),"lower hatch has separate fuel-input port");
        var menu=new KilnMenu(10,p.getInventory(),k);menu.setCarried(new ItemStack(Items.BUCKET,2));int before=k.tank.getFluidAmount();check(menu.clickMenuButton(p,1),"GUI tank fills bucket");
        check(k.tank.getFluidAmount()==before-1000&&menu.getCarried().is(Items.BUCKET)&&menu.getCarried().getCount()==1&&p.getInventory().items.stream().anyMatch(s->s.is(KilnFluids.BUCKET.get())),"stacked GUI buckets conserve empty buckets and fluid");
        check(menu.clickMenuButton(p,1)&&menu.getCarried().is(KilnFluids.BUCKET.get()),"single cursor bucket becomes filled bucket");
        check(FluidUtil.getFluidContained(menu.getCarried()).orElse(FluidStack.EMPTY).getAmount()==1000,"filled bucket supports Forge fluid capability");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BUCKET));var drainAt=at.below();l.getBlockState(drainAt).use(l,p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(drainAt),Direction.NORTH,drainAt,false));check(p.getMainHandItem().is(KilnFluids.BUCKET.get()),"right-click drain with empty bucket works");
        k.tank.setFluid(new FluidStack(KilnFluids.SOURCE.get(),250));menu.setCarried(new ItemStack(Items.BUCKET));check(!menu.clickMenuButton(p,1)&&k.tank.getFluidAmount()==250,"partial bucket cannot create fluid");
        var legacy=k.saveWithoutMetadata();legacy.remove("Heat");legacy.remove("HeatCapacity");legacy.remove("Creosote");legacy.getCompound("Inventory").putInt("Size",2);var list=legacy.getCompound("Inventory").getList("Items",10);for(int i=list.size()-1;i>=0;i--)if(list.getCompound(i).getByte("Slot")==2)list.remove(i);
        var migrated=new KilnBlockEntity(at,k.getBlockState());migrated.load(legacy);check(migrated.inventory.getSlots()==3&&migrated.inventory.getStackInSlot(2).isEmpty()&&migrated.tank.isEmpty()&&migrated.data.get(6)==0,"older two-slot saves migrate without free fuel or fluid");
        k.tank.setFluid(new FluidStack(KilnFluids.SOURCE.get(),625));int savedHeat=k.data.get(6);for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4)))e.discard();l.destroyBlock(at,true);
        var controller=l.getEntitiesOfClass(ItemEntity.class,new AABB(at).inflate(4)).stream().filter(e->e.getItem().is(ResourceContent.CONTROLLER_ITEM.get())).toList();check(controller.size()==1,"mining creates one controller carrier");
        var carrier=controller.get(0).getItem().copy();controller.get(0).discard();check(carrier.getTag().getCompound("BlockEntityTag").getCompound("Creosote").getInt("Amount")==625,"partial creosote survives mining in controller item");
        p.setItemInHand(InteractionHand.MAIN_HAND,carrier);p.setYRot(Direction.SOUTH.toYRot());var ctx=new BlockPlaceContext(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(at.below()).add(0,.5,0),Direction.UP,at.below(),false)));
        check(((BlockItem)carrier.getItem()).place(ctx).consumesAction(),"stored controller can be placed again");var restored=(KilnBlockEntity)l.getBlockEntity(at);
        check(restored.tank.getFluidAmount()==625&&restored.data.get(6)==savedHeat&&!restored.formed,"replaced controller restores tank and heat but requires formation");check(restored.inventory.getStackInSlot(0).isEmpty()&&restored.inventory.getStackInSlot(1).isEmpty(),"dropped inventory is not copied into carrier");
        var fluidAt=new BlockPos(6,80,40);check(KilnFluids.BUCKET.get().emptyContents(p,l,fluidAt,null),"creosote bucket can place world fluid");check(l.getFluidState(fluidAt).getType()==KilnFluids.SOURCE.get(),"placed creosote source registered");var picked=((BucketPickup)KilnFluids.BLOCK.get()).pickupBlock(l,fluidAt,l.getBlockState(fluidAt));check(picked.is(KilnFluids.BUCKET.get()),"world fluid can be picked back up");
        Files.write(Path.of("creosote-smoke-report.txt"),report);SuitcaseGlider.LOG.info("CREOSOTE_SMOKE_OK {} checks",report.size());stop=server;countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(stop!=null&&e.phase==TickEvent.Phase.END&&--countdown<=0){var server=stop;stop=null;server.halt(false);}}
}
