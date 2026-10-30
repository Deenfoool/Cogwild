package dev.salum.suitcaseglider.advancement;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdvancementCatalogTest {
    private JsonObject json(String path)throws Exception{try(var stream=getClass().getResourceAsStream(path);var reader=new InputStreamReader(Objects.requireNonNull(stream),StandardCharsets.UTF_8)){return JsonParser.parseReader(reader).getAsJsonObject();}}
    @Test void allNotionEntriesKeepGraphRewardsVisibilityAndRealIcons()throws Exception{
        var entries=AdvancementCatalog.entries();assertEquals(24,entries.size());assertEquals(535,entries.stream().mapToInt(AdvancementCatalog.Entry::experience).sum());assertEquals(2,entries.stream().filter(AdvancementCatalog.Entry::hidden).count());
        Map<String,AdvancementCatalog.Entry> byId=new HashMap<>();for(var entry:entries)assertNull(byId.put(entry.id(),entry));
        for(var entry:entries){
            assertTrue(entry.id().matches("cogwild:[a-z0-9_]+"));Set<String> ancestors=new HashSet<>();var cursor=entry;
            while(cursor!=null){assertTrue(ancestors.add(cursor.id()),"Cycle at "+entry.id());if(cursor.parent()==null)break;assertTrue(byId.containsKey(cursor.parent()));cursor=byId.get(cursor.parent());}
            String name=entry.id().split(":")[1];var advancement=json("/data/cogwild/advancements/"+name+".json");var display=advancement.getAsJsonObject("display");
            assertEquals(entry.experience(),advancement.getAsJsonObject("rewards").get("experience").getAsInt());assertEquals(entry.hidden(),display.get("hidden").getAsBoolean());assertEquals(entry.frame(),display.get("frame").getAsString());
            if(entry.parent()!=null)assertEquals(entry.parent(),advancement.get("parent").getAsString());
            assertEquals("suitcaseglider:advancement_icon_"+name,display.getAsJsonObject("icon").get("item").getAsString());
            try(var png=getClass().getResourceAsStream("/assets/suitcaseglider/textures/item/advancement/"+name+".png")){var image=ImageIO.read(Objects.requireNonNull(png));assertNotNull(image);assertTrue(image.getWidth()>16);}
            var model=json("/assets/suitcaseglider/models/item/advancement_icon_"+name+".json");assertEquals("suitcaseglider:item/advancement/"+name,model.getAsJsonObject("textures").get("layer0").getAsString());
        }
    }
    @Test void criteriaKeepProductionEventsAndAndOrSemantics()throws Exception{
        for(var entry:AdvancementCatalog.entries()){
            var advancement=json("/data/cogwild/advancements/"+entry.id().split(":")[1]+".json");var criteria=advancement.getAsJsonObject("criteria");
            for(var value:criteria.entrySet()){
                var criterion=value.getValue().getAsJsonObject();String trigger=criterion.get("trigger").getAsString();assertNotEquals("minecraft:impossible",trigger);
                if(trigger.equals("cogwild:industrial_progress")){var conditions=criterion.getAsJsonObject("conditions");assertTrue(IndustrialProgress.Action.valid(conditions.get("action").getAsString()));assertTrue(conditions.get("minimum").getAsLong()>0);}
            }
            for(var group:advancement.getAsJsonArray("requirements")){assertFalse(group.getAsJsonArray().isEmpty());for(var key:group.getAsJsonArray())assertTrue(criteria.has(key.getAsString()));}
        }
        var blueprint=json("/data/cogwild/advancements/first_blueprint.json");assertEquals(1,blueprint.getAsJsonArray("requirements").size());assertEquals(2,blueprint.getAsJsonArray("requirements").get(0).getAsJsonArray().size());
        var rare=json("/data/cogwild/advancements/rare_ores.json");assertEquals(2,rare.getAsJsonArray("requirements").size());
        var menagerie=json("/data/cogwild/advancements/mechanical_menagerie.json");assertEquals(3,menagerie.getAsJsonArray("requirements").size());
        var diesel=json("/data/cogwild/advancements/diesel.json");assertEquals(1000,diesel.getAsJsonObject("criteria").getAsJsonObject("complete").getAsJsonObject("conditions").get("minimum").getAsInt());
    }
}
