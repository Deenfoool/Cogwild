package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.cable.*;
import dev.salum.suitcaseglider.machine.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Isolated client/server snapshot test, explicitly enabled with -PcableQa. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class CableVisualChecks {
    private static int startup,ticks;private static boolean loading,done;private static volatile String failure;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("cogwild.cableQa")||event.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){if(++startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Cable-Test");}return;}
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){mc.setScreen(null);mc.options.hideGui=true;mc.options.fov().set(65);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.getSingleplayerServer().execute(()->{
            try{
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();var world=CableWorld.get(level);world.links.clear();world.setDirty();
                for(int x=-10;x<=14;x++)for(int z=-10;z<=14;z++){level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=80;y<88;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                BlockPos gen=new BlockPos(-3,80,4),low=new BlockPos(0,80,2),high=new BlockPos(0,82,9),lathe=new BlockPos(6,80,0),crusher=new BlockPos(6,80,6);
                level.setBlockAndUpdate(gen,CableContent.GENERATOR.get().defaultBlockState().setValue(EnergyNodeBlock.FACING,Direction.SOUTH));level.setBlockAndUpdate(low,CableContent.RELAY.get().defaultBlockState());
                for(int y=80;y<83;y++)level.setBlockAndUpdate(new BlockPos(0,y,10),Blocks.POLISHED_ANDESITE.defaultBlockState());level.setBlockAndUpdate(high,CableContent.RELAY.get().defaultBlockState().setValue(EnergyNodeBlock.FACING,Direction.NORTH));
                for(BlockPos pos:List.of(lathe,crusher)){var block=pos.equals(lathe)?MachineContent.LATHE.get():MachineContent.CRUSHER.get();var state=block.defaultBlockState();level.setBlockAndUpdate(pos,state);block.setPlacedBy(level,pos,state,player,new ItemStack(block));}
                for(BlockPos[] pair:new BlockPos[][]{{gen,low},{low,lathe},{gen,high},{high,crusher}}){boolean floor=pair[1].equals(low)||pair[1].equals(lathe);var path=world.route(level,pair[0],pair[1],floor);if(path.isEmpty())throw new IllegalStateException("Client scene route failed: "+pair[0]+" -> "+pair[1]);world.add(new CableWorld.Link(UUID.randomUUID(),pair[0],pair[1],floor,(int)Math.ceil(CableRoutes.length(path)),path));}
                ((EnergyNodeEntity)level.getBlockEntity(gen)).fuelTicks=64000;level.setDayTime(6000);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.teleportTo(level,11,85,-7,38,22);CableNetwork.broadcast(level);
            }catch(Exception ex){failure=ex.toString();SuitcaseGlider.LOG.error("Cable visual setup",ex);}
        });}
        if(ticks==160){
            RenderGeometryChecks.glider();
            if(failure!=null)throw new IllegalStateException(failure);
            if(CableRenderer.linksReceived()!=4||CableRenderer.renderedSegments<50)throw new IllegalStateException("Cable client snapshot/render failed: "+CableRenderer.linksReceived()+" / "+CableRenderer.renderedSegments);
            Screenshot.grab(mc.gameDirectory,"cables-front.png",mc.getMainRenderTarget(),c->{});
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.teleportTo(p.serverLevel(),-7,84,10,-128,19);});
        }
        if(ticks==200){Screenshot.grab(mc.gameDirectory,"cables-rear.png",mc.getMainRenderTarget(),c->{});try{Files.writeString(mc.gameDirectory.toPath().resolve("cable-client-report.txt"),"links="+CableRenderer.linksReceived()+"\nrenderedSegments="+CableRenderer.renderedSegments+"\nCLIENT_OK\n");}catch(Exception ex){throw new RuntimeException(ex);}done=true;SuitcaseGlider.LOG.info("CABLE_CLIENT_OK");mc.level.disconnect();mc.clearLevel(new TitleScreen());mc.stop();}
    }
}
