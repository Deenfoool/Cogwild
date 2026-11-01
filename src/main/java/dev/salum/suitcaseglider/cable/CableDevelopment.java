package dev.salum.suitcaseglider.cable;

import com.mojang.authlib.GameProfile;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Real server integration checks, opt-in only; never touches an ordinary user's world. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class CableDevelopment {
    private static MinecraftServer stop;private static int countdown;private static final List<String> report=new ArrayList<>();
    private static void check(boolean condition,String label){report.add(label+"="+condition);if(!condition)throw new IllegalStateException("CABLE_SMOKE_FAILED "+label);}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("cogwild.cableSmoke"))return;
        var level=event.getServer().overworld();var player=FakePlayerFactory.get(level,new GameProfile(UUID.nameUUIDFromBytes("cable-checks".getBytes()),"CableChecks"));player.setGameMode(GameType.SURVIVAL);
        for(int x=-8;x<=20;x++)for(int z=-8;z<=20;z++){level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());for(int y=80;y<85;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
        var world=CableWorld.get(level);world.links.clear();world.setDirty();
        BlockPos gen=new BlockPos(0,80,4),relay=new BlockPos(3,80,3),machine=new BlockPos(6,80,0);
        level.setBlockAndUpdate(gen,CableContent.GENERATOR.get().defaultBlockState().setValue(EnergyNodeBlock.FACING,Direction.NORTH));
        level.setBlockAndUpdate(relay,CableContent.RELAY.get().defaultBlockState());
        var state=MachineContent.LATHE.get().defaultBlockState();level.setBlockAndUpdate(machine,state);MachineContent.LATHE.get().setPlacedBy(level,machine,state,player,new ItemStack(MachineContent.LATHE_ITEM.get()));
        check(CableWorld.endpoint(level,MachineBlock.other(machine,state)).equals(machine),"secondary machine cell resolves to PWR");
        var generator=(EnergyNodeEntity)level.getBlockEntity(gen);var sink=(MachineBlockEntity)level.getBlockEntity(machine);
        check(generator.getCapability(ForgeCapabilities.ENERGY).isPresent()&&sink.getCapability(ForgeCapabilities.ENERGY).isPresent(),"Forge Energy capabilities");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COAL,2));
        CableContent.GENERATOR.get().use(level.getBlockState(gen),level,gen,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(gen),Direction.NORTH,gen,false));
        check(generator.fuelTicks==1600&&player.getMainHandItem().getCount()==1,"generator accepts and consumes one coal");
        generator.tickEnergy();check(generator.stored==40&&generator.fuelTicks==1599,"fuel generates real energy");
        var stack=new ItemStack(CableContent.CABLE.get(),64);stack.getOrCreateTag().putBoolean("Floor",true);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
        for(BlockPos p:List.of(gen,machine))check(CableContent.CABLE.get().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(p),Direction.UP,p,false))).consumesAction(),"two click endpoint "+p);
        check(world.links.size()==1&&world.links.get(0).floor(),"floor cable created through item interaction");
        var floor=world.links.get(0);check(stack.getCount()==64-floor.cost(),"survival cable cost equals rounded route length");
        for(int i=0;i<20;i++){generator.tickEnergy();world.tick(level);sink.tickEnergy();}
        check(sink.powered()&&sink.cableEnergy()>0,"cable powers existing machine without redstone");
        check(generator.stored+sink.cableEnergy()==840-160,"energy transfer conserves production minus actual machine consumption");
        var loaded=CableWorld.load(world.save(new net.minecraft.nbt.CompoundTag()));check(loaded.links.equals(world.links),"all connections and path points survive save/load");
        check(!world.available(gen,machine)&&!world.available(gen,gen),"duplicate and self links refused");
        var air=world.route(level,gen,relay,false);check(!air.isEmpty(),"air connection routed to relay");
        world.add(new CableWorld.Link(UUID.randomUUID(),gen,relay,false,(int)Math.ceil(CableRoutes.length(air)),air));generator.receiveEnergy(4000,false);sink.receiveCableEnergy(4000,false);world.tick(level);check(((EnergyNodeEntity)level.getBlockEntity(relay)).stored>0,"relay transfers/buffers energy");
        BlockPos obstruction=BlockPos.containing(floor.points().get(floor.points().size()/2));if(obstruction.equals(gen)||obstruction.equals(machine))obstruction=new BlockPos(3,80,1);
        level.setBlockAndUpdate(obstruction,Blocks.STONE.defaultBlockState());check(!world.route(level,gen,machine,true).isEmpty(),"new floor route detours around obstacle");level.setBlockAndUpdate(obstruction,Blocks.AIR.defaultBlockState());
        for(Direction facing:Direction.Plane.HORIZONTAL){
            BlockPos p=new BlockPos(12,80,12);level.setBlockAndUpdate(p,state.setValue(MachineBlock.FACING,facing));Vec3 port=CableWorld.anchor(level,p);var normal=facing.getOpposite();check(port.subtract(Vec3.atCenterOf(p)).dot(new Vec3(normal.getStepX(),0,normal.getStepZ()))>.5,"PWR anchor rotates outside rear face "+facing);
        }
        // Remove a supporting floor cell on a real horizontal cable span.
        Vec3 middle=null;for(int i=2;i<floor.points().size()-1;i++){Vec3 a=floor.points().get(i-1),b=floor.points().get(i);if(a.y==b.y&&a.distanceTo(b)>.8){middle=a.lerp(b,.5);break;}}
        check(middle!=null,"floor span exists");BlockPos support=BlockPos.containing(middle.add(0,-.1,0));level.setBlockAndUpdate(support,Blocks.AIR.defaultBlockState());
        world.validate(level);check(world.links.stream().noneMatch(e->e.id().equals(floor.id())),"missing floor support removes cable");
        check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(gen).inflate(3)).stream().filter(e->e.getItem().is(CableContent.CABLE.get())).mapToInt(e->e.getItem().getCount()).sum()==floor.cost(),"broken cable refunds its exact material cost");
        player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));
        var cut=new net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock(player,InteractionHand.MAIN_HAND,relay,new BlockHitResult(CableWorld.anchor(level,relay),Direction.UP,relay,false));CableWorld.cut(cut);
        check(cut.isCanceled()&&world.links.isEmpty()&&player.getMainHandItem().getDamageValue()==1,"shears remove one connection and lose one durability");
        check(level.getServer().getRecipeManager().byKey(SuitcaseGlider.id("power_cable")).isPresent(),"cable recipe loaded");
        for(String key:List.of("cable_relay","fuel_generator"))check(level.getServer().getRecipeManager().byKey(SuitcaseGlider.id(key)).isPresent(),key+" recipe loaded");
        Files.write(Path.of("cable-smoke-report.txt"),report);SuitcaseGlider.LOG.info("CABLE_SMOKE_OK {} checks",report.size());stop=event.getServer();countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(stop!=null&&e.phase==TickEvent.Phase.END&&--countdown<=0){var server=stop;stop=null;server.halt(false);}}
}
