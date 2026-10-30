package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.*;
import net.minecraft.resources.ResourceLocation;
import dev.salum.suitcaseglider.*;

public final class GliderItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static GliderItemRenderer instance;
    public static GliderItemRenderer instance(){if(instance==null)instance=new GliderItemRenderer();return instance;}
    private GliderItemRenderer(){super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(ItemStack item,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        // The player layer owns third-person rendering, including the suitcase.
        if(context==ItemDisplayContext.THIRD_PERSON_LEFT_HAND || context==ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)return;
        var mc=Minecraft.getInstance();
        if((context==ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context==ItemDisplayContext.FIRST_PERSON_RIGHT_HAND)
                && mc.player!=null && GliderController.selected(mc.player)
                && item==mc.player.getMainHandItem() && GliderClient.pose(mc.player,mc.getFrameTime()).phase()>0)return;
        if(context==ItemDisplayContext.GUI || context==ItemDisplayContext.FIXED || context==ItemDisplayContext.GROUND){
            pose.pushPose();if(context==ItemDisplayContext.GROUND){pose.translate(.15,.15,.5);pose.scale(.7f,.7f,.7f);}else pose.translate(0,0,.5);
            icon(pose,buffers,SuitcaseGlider.id("textures/item/glider_frame.png"),0xffffff,light);
            pose.translate(0,0,.001);icon(pose,buffers,SuitcaseGlider.id("textures/item/glider_fabric.png"),GliderItem.fabricColor(item),light);pose.popPose();return;
        }
        // First person uses vanilla hand placement, bobbing and item transforms.
        pose.pushPose();pose.translate(.5,.5,.5);pose.scale(.8f,.8f,.8f);pose.translate(-6/16f,-3/16f,0);
        GliderScene.get().render(pose,buffers,light,"carry_idle",0,GliderItem.fabricColor(item),null,false,false,false);pose.popPose();
    }
    private void icon(PoseStack pose,MultiBufferSource buffers,ResourceLocation texture,int color,int light){
        VertexConsumer v=buffers.getBuffer(RenderType.entityCutoutNoCull(texture));float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
        v.vertex(pose.last().pose(),0,1,0).color(r,g,b,1).uv(0,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
        v.vertex(pose.last().pose(),0,0,0).color(r,g,b,1).uv(0,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
        v.vertex(pose.last().pose(),1,0,0).color(r,g,b,1).uv(1,1).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
        v.vertex(pose.last().pose(),1,1,0).color(r,g,b,1).uv(1,0).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.last().normal(),0,0,1).endVertex();
    }
}
