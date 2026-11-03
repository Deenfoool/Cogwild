package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.blast.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class BlastRenderer implements BlockEntityRenderer<BlastBlockEntity> {
    public BlastRenderer(BlockEntityRendererProvider.Context c){}
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(BlastContent.TYPE.get(),BlastRenderer::new);}
    @Override public void render(BlastBlockEntity b,float partial,PoseStack stack,MultiBufferSource out,int light,int overlay){
        if(!b.formed())return;if(Boolean.getBoolean("cogwild.blastQa"))BlastVisualChecks.rendered=true;stack.pushPose();stack.translate(.5,0,.5);stack.mulPose(Axis.YP.rotationDegrees(180-b.facing().toYRot()));stack.translate(-.5,0,-.5);
        // The controller sits inside the structure; use exposed light rather than its enclosed voxel.
        int exterior=net.minecraft.client.renderer.LevelRenderer.getLightColor(b.getLevel(),b.getBlockPos().above(5));
        light=net.minecraft.client.renderer.LightTexture.pack(Math.max(net.minecraft.client.renderer.LightTexture.block(light),net.minecraft.client.renderer.LightTexture.block(exterior)),Math.max(net.minecraft.client.renderer.LightTexture.sky(light),net.minecraft.client.renderer.LightTexture.sky(exterior)));
        var rig=MachineRenderer.rig("blast_furnace");double seconds=b.getLevel()==null?0:(b.getLevel().getGameTime()+partial)/20;
        var pose=rig.pose("processing",b.active()?seconds:0,"processing",0,1);double feeding=b.feedSeconds(partial);
        var loading=feeding>=0&&feeding<3?rig.pose("loading",feeding,"loading",0,1):null;
        for(int i=0;i<rig.bones.size();i++){
            String name=rig.bones.get(i).name();
            if(name.equals("heat_windows")&&!b.hot())pose.bones[i].scale(.001f);
            if(loading!=null&&(name.equals("hopper_lid_left")||name.equals("hopper_lid_right")))pose.replaceSubtree(i,loading.bones[i]);
        }
        MachineRenderer.drawPose("blast_furnace",pose,b.hot(),stack,out,light,overlay);stack.popPose();
    }
    @Override public boolean shouldRenderOffScreen(BlastBlockEntity b){return false;}
    @Override public int getViewDistance(){return 128;}
}
