package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.mech.*;
import java.nio.file.*;
import java.util.*;
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

/** Regression run only: a separate world/profile, with real client input and server packets. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class MechRegressionClient {
    private static int startup,ticks;private static boolean loading;
    private static volatile int antId=-1;
    private static final List<String> report=new ArrayList<>();
    private static void check(boolean value,String label){report.add(label+"="+value);if(!value)throw new IllegalStateException("MECH_CLIENT_FAILED: "+label);}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),c->SuitcaseGlider.LOG.info("MECH_QA_SCREENSHOT {}",name));}
    private static void serverMount(Minecraft mc,boolean initial){
        mc.getSingleplayerServer().execute(()->{
            var server=mc.getSingleplayerServer();ServerPlayer player=server.getPlayerList().getPlayer(mc.player.getUUID());var level=player.serverLevel();
            MechAntEntity ant;
            if(initial){ant=MechContent.ANT.get().create(level);ant.moveTo(0,80,10,0,0);ant.setNoAi(true);ant.tameWithName(player);ant.equipSaddle(null);level.addFreshEntity(ant);antId=ant.getId();}
            else ant=(MechAntEntity)level.getEntity(antId);
            player.setGameMode(GameType.CREATIVE);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);player.setShiftKeyDown(false);player.teleportTo(level,0,80,7,0,0);ant.mobInteract(player,InteractionHand.MAIN_HAND);
        });
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("mechant.qa")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){
            if(++startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());
            if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Mech-Ant-Regression");}return;
        }
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){mc.setScreen(null);mc.options.hideGui=true;mc.options.fov().set(65);serverMount(mc,true);}
        if(ticks==110){
            check(mc.player.getVehicle() instanceof MechAntEntity,"mounted-client");
            ArmorStand camera=new ArmorStand(mc.level,4,81.5,6.5);camera.setYRot(130);camera.yRotO=130;camera.setXRot(8);camera.xRotO=8;camera.xo=camera.getX();camera.yo=camera.getY();camera.zo=camera.getZ();mc.setCameraEntity(camera);mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if(ticks==130){check(MechClient.renderedArmPairs>5,"third-person-arm-geometry-rendered");shot(mc,"mech-fixed-third-person-grips");}
        if(ticks==140){mc.setCameraEntity(mc.player);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.setXRot(38);}
        if(ticks==160){check(MechClient.renderedArmPairs>20,"first-person-arm-geometry-rendered");shot(mc,"mech-fixed-first-person-grips");}
        if(ticks==175)mc.options.keyShift.setDown(true);
        if(ticks==195)mc.options.keyShift.setDown(false);
        if(ticks==270){check(!mc.player.isPassenger(),"dismount-client-confirmed");shot(mc,"mech-fixed-dismounted");}
        if(ticks==280)serverMount(mc,false);
        if(ticks==291)mc.options.keyShift.setDown(true);
        if(ticks==301)mc.options.keyShift.setDown(false);
        if(ticks==325)check(!mc.player.isPassenger(),"cancel-boarding-with-shift");
        if(ticks==340)serverMount(mc,false);
        if(ticks==430)mc.options.keyShift.setDown(true);
        if(ticks==520)mc.options.keyShift.setDown(false);
        if(ticks==545){
            check(!mc.player.isPassenger(),"held-shift-does-not-restart-dismount");
            mc.getSingleplayerServer().execute(()->{var sp=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var ant=(MechAntEntity)sp.serverLevel().getEntity(antId);check(!ant.isVehicle(),"server-passenger-list-empty");});
        }
        if(ticks==565){
            try{Files.write(mc.gameDirectory.toPath().resolve("mech-client-regression.txt"),report);}catch(Exception ex){throw new RuntimeException(ex);}
            mc.options.hideGui=false;mc.options.keyShift.setDown(false);mc.setCameraEntity(mc.player);mc.player.setXRot(0);SuitcaseGlider.LOG.info("MECH_CLIENT_REGRESSION_OK {}",report);
        }
    }
}
