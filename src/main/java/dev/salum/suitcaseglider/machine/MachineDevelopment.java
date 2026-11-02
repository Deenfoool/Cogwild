package dev.salum.suitcaseglider.machine;

import com.mojang.authlib.GameProfile;
import dev.salum.suitcaseglider.SuitcaseGlider;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated dedicated-server checks, disabled in ordinary installations. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class MachineDevelopment {
    private static MinecraftServer stop;private static int countdown;
    private static final List<String> report=new ArrayList<>();
    private static void check(boolean pass,String label){report.add(label+"="+pass);if(!pass)throw new IllegalStateException("MACHINE_SMOKE_FAILED "+label);}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("machines.smoke"))return;
        var level=event.getServer().overworld();var player=FakePlayerFactory.get(level,new GameProfile(UUID.nameUUIDFromBytes("machine-checks".getBytes()),"MachineChecks"));
        for(int x=-5;x<=40;x++)for(int z=-5;z<=65;z++){level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());for(int y=80;y<=84;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        int index=0;
        for(var block:MachineContent.MACHINES.values().stream().map(net.minecraftforge.registries.RegistryObject::get).toList())for(Direction facing:Direction.Plane.HORIZONTAL){
            player.setGameMode(GameType.SURVIVAL);player.setYRot(facing.getOpposite().toYRot());BlockPos pos=new BlockPos((index%4)*4,80,(index/4)*7);index++;player.setPos(pos.getX()+.5,80,pos.getZ()+.5);
            var stack=new ItemStack(block,2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false);
            var context=new BlockPlaceContext(new UseOnContext(player,InteractionHand.MAIN_HAND,hit));
            if(block.spec().depth()>1){
                BlockPos back=pos.relative(facing.getOpposite());level.setBlockAndUpdate(back,Blocks.STONE.defaultBlockState());
                check(block.getStateForPlacement(context)==null,"occupied back row rejects placement "+facing);
                level.setBlockAndUpdate(back,Blocks.AIR.defaultBlockState());
            }
            check(((BlockItem)stack.getItem()).place(context).consumesAction(),block.kind+" placement "+facing);
            var state=level.getBlockState(pos);var second=MachineBlock.other(pos,state);
            check(state.is(block)&&state.getValue(MachineBlock.FACING)==facing&&!state.getValue(MachineBlock.SECONDARY),"primary orientation "+index);
            check(level.getBlockState(second).is(block)&&level.getBlockState(second).getValue(MachineBlock.SECONDARY),"second occupied cell "+index);
            check(block.cells(pos,state).stream().filter(p->level.getBlockEntity(p)!=null).count()==1&&level.getBlockEntity(pos) instanceof MachineBlockEntity,"single renderer entity "+index);
            check(block.cells(pos,state).size()==block.spec().width()*block.spec().depth()*block.spec().layers(),"all occupied cells "+index);
            for(var p:block.cells(pos,state))check(MachineBlock.base(p,level.getBlockState(p)).equals(pos),"part resolves origin "+index+" "+p);
            check(stack.getCount()==1,"one placement consumes one item "+index);
            BlockPos power=pos.below();level.setBlockAndUpdate(power,Blocks.REDSTONE_BLOCK.defaultBlockState());
            check(level.getBlockState(pos).getValue(MachineBlock.POWERED)&&level.getBlockState(second).getValue(MachineBlock.POWERED),"powered pair "+index);
            level.setBlockAndUpdate(power,Blocks.STONE.defaultBlockState());check(!level.getBlockState(pos).getValue(MachineBlock.POWERED),"power off "+index);
            for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)))drop.discard();
            level.destroyBlock(second,true);check(block.cells(pos,state).stream().allMatch(level::isEmptyBlock),"breaking clears entire footprint "+index);
            int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum();check(count==1,"single survival drop "+index);
            for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)))drop.discard();
            player.setGameMode(GameType.CREATIVE);player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(block));((BlockItem)block.asItem()).place(context);
            state=level.getBlockState(pos);block.playerWillDestroy(level,pos,state,player);level.destroyBlock(pos,false);
            check(block.cells(pos,state).stream().allMatch(level::isEmptyBlock)&&level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(5)).isEmpty(),"creative removal without drops "+index);
        }
        for(String kind:MachineCatalog.ALL.keySet())if(!java.util.Set.of("lathe","crusher").contains(kind))check(event.getServer().getRecipeManager().byKey(SuitcaseGlider.id(kind)).isPresent(),kind+" recipe loaded");
        Path file=Path.of("machine-smoke-report.txt");Files.write(file,report);SuitcaseGlider.LOG.info("MACHINE_SMOKE_OK {} checks",report.size());stop=event.getServer();countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event){if(stop!=null&&event.phase==TickEvent.Phase.END&&--countdown<=0){var server=stop;stop=null;server.halt(false);}}
}
