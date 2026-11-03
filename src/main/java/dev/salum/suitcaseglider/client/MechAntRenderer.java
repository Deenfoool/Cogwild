package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.mech.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public final class MechAntRenderer extends EntityRenderer<MechAntEntity> {
    private static final ResourceLocation TEXTURE=SuitcaseGlider.id("textures/entity/mech_ant.png");
    public MechAntRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=1.1f;}
    @Override public ResourceLocation getTextureLocation(MechAntEntity entity){return TEXTURE;}
    @Override public boolean shouldRender(MechAntEntity e,Frustum f,double x,double y,double z){return e.shouldRender(x,y,z)&&f.isVisible(e.getBoundingBox().inflate(3));}
    @Override public void render(MechAntEntity ant,float yaw,float partial,PoseStack stack,MultiBufferSource buffers,int light){
        if(ant.isInvisible()){super.render(ant,yaw,partial,stack,buffers,light);return;}
        stack.pushPose();stack.mulPose(Axis.YP.rotationDegrees(180-Mth.rotLerp(partial,ant.yRotO,ant.getYRot())));if(ant.isBaby())stack.scale(.5f,.5f,.5f);
        var rig=MechRig.get();var pose=ant.rigPose(partial);Matrix4f[] matrices=new Matrix4f[rig.bones.size()];Matrix3f[] normals=new Matrix3f[matrices.length];
        for(int i=0;i<matrices.length;i++){matrices[i]=new Matrix4f(stack.last().pose()).mul(pose.bones[i]);if(Math.abs(matrices[i].determinant())>=1e-8)normals[i]=new Matrix3f(matrices[i]).invert().transpose();}
        VertexConsumer out=buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        var normal=new org.joml.Vector3f();var vertex=new org.joml.Vector3f();
        for(var face:rig.faces){var matrix=matrices[face.bone()];if(normals[face.bone()]==null)continue;int packed=face.emissive()?LightTexture.FULL_BRIGHT:light;float[] n=face.normal();normal.set(n[0],n[1],n[2]);normals[face.bone()].transform(normal);
            for(int i=0;i<4;i++){float[] v=face.vertices()[i],uv=face.uv()[i];vertex.set(v[0],v[1],v[2]).mulPosition(matrix);out.vertex(vertex.x,vertex.y,vertex.z,1f,1f,1f,1f,uv[0]/1024,uv[1]/2048,OverlayTexture.NO_OVERLAY,packed,normal.x,normal.y,normal.z);}}
        stack.popPose();super.render(ant,yaw,partial,stack,buffers,light);
    }
}
