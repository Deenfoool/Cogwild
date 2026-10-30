package dev.salum.suitcaseglider.mech;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Shared, client-independent rig evaluation: the server uses the very same seat/step anchors. */
public final class MechRig {
    public record Bone(String name,int parent,float[] origin,float[] rotation){}
    public record Face(int bone,float[][] vertices,float[][] uv,float[] normal,boolean emissive,int texture){}
    public record Texture(String resource,int width,int height,boolean translucent){}
    public record Anchor(int bone,float[] position){}
    public record Track(int bone,String channel,double[] times,float[][] values){}
    public record Clip(double length,boolean loop,List<Track> tracks){}
    private static final class Holder { static final MechRig INSTANCE=new MechRig(); }
    public static MechRig get(){return Holder.INSTANCE;}
    public final List<Bone> bones=new ArrayList<>();
    public final List<Face> faces=new ArrayList<>();
    public final List<Texture> textures=new ArrayList<>();
    public final Map<String,Clip> clips=new HashMap<>();
    public final Map<String,Anchor> anchors=new HashMap<>();
    private final Map<String,Integer> boneIds=new HashMap<>();
    private static float[] v(JsonArray a){float[] v=new float[a.size()];for(int i=0;i<v.length;i++)v[i]=a.get(i).getAsFloat();return v;}
    private static float[][] vv(JsonArray a){float[][] v=new float[a.size()][];for(int i=0;i<v.length;i++)v[i]=v(a.get(i).getAsJsonArray());return v;}
    public static MechRig load(String resource){return new MechRig(resource);}
    private MechRig(){this("/assets/suitcaseglider/rigs/mech_ant.json");}
    private MechRig(String resource){
        try(var in=MechRig.class.getResourceAsStream(resource);var r=new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)){
            var data=JsonParser.parseReader(r).getAsJsonObject();
            if(data.has("textures"))for(var x:data.getAsJsonArray("textures")){var t=x.getAsJsonObject();textures.add(new Texture(t.get("resource").getAsString(),t.get("width").getAsInt(),t.get("height").getAsInt(),t.get("translucent").getAsBoolean()));}
            for(var x:data.getAsJsonArray("bones")){var b=x.getAsJsonObject();boneIds.put(b.get("name").getAsString(),bones.size());bones.add(new Bone(b.get("name").getAsString(),b.get("parent").getAsInt(),v(b.getAsJsonArray("origin")),v(b.getAsJsonArray("rotation"))));}
            for(var x:data.getAsJsonArray("faces")){var f=x.getAsJsonObject();faces.add(new Face(f.get("bone").getAsInt(),vv(f.getAsJsonArray("v")),vv(f.getAsJsonArray("uv")),v(f.getAsJsonArray("n")),f.get("emissive").getAsBoolean(),f.has("texture")?f.get("texture").getAsInt():0));}
            for(var e:data.getAsJsonObject("anchors").entrySet()){var a=e.getValue().getAsJsonObject();anchors.put(e.getKey(),new Anchor(a.get("bone").getAsInt(),v(a.getAsJsonArray("position"))));}
            for(var e:data.getAsJsonObject("animations").entrySet()){
                var a=e.getValue().getAsJsonObject();List<Track> tracks=new ArrayList<>();
                for(var x:a.getAsJsonArray("tracks")){var t=x.getAsJsonObject();var ts=t.getAsJsonArray("times");double[] times=new double[ts.size()];for(int i=0;i<times.length;i++)times[i]=ts.get(i).getAsDouble();tracks.add(new Track(t.get("bone").getAsInt(),t.get("channel").getAsString(),times,vv(t.getAsJsonArray("values"))));}
                clips.put(e.getKey(),new Clip(a.get("length").getAsDouble(),a.get("loop").getAsBoolean(),tracks));
            }
        }catch(Exception ex){throw new IllegalStateException("Cannot load mechanical ant rig",ex);}
    }
    private Clip clip(String name){return clips.getOrDefault(name,clips.getOrDefault("idle",clips.get("unpowered")));}
    public double duration(String name){return clip(name).length;}
    public int boneIndex(String name){Integer index=boneIds.get(name);if(index==null)throw new IllegalArgumentException("Missing bone "+name);return index;}
    public Vector3f origin(String name){return new Vector3f(bones.get(boneIndex(name)).origin).div(16);}
    public boolean descendant(int child,int parent){while(child>=0){if(child==parent)return true;child=bones.get(child).parent;}return false;}
    private float[][][] channels(String name,double time){
        var clip=clip(name);time=clip.loop?Math.max(0,time)%clip.length:Math.min(clip.length,Math.max(0,time));
        float[][][] out=new float[bones.size()][3][3];for(var b:out)Arrays.fill(b[2],1);
        for(var tr:clip.tracks){int lo=0,hi=tr.times.length-1;while(hi-lo>1){int mid=(lo+hi)/2;if(tr.times[mid]<=time)lo=mid;else hi=mid;}if(time<=tr.times[0])hi=lo=0;else if(time>=tr.times[tr.times.length-1])hi=lo=tr.times.length-1;
            float w=lo==hi?0:(float)((time-tr.times[lo])/(tr.times[hi]-tr.times[lo]));int ch=switch(tr.channel){case "position"->0;case "rotation"->1;default->2;};for(int i=0;i<3;i++)out[tr.bone][ch][i]=tr.values[lo][i]+(tr.values[hi][i]-tr.values[lo][i])*w;
        }return out;
    }
    public Pose pose(String name,double time,String previous,double previousTime,float blend){
        return pose(name,time,previous,previousTime,blend,0,0);
    }
    public Pose pose(String name,double time,String previous,double previousTime,float blend,float throttle,float steering){
        var values=channels(name,time);var old=blend<1?channels(previous,previousTime):null;Matrix4f[] matrices=new Matrix4f[bones.size()];
        for(int i=0;i<bones.size();i++){
            var b=bones.get(i);var vs=values[i];if(old!=null)for(int ch=0;ch<3;ch++)for(int ax=0;ax<3;ax++)vs[ch][ax]=old[i][ch][ax]+(vs[ch][ax]-old[i][ch][ax])*blend;
            if(b.name.equals("control_left"))vs[1][0]+=throttle*9+steering*8;
            if(b.name.equals("control_right"))vs[1][0]+=throttle*9-steering*8;
            float[] parent=b.parent<0?new float[3]:bones.get(b.parent).origin;
            var m=b.parent<0?new Matrix4f():new Matrix4f(matrices[b.parent]);
            m.translate((b.origin[0]-parent[0]+vs[0][0])/16,(b.origin[1]-parent[1]+vs[0][1])/16,(b.origin[2]-parent[2]+vs[0][2])/16);
            m.rotateZYX((float)Math.toRadians(b.rotation[2]+vs[1][2]),(float)Math.toRadians(b.rotation[1]+vs[1][1]),(float)Math.toRadians(b.rotation[0]+vs[1][0])).scale(vs[2][0],vs[2][1],vs[2][2]);matrices[i]=m;
        }return new Pose(matrices);
    }
    public final class Pose {
        public final Matrix4f[] bones;
        Pose(Matrix4f[] bones){this.bones=bones;}
        public void transform(Matrix4f delta){for(int i=0;i<bones.length;i++)bones[i]=new Matrix4f(delta).mul(bones[i]);}
        public void replaceSubtree(int index,Matrix4f matrix){
            Matrix4f delta=new Matrix4f(matrix).mul(new Matrix4f(bones[index]).invert());
            for(int i=0;i<bones.length;i++)if(MechRig.this.descendant(i,index))bones[i]=new Matrix4f(delta).mul(bones[i]);
        }
        public Vector3f anchor(String name){var a=anchors.get(name);if(a==null)throw new IllegalArgumentException("Missing anchor "+name);var o=MechRig.this.bones.get(a.bone).origin;return bones[a.bone].transformPosition(new Vector3f((a.position[0]-o[0])/16,(a.position[1]-o[1])/16,(a.position[2]-o[2])/16));}
        public Vector3f bonePosition(String name){return bones[boneIds.get(name)].getTranslation(new Vector3f());}
    }
}
