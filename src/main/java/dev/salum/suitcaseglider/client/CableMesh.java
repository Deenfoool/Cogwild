package dev.salum.suitcaseglider.client;

import com.mojang.blaze3d.vertex.*;
import java.util.*;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** Immutable cable tessellation; world lighting refreshes once per tick, geometry only on route changes. */
final class CableMesh {
    private record Segment(float[] vertices,float[] normals,float[] colors,BlockPos lightPos){}
    private final List<Segment> segments=new ArrayList<>();
    final AABB bounds;
    private final int[] lights;
    private long lightTick=Long.MIN_VALUE;
    CableMesh(List<Vec3> points){
        double minX=Double.POSITIVE_INFINITY,minY=minX,minZ=minX,maxX=-minX,maxY=-minX,maxZ=-minX;
        for(var p:points){minX=Math.min(minX,p.x);minY=Math.min(minY,p.y);minZ=Math.min(minZ,p.z);maxX=Math.max(maxX,p.x);maxY=Math.max(maxY,p.y);maxZ=Math.max(maxZ,p.z);}
        bounds=new AABB(minX,minY,minZ,maxX,maxY,maxZ).inflate(.04);
        double distance=0;
        for(int i=1;i<points.size();i++){
            Vec3 a=points.get(i-1),b=points.get(i);double len=a.distanceTo(b);if(len<.0001)continue;
            int pieces=Math.max(1,(int)Math.ceil(len*8));
            Vec3 dir=b.subtract(a).normalize(),u=dir.cross(Math.abs(dir.y)<.9?new Vec3(0,1,0):new Vec3(1,0,0)).normalize(),v=dir.cross(u).normalize();
            for(int j=0;j<pieces;j++){
                double t=(double)j/pieces,T=(double)(j+1)/pieces;Vec3 from=a.lerp(b,t),to=a.lerp(b,T);boolean ring=(distance+len*t)%1<.12;
                float[] vertices=new float[72],normals=new float[18],colors=new float[18];
                for(int k=0;k<6;k++){
                    double angle=k*Math.PI/3,next=(k+1)*Math.PI/3;
                    Vec3 n=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle))),N=u.scale(Math.cos(next)).add(v.scale(Math.sin(next))),normal=n.add(N).normalize();
                    Vec3[] corners={from.add(n.scale(.032)),to.add(n.scale(.032)),to.add(N.scale(.032)),from.add(N.scale(.032))};
                    float shade=.78f+.22f*(float)Math.max(0,normal.y);int c=k*3;
                    normals[c]=(float)normal.x;normals[c+1]=(float)normal.y;normals[c+2]=(float)normal.z;
                    colors[c]=(ring?.65f:.12f)*shade;colors[c+1]=(ring?.37f:.16f)*shade;colors[c+2]=(ring?.12f:.18f)*shade;
                    for(int q=0;q<4;q++){int at=k*12+q*3;vertices[at]=(float)corners[q].x;vertices[at+1]=(float)corners[q].y;vertices[at+2]=(float)corners[q].z;}
                }
                segments.add(new Segment(vertices,normals,colors,BlockPos.containing(from)));
            }distance+=len;
        }
        lights=new int[segments.size()];
    }
    int draw(PoseStack stack,VertexConsumer out,Level level,Vec3 camera){
        if(lightTick!=level.getGameTime()){lightTick=level.getGameTime();for(int i=0;i<lights.length;i++)lights[i]=LevelRenderer.getLightColor(level,segments.get(i).lightPos);}
        int drawn=0;var matrix=stack.last().pose();var normalMatrix=stack.last().normal();
        for(int i=0;i<segments.size();i++){
            var s=segments.get(i);if(s.lightPos.distToCenterSqr(camera.x,camera.y,camera.z)>112*112)continue;
            for(int k=0;k<6;k++){int c=k*3;for(int q=0;q<4;q++){int at=k*12+q*3;
                out.vertex(matrix,s.vertices[at],s.vertices[at+1],s.vertices[at+2]).color(s.colors[c],s.colors[c+1],s.colors[c+2],1).uv(.5f,.5f).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(lights[i]).normal(normalMatrix,s.normals[c],s.normals[c+1],s.normals[c+2]).endVertex();
            }}drawn++;
        }return drawn;
    }
}
