package dev.salum.suitcaseglider.blast;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.MachineContent;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.*;
import net.minecraftforge.registries.*;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;

/** Construction modules, deliberately independent of the single-block machine catalog. */
public final class BlastContent {
    public static final Map<String,RegistryObject<Block>> BLOCKS=new LinkedHashMap<>();
    public static final List<RegistryObject<Item>> ITEMS=new ArrayList<>();
    public static final RegistryObject<Block> HEARTH=module("blast_hearth"),SHAFT=module("blast_shaft"),DUCT=module("blast_duct"),PREHEATER=module("blast_preheater"),BLOWER=module("blast_blower"),HOPPER=module("blast_hopper");
    public static final RegistryObject<Block> CONTROLLER=block("blast_controller",BlastControllerBlock::new);
    public static final RegistryObject<Block> PART=MachineContent.BLOCKS.register("blast_formed_part",BlastPartBlock::new);
    public static final RegistryObject<BlockEntityType<BlastBlockEntity>> TYPE=MachineContent.ENTITIES.register("blast_furnace",()->BlockEntityType.Builder.of(BlastBlockEntity::new,CONTROLLER.get()).build(null));
    public static final RegistryObject<Item> WIRE=item("copper_wire"),COIL=item("copper_coil"),MAGNET=item("magnet"),HOUSING=item("motor_housing"),MOTOR=item("electric_motor");
    public static final RegistryObject<Item> FLUX=item("limestone_flux"),PIG_IRON=item("pig_iron_ingot"),SLAG=item("blast_slag");
    public static final RegistryObject<Block> LIMESTONE=block("limestone",()->new Block(Block.Properties.of().strength(1.5f,6).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> SLAG_BRICKS=block("slag_bricks",()->new Block(Block.Properties.of().strength(3,6).sound(SoundType.STONE).requiresCorrectToolForDrops()));
    public static final RegistryObject<Block> PIG_IRON_BLOCK=block("pig_iron_block",()->new Block(Block.Properties.of().strength(5,6).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<Item> STEEL=item("steel_ingot");
    public static final RegistryObject<Block> STEEL_BLOCK=block("steel_block",()->new Block(Block.Properties.of().strength(5,6).sound(SoundType.METAL).requiresCorrectToolForDrops()));
    public static final RegistryObject<BlockEntityType<BlastPortBlockEntity>> PORT_TYPE=MachineContent.ENTITIES.register("blast_port",()->BlockEntityType.Builder.of(BlastPortBlockEntity::new,PART.get()).build(null));
    private static final DeferredRegister<MenuType<?>> MENUS=DeferredRegister.create(ForgeRegistries.MENU_TYPES,SuitcaseGlider.ID);
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS=DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS,SuitcaseGlider.ID);
    private static final DeferredRegister<RecipeType<?>> TYPES=DeferredRegister.create(ForgeRegistries.RECIPE_TYPES,SuitcaseGlider.ID);
    public static final RegistryObject<MenuType<BlastMenu>> MENU=MENUS.register("blast_furnace",()->IForgeMenuType.create(BlastMenu::new));
    public static final RegistryObject<RecipeType<BlastRecipe>> RECIPE_TYPE=TYPES.register("blast_smelting",()->new RecipeType<>(){public String toString(){return SuitcaseGlider.ID+":blast_smelting";}});
    public static final RegistryObject<RecipeSerializer<BlastRecipe>> SERIALIZER=SERIALIZERS.register("blast_smelting",BlastRecipe.Serializer::new);
    private static RegistryObject<Item> item(String id){var r=SuitcaseGlider.ITEMS.register(id,()->new Item(new Item.Properties()));ITEMS.add(r);return r;}
    private static RegistryObject<Block> module(String id){return block(id,BlastModuleBlock::new);}
    private static RegistryObject<Block> block(String id,java.util.function.Supplier<Block> supplier){var r=MachineContent.BLOCKS.register(id,supplier);BLOCKS.put(id,r);ITEMS.add(SuitcaseGlider.ITEMS.register(id,()->new BlockItem(r.get(),new Item.Properties())));return r;}
    public static void init(){}
    public static void register(IEventBus bus){ITEMS.removeIf(r->r.getId().getPath().startsWith("pig_iron"));MENUS.register(bus);SERIALIZERS.register(bus);TYPES.register(bus);}
}
