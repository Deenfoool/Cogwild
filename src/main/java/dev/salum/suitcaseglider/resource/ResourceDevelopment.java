package dev.salum.suitcaseglider.resource;

import dev.salum.suitcaseglider.SuitcaseGlider;
import java.nio.file.*;
import java.util.*;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class ResourceDevelopment {
    private static net.minecraft.server.MinecraftServer stop;
    private static int countdown;
    private static final List<String> report=new ArrayList<>();
    private static void check(boolean value,String name){if(!value)throw new IllegalStateException("RESOURCE_SMOKE_FAILED "+name);report.add(name+"=true");}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("cogwild.resourceSmoke"))return;
        var server=event.getServer();var level=server.overworld();var recipes=server.getRecipeManager();
        for(var item:ResourceContent.ALL)check(new ItemStack(item.get()).getMaxStackSize()==64,"registered stackable item "+item.getId());
        var player=FakePlayerFactory.getMinecraft(level);
        var grid=new TransientCraftingContainer(player.inventoryMenu,3,3);
        for(var sand:List.of(Items.SAND,Items.RED_SAND)){
            for(int i=0;i<4;i++)grid.setItem(i,new ItemStack(Items.CLAY_BALL));
            for(int i=4;i<8;i++)grid.setItem(i,new ItemStack(sand));grid.setItem(8,new ItemStack(Items.GRAVEL));
            var recipe=recipes.getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow();var output=recipe.assemble(grid,server.registryAccess());
            check(output.is(ResourceContent.UNFIRED_BRICK.get())&&output.getCount()==8,"eight unfired bricks with "+sand);
        }
        grid.setItem(8,ItemStack.EMPTY);check(recipes.getRecipeFor(RecipeType.CRAFTING,grid,level).isEmpty(),"missing gravel cannot craft bricks");
        var input=new SimpleContainer(new ItemStack(ResourceContent.UNFIRED_BRICK.get()));
        var firing=recipes.getRecipeFor(RecipeType.SMELTING,input,level).orElseThrow();
        check(firing.assemble(input,server.registryAccess()).is(ResourceContent.REFRACTORY_BRICK.get()),"firing yields refractory brick");
        check(firing.getCookingTime()==200&&Math.abs(firing.getExperience()-.1f)<.001,"firing duration and experience");
        check(ForgeHooks.getBurnTime(new ItemStack(ResourceContent.COKE.get()),RecipeType.SMELTING)==3200,"coke fuel duration");
        for(var item:List.of(ResourceContent.UNFIRED_BRICK,ResourceContent.REFRACTORY_BRICK))check(ForgeHooks.getBurnTime(new ItemStack(item.get()),RecipeType.SMELTING)==0,"brick is not fuel "+item.getId());
        check(recipes.getRecipeFor(RecipeType.SMELTING,new SimpleContainer(new ItemStack(Items.COAL)),level).isEmpty(),"ordinary furnace cannot produce coke");
        Files.write(Path.of("resource-smoke-report.txt"),report);SuitcaseGlider.LOG.info("RESOURCE_SMOKE_OK {} checks",report.size());stop=server;countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(stop!=null&&e.phase==TickEvent.Phase.END&&--countdown<=0){var server=stop;stop=null;server.halt(false);}}
}
