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
        for(String kind:List.of("rolling_mill","quenching_bath","air_separator","air_compressor","industrial_press","diesel_generator")){
            var block=BLOCKS.register(kind,()->new MachineBlock(kind));MACHINES.put(kind,block);
            MACHINE_ITEMS.put(kind,SuitcaseGlider.ITEMS.register(kind,()->new MachineItem(block.get())));
        }
    }
    public static final RegistryObject<BlockEntityType<MachineBlockEntity>> TYPE=ENTITIES.register("machine",()->BlockEntityType.Builder.of(MachineBlockEntity::new,MACHINES.values().stream().map(RegistryObject::get).toArray(Block[]::new)).build(null));
    public static void register(IEventBus bus){BLOCKS.register(bus);ENTITIES.register(bus);}
}
