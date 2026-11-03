package dev.salum.suitcaseglider.client;

import com.google.gson.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;
import dev.salum.suitcaseglider.*;

/** Runtime reader for the authored bone hierarchy, UVs and exact sampled animation tracks. */
public final class GliderScene {
    private static GliderScene cached;
    public static void invalidate(){cached=null;}
    public static GliderScene get(){if(cached==null)cached=new GliderScene();return cached;}
    public float[] boneRotation(String name,String animation,double time){for(var e:groups.entrySet())if(e.getValue().get("name").getAsString().equals(name))return sample(animation,e.getKey(),"rotation",time,new float[3]);return new float[3];}
    private final Map<String,JsonObject> groups=new HashMap<>(),elements=new HashMap<>();
    private final Map<String,Map<String,Map<String,List<Key>>>> animations=new HashMap<>();
    private record Surface(float[][] vertices,float[][] uv,Vector3f normal,int texture){}
    private record ModelElement(String name,float[] origin,float[] rotation,List<Surface> faces){}
    private final Map<String,ModelElement> compiled=new HashMap<>();
    private JsonArray roots;
    private float walkSwing;
    public void walkSwing(float value){walkSwing=value;}
    private record Key(double time,float[] value){}
    private GliderScene(){
        try(var input=Minecraft.getInstance().getResourceManager().open(SuitcaseGlider.id("rigs/glider_scene.json"));var reader=new InputStreamReader(input,StandardCharsets.UTF_8)){
            JsonObject model=JsonParser.parseReader(reader).getAsJsonObject();roots=model.getAsJsonArray("outliner");
            for(JsonElement e:model.getAsJsonArray("groups")){JsonObject o=e.getAsJsonObject();groups.put(o.get("uuid").getAsString(),o);}
            for(JsonElement e:model.getAsJsonArray("elements")){JsonObject o=e.getAsJsonObject();elements.put(o.get("uuid").getAsString(),o);compiled.put(o.get("uuid").getAsString(),compile(o));}
            for(JsonElement e:model.getAsJsonArray("animations")){JsonObject a=e.getAsJsonObject();Map<String,Map<String,List<Key>>> bones=new HashMap<>();
                for(var bone:a.getAsJsonObject("animators").entrySet()){Map<String,List<Key>> channels=new HashMap<>();JsonArray keys=bone.getValue().getAsJsonObject().getAsJsonArray("keyframes");if(keys==null)continue;
                    for(JsonElement k:keys){JsonObject key=k.getAsJsonObject(),v=key.getAsJsonArray("data_points").get(0).getAsJsonObject();String channel=key.get("channel").getAsString();channels.computeIfAbsent(channel,x->new ArrayList<>()).add(new Key(key.get("time").getAsDouble(),new float[]{number(v,"x",0),number(v,"y",0),number(v,"z",0)}));}
                    channels.values().forEach(list->list.sort(Comparator.comparingDouble(Key::time)));bones.put(bone.getKey(),channels);}
                animations.put(a.get("name").getAsString().replace("animation.hand_glider.",""),bones);}
        }catch(Exception ex){throw new IllegalStateException("Cannot load glider rig",ex);}
    }
    static float number(JsonObject o,String key,float fallback){try{return o.has(key)?Float.parseFloat(o.get(key).getAsString()):fallback;}catch(Exception e){return fallback;}}
    static float[] vector(JsonObject o,String key,float[] fallback){if(!o.has(key))return fallback.clone();JsonArray a=o.getAsJsonArray(key);return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    private float[] sample(String anim,String bone,String channel,double time,float[] fallback){var channels=animations.getOrDefault(anim,Map.of()).get(bone);if(channels==null)return fallback.clone();List<Key> keys=channels.get(channel);if(keys==null||keys.isEmpty())return fallback.clone();if(time<=keys.get(0).time)return keys.get(0).value.clone();if(time>=keys.get(keys.size()-1).time)return keys.get(keys.size()-1).value.clone();int lo=0,hi=keys.size()-1;while(hi-lo>1){int m=(lo+hi)/2;if(keys.get(m).time<=time)lo=m;else hi=m;}Key a=keys.get(lo),b=keys.get(hi);float w=(float)((time-a.time)/(b.time-a.time));return new float[]{a.value[0]+w*(b.value[0]-a.value[0]),a.value[1]+w*(b.value[1]-a.value[1]),a.value[2]+w*(b.value[2]-a.value[2])};}
    private static void rotate(PoseStack p,float[] r){p.mulPose(Axis.ZP.rotationDegrees(r[2]));p.mulPose(Axis.YP.rotationDegrees(r[1]));p.mulPose(Axis.XP.rotationDegrees(r[0]));}
    public void render(PoseStack pose,MultiBufferSource buffers,int light,String animation,double time,int color,ResourceLocation skin,boolean arms,boolean legs,boolean mirror){
        pose.pushPose();if(mirror)pose.scale(-1,1,1);for(JsonElement root:roots)node(root,new float[]{0,0,0},pose,buffers,light,animation,time,color,skin,arms,legs);pose.popPose();
    }
    private void node(JsonElement node,float[] parent,PoseStack pose,MultiBufferSource buffers,int light,String animation,double time,int color,ResourceLocation skin,boolean arms,boolean legs){
        if(node.isJsonPrimitive()){JsonObject e=elements.get(node.getAsString());if(e!=null)element(e,parent,pose,buffers,light,color,skin,arms,legs);return;}
        JsonObject tree=node.getAsJsonObject(),g=groups.get(tree.get("uuid").getAsString());if(g==null)return;String id=g.get("uuid").getAsString(),name=g.get("name").getAsString();float[] origin=vector(g,"origin",new float[3]),rotation=vector(g,"rotation",new float[3]);
        float[] move=sample(animation,id,"position",time,new float[3]),r=sample(animation,id,"rotation",time,new float[3]),scale=sample(animation,id,"scale",time,new float[]{1,1,1});
        if(name.equals("suspension")){move=new float[3];r=new float[3];}
        if(name.equals("steve_arm_right") && animation.equals("carry_idle"))r[0]+=walkSwing;
        if(Math.min(Math.abs(scale[0]),Math.min(Math.abs(scale[1]),Math.abs(scale[2])))<.0001f)return;
        pose.pushPose();pose.translate((origin[0]-parent[0]+move[0])/16,(origin[1]-parent[1]+move[1])/16,(origin[2]-parent[2]+move[2])/16);rotate(pose,new float[]{rotation[0]+r[0],rotation[1]+r[1],rotation[2]+r[2]});pose.scale(scale[0],scale[1],scale[2]);
        for(JsonElement child:tree.getAsJsonArray("children"))node(child,origin,pose,buffers,light,animation,time,color,skin,arms,legs);pose.popPose();
    }
    private static ModelElement compile(JsonObject e){
        float[] origin=vector(e,"origin",new float[3]);List<Surface> faces=new ArrayList<>();
        if(e.get("type").getAsString().equals("mesh")){
            JsonObject vertices=e.getAsJsonObject("vertices");for(var entry:e.getAsJsonObject("faces").entrySet()){JsonObject f=entry.getValue().getAsJsonObject();List<float[]> points=new ArrayList<>(),uv=new ArrayList<>();for(JsonElement k:f.getAsJsonArray("vertices")){String key=k.getAsString();JsonArray v=vertices.getAsJsonArray(key),u=f.getAsJsonObject("uv").getAsJsonArray(key);points.add(new float[]{v.get(0).getAsFloat(),v.get(1).getAsFloat(),v.get(2).getAsFloat()});uv.add(new float[]{u.get(0).getAsFloat(),u.get(1).getAsFloat()});}faces.add(surface(points,uv,textureIndex(f)));}
        }else{
            float[] f=vector(e,"from",new float[3]),t=vector(e,"to",new float[3]);float inflate=number(e,"inflate",0);for(int i=0;i<3;i++){f[i]-=origin[i]+inflate;t[i]=t[i]-origin[i]+inflate;}
            for(var entry:e.getAsJsonObject("faces").entrySet()){JsonObject face=entry.getValue().getAsJsonObject();if(!face.has("texture")||face.get("texture").isJsonNull())continue;
                float[][] points=switch(entry.getKey()){
                    case "north"->new float[][]{{t[0],t[1],f[2]},{f[0],t[1],f[2]},{f[0],f[1],f[2]},{t[0],f[1],f[2]}};
                    case "south"->new float[][]{{f[0],t[1],t[2]},{t[0],t[1],t[2]},{t[0],f[1],t[2]},{f[0],f[1],t[2]}};
                    case "west"->new float[][]{{f[0],t[1],f[2]},{f[0],t[1],t[2]},{f[0],f[1],t[2]},{f[0],f[1],f[2]}};
                    case "east"->new float[][]{{t[0],t[1],t[2]},{t[0],t[1],f[2]},{t[0],f[1],f[2]},{t[0],f[1],t[2]}};
                    case "up"->new float[][]{{t[0],t[1],t[2]},{f[0],t[1],t[2]},{f[0],t[1],f[2]},{t[0],t[1],f[2]}};
                    default->new float[][]{{t[0],f[1],f[2]},{f[0],f[1],f[2]},{f[0],f[1],t[2]},{t[0],f[1],t[2]}};};
                JsonArray rect=face.getAsJsonArray("uv");float u0=rect.get(0).getAsFloat(),v0=rect.get(1).getAsFloat(),u1=rect.get(2).getAsFloat(),v1=rect.get(3).getAsFloat();faces.add(surface(Arrays.asList(points),List.of(new float[]{u0,v0},new float[]{u1,v0},new float[]{u1,v1},new float[]{u0,v1}),textureIndex(face)));}
        }return new ModelElement(e.get("name").getAsString(),origin,vector(e,"rotation",new float[3]),List.copyOf(faces));
    }
    private static Surface surface(List<float[]> vertices,List<float[]> uv,int texture){
        float[] a=vertices.get(0),b=vertices.get(1),c=vertices.get(2);
        Vector3f normal=new Vector3f(b[0]-a[0],b[1]-a[1],b[2]-a[2]).cross(new Vector3f(c[0]-a[0],c[1]-a[1],c[2]-a[2])).normalize();
        float[][] points=new float[4][3],coords=new float[4][2];int[] indices=vertices.size()==3?new int[]{0,1,2,2}:new int[]{0,1,2,3};
        for(int i=0;i<4;i++){int j=indices[i];for(int k=0;k<3;k++)points[i][k]=vertices.get(j)[k]/16;coords[i][0]=uv.get(j)[0]/(texture==0?128:64);coords[i][1]=uv.get(j)[1]/64;}
        return new Surface(points,coords,normal,texture);
    }
    private void element(JsonObject json,float[] parent,PoseStack pose,MultiBufferSource buffers,int light,int color,ResourceLocation skin,boolean arms,boolean legs){
        var e=compiled.get(json.get("uuid").getAsString());String name=e.name;
        if(name.startsWith("Steve_")&&!(arms&&(name.contains("_arm")||name.contains("_sleeve"))||legs&&(name.contains("_leg")||name.contains("_trouser"))))return;
        float[] origin=e.origin;pose.pushPose();pose.translate((origin[0]-parent[0])/16,(origin[1]-parent[1])/16,(origin[2]-parent[2])/16);rotate(pose,e.rotation);
        var normal=new Vector3f();var vertex=new Vector3f();
        for(var face:e.faces){
            ResourceLocation resource=face.texture==0?CLOTH:face.texture==1?FRAME:skin;if(resource==null)continue;
            var out=buffers.getBuffer(RenderType.entityCutoutNoCull(resource));float r=1,g=1,b=1;
            if(face.texture==0){r=Math.min(1,((color>>16)&255)/255f*1.1f);g=Math.min(1,((color>>8)&255)/255f*1.1f);b=Math.min(1,(color&255)/255f*1.1f);}
            normal.set(face.normal);pose.last().normal().transform(normal);
            for(int i=0;i<4;i++){var v=face.vertices[i];var uv=face.uv[i];vertex.set(v[0],v[1],v[2]).mulPosition(pose.last().pose());
                out.vertex(vertex.x,vertex.y,vertex.z,r,g,b,1,uv[0],uv[1],OverlayTexture.NO_OVERLAY,light,normal.x,normal.y,normal.z);}
        }pose.popPose();
    }
    private static final ResourceLocation CLOTH=SuitcaseGlider.id("textures/entity/cloth_shading.png"),FRAME=SuitcaseGlider.id("textures/entity/frame.png");
    private static int textureIndex(JsonObject face){try{return Integer.parseInt(face.get("texture").getAsString());}catch(Exception e){return 0;}}
}
