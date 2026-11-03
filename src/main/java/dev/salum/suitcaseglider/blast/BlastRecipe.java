package dev.salum.suitcaseglider.blast;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;

/** Ordered charge: iron-bearing input, coke, limestone flux. Reloadable and synced by vanilla recipes. */
public record BlastRecipe(ResourceLocation id,Ingredient ore,Ingredient coke,Ingredient flux,ItemStack iron,ItemStack slag,int duration,int temperature,int energy) implements Recipe<SimpleContainer> {
    public BlastRecipe {
        if(iron.isEmpty()||slag.isEmpty()||iron.getCount()>64||slag.getCount()>64||duration<1||duration>32000||temperature<21||temperature>1400||energy<1||energy>1000)throw new IllegalArgumentException("Invalid blast furnace recipe "+id);
    }
    public boolean accepts(int slot,ItemStack stack){return switch(slot){case 0->ore.test(stack);case 1->coke.test(stack);case 2->flux.test(stack);default->false;};}
    @Override public boolean matches(SimpleContainer c,Level l){return c.getContainerSize()>=3&&accepts(0,c.getItem(0))&&accepts(1,c.getItem(1))&&accepts(2,c.getItem(2));}
    @Override public ItemStack assemble(SimpleContainer c,RegistryAccess a){return iron.copy();}
    @Override public boolean canCraftInDimensions(int w,int h){return w*h>=3;}
    @Override public ItemStack getResultItem(RegistryAccess a){return iron.copy();}
    @Override public ResourceLocation getId(){return id;}
    @Override public RecipeSerializer<?> getSerializer(){return BlastContent.SERIALIZER.get();}
    @Override public RecipeType<?> getType(){return BlastContent.RECIPE_TYPE.get();}
    @Override public boolean isSpecial(){return true;}
    public static final class Serializer implements RecipeSerializer<BlastRecipe> {
        @Override public BlastRecipe fromJson(ResourceLocation id,JsonObject j){return new BlastRecipe(id,Ingredient.fromJson(j.get("ore")),Ingredient.fromJson(j.get("coke")),Ingredient.fromJson(j.get("flux")),ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(j,"result")),ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(j,"slag")),GsonHelper.getAsInt(j,"duration",1200),GsonHelper.getAsInt(j,"temperature",1200),GsonHelper.getAsInt(j,"energy",32));}
        @Override public BlastRecipe fromNetwork(ResourceLocation id,FriendlyByteBuf b){return new BlastRecipe(id,Ingredient.fromNetwork(b),Ingredient.fromNetwork(b),Ingredient.fromNetwork(b),b.readItem(),b.readItem(),b.readVarInt(),b.readVarInt(),b.readVarInt());}
        @Override public void toNetwork(FriendlyByteBuf b,BlastRecipe r){r.ore.toNetwork(b);r.coke.toNetwork(b);r.flux.toNetwork(b);b.writeItem(r.iron);b.writeItem(r.slag);b.writeVarInt(r.duration);b.writeVarInt(r.temperature);b.writeVarInt(r.energy);}
    }
}
