package dev.salum.suitcaseglider.blast;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.kiln.KilnContent;
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
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in actual server checks: assembly transaction, orientation, restart NBT, drops and recovery. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class BlastDevelopment {
    private static MinecraftServer stop;private static int countdown;
    private static final List<String> report=new ArrayList<>();
    private static void check(boolean b,String label){if(!b)throw new IllegalStateException("BLAST_SMOKE_FAILED "+label);report.add(label+"=true");}
    public static BlastBlockEntity build(ServerLevel l,BlockPos root,Direction front){
        var old=l.getBlockEntity(root);if(old instanceof BlastBlockEntity b)b.restore(null);
        for(int i=0;i<BlastBlueprint.CELLS;i++)l.setBlock(BlastBlueprint.at(root,front,i),BlastBlueprint.expected(i,front),3);
        return (BlastBlockEntity)l.getBlockEntity(root);
    }
    private static int dropped(ServerLevel l,BlockPos root){return l.getEntitiesOfClass(ItemEntity.class,new AABB(root).inflate(8)).stream().mapToInt(e->e.getItem().getCount()).sum();}
    private static void clearDrops(ServerLevel l,BlockPos root){for(var e:l.getEntitiesOfClass(ItemEntity.class,new AABB(root).inflate(8)))e.discard();}
    @SubscribeEvent public static void started(ServerStartedEvent e)throws Exception{
        if(!Boolean.getBoolean("cogwild.blastSmoke"))return;var server=e.getServer();var l=server.overworld();var p=FakePlayerFactory.getMinecraft(l);p.setGameMode(GameType.SURVIVAL);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(KilnContent.KEY.get()));
        for(var f:Direction.Plane.HORIZONTAL){
            var root=new BlockPos(f.get2DDataValue()*16,80,96);var b=build(l,root,f);
            var count=new HashMap<Block,Integer>();for(int i=0;i<45;i++){var want=BlastBlueprint.expected(i,f);if(!want.isAir())count.merge(want.getBlock(),1,Integer::sum);check(BlastBlueprint.root(BlastBlueprint.at(root,f,i),f,i).equals(root),"coordinate roundtrip "+f+" "+i);}
            check(count.values().stream().mapToInt(Integer::intValue).sum()==34,"34 placed blocks "+f);check(count.get(BlastContent.SHAFT.get())==17&&count.get(BlastContent.HEARTH.get())==7&&count.get(BlastContent.DUCT.get())==4,"blueprint module totals "+f);
            var obstruction=BlastBlueprint.at(root,f,36);l.setBlockAndUpdate(obstruction,Blocks.STONE.defaultBlockState());check(!b.form(p)&&!b.formed(),"headroom obstruction rejects transaction "+f);l.setBlockAndUpdate(obstruction,Blocks.AIR.defaultBlockState());
            var core=BlastBlueprint.at(root,f,13);l.setBlockAndUpdate(core,Blocks.DIRT.defaultBlockState());check(!b.form(p)&&l.getBlockState(core).is(Blocks.DIRT),"inner clearance preserved on failure "+f);l.setBlockAndUpdate(core,Blocks.AIR.defaultBlockState());
            var hopperAt=BlastBlueprint.at(root,f,31);var hopper=(HopperBlockEntity)l.getBlockEntity(hopperAt);hopper.setItem(0,new ItemStack(Items.DIAMOND,3));check(!b.form(p)&&hopper.getItem(0).getCount()==3,"nonempty hopper rejected without loss "+f);hopper.clearContent();
            var wrong=BlastBlueprint.at(root,f,8);l.setBlockAndUpdate(wrong,BlastContent.HEARTH.get().defaultBlockState().setValue(BlastModuleBlock.FACING,f.getOpposite()));check(!b.form(p),"wrong module orientation rejected "+f);l.setBlockAndUpdate(wrong,BlastBlueprint.expected(8,f));
            check(b.form(p)&&b.formed(),"key forms complete construction "+f);
            for(int i=1;i<45;i++)check(l.getBlockState(BlastBlueprint.at(root,f,i)).is(BlastContent.PART.get()),"formed proxy "+f+" "+i);
            check(l.getBlockState(obstruction).getCollisionShape(l,obstruction).bounds().maxY==.5,"top collision ends at 4.5 blocks "+f);
            var reloaded=new BlastBlockEntity(root,b.getBlockState());reloaded.load(b.saveWithoutMetadata());l.setBlockEntity(reloaded);b=reloaded;check(b.formed()&&b.original(31).is(Blocks.HOPPER),"saved construction survives NBT reload "+f);
            p.setShiftKeyDown(true);var keyHit=new BlockHitResult(Vec3.atCenterOf(root),f,root,false);var keyResult=p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,keyHit);p.setShiftKeyDown(false);check(keyResult.consumesAction()&&!b.formed(),"real sneak-key interaction dismantles "+f);
            for(int i=0;i<45;i++)check(l.getBlockState(BlastBlueprint.at(root,f,i)).equals(BlastBlueprint.expected(i,f)),"explicit dismantle restores cell "+f+" "+i);
            check(b.form(p),"restored structure reforms "+f);clearDrops(l,root);var broken=BlastBlueprint.at(root,f,17);l.destroyBlock(broken,true);
            check(!b.formed()&&l.getBlockState(broken).isAir()&&dropped(l,root)==1,"mined proxy returns one original module "+f);for(int i=0;i<45;i++)if(i!=17)check(l.getBlockState(BlastBlueprint.at(root,f,i)).equals(BlastBlueprint.expected(i,f)),"mining restores remaining cell "+f+" "+i);
            l.setBlockAndUpdate(broken,BlastBlueprint.expected(17,f));check(b.form(p),"repaired structure reforms "+f);clearDrops(l,root);l.destroyBlock(root,true);check(dropped(l,root)==1&&l.getBlockState(root).isAir(),"controller mining returns one controller "+f);check(l.getBlockState(hopperAt).is(Blocks.HOPPER),"controller removal restores vanilla hopper "+f);
            b=build(l,root,f);b.form(p);clearDrops(l,root);l.destroyBlock(core,true);check(!b.formed()&&dropped(l,root)==0,"breaking originally empty cell creates no item "+f);
            b=build(l,root,f);b.form(p);clearDrops(l,root);l.destroyBlock(BlastBlueprint.at(root,f,17),false);check(!b.formed()&&dropped(l,root)==0,"creative removal does not duplicate modules "+f);
            b=build(l,root,f);b.form(p);var orphanAt=BlastBlueprint.at(root,f,17);var orphanState=l.getBlockState(orphanAt);b.restore(null);l.setBlockAndUpdate(orphanAt,orphanState);((BlastPartBlock)BlastContent.PART.get()).tick(orphanState,l,orphanAt,l.random);check(l.getBlockState(orphanAt).equals(BlastBlueprint.expected(17,f)),"orphan proxy self-recovers "+f);
        }
        var grid=new TransientCraftingContainer(p.inventoryMenu,3,3);grid.setItem(0,new ItemStack(Items.COPPER_INGOT));grid.setItem(1,new ItemStack(Items.COPPER_INGOT));grid.setItem(2,new ItemStack(Items.COPPER_INGOT));var r=server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,l).orElseThrow().assemble(grid,server.registryAccess());check(r.is(BlastContent.WIRE.get())&&r.getCount()==6,"early copper wire recipe works");
        for(var id:BlastContent.BLOCKS.keySet())check(server.getRecipeManager().byKey(SuitcaseGlider.id(id)).isPresent(),"construction recipe registered "+id);
        Files.write(Path.of("blast-smoke-report.txt"),report);SuitcaseGlider.LOG.info("BLAST_SMOKE_OK {} checks",report.size());stop=server;countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(stop!=null&&e.phase==TickEvent.Phase.END&&--countdown<=0){var s=stop;stop=null;s.halt(false);}}
}
