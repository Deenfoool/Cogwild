package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in isolated client scene, never runs in a normal game. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class MachineVisualChecks {
    private static int startup,ticks,index;private static boolean loading,done;private static volatile String failure;
    public static final Set<String> WORLD=new HashSet<>(),ITEMS=new HashSet<>();
    private static final List<String> kinds=new ArrayList<>(MachineCatalog.ALL.keySet());
    private static BlockPos position(int i){return new BlockPos((i%4)*6,80,(i/4)*7);}
    private static void camera(Minecraft mc,int i){mc.player.getInventory().selected=0;mc.getSingleplayerServer().execute(()->{
        var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var at=position(i);p.getInventory().setItem(0,new ItemStack(MachineContent.MACHINE_ITEMS.get(kinds.get(i)).get()));p.getInventory().selected=0;p.inventoryMenu.broadcastChanges();double center=at.getX()+MachineCatalog.get(kinds.get(i)).width()/2.0;
        if(kinds.get(i).equals("quenching_bath"))p.teleportTo(p.serverLevel(),center-2.3,83.0,at.getZ()-3.8,-31,31);
        else p.teleportTo(p.serverLevel(),center-3,82.0,at.getZ()-5,-31,13);
    });}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("cogwild.machineQa")||event.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){
            if(++startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());
            if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Machines-Test");}return;
        }
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){mc.setScreen(null);mc.options.fov().set(65);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.getSingleplayerServer().execute(()->{
            try{
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
                for(int x=-7;x<28;x++)for(int z=-8;z<17;z++){level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=80;y<86;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                for(var drop:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(-8,78,-9,30,88,19)))drop.discard();
                player.getInventory().clearContent();
                for(int i=0;i<kinds.size();i++){
                    var block=MachineContent.MACHINES.get(kinds.get(i)).get();var pos=position(i);var state=block.defaultBlockState();
                    level.setBlockAndUpdate(pos,state);block.setPlacedBy(level,pos,state,player,new ItemStack(block));level.setBlockAndUpdate(pos.below(),Blocks.REDSTONE_BLOCK.defaultBlockState());
                    player.getInventory().setItem(i,new ItemStack(block));
                }
                level.setDayTime(6000);player.setGameMode(GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
            }catch(Exception e){failure=e.toString();SuitcaseGlider.LOG.error("Machine visual scene setup",e);}
        });}
        if(ticks==60)camera(mc,0);
        if(ticks>=140&&(ticks-140)%50==0){
            if(failure!=null)throw new IllegalStateException(failure);
            Screenshot.grab(mc.gameDirectory,"machine-"+kinds.get(index)+".png",mc.getMainRenderTarget(),c->{});
            if(++index<kinds.size()){mc.options.hideGui=false;camera(mc,index);}
            else{
                if(!WORLD.containsAll(kinds)||!ITEMS.containsAll(kinds))throw new IllegalStateException("Missing machine rendering: "+WORLD+" / "+ITEMS);
                try{Files.writeString(mc.gameDirectory.toPath().resolve("machine-client-report.txt"),"world="+WORLD+"\nitems="+ITEMS+"\nrenderCalls="+MachineRenderer.renderCalls+"\nrenderNanos="+MachineRenderer.renderNanos+"\nCLIENT_OK\n");}catch(Exception e){throw new RuntimeException(e);}
                done=true;SuitcaseGlider.LOG.info("MACHINE_CLIENT_OK");mc.level.disconnect();mc.clearLevel(new TitleScreen());mc.stop();
            }
        }
    }
}
