package dev.salum.suitcaseglider.blast;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.MachineContent;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

/** Construction modules, deliberately independent of the single-block machine catalog. */
public final class BlastContent {
    public static final Map<String,RegistryObject<Block>> BLOCKS=new LinkedHashMap<>();
    public static final List<RegistryObject<Item>> ITEMS=new ArrayList<>();
    public static final RegistryObject<Block> HEARTH=module("blast_hearth"),SHAFT=module("blast_shaft"),DUCT=module("blast_duct"),PREHEATER=module("blast_preheater"),BLOWER=module("blast_blower"),HOPPER=module("blast_hopper");
    public static final RegistryObject<Block> CONTROLLER=block("blast_controller",BlastControllerBlock::new);
    public static final RegistryObject<Block> PART=MachineContent.BLOCKS.register("blast_formed_part",BlastPartBlock::new);
    public static final RegistryObject<BlockEntityType<BlastBlockEntity>> TYPE=MachineContent.ENTITIES.register("blast_furnace",()->BlockEntityType.Builder.of(BlastBlockEntity::new,CONTROLLER.get()).build(null));
    public static final RegistryObject<Item> WIRE=item("copper_wire"),COIL=item("copper_coil"),MAGNET=item("magnet"),HOUSING=item("motor_housing"),MOTOR=item("electric_motor");
    private static RegistryObject<Item> item(String id){var r=SuitcaseGlider.ITEMS.register(id,()->new Item(new Item.Properties()));ITEMS.add(r);return r;}
    private static RegistryObject<Block> module(String id){return block(id,BlastModuleBlock::new);}
    private static RegistryObject<Block> block(String id,java.util.function.Supplier<Block> supplier){var r=MachineContent.BLOCKS.register(id,supplier);BLOCKS.put(id,r);ITEMS.add(SuitcaseGlider.ITEMS.register(id,()->new BlockItem(r.get(),new Item.Properties())));return r;}
    public static void init(){}
}
