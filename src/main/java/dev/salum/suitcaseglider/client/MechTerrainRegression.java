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
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Opt-in integration test in an isolated world; never runs in a regular game. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class MechTerrainRegression {
    private static int startup,ticks,snapshotTick;private static boolean loading,done,mixed;private static volatile int antId=-1;
    private static double zForward,zBack,maxY,minAfter=1000,maxError;private static int mixedFrames,plantFrames;private static double forwardSpeed,backwardSpeed;
    private static final List<String> report=new ArrayList<>();private static final Set<String> jump=new HashSet<>();private static double jumpBase,jumpTop;
    private static void release(Minecraft mc){mc.options.keyUp.setDown(false);mc.options.keyDown.setDown(false);mc.options.keyJump.setDown(false);mc.options.keyShift.setDown(false);}
    private static void write(Minecraft mc){try{Files.write(mc.gameDirectory.toPath().resolve("mech-terrain-regression.txt"),report);}catch(Exception ex){throw new RuntimeException(ex);}}
    private static void check(Minecraft mc,boolean value,String label){report.add(label+"="+value);write(mc);if(!value){release(mc);done=true;throw new IllegalStateException("MECH_TERRAIN_FAILED: "+label);}}
    private static void shot(Minecraft mc,String name){Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),c->{});}
    private static void view(Minecraft mc,MechAntEntity ant){
        mc.setCameraEntity(mc.player);mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("mechant.terrainQa")||event.phase!=TickEvent.Phase.END||done)return;var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){
            if(++startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());
            if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Mech-Ant-Terrain");}return;
        }
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){release(mc);mc.setScreen(null);mc.options.hideGui=true;mc.options.fov().set(65);
            mc.getSingleplayerServer().execute(()->{ServerPlayer p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());var level=p.serverLevel();
                for(var old:level.getEntitiesOfClass(MechAntEntity.class,p.getBoundingBox().inflate(200)))old.discard();
                for(int x=-10;x<=10;x++)for(int z=-12;z<=24;z++){level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());for(int y=80;y<=85;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());if(z>=4&&z<=8)level.setBlockAndUpdate(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState());}
                var ant=MechContent.ANT.get().create(level);ant.moveTo(0,80,0,0,0);ant.setInvulnerable(true);ant.tameWithName(p);ant.equipSaddle(null);level.addFreshEntity(ant);antId=ant.getId();p.setGameMode(GameType.SURVIVAL);p.getAbilities().invulnerable=true;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.teleportTo(level,0,80,-3,0,0);ant.mobInteract(p,InteractionHand.MAIN_HAND);
            });
        }
        if(!(mc.level.getEntity(antId) instanceof MechAntEntity ant))return;
        mc.player.setYRot(0);mc.player.setXRot(0);
        if(ticks==105){check(mc,mc.player.getVehicle()==ant,"mounted");mc.options.keyUp.setDown(true);}
        if(ticks==140)zForward=ant.getZ();
        if(ticks==180){forwardSpeed=(ant.getZ()-zForward)/2;report.add("motion-state clip="+ant.clip()+" z="+ant.getZ()+" input="+mc.player.zza+" dead="+mc.player.isDeadOrDying()+" screen="+mc.screen+" grounded="+ant.onGround()+" "+ant.terrainDebug());report.add("forward-blocks-per-second="+forwardSpeed);check(mc,Math.abs(forwardSpeed-MechGait.FORWARD)<.07,"forward-matches-authored-stride");mc.options.keyUp.setDown(false);}
        if(ticks==220)mc.options.keyDown.setDown(true);
        if(ticks==250)zBack=ant.getZ();
        if(ticks==290){backwardSpeed=(zBack-ant.getZ())/2;report.add("reverse-blocks-per-second="+backwardSpeed);check(mc,Math.abs(backwardSpeed-MechGait.BACKWARD)<.05,"reverse-matches-authored-stride");mc.options.keyDown.setDown(false);}
        if(ticks==330)mc.options.keyUp.setDown(true);
        if(ticks>=330&&ticks<=1600){
            var d=ant.terrainDebug();maxY=Math.max(maxY,ant.getY());if(ant.getZ()>10)minAfter=Math.min(minAfter,ant.getY());
            if(d.planted()>=3){plantFrames++;maxError=Math.max(maxError,d.maxPlantError());}
            if(d.planted()>=3&&d.highestSupport()-d.lowestSupport()>.8){mixedFrames++;if(!mixed){mixed=true;view(mc,ant);snapshotTick=ticks;}}
            if(ticks%100==0){report.add("terrain-tick="+ticks+" z="+ant.getZ()+" y="+ant.getY()+" clip="+ant.clip()+" "+d);write(mc);}
            if(mixed&&ticks==snapshotTick+2)shot(mc,"mech-terrain-mixed-foot-heights");
            if(mixed&&ticks==snapshotTick+5)mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if(ticks==1600){
            mc.options.keyUp.setDown(false);report.add("highest-root-y="+maxY);report.add("lowest-root-after-descent="+minAfter);report.add("mixed-support-frames="+mixedFrames);report.add("max-planted-foot-error="+maxError);
            check(mc,ant.getZ()>10&&maxY>80.9&&minAfter<80.1,"climbs-and-descends-one-block");check(mc,mixedFrames>5,"feet-use-different-real-support-heights");check(mc,plantFrames>500&&maxError<.06,"planted-feet-stay-on-surface");report.add("final-terrain-state="+ant.terrainDebug());check(mc,ant.terrainDebug().longestTransfer()<45&&Math.abs(ant.terrainDebug().height())<.15,"returns-to-flat-ground-stance");
        }
        if(ticks==1640){jumpBase=ant.getY();jumpTop=jumpBase;mc.options.keyJump.setDown(true);}
        if(ticks>=1640&&ticks<=1760){jump.add(ant.clip());jumpTop=Math.max(jumpTop,ant.getY());}
        if(ticks==1665)mc.options.keyJump.setDown(false);
        if(ticks==1760){report.add("jump-height="+(jumpTop-jumpBase));check(mc,jumpTop-jumpBase>.4&&jump.containsAll(Set.of("jump_charge","jump_release","jump_air","jump_land")),"charged-horse-jump-regression");mc.options.keyShift.setDown(true);}
        if(ticks==1780)mc.options.keyShift.setDown(false);
        if(ticks==1855){check(mc,!mc.player.isPassenger(),"dismount-regression");release(mc);mc.setCameraEntity(mc.player);mc.options.hideGui=false;done=true;write(mc);SuitcaseGlider.LOG.info("MECH_TERRAIN_REGRESSION_OK {}",report);}
    }
}
