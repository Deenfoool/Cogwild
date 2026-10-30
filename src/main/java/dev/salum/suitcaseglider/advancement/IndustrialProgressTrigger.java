package dev.salum.suitcaseglider.advancement;

import com.google.gson.*;
import net.minecraft.advancements.critereon.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;

public final class IndustrialProgressTrigger extends SimpleCriterionTrigger<IndustrialProgressTrigger.Instance> {
    public static final ResourceLocation ID=new ResourceLocation("cogwild","industrial_progress");
    @Override public ResourceLocation getId(){return ID;}
    @Override protected Instance createInstance(JsonObject json,ContextAwarePredicate player,DeserializationContext context){
        String action=GsonHelper.getAsString(json,"action");long minimum=GsonHelper.getAsLong(json,"minimum",1);
        if(!IndustrialProgress.Action.valid(action)||minimum<1)throw new JsonSyntaxException("Invalid Cogwild progress condition: "+action);
        return new Instance(player,action,minimum);
    }
    public void trigger(ServerPlayer player,String action,long total){trigger(player,condition->condition.action.equals(action)&&total>=condition.minimum);}
    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final String action;private final long minimum;
        public Instance(ContextAwarePredicate player,String action,long minimum){super(ID,player);this.action=action;this.minimum=minimum;}
        @Override public JsonObject serializeToJson(SerializationContext context){var json=super.serializeToJson(context);json.addProperty("action",action);json.addProperty("minimum",minimum);return json;}
    }
}
