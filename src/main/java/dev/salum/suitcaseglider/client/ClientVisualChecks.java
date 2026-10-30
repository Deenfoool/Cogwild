package dev.salum.suitcaseglider.client;
import java.nio.file.*;
import java.util.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.salum.suitcaseglider.*;

/** Opt-in, loopback-only development client; no user account or launcher files are used. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class ClientVisualChecks {
    private static int startup,ticks;private static boolean connected;private static final List<String> report=new ArrayList<>();
    private static void command(String text){Minecraft.getInstance().player.connection.sendCommand(text);}
    private static void screenshot(String name){var mc=Minecraft.getInstance();Screenshot.grab(mc.gameDirectory,name+".png",mc.getMainRenderTarget(),c->SuitcaseGlider.LOG.info("VISUAL_SCREENSHOT {}",name));}
    private static void phase(String label,int expected){var mc=Minecraft.getInstance();int got=GliderClient.pose(mc.player,0).phase();String line=label+": expected="+expected+", actual="+got;report.add(line);SuitcaseGlider.LOG.info("VISUAL_CHECK {}",line);if(got!=expected)report.add("FAILED: "+label);}
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("suitcaseglider.visual")||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null){
            if(++startup>100&&!connected&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());
            if(startup>100&&!connected&&mc.screen instanceof TitleScreen){connected=true;ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString("127.0.0.1:25577"),new ServerData("Private Glider Visual Test","127.0.0.1:25577",false),false);}return;
        }
        if(Boolean.getBoolean("suitcaseglider.rendercheck")){renderCheck(mc);return;}
        ticks++;if(ticks==20){mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);mc.options.fov().set(80);mc.player.setXRot(0);}
        if(ticks==90){phase("carry",0);screenshot("glider-carry");}
        if(ticks==100)command("tp @s 8.5 198.1 0.5 180 0");
        if(ticks==112){phase("short-drop-stays-closed",0);screenshot("glider-short-drop");}
        if(ticks==130)command("tp @s 0.5 215 0.5 180 0");
        if(ticks==145){phase("danger-deploy",1);screenshot("glider-deploy");}
        if(ticks==180){phase("flight",2);screenshot("glider-flight");report.add("flight-velocity-y="+mc.player.getDeltaMovement().y);}
        if(ticks==195)command("tp @s 0.5 197 0.5 180 0");
        if(ticks==204){phase("grounded-stow",3);screenshot("glider-stow");}
        if(ticks==235){phase("carry-after-landing",0);screenshot("glider-landed");}
        if(ticks==245)command("item replace entity @s hotbar.0 with suitcaseglider:glider{display:{color:6327704}}");
        if(ticks==265)screenshot("glider-dyed-carry");
        if(ticks==280){mc.options.setCameraType(CameraType.FIRST_PERSON);command("tp @s 0.5 215 0.5 180 -65");}
        if(ticks==330){screenshot("glider-first-person");phase("first-person-flight",2);}
        if(ticks==350){try{Files.write(Path.of(mc.gameDirectory.toString(),"client-visual-report.txt"),report);}catch(Exception ex){SuitcaseGlider.LOG.error("Cannot write visual report",ex);}if(Boolean.getBoolean("suitcaseglider.visual.close"))mc.stop();else{mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);command("tp @s 0.5 197 0.5 180 0");}}
    }
    private static void renderCheck(Minecraft mc){
        ticks++;
        if(ticks==10){command("tp @s 0.5 197 0.5 180 0");mc.options.setCameraType(CameraType.FIRST_PERSON);}
        if(ticks==45)screenshot("fixed-first-person-level");
        if(ticks==50)command("tp @s ~ ~ ~ 180 75");
        if(ticks==80)screenshot("fixed-first-person-down");
        if(ticks==85){command("tp @s ~ ~ ~ 180 0");mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);}
        if(ticks==120)screenshot("fixed-third-person-back");
        if(ticks==125)mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
        if(ticks==155)screenshot("fixed-third-person-front");
        if(ticks==170){mc.options.setCameraType(CameraType.FIRST_PERSON);command("tp @s 0.5 215 0.5 180 -55");}
        if(ticks>170 && ticks<250){var state=GliderClient.pose(mc.player,0);if(state.phase()==1 && state.time()>2 && !report.contains("deploy-seen")){report.add("deploy-seen");screenshot("fixed-first-person-deploy");}if(state.phase()==2 && !report.contains("flight-seen")){report.add("flight-seen");screenshot("fixed-first-person-flight");}}
        if(ticks==260)command("tp @s 0.5 197 0.5 180 -55");
        if(ticks>260 && ticks<310){var state=GliderClient.pose(mc.player,0);if(state.phase()==3 && state.time()>1 && !report.contains("stow-seen")){report.add("stow-seen");screenshot("fixed-first-person-stow");}}
        if(ticks==320){try{Files.write(Path.of(mc.gameDirectory.toString(),"first-person-animation-report.txt"),report);}catch(Exception ex){throw new RuntimeException(ex);}mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);}
        if(ticks==325)SuitcaseGlider.LOG.info("RENDER_CHECK_READY: first-person vanilla; third-person single layer. Screenshots require visual review.");
    }

}
