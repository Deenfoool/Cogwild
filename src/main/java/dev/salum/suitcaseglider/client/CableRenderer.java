package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.*;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.cable.*;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class CableRenderer {
    private static CableNetwork.Snapshot snapshot=new CableNetwork.Snapshot("",List.of());
    private static final RenderType TYPE=RenderType.entityCutoutNoCull(SuitcaseGlider.id("textures/misc/cable_white.png"));
    private static Map<CableNetwork.Picture,CableMesh> meshes=Map.of();
    public static void accept(CableNetwork.Snapshot s){
        if(s.equals(snapshot))return;
        var next=new LinkedHashMap<CableNetwork.Picture,CableMesh>();
        boolean sameDimension=s.dimension().equals(snapshot.dimension());
        for(var link:s.links()){var cached=sameDimension?meshes.get(link):null;next.put(link,cached==null?new CableMesh(link.points()):cached);}
        meshes=next;snapshot=s;
    }
    static int linksReceived(){return snapshot.links().size();}
    static int renderedSegments;
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut e){snapshot=new CableNetwork.Snapshot("",List.of());meshes=Map.of();preview=List.of();previewTime=Long.MIN_VALUE;}
    private static long previewTime=Long.MIN_VALUE;private static List<Vec3> preview=List.of();private static boolean previewValid;
    @SubscribeEvent public static void render(RenderLevelStageEvent e){
        if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        renderedSegments=0;var stack=e.getPoseStack();Vec3 cam=e.getCamera().getPosition();stack.pushPose();stack.translate(-cam.x,-cam.y,-cam.z);
        var buffers=mc.renderBuffers().bufferSource();var out=buffers.getBuffer(TYPE);
        if(snapshot.dimension().equals(mc.level.dimension().location().toString()))for(var mesh:meshes.values())
            if(mesh.bounds.distanceToSqr(cam)<112*112&&e.getFrustum().isVisible(mesh.bounds))renderedSegments+=mesh.draw(stack,out,mc.level,cam);
        var item=mc.player.getMainHandItem().getItem() instanceof CableItem?mc.player.getMainHandItem():mc.player.getOffhandItem();var tag=item.getTag();
        if(item.getItem() instanceof CableItem&&tag!=null&&tag.contains("Start")&&tag.getString("Dimension").equals(mc.level.dimension().location().toString())&&mc.hitResult instanceof BlockHitResult hit){
            BlockPos a=BlockPos.of(tag.getLong("Start")),b=CableWorld.endpoint(mc.level,hit.getBlockPos());
            if(b!=null&&mc.level.hasChunkAt(a)&&CableWorld.endpoint(mc.level,a)!=null&&!a.equals(b)){
                if(mc.level.getGameTime()!=previewTime&&mc.level.getGameTime()%3==0){
                    previewTime=mc.level.getGameTime();Vec3 from=CableWorld.anchor(mc.level,a),to=CableWorld.anchor(mc.level,b);
                    preview=CableWorld.plan(mc.level,a,b,tag.getBoolean("Floor"));previewValid=!preview.isEmpty();if(!previewValid)preview=List.of(from,to);
                }draw(stack,out,preview,true,previewValid,mc);
            }
        }
        stack.popPose();buffers.endBatch(TYPE);
    }
    private static void draw(PoseStack stack,VertexConsumer out,List<Vec3> points,boolean preview,boolean valid,Minecraft mc){
        double distance=0;for(int i=1;i<points.size();i++){
            Vec3 a=points.get(i-1),b=points.get(i);double len=a.distanceTo(b);if(len<.0001)continue;
            if(a.distanceToSqr(mc.gameRenderer.getMainCamera().getPosition())>112*112){distance+=len;continue;}
            int pieces=Math.max(1,(int)Math.ceil(len*8));for(int j=0;j<pieces;j++){
                double t=(double)j/pieces,T=(double)(j+1)/pieces;boolean ring=(distance+len*t)%1<.12;
                float[] color=preview?(valid?new float[]{.16f,.72f,.48f}:new float[]{.85f,.18f,.13f}):ring?new float[]{.65f,.37f,.12f}:new float[]{.12f,.16f,.18f};
                tube(stack,out,a.lerp(b,t),a.lerp(b,T),preview?.022:.032,color,LevelRenderer.getLightColor(mc.level,BlockPos.containing(a.lerp(b,t))));
                renderedSegments++;
            }distance+=len;
        }
    }
    private static void tube(PoseStack s,VertexConsumer out,Vec3 a,Vec3 b,double radius,float[] color,int light){
        Vec3 dir=b.subtract(a).normalize(),u=dir.cross(Math.abs(dir.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize(),v=dir.cross(u).normalize();
        for(int k=0;k<6;k++){
            double t=k*Math.PI/3,T=(k+1)*Math.PI/3;Vec3 n=u.scale(Math.cos(t)).add(v.scale(Math.sin(t))),N=u.scale(Math.cos(T)).add(v.scale(Math.sin(T)));
            Vec3[] points={a.add(n.scale(radius)),b.add(n.scale(radius)),b.add(N.scale(radius)),a.add(N.scale(radius))};Vec3 normal=n.add(N).normalize();float shade=.78f+.22f*(float)Math.max(0,normal.y);
            for(Vec3 p:points)out.vertex(s.last().pose(),(float)p.x,(float)p.y,(float)p.z).color(color[0]*shade,color[1]*shade,color[2]*shade,1).uv(.5f,.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(s.last().normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
        }
    }
}
