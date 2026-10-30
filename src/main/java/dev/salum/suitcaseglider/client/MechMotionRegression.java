package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.mech.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class MechMotionRegression {
    private static int startup,ticks;private static boolean loading;private static volatile int antId=-1;
    private static double forwardZ,backZ,groundY,maxJumpY;private static final Set<String> seen=new HashSet<>();
    private static final List<String> report=new CopyOnWriteArrayList<>();
    private static void release(Minecraft mc){mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);}
    private static void check(boolean value,String label){report.add(label+"="+value);if(!value){release(Minecraft.getInstance());throw new IllegalStateException("MECH_MOTION_FAILED: "+label);}}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),c->SuitcaseGlider.LOG.info("MECH_MOTION_SCREENSHOT {}",name));}
    private static void view(Minecraft mc,MechAntEntity ant){
        double x=ant.getX()+4,z=ant.getZ()+4,y=ant.getY()+2.1;ArmorStand camera=new ArmorStand(mc.level,x,y,z);
        float yaw=135,pitch=10;camera.setYRot(yaw);camera.yRotO=yaw;camera.setYHeadRot(yaw);camera.yHeadRotO=yaw;camera.setXRot(pitch);camera.xRotO=pitch;camera.xo=x;camera.yo=y;camera.zo=z;
        mc.setCameraEntity(camera);mc.options.setCameraType(CameraType.FIRST_PERSON);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("mechant.motionQa")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){
            if(++startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());
            if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Mech-Ant-Motion");}return;
        }
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){
            release(mc);mc.setScreen(null);mc.options.hideGui=true;mc.options.fov().set(65);
            mc.getSingleplayerServer().execute(()->{var server=mc.getSingleplayerServer();ServerPlayer p=server.getPlayerList().getPlayer(mc.player.getUUID());var level=p.serverLevel();
                for(int x=-48;x<=48;x++)for(int z=-96;z<=96;z++)level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x,79,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                var ant=MechContent.ANT.get().create(level);ant.moveTo(0,80,10,0,0);ant.setInvulnerable(true);ant.tameWithName(p);ant.equipSaddle(null);level.addFreshEntity(ant);antId=ant.getId();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(level,0,80,7,0,0);ant.mobInteract(p,InteractionHand.MAIN_HAND);});
        }
        if(!(mc.level.getEntity(antId) instanceof MechAntEntity ant))return;
        if(ticks<420){mc.player.setYRot(0);mc.player.setXRot(0);}
        if(ticks>=290&&ticks<=410){seen.add(ant.clip());maxJumpY=Math.max(maxJumpY,ant.getY());}
        if(ticks==105){check(mc.player.getVehicle()==ant,"mounted");forwardZ=ant.getZ();mc.player.setYRot(0);mc.player.setXRot(0);}
        if(ticks==110)mc.options.keyUp.setDown(true);
        if(ticks==145)view(mc,ant);
        if(ticks==147)shot(mc,"mech-ridden-forward");
        if(ticks==149)mc.setCameraEntity(mc.player);
        if(ticks==155){check(ant.getZ()-forwardZ>1,"forward-physical-motion");check(ant.clip().equals("ridden_walk"),"forward-ridden-walk-clip");report.add("forward-distance="+(ant.getZ()-forwardZ));}
        if(ticks==160)mc.options.keyUp.setDown(false);
        if(ticks==195){backZ=ant.getZ();mc.options.keyDown.setDown(true);}
        if(ticks==233)view(mc,ant);
        if(ticks==235)shot(mc,"mech-ridden-backward");
        if(ticks==237)mc.setCameraEntity(mc.player);
        if(ticks==242){check(backZ-ant.getZ()>.5,"backward-physical-motion");check(ant.clip().equals("ridden_walk_backward"),"backward-ridden-walk-clip");}
        if(ticks==245)mc.options.keyDown.setDown(false);
        if(ticks==290){groundY=ant.getY();maxJumpY=groundY;mc.options.keyJump.setDown(true);}
        if(ticks==305){check(ant.clip().equals("jump_charge"),"space-hold-charge-clip");check(ant.jumpCharge()>.5,"charge-progress");view(mc,ant);}
        if(ticks==307)shot(mc,"mech-jump-charge");
        if(ticks==309)mc.setCameraEntity(mc.player);
        if(ticks==315)mc.options.keyJump.setDown(false);
        if(ticks==319)view(mc,ant);
        if(ticks==320)shot(mc,"mech-jump-release");
        if(ticks==321)mc.setCameraEntity(mc.player);
        if(ticks==328)view(mc,ant);
        if(ticks==329)shot(mc,"mech-jump-air");
        if(ticks==330)mc.setCameraEntity(mc.player);
        if(ticks==410){
            check(maxJumpY-groundY>.4,"native-horse-jump-height");check(seen.containsAll(Set.of("jump_charge","jump_release","jump_air","jump_land")),"all-four-jump-stages");check(mc.player.getVehicle()==ant,"rider-stays-attached-after-landing");report.add("jump-height="+(maxJumpY-groundY));report.add("jump-clips="+seen);
        }
        if(ticks==420)mc.options.keyShift.setDown(true);
        if(ticks==440)mc.options.keyShift.setDown(false);
        if(ticks==515){
            check(!mc.player.isPassenger(),"dismount-still-works");release(mc);mc.setCameraEntity(mc.player);mc.options.hideGui=false;
            try{Files.write(mc.gameDirectory.toPath().resolve("mech-motion-regression.txt"),report);}catch(Exception ex){throw new RuntimeException(ex);}
            SuitcaseGlider.LOG.info("MECH_MOTION_REGRESSION_OK {}",report);
        }
    }
}
