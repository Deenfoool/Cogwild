package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in isolated GUI walkthrough; never touches the user's usual world. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class MachineGuiVisualChecks {
    private static int startup,ticks;private static boolean loading,done;private static volatile String failure;
    private static final List<String> kinds=new ArrayList<>(MachineCatalog.ALL.keySet());
    private static final List<String> report=new ArrayList<>();
    private static int motionFrame,pausedFrame;
    private static BlockPos pos(int i){return new BlockPos(i%4*6,80,i/4*6);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("cogwild.machineGuiQa")||e.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){if(++startup==1&&!mc.getLanguageManager().getSelected().equals("ru_ru")){mc.options.languageCode="ru_ru";mc.getLanguageManager().setSelected("ru_ru");mc.reloadResourcePacks();}if(startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Machines-Test");}return;}
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){mc.setScreen(null);mc.options.hideGui=false;mc.options.guiScale().set(3);mc.resizeDisplay();mc.options.setCameraType(CameraType.FIRST_PERSON);mc.getSingleplayerServer().execute(()->{
            try{
                var server=mc.getSingleplayerServer();var player=server.getPlayerList().getPlayer(mc.player.getUUID());var l=player.serverLevel();
                for(int x=-5;x<=26;x++)for(int z=-5;z<=19;z++){l.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.SMOOTH_STONE.defaultBlockState());for(int y=80;y<88;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());}
                player.getInventory().clearContent();player.setGameMode(GameType.CREATIVE);player.setNoGravity(true);l.setDayTime(6000);
                for(int i=0;i<kinds.size();i++){String kind=kinds.get(i);var block=MachineContent.MACHINES.get(kind).get();var p=pos(i);var state=block.defaultBlockState();l.setBlockAndUpdate(p,state);block.setPlacedBy(l,p,state,player,new ItemStack(block));var b=(MachineBlockEntity)l.getBlockEntity(p);b.receiveCableEnergy(4000,false);
                    if(kind.equals("crusher"))b.inventory.setStackInSlot(0,new ItemStack(Items.COBBLESTONE,64));
                    if(kind.equals("air_separator"))b.inventory.setStackInSlot(0,new ItemStack(Items.GRAVEL,64));
                }
            }catch(Exception ex){failure=ex.toString();}
        });}
        if(ticks>=80&&(ticks-80)%70==0&&(ticks-80)/70<kinds.size()){
            int i=(ticks-80)/70;mc.getSingleplayerServer().execute(()->{
                try{var player=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var l=player.serverLevel();player.closeContainer();player.teleportTo(l,pos(i).getX()+1,81,pos(i).getZ()-2,0,15);var block=MachineContent.MACHINES.get(kinds.get(i)).get();var state=l.getBlockState(pos(i));var secondary=MachineBlock.other(pos(i),state);
                    var result=l.getBlockState(secondary).use(l,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(secondary),Direction.NORTH,secondary,false));
                    if(!result.consumesAction())throw new IllegalStateException("Secondary interaction failed");
                }catch(Exception ex){failure=ex.toString();}
            });
        }
        if(ticks>=110&&(ticks-110)%70==0&&(ticks-110)/70<kinds.size()){
            if(failure!=null)throw new IllegalStateException(failure);int i=(ticks-110)/70;String kind=kinds.get(i);
            if(!(mc.screen instanceof MachineScreen)||!(mc.player.containerMenu instanceof MachineMenu menu)||!menu.machine.kind().equals(kind)||menu.slots.size()!=36+menu.layout.visibleSlots()||menu.machine.data.get(3)<=0)throw new IllegalStateException("Machine GUI/data failed "+kind);
            if(mc.screen.children().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.Button))throw new IllegalStateException("Unexpected manual button");
            if(kind.equals("crusher")||kind.equals("air_separator"))if(menu.machine.data.get(0)<=0||menu.machine.data.get(2)!=MachineBlockEntity.RUNNING)throw new IllegalStateException("Live recipe progress not synced "+kind);
            motionFrame=((MachineScreen)mc.screen).animationFrame();Screenshot.grab(mc.gameDirectory,"gui-"+kind+".png",mc.getMainRenderTarget(),c->{});report.add(kind+": slots="+menu.slots.size()+", state="+menu.machine.data.get(2)+", energy="+menu.machine.data.get(3)+", progress="+menu.machine.data.get(0));
            if(i==kinds.size()-1){try{Files.write(mc.gameDirectory.toPath().resolve("machine-gui-client-report.txt"),report);}catch(Exception ex){throw new RuntimeException(ex);}done=true;SuitcaseGlider.LOG.info("MACHINE_GUI_CLIENT_OK {}",report);mc.level.disconnect();mc.clearLevel(new TitleScreen());mc.stop();}
        }
        if(ticks>=110){
            int i=(ticks-110)/70,phase=(ticks-110)%70;
            if(i<kinds.size()&&(kinds.get(i).equals("crusher")||kinds.get(i).equals("air_separator"))&&mc.screen instanceof MachineScreen screen&&mc.player.containerMenu instanceof MachineMenu menu){
                String kind=kinds.get(i);
                if(phase==3){
                    if(screen.animationFrame()==motionFrame)throw new IllegalStateException("Machine icon does not rotate "+kind);
                    Screenshot.grab(mc.gameDirectory,"gui-"+kind+"-motion.png",mc.getMainRenderTarget(),c->{});
                    mc.getSingleplayerServer().execute(()->{var b=(MachineBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(pos(i));var tag=b.saveWithoutMetadata();tag.putInt("CableEnergy",0);b.load(tag);b.tickEnergy();});
                }
                if(phase==8){if(menu.machine.data.get(2)!=MachineBlockEntity.NO_POWER)throw new IllegalStateException("No-power pause not synchronized");pausedFrame=screen.animationFrame();}
                if(phase==11){
                    if(screen.animationFrame()!=pausedFrame)throw new IllegalStateException("Icon moves without power");
                    mc.getSingleplayerServer().execute(()->{var b=(MachineBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(pos(i));b.receiveCableEnergy(4000,false);b.inventory.setStackInSlot(3,new ItemStack(kind.equals("crusher")?Items.GRAVEL:Items.FLINT,64));b.tickEnergy();});
                }
                if(phase==15){if(menu.machine.data.get(2)!=MachineBlockEntity.BLOCKED)throw new IllegalStateException("Blocked pause not synchronized");pausedFrame=screen.animationFrame();}
                if(phase==18){
                    if(screen.animationFrame()!=pausedFrame)throw new IllegalStateException("Icon moves with blocked output");
                    mc.getSingleplayerServer().execute(()->{var b=(MachineBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(pos(i));b.inventory.setStackInSlot(3,ItemStack.EMPTY);b.tickEnergy();});
                }
                if(phase==23){if(!menu.machine.processing()||screen.animationFrame()==pausedFrame)throw new IllegalStateException("Icon does not resume "+kind);report.add(kind+": animated, freezes without power and with full output, resumes");}
            }
        }
    }
}
