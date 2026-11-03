package dev.salum.suitcaseglider.blast;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.resource.ResourceContent;
import dev.salum.suitcaseglider.cable.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Real Forge server tests, including native hopper transfer and the actual cable network. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class BlastRuntimeDevelopment {
    private static MinecraftServer stop;private static int countdown;private static final List<String> report=new ArrayList<>();
    private static void check(boolean b,String label){if(!b)throw new IllegalStateException("BLAST_RUNTIME_FAILED "+label);report.add(label+"=true");}
    private static void charge(BlastBlockEntity b,int n){b.inventory.setStackInSlot(0,new ItemStack(Items.RAW_IRON,n));b.inventory.setStackInSlot(1,new ItemStack(ResourceContent.COKE.get(),n));b.inventory.setStackInSlot(2,new ItemStack(BlastContent.FLUX.get(),n));}
    private static void poweredTick(BlastBlockEntity b){b.receiveEnergy(32,false);b.tick();}
    private static void finish(BlastBlockEntity b){for(int i=0;i<4000&&b.hasJob();i++)poweredTick(b);check(!b.hasJob(),"job completes within bounded ticks");}
    private static int count(ServerLevel l,BlockPos p,Item item){return l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(6)).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
    private static void clear(ServerLevel l,BlockPos p){for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(6)))e.discard();}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("cogwild.blastRuntime")&&!Boolean.getBoolean("cogwild.blastPersistence"))return;
        var server=event.getServer();var l=server.overworld();var p=FakePlayerFactory.getMinecraft(l);p.setGameMode(GameType.SURVIVAL);
        if(Boolean.getBoolean("cogwild.blastPersistence")){
            var at=new BlockPos(80,80,160);l.getChunkAt(at);var b=(BlastBlockEntity)l.getBlockEntity(at);
            check(b!=null&&b.formed()&&b.hasJob(),"real restart retains active charge");check(b.data.get(0)==137&&b.temperature()>=1199&&b.temperature()<=1200&&b.inventory.getStackInSlot(0).getCount()==4,"real restart preserves temperature progress and remaining input");
            finish(b);check(b.inventory.getStackInSlot(3).getCount()==1&&b.inventory.getStackInSlot(4).getCount()==1,"real restart completes one pair of outputs");
            Files.write(Path.of("blast-persistence-report.txt"),report);SuitcaseGlider.LOG.info("BLAST_PERSISTENCE_OK {} checks",report.size());stop=server;countdown=5;return;
        }
        check(l.getRecipeManager().getAllRecipesFor(BlastContent.RECIPE_TYPE.get()).size()==1,"blast recipe data loaded");
        for(var iron:new Item[]{Items.IRON_ORE,Items.DEEPSLATE_IRON_ORE,Items.RAW_IRON}){
            var test=new SimpleContainer(new ItemStack(iron),new ItemStack(ResourceContent.COKE.get()),new ItemStack(BlastContent.FLUX.get()));check(l.getRecipeManager().getRecipeFor(BlastContent.RECIPE_TYPE.get(),test,l).isPresent(),"iron-bearing ingredient accepted "+iron);
        }
        for(var f:Direction.Plane.HORIZONTAL){
            var at=new BlockPos(f.get2DDataValue()*16,80,160);var b=BlastDevelopment.build(l,at,f);check(b.form(p),"construction forms "+f);charge(b,2);b.tick();check(b.state()==BlastBlockEntity.NO_POWER&&!b.hasJob()&&b.inventory.getStackInSlot(0).getCount()==2,"unpowered charge is not consumed "+f);
            check(b.receiveEnergy(-10,false)==0&&b.receiveEnergy(32,true)==32&&b.energy()==0,"energy simulation and negative transfer safe "+f);
            var feedAt=BlastBlueprint.at(at,f,40);var feed=l.getBlockEntity(feedAt).getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException("Missing feed port"));
            check(feed.getSlots()==3&&feed.extractItem(0,1,false).isEmpty()&&!feed.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"feed direction and filter enforced "+f);
            var outputAt=BlastBlueprint.at(at,f,1);var out=l.getBlockEntity(outputAt).getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.DOWN).orElseThrow(()->new IllegalStateException("Missing output port"));
            check(out.getSlots()==2&&!out.insertItem(0,new ItemStack(BlastContent.PIG_IRON.get()),false).isEmpty(),"output rejects incoming items "+f);
            var blowerAt=BlastBlueprint.at(at,f,5);check(l.getBlockEntity(blowerAt).getCapability(ForgeCapabilities.ENERGY,f.getClockWise()).isPresent()&&!l.getBlockEntity(blowerAt).getCapability(ForgeCapabilities.ENERGY,f.getOpposite()).isPresent(),"blower exposes power on socket side "+f);
            poweredTick(b);check(b.hasJob()&&b.temperature()==22&&b.data.get(0)==0&&b.inventory.getStackInSlot(0).getCount()==1,"cold charge heats before smelting and reserves exactly once "+f);
            for(int i=0;i<589;i++)poweredTick(b);check(b.temperature()==1200&&b.data.get(0)==0,"cold warmup takes 590 powered ticks "+f);
            for(int i=0;i<51;i++)poweredTick(b);int progress=b.data.get(0),energy=b.energy();b.tick();check(b.state()==BlastBlockEntity.NO_POWER&&b.data.get(0)==progress&&b.energy()==energy,"power loss freezes progress "+f);
            b.inventory.setStackInSlot(3,new ItemStack(BlastContent.PIG_IRON.get(),64));b.receiveEnergy(1000,false);energy=b.energy();b.tick();check(b.state()==BlastBlockEntity.BLOCKED&&b.data.get(0)==progress&&b.energy()==energy,"blocked iron output spends no power or charge "+f);
            b.inventory.setStackInSlot(3,ItemStack.EMPTY);b.inventory.setStackInSlot(4,new ItemStack(BlastContent.SLAG.get(),64));b.tick();check(b.state()==BlastBlockEntity.BLOCKED&&b.data.get(0)==progress,"slag capacity also gates processing "+f);b.inventory.setStackInSlot(4,ItemStack.EMPTY);
            int savedTemperature=b.temperature();var copy=new BlastBlockEntity(at,b.getBlockState());copy.load(b.saveWithoutMetadata());l.setBlockEntity(copy);b=copy;check(b.hasJob()&&b.data.get(0)==progress&&b.temperature()==savedTemperature,"NBT keeps active charge and heat "+f);
            b.restore(null);check(!b.formed()&&b.hasJob()&&b.receiveEnergy(32,false)==0,"dismantle retains pending charge and disables energy "+f);check(b.form(p),"charged controller reforms "+f);finish(b);
            check(b.inventory.getStackInSlot(3).getCount()==1&&b.inventory.getStackInSlot(4).getCount()==1&&b.inventory.getStackInSlot(0).getCount()==1,"one charge creates one iron and slag "+f);
            check(out.extractItem(0,64,false).getCount()==1&&out.extractItem(1,64,false).getCount()==1,"old port capability resolves the current owner after re-form "+f);
            var menu=new BlastMenu(20,p.getInventory(),b);p.getInventory().clearContent();p.getInventory().setItem(9,new ItemStack(Items.RAW_IRON,5));menu.quickMoveStack(p,5);check(b.inventory.getStackInSlot(0).getCount()==6&&p.getInventory().getItem(9).isEmpty(),"shift transfer conserves input "+f);
            check(!menu.slots.get(3).mayPlace(new ItemStack(Items.RAW_IRON)),"menu result slot rejects items "+f);
            // A real vanilla hopper above the top port must push into the Forge item handler.
            var hopperAt=feedAt.above();l.setBlockAndUpdate(hopperAt,Blocks.HOPPER.defaultBlockState());var hopper=(HopperBlockEntity)l.getBlockEntity(hopperAt);hopper.setItem(0,new ItemStack(BlastContent.FLUX.get(),3));int before=b.inventory.getStackInSlot(2).getCount();
            for(int i=0;i<10;i++)HopperBlockEntity.pushItemsTick(l,hopperAt,l.getBlockState(hopperAt),hopper);
            check(b.inventory.getStackInSlot(2).getCount()>before&&b.inventory.getStackInSlot(2).getCount()+hopper.getItem(0).getCount()==before+3,"vanilla hopper pushes real items into bunker "+f);l.setBlockAndUpdate(hopperAt,Blocks.AIR.defaultBlockState());
            // Existing Cogwild cable must deliver actual FE to the formed blower endpoint.
            var generator=at.relative(f.getClockWise(),8);l.setBlockAndUpdate(generator,CableContent.GENERATOR.get().defaultBlockState().setValue(EnergyNodeBlock.FACING,f.getCounterClockWise()));var source=(EnergyNodeEntity)l.getBlockEntity(generator);source.stored=1000;
            for(int x=-5;x<=10;x++)for(int z=-2;z<=5;z++)l.setBlockAndUpdate(at.relative(f.getClockWise(),x).relative(f.getOpposite(),z).below(),Blocks.SMOOTH_STONE.defaultBlockState());
            check(CableWorld.endpoint(l,blowerAt).equals(at),"blower click resolves cable endpoint "+f);var cableWorld=CableWorld.get(l);var air=cableWorld.route(l,generator,at,false);check(!air.isEmpty(),"air cable route reaches blower socket "+f);check(!cableWorld.route(l,generator,at,true).isEmpty(),"floor cable route reaches blower socket "+f);
            var opposite=at.relative(f.getClockWise(),-4).relative(f.getOpposite());l.setBlockAndUpdate(opposite,CableContent.GENERATOR.get().defaultBlockState().setValue(EnergyNodeBlock.FACING,f.getClockWise()));
            check(cableWorld.route(l,opposite,at,false).isEmpty(),"air cable cannot cut through furnace body "+f);check(!cableWorld.route(l,opposite,at,true).isEmpty(),"floor cable automatically goes around furnace base "+f);
            p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(CableContent.CABLE.get(),32));
            p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(generator),f.getCounterClockWise(),generator,false));
            var clicked=p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(blowerAt),f.getClockWise(),blowerAt,false));
            check(clicked.consumesAction()&&cableWorld.links.stream().anyMatch(link->link.a().equals(generator)&&link.b().equals(at)),"normal cable clicks connect without opening furnace menu "+f);
            energy=b.energy();cableWorld.tick(l);check(b.energy()>energy&&source.stored<1000,"cable network transfers conserved energy "+f);
            b.restore(null);cableWorld.validate(l);check(cableWorld.links.stream().noneMatch(link->link.a().equals(at)||link.b().equals(at)),"dismantle removes cable endpoint cleanly "+f);check(b.form(p),"post-cable fixture reforms "+f);
            // Start a fresh pending job, then break the controller: originals and all charge return once.
            for(int i=0;i<5;i++)b.inventory.setStackInSlot(i,ItemStack.EMPTY);charge(b,5);poweredTick(b);clear(l,at);l.destroyBlock(at,true);
            check(count(l,at,Items.RAW_IRON)==5&&count(l,at,ResourceContent.COKE.get())==5&&count(l,at,BlastContent.FLUX.get())==5,"controller mining refunds stored and pending charge once "+f);
            check(count(l,at,BlastContent.PIG_IRON.get())==0&&count(l,at,BlastContent.SLAG.get())==0,"mining pending job grants no premature output "+f);b.dropContents();check(count(l,at,Items.RAW_IRON)==5,"repeated cleanup cannot duplicate charge "+f);
        }
        var fixture=BlastDevelopment.build(l,new BlockPos(80,80,160),Direction.NORTH);fixture.form(p);charge(fixture,5);while(fixture.data.get(0)<137)poweredTick(fixture);check(fixture.temperature()==1200&&fixture.hasJob(),"real-restart fixture prepared");
        Files.write(Path.of("blast-runtime-report.txt"),report);SuitcaseGlider.LOG.info("BLAST_RUNTIME_OK {} checks",report.size());stop=server;countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(stop!=null&&e.phase==TickEvent.Phase.END&&--countdown<=0){var s=stop;stop=null;s.halt(false);}}
}
