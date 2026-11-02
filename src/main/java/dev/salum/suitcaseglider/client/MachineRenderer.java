package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.*;
import dev.salum.suitcaseglider.mech.MechRig;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class MachineRenderer implements BlockEntityRenderer<MachineBlockEntity> {
    private static final Map<String,MechRig> RIGS=new HashMap<>();
    private static final Map<String,ResourceLocation[]> IMAGES=new HashMap<>();
    public MachineRenderer(BlockEntityRendererProvider.Context context){}
    public static MechRig rig(String kind){return RIGS.computeIfAbsent(kind,k->MechRig.load("/assets/suitcaseglider/rigs/"+k+".json"));}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(MachineContent.TYPE.get(),MachineRenderer::new);}
    @Override public void render(MachineBlockEntity machine,float partial,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
        String kind=machine.kind();if(Boolean.getBoolean("cogwild.machineQa"))MachineVisualChecks.WORLD.add(kind);var rig=rig(kind);var spec=MachineCatalog.get(kind);String clip=machine.powered()?spec.on():spec.off();double time=machine.seconds(partial);
        String transition=machine.powered()?spec.start():spec.stop();
        if(rig.clips.containsKey(transition)&&machine.transitionTime(partial)<rig.duration(transition)){clip=transition;time=machine.transitionTime(partial);}
        stack.pushPose();stack.translate(.5,0,.5);
        stack.mulPose(Axis.YP.rotationDegrees(180-machine.getBlockState().getValue(MachineBlock.FACING).toYRot()));
        var offset=spec.offset();stack.translate(offset[0],offset[1],offset[2]);
        if(kind.equals("fermenter"))drawBroth(machine,rig.pose(clip,time,clip,0,1),stack,buffers,light);
        draw(kind,clip,time,machine.powered(),stack,buffers,light,overlay);
        if(kind.equals("quenching_bath"))drawWater(machine,rig.pose(clip,time,clip,0,1),stack,buffers,light);
        stack.popPose();
    }
    public static void draw(String kind,String clip,double time,boolean powered,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
        var rig=rig(kind);var images=IMAGES.computeIfAbsent(kind,k->rig.textures.stream().map(t->new ResourceLocation(t.resource())).toArray(ResourceLocation[]::new));
        var pose=rig.pose(clip,time,clip,0,1);Matrix4f[] matrices=new Matrix4f[pose.bones.length];Matrix3f[] normals=new Matrix3f[matrices.length];
        for(int i=0;i<matrices.length;i++){matrices[i]=new Matrix4f(stack.last().pose()).mul(pose.bones[i]);if(Math.abs(matrices[i].determinant())>1e-8)normals[i]=new Matrix3f(matrices[i]).invert().transpose();}
        for(var face:rig.faces){if(normals[face.bone()]==null)continue;var texture=rig.textures.get(face.texture());ResourceLocation resource=images[face.texture()];
            VertexConsumer out=buffers.getBuffer(texture.translucent()?RenderType.entityTranslucent(resource):RenderType.entityCutoutNoCull(resource));
            int packed=powered&&face.emissive()?LightTexture.FULL_BRIGHT:light;
            for(int i=0;i<4;i++){float[] v=face.vertices()[i],uv=face.uv()[i],n=face.normal();out.vertex(matrices[face.bone()],v[0],v[1],v[2]).color(1f,1f,1f,1f).uv(uv[0]/texture.width(),uv[1]/texture.height()).overlayCoords(overlay).uv2(packed).normal(normals[face.bone()],n[0],n[1],n[2]).endVertex();}
        }
    }
    private static void drawWater(MachineBlockEntity machine,MechRig.Pose pose,PoseStack stack,MultiBufferSource buffers,int light){
        var lo=pose.anchor("fluid_min");var hi=pose.anchor("fluid_max");if(hi.y-lo.y<.005)return;
        var sprite=Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(new ResourceLocation("minecraft:block/water_still"));
        int color=net.minecraft.client.renderer.BiomeColors.getAverageWaterColor(machine.getLevel(),machine.getBlockPos());
        float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
        var out=buffers.getBuffer(RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));var m=stack.last();
        float[][] vertices={{lo.x,hi.y,lo.z},{lo.x,hi.y,hi.z},{hi.x,hi.y,hi.z},{hi.x,hi.y,lo.z}};
        float[][] uv={{sprite.getU0(),sprite.getV0()},{sprite.getU0(),sprite.getV1()},{sprite.getU1(),sprite.getV1()},{sprite.getU1(),sprite.getV0()}};
        for(int i=0;i<4;i++)out.vertex(m.pose(),vertices[i][0],vertices[i][1],vertices[i][2]).color(r,g,b,.78f).uv(uv[i][0],uv[i][1]).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(m.normal(),0,1,0).endVertex();
    }
    private static void drawBroth(MachineBlockEntity machine,MechRig.Pose pose,PoseStack stack,MultiBufferSource buffers,int light){
        var lo=pose.anchor("fluid_min");var hi=pose.anchor("fluid_max");if(hi.y-lo.y<.005)return;
        var atlas=net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS;
        var sprite=Minecraft.getInstance().getTextureAtlas(atlas).apply(new ResourceLocation("minecraft:block/water_still"));
        var out=buffers.getBuffer(RenderType.entityTranslucent(atlas));var m=stack.last();float cx=(lo.x+hi.x)/2,cz=(lo.z+hi.z)/2,r=11.2f/16;
        for(int i=0;i<32;i++){
            double a=i*Math.PI/16,b=(i+1)*Math.PI/16;float x=cx+r*(float)Math.cos(a),z=cz+r*(float)Math.sin(a),X=cx+r*(float)Math.cos(b),Z=cz+r*(float)Math.sin(b);
            float[][] side={{X,lo.y,Z},{x,lo.y,z},{x,hi.y,z},{X,hi.y,Z}};
            float[][] top={{cx,hi.y,cz},{X,hi.y,Z},{x,hi.y,z},{x,hi.y,z}};
            for(var vertices:new float[][][]{side,top})for(int j=0;j<4;j++)out.vertex(m.pose(),vertices[j][0],vertices[j][1],vertices[j][2]).color(.20f,.61f,.76f,.80f).uv(j==0||j==3?sprite.getU0():sprite.getU1(),j<2?sprite.getV1():sprite.getV0()).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(m.normal(),0,1,0).endVertex();
        }
    }
    private static BlockEntityWithoutLevelRenderer itemRenderer;
    public static BlockEntityWithoutLevelRenderer itemRenderer(){
        if(itemRenderer==null){var mc=Minecraft.getInstance();itemRenderer=new BlockEntityWithoutLevelRenderer(mc.getBlockEntityRenderDispatcher(),mc.getEntityModels()){
            @Override public void renderByItem(ItemStack item,ItemDisplayContext context,PoseStack stack,MultiBufferSource buffers,int light,int overlay){
                if(!(item.getItem() instanceof MachineItem machine))return;String kind=((MachineBlock)machine.getBlock()).kind;if(Boolean.getBoolean("cogwild.machineQa"))MachineVisualChecks.ITEMS.add(kind);
                var spec=MachineCatalog.get(kind);stack.pushPose();stack.translate(.5,.05,.5);float size=.9f/Math.max(Math.max(spec.width(),spec.depth()),spec.height()/16);stack.scale(size,size,size);var o=spec.offset();stack.translate(o[0]-(spec.width()-1)/2.0,o[1],o[2]-(spec.depth()-1)/2.0);
                draw(kind,spec.off(),0,false,stack,buffers,light,overlay);stack.popPose();
            }
        };}return itemRenderer;
    }
}
