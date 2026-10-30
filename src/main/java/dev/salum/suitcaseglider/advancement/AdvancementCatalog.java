package dev.salum.suitcaseglider.advancement;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class AdvancementCatalog {
    public record Entry(String id,String title,String description,String condition,String parent,String frame,boolean hidden,int experience){}
    private static final List<Entry> ENTRIES=load();
    private static List<Entry> load(){
        try(var in=AdvancementCatalog.class.getResourceAsStream("/data/cogwild/advancement_catalog.json");var reader=new InputStreamReader(Objects.requireNonNull(in),StandardCharsets.UTF_8)){
            List<Entry> entries=new ArrayList<>();for(var value:JsonParser.parseReader(reader).getAsJsonArray()){
                var a=value.getAsJsonObject();entries.add(new Entry(a.get("id").getAsString(),a.get("title").getAsString(),a.get("description").getAsString(),a.get("condition").getAsString(),a.get("parent").isJsonNull()?null:a.get("parent").getAsString(),a.get("frame").getAsString(),a.get("hidden").getAsBoolean(),a.get("experience").getAsInt()));
            }return List.copyOf(entries);
        }catch(IOException ex){throw new IllegalStateException("Cannot load Cogwild advancement catalog",ex);}
    }
    public static List<Entry> entries(){return ENTRIES;}
}
