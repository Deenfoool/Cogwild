package dev.salum.suitcaseglider.machine;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Small, server-safe layout manifest baked from the same models as the client rigs. */
public final class MachineCatalog {
    public record Spec(int width,float height,float[] offset,float[] power,String off,String on,String start,String stop) {
        public int layers(){return (int)Math.ceil(height/16);}
    }
    public static final Map<String,Spec> ALL=load();
    private static float[] vector(JsonArray a){return new float[]{a.get(0).getAsFloat(),a.get(1).getAsFloat(),a.get(2).getAsFloat()};}
    private static Map<String,Spec> load(){
        try(var in=MachineCatalog.class.getResourceAsStream("/assets/suitcaseglider/rigs/machines.json");var reader=new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)){
            Map<String,Spec> result=new LinkedHashMap<>();
            for(var e:JsonParser.parseReader(reader).getAsJsonObject().entrySet()){
                var j=e.getValue().getAsJsonObject();result.put(e.getKey(),new Spec(j.get("width").getAsInt(),j.get("height").getAsFloat(),vector(j.getAsJsonArray("offset")),vector(j.getAsJsonArray("power")),j.get("off").getAsString(),j.get("on").getAsString(),j.get("start").getAsString(),j.get("stop").getAsString()));
            }return Collections.unmodifiableMap(result);
        }catch(Exception e){throw new IllegalStateException("Cannot load machine layouts",e);}
    }
    public static Spec get(String kind){return Objects.requireNonNull(ALL.get(kind),kind);}
}
