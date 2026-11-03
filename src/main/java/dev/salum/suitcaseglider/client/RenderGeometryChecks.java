package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.*;
import dev.salum.suitcaseglider.SuitcaseGlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;

/** Development-only regression for cached glider geometry, animation and optional rider limbs. */
final class RenderGeometryChecks {
    private static final class Counter implements VertexConsumer {
        long vertices,hash=1;
        public VertexConsumer vertex(double x,double y,double z){
            if(!Double.isFinite(x+y+z))throw new IllegalStateException("Nonfinite cached glider vertex");
            hash=31*hash+Double.doubleToLongBits(x)+Double.doubleToLongBits(y)+Double.doubleToLongBits(z);return this;
        }
        public VertexConsumer color(int r,int g,int b,int a){return this;}
        public VertexConsumer uv(float u,float v){if(!Float.isFinite(u+v))throw new IllegalStateException("Invalid UV");return this;}
        public VertexConsumer overlayCoords(int u,int v){return this;}
        public VertexConsumer uv2(int u,int v){return this;}
        public VertexConsumer normal(float x,float y,float z){if(!Float.isFinite(x+y+z))throw new IllegalStateException("Invalid normal");return this;}
        public void endVertex(){vertices++;}
        public void defaultColor(int r,int g,int b,int a){}
        public void unsetDefaultColor(){}
    }
    static void glider(){
        var scene=GliderScene.get();var skin=Minecraft.getInstance().player.getSkinTextureLocation();
        Counter a=new Counter(),b=new Counter(),limbs=new Counter();
        scene.render(new PoseStack(),type->a,15728880,"carry_idle",0,0xdcd6be,skin,false,false,false);
        scene.render(new PoseStack(),type->b,15728880,"flight",.3,0x3bb5c4,skin,false,false,false);
        scene.render(new PoseStack(),type->limbs,15728880,"carry_idle",.2,0xdcd6be,skin,true,true,false);
        if(a.vertices==0||b.vertices==0||a.hash==b.hash||limbs.vertices<=a.vertices)throw new IllegalStateException("Cached glider animation or limb filtering failed");
        SuitcaseGlider.LOG.info("GLIDER_GEOMETRY_OK carry={} flight={} withLimbs={}",a.vertices,b.vertices,limbs.vertices);
    }
}
