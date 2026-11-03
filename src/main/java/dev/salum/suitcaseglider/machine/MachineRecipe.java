package dev.salum.suitcaseglider.machine;

import com.google.gson.*;
import java.util.*;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Ordered, reloadable processing recipes. Outputs are committed together, never partially. */
public record MachineRecipe(ResourceLocation id,String machine,List<Ingredient> inputs,List<ItemStack> results,int duration,int energy) implements Recipe<SimpleContainer> {
    public MachineRecipe {
        var ui=MachineUiSpec.get(machine);inputs=List.copyOf(inputs);results=results.stream().map(ItemStack::copy).toList();
        if(inputs.isEmpty()||inputs.size()>ui.inputs()||results.isEmpty()||results.size()>ui.outputs()||duration<1||duration>32000||energy<1||energy>4000||results.stream().anyMatch(s->s.isEmpty()||s.getCount()>s.getMaxStackSize()))throw new IllegalArgumentException("Invalid machine recipe "+id);
    }
    @Override public boolean matches(SimpleContainer c,Level l){if(c.getContainerSize()<3)return false;for(int i=0;i<3;i++){if(i<inputs.size()){if(!inputs.get(i).test(c.getItem(i)))return false;}else if(!c.getItem(i).isEmpty())return false;}return true;}
    @Override public ItemStack assemble(SimpleContainer c,RegistryAccess a){return results.get(0).copy();}
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=inputs.size();}
    @Override public ItemStack getResultItem(RegistryAccess a){return results.get(0).copy();}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return MachineContent.PROCESSING_SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return MachineContent.PROCESSING_TYPE.get();}
    @Override public boolean isSpecial(){return true;}
    public static final class Serializer implements RecipeSerializer<MachineRecipe> {
        @Override public MachineRecipe fromJson(ResourceLocation id,JsonObject j){
            List<Ingredient> inputs=new ArrayList<>();for(var e:GsonHelper.getAsJsonArray(j,"ingredients"))inputs.add(Ingredient.fromJson(e));
            List<ItemStack> outputs=new ArrayList<>();for(var e:GsonHelper.getAsJsonArray(j,"results"))outputs.add(ShapedRecipe.itemStackFromJson(e.getAsJsonObject()));
            return new MachineRecipe(id,GsonHelper.getAsString(j,"machine"),inputs,outputs,GsonHelper.getAsInt(j,"duration",100),GsonHelper.getAsInt(j,"energy",8));
        }
        @Override public MachineRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){
            String machine=b.readUtf(64);int n=b.readVarInt();if(n<1||n>3)throw new IllegalArgumentException("Machine ingredients");
            List<Ingredient> inputs=new ArrayList<>();for(int i=0;i<n;i++)inputs.add(Ingredient.fromNetwork(b));n=b.readVarInt();if(n<1||n>2)throw new IllegalArgumentException("Machine outputs");
            List<ItemStack> outputs=new ArrayList<>();for(int i=0;i<n;i++)outputs.add(b.readItem());return new MachineRecipe(id,machine,inputs,outputs,b.readVarInt(),b.readVarInt());
        }
        @Override public void toNetwork(FriendlyByteBuf b,MachineRecipe r){b.writeUtf(r.machine);b.writeVarInt(r.inputs.size());r.inputs.forEach(i->i.toNetwork(b));b.writeVarInt(r.results.size());r.results.forEach(b::writeItem);b.writeVarInt(r.duration);b.writeVarInt(r.energy);}
    }
}
