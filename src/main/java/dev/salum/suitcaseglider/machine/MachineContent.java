package dev.salum.suitcaseglider.machine;

import dev.salum.suitcaseglider.SuitcaseGlider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.*;
import java.util.*;

public final class MachineContent {
    public static final DeferredRegister<Block> BLOCKS=DeferredRegister.create(ForgeRegistries.BLOCKS,SuitcaseGlider.ID);
    public static final DeferredRegister<BlockEntityType<?>> ENTITIES=DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES,SuitcaseGlider.ID);
    public static final RegistryObject<MachineBlock> LATHE=BLOCKS.register("lathe",()->new MachineBlock("lathe"));
    public static final RegistryObject<MachineBlock> CRUSHER=BLOCKS.register("crusher",()->new MachineBlock("crusher"));
    public static final RegistryObject<Item> LATHE_ITEM=SuitcaseGlider.ITEMS.register("lathe",()->new MachineItem(LATHE.get()));
    public static final RegistryObject<Item> CRUSHER_ITEM=SuitcaseGlider.ITEMS.register("crusher",()->new MachineItem(CRUSHER.get()));
    public static final Map<String,RegistryObject<MachineBlock>> MACHINES=new LinkedHashMap<>();
    public static final Map<String,RegistryObject<Item>> MACHINE_ITEMS=new LinkedHashMap<>();
    static {
        MACHINES.put("lathe",LATHE);MACHINES.put("crusher",CRUSHER);
        MACHINE_ITEMS.put("lathe",LATHE_ITEM);MACHINE_ITEMS.put("crusher",CRUSHER_ITEM);
        for(String kind:List.of("rolling_mill","quenching_bath","air_separator","air_compressor","industrial_press","diesel_generator","fermenter","universal_mixer")){
            var block=BLOCKS.register(kind,()->new MachineBlock(kind));MACHINES.put(kind,block);
            MACHINE_ITEMS.put(kind,SuitcaseGlider.ITEMS.register(kind,()->new MachineItem(block.get())));
        }
    }
    public static final RegistryObject<BlockEntityType<MachineBlockEntity>> TYPE=ENTITIES.register("machine",()->BlockEntityType.Builder.of(MachineBlockEntity::new,MACHINES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,SuitcaseGlider.ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,SuitcaseGlider.ID);
    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeType<?>> RECIPES=DeferredRegister.create(ForgeRegistries.RECIPE_TYPES,SuitcaseGlider.ID);
    public static final RegistryObject<net.minecraft.world.inventory.MenuType<MachineMenu>> MENU=MENUS.register("machine",()->net.minecraftforge.common.extensions.IForgeMenuType.create(MachineMenu::new));
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeType<MachineRecipe>> PROCESSING_TYPE=RECIPES.register("machine_processing",()->new net.minecraft.world.item.crafting.RecipeType<>(){});
    public static final RegistryObject<net.minecraft.world.item.crafting.RecipeSerializer<MachineRecipe>> PROCESSING_SERIALIZER=SERIALIZERS.register("machine_processing",MachineRecipe.Serializer::new);
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);MENUS.register(bus);SERIALIZERS.register(bus);RECIPES.register(bus);}
}
