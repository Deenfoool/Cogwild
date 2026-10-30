package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import dev.salum.suitcaseglider.*;
import com.mojang.math.Axis;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class GliderClient {
    public static final HumanoidModel.ArmPose ARM_POSE=HumanoidModel.ArmPose.create("SUITCASE_GLIDER",true,(model,entity,arm)->{
        if(!(entity instanceof Player p))return;Pose state=pose(p,Minecraft.getInstance().getFrameTime());boolean mirror=p.getMainArm()==HumanoidArm.RIGHT;
        float[] first=GliderScene.get().boneRotation("steve_arm_left",state.animation(),state.time()),second=GliderScene.get().boneRotation("steve_arm_right",state.animation(),state.time());
        model.rightArm.xRot=-(mirror?first[0]:second[0])*(float)Math.PI/180;model.leftArm.xRot=-(mirror?second[0]:first[0])*(float)Math.PI/180;
        model.rightArm.yRot=0;model.leftArm.yRot=0;model.rightArm.zRot=0;model.leftArm.zRot=0;
    });
    public record Pose(String animation,double time,int phase){}
    public static Pose pose(Player p,float partial){
        var state=GliderNetwork.CLIENT.get(p.getUUID());int phase=state==null?0:state.phase();
        if(!GliderController.selected(p))phase=-1;
        double elapsed=state==null?0:Math.max(0,p.level().getGameTime()+partial-state.started());
        return switch(phase){
            case 1->new Pose("deploy",Math.min(4.6,elapsed/GliderController.DEPLOY_TICKS*4.6),phase);
            case 2->new Pose("flight",(elapsed%48)/20,phase);
            case 3->new Pose("stow",Math.min(4.6,elapsed/GliderController.STOW_TICKS*4.6),phase);
            default->new Pose("carry_idle",(p.tickCount+partial)%48/20.0,phase);
        };
    }
    @SubscribeEvent public static void playerPre(RenderPlayerEvent.Pre e){
        if(!GliderController.selected(e.getEntity()))return;
        PlayerModel<?> m=e.getRenderer().getModel();m.leftArm.visible=false;m.rightArm.visible=false;m.leftSleeve.visible=false;m.rightSleeve.visible=false;
        if(pose(e.getEntity(),e.getPartialTick()).phase()>0){m.leftLeg.visible=false;m.rightLeg.visible=false;m.leftPants.visible=false;m.rightPants.visible=false;}
    }
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent e){
        if(e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null){GliderNetwork.CLIENT.clear();return;}
        var state=pose(mc.player,0);if(FlightPolicy.airbornePhase(state.phase())&&GliderController.selected(mc.player))GliderController.physics(mc.player,state.phase()==1);
    }
    @SubscribeEvent public static void click(InputEvent.InteractionKeyMappingTriggered e){
        var p=Minecraft.getInstance().player;if(p!=null&&FlightPolicy.airbornePhase(pose(p,0).phase())){e.setCanceled(true);e.setSwingHand(false);}
    }
    @SubscribeEvent public static void hand(RenderHandEvent e){
        var p=Minecraft.getInstance().player;
        if(p==null || !GliderController.selected(p) || e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        var state=pose(p,e.getPartialTick());if(state.phase()<=0)return;
        // Animate only the glider in world space; vanilla still owns both hands.
        PoseStack stack=e.getPoseStack();stack.pushPose();
        stack.mulPose(Axis.XP.rotationDegrees(p.getXRot()));
        stack.mulPose(Axis.YP.rotationDegrees(p.yBodyRot-p.getYRot()));
        stack.translate(0,-p.getEyeHeight(),0);
        GliderScene.get().render(stack,e.getMultiBufferSource(),e.getPackedLight(),state.animation(),state.time(),GliderItem.fabricColor(p.getMainHandItem()),null,false,false,p.getMainArm()==HumanoidArm.RIGHT);
        stack.popPose();
    }
    public static final class Layer extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        public Layer(PlayerRenderer renderer){super(renderer);}
        @Override public void render(PoseStack stack,MultiBufferSource buffer,int light,AbstractClientPlayer player,float swing,float amount,float partial,float age,float yaw,float pitch){
            if(!GliderController.selected(player))return;var pose=GliderClient.pose(player,partial);stack.pushPose();
            stack.translate(0,1.5,0);stack.scale(1,-1,1);if(player.isCrouching())stack.translate(0,-.125,0);
            GliderScene scene=GliderScene.get();scene.walkSwing(pose.phase()<=0?(float)Math.cos(swing*.6662f)*30*amount:0);
            scene.render(stack,buffer,light,pose.animation(),pose.time(),GliderItem.fabricColor(player.getMainHandItem()),player.getSkinTextureLocation(),true,pose.phase()>0,player.getMainArm()==HumanoidArm.RIGHT);scene.walkSwing(0);stack.popPose();
        }
    }
    @Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers e){for(String skin:e.getSkins()){PlayerRenderer r=e.getSkin(skin);if(r!=null)r.addLayer(new Layer(r));}}
        @SubscribeEvent public static void reload(RegisterClientReloadListenersEvent e){e.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener)manager->GliderScene.invalidate());}
    }
}
