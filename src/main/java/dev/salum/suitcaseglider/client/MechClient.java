package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.mech.*;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.*;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class MechClient {
    @SubscribeEvent public static void player(RenderPlayerEvent.Pre e){
        if(!(e.getEntity().getVehicle() instanceof MechAntEntity))return;
        var m=e.getRenderer().getModel();m.rightArm.visible=false;m.leftArm.visible=false;m.rightSleeve.visible=false;m.leftSleeve.visible=false;
    }
    @SubscribeEvent public static void playerAfter(RenderPlayerEvent.Post e){
        if(!(e.getEntity() instanceof AbstractClientPlayer player)||!(player.getVehicle() instanceof MechAntEntity ant))return;
        try {
            var stack=e.getPoseStack();stack.pushPose();
            stack.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(e.getPartialTick(),ant.yRotO,ant.getYRot())));
            RiderArms.render(stack,e.getMultiBufferSource(),e.getPackedLight(),player,ant,e.getPartialTick());
            stack.popPose();
        } finally {var m=e.getRenderer().getModel();m.rightArm.visible=true;m.leftArm.visible=true;m.rightSleeve.visible=true;m.leftSleeve.visible=true;}
    }
    private static long nextDismountRequest;
    private static int reportedVehicle=-1;
    private static long nextMotionReport;
    private static boolean lastJumpHeld,lastGrounded;
    @SubscribeEvent public static void vanillaHand(RenderHandEvent e){
        var player=net.minecraft.client.Minecraft.getInstance().player;
        if(player!=null&&player.getVehicle() instanceof MechAntEntity)e.setCanceled(true);
    }
    @SubscribeEvent public static void firstPerson(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES)return;
        var mc=net.minecraft.client.Minecraft.getInstance();var player=mc.player;
        if(player==null||!mc.options.getCameraType().isFirstPerson()||!(player.getVehicle() instanceof MechAntEntity ant)||mc.getCameraEntity()!=player)return;
        float partial=e.getPartialTick();var camera=e.getCamera().getPosition();var stack=e.getPoseStack();stack.pushPose();
        stack.translate(Mth.lerp(partial,player.xo,player.getX())-camera.x,Mth.lerp(partial,player.yo,player.getY())-camera.y,Mth.lerp(partial,player.zo,player.getZ())-camera.z);
        stack.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,ant.yRotO,ant.getYRot())));
        var buffers=mc.renderBuffers().bufferSource();int light=mc.getEntityRenderDispatcher().getPackedLightCoords(player,partial);
        RiderArms.render(stack,buffers,light,player,ant,partial);stack.popPose();buffers.endBatch(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));
    }
    @SubscribeEvent public static void movement(MovementInputUpdateEvent e){
        if(!(e.getEntity().getVehicle() instanceof MechAntEntity ant)||!ant.isTamed())return;
        var mc=net.minecraft.client.Minecraft.getInstance();var input=e.getInput();long time=e.getEntity().level().getGameTime();
        boolean held=input.jumping,grounded=ant.onGround();ant.predictJumpHeld(held&&grounded&&!ant.boarding());
        if(reportedVehicle!=ant.getId()||time>=nextMotionReport||nextMotionReport-time>2||held!=lastJumpHeld||grounded!=lastGrounded){
            if(ant.isSaddled())MechNetwork.reportMotion(input.forwardImpulse,input.leftImpulse,held,mc.player.getJumpRidingScale(),grounded,(float)ant.getDeltaMovement().horizontalDistance());
            reportedVehicle=ant.getId();nextMotionReport=time+2;lastJumpHeld=held;lastGrounded=grounded;
        }
        if(e.getInput().shiftKeyDown){
            long now=e.getEntity().level().getGameTime();
            if(now>=nextDismountRequest||nextDismountRequest-now>10){MechNetwork.requestDismount();nextDismountRequest=now+10;}
            // The server owns the animation and detachment; never reject its passenger update.
            e.getInput().shiftKeyDown=false;
        }
    }
    @Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
    public static final class Setup {
        @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(MechContent.ANT.get(),MechAntRenderer::new);}
    }
    /** Two-link skinned arms keep the hands on the authored control grips, with real elbow flexion. */
    private static final class RiderArms {
        static void render(PoseStack stack,MultiBufferSource buffers,int light,AbstractClientPlayer p,MechAntEntity ant,float partial){
            if(p.isInvisible())return;
            var rig=ant.rigPose(partial);Vector3f pelvis=rig.anchor(ant.boarding()?"rider_boarding_pelvis":"rider_seat_anchor"),foot=new Vector3f(pelvis).sub(0,.75f,0);
            stack.pushPose();
            VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(p.getSkinTextureLocation()));
            for(int side:new int[]{-1,1}){
                Vector3f shoulder=new Vector3f(side*.3125f,1.375f,0),hand=rig.anchor(side<0?"rider_hand_left":"rider_hand_right").sub(foot);
                if(ant.boarding()){
                    float u=(float)(ant.elapsed(partial)/3.2);if(ant.clip().equals("dismount"))u=1-u;u=Mth.clamp((u-.25f)/.5f,0,1);u=u*u*(3-2*u);
                    var rest=rig.anchor("boarding_step_left").sub(foot).add(side<0?0:.35f,.45f,-.05f);hand=rest.lerp(hand,u);
                }
                Vector3f d=new Vector3f(hand).sub(shoulder);float dist=Mth.clamp(d.length(),.05f,.745f);d.normalize();hand=new Vector3f(shoulder).fma(dist,d);
                Vector3f pole=new Vector3f(side*.7f,-.8f,.35f);pole.fma(-pole.dot(d),d).normalize();float h=(float)java.lang.Math.sqrt(java.lang.Math.max(0,.375f*.375f-dist*dist/4));Vector3f elbow=new Vector3f(shoulder).fma(dist/2,d).fma(h,pole);
                int tx=side<0?32:40,ty=side<0?48:16;segment(stack,out,light,shoulder,elbow,tx,ty);segment(stack,out,light,elbow,hand,tx,ty+6);
            }stack.popPose();
            renderedArmPairs++;
        }
        private static void segment(PoseStack parent,VertexConsumer out,int light,Vector3f a,Vector3f b,int tx,int ty){
            Vector3f delta=new Vector3f(b).sub(a);float length=delta.length();Quaternionf rotation=new Quaternionf().rotationTo(new Vector3f(0,1,0),delta.normalize());
            parent.pushPose();parent.translate(a.x,a.y,a.z);parent.mulPose(rotation);float w=.125f;
            float[][] v={{-w,0,-w},{w,0,-w},{w,length,-w},{-w,length,-w},{-w,0,w},{w,0,w},{w,length,w},{-w,length,w}};
            int[][] fs={{1,0,3,2},{4,5,6,7},{0,4,7,3},{5,1,2,6},{3,7,6,2},{0,1,5,4}};
            for(int face=0;face<6;face++){int[] ids=fs[face];float u=tx+(face==0?4:face==1?12:face==2?0:face==3?8:4),vv=ty+(face<4?4:0),ww=4,hh=face<4?6:4;
                float[][] uv={{u,vv},{u+ww,vv},{u+ww,vv+hh},{u,vv+hh}};Vector3f n=new Vector3f(v[ids[1]]).sub(new Vector3f(v[ids[0]])).cross(new Vector3f(v[ids[2]]).sub(new Vector3f(v[ids[0]]))).normalize();
                for(int i=0;i<4;i++){var q=v[ids[i]];out.vertex(parent.last().pose(),q[0],q[1],q[2]).color(1f,1f,1f,1f).uv(uv[i][0]/64,uv[i][1]/64).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(parent.last().normal(),n.x,n.y,n.z).endVertex();}
            }parent.popPose();
        }
    }
    static int renderedArmPairs;
}
