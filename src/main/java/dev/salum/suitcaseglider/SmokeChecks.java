package dev.salum.suitcaseglider;

import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** Only enabled explicitly by the development smoke run; never runs in a normal game. */
public final class SmokeChecks {
    private MinecraftServer tested;
    private int stop;
    private static void require(boolean value, String label) { if (!value) throw new IllegalStateException("GLIDER_SMOKE_FAILED: "+label); }
    @SubscribeEvent public void visualLogin(net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if(!Boolean.getBoolean("suitcaseglider.visual") || !(event.getEntity() instanceof ServerPlayer p))return;
        var level=p.serverLevel();for(int x=-20;x<=20;x++)for(int z=-20;z<=20;z++)level.setBlockAndUpdate(new BlockPos(x,196,z),Blocks.STONE.defaultBlockState());
        p.getServer().getPlayerList().op(p.getGameProfile());p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();p.getInventory().selected=0;p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(SuitcaseGlider.GLIDER.get()));
        p.teleportTo(level,.5,197,.5,180,0);p.setHealth(20);p.getFoodData().setFoodLevel(20);level.setDayTime(6000);level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,p.getServer());p.inventoryMenu.broadcastChanges();
    }
    @SubscribeEvent public void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("suitcaseglider.smoke")) return;
        MinecraftServer server=event.getServer();
        try {
            var level=server.overworld(); var controller=new GliderController();
            ServerPlayer p=FakePlayerFactory.get(level,new GameProfile(UUID.nameUUIDFromBytes("suitcaseglider-smoke".getBytes()),"GliderSmoke"));
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)level.setBlockAndUpdate(new BlockPos(x,196,z),Blocks.STONE.defaultBlockState());
            ItemStack stack=new ItemStack(SuitcaseGlider.GLIDER.get());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
            p.setOnGround(false);p.setPos(.5,197.9,.5);p.setDeltaMovement(0,-.1,0);p.fallDistance=0;
            controller.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));require(GliderController.phase(p)==0,"one block must stay closed");
            p.setPos(.5,204,.5);p.setDeltaMovement(0,-.3,0);p.fallDistance=.5f;
            controller.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));require(GliderController.phase(p)==1,"dangerous drop deploys");require(p.getDeltaMovement().y>=-.056,"deployment slows descent immediately");
            p.getPersistentData().putLong("sg_started",level.getGameTime()-GliderController.DEPLOY_TICKS-1);
            controller.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));require(GliderController.phase(p)==2,"deploy enters flight");
            AttackEntityEvent attack=new AttackEntityEvent(p,EntityType.COW.create(level));controller.attack(attack);require(attack.isCanceled(),"flight blocks attack");
            p.setOnGround(true);p.setPos(.5,197,.5);controller.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));require(GliderController.phase(p)==3,"landing stows");
            p.getPersistentData().putLong("sg_started",level.getGameTime()-GliderController.STOW_TICKS-1);controller.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));require(GliderController.phase(p)==0,"stow returns carry");
            ((DyeableLeatherItem)stack.getItem()).setColor(stack,0x408080);require(GliderItem.fabricColor(stack)==0x408080,"dye persists");
            var menu=new net.minecraft.world.inventory.AbstractContainerMenu(null,0){
                @Override public net.minecraft.world.item.ItemStack quickMoveStack(net.minecraft.world.entity.player.Player who,int slot){return net.minecraft.world.item.ItemStack.EMPTY;}
                @Override public boolean stillValid(net.minecraft.world.entity.player.Player who){return true;}
            };
            var grid=new net.minecraft.world.inventory.TransientCraftingContainer(menu,3,3);grid.setItem(0,new ItemStack(SuitcaseGlider.GLIDER.get()));grid.setItem(1,new ItemStack(Items.RED_DYE));
            var dyeRecipe=server.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING,grid,level);require(dyeRecipe.isPresent(),"vanilla dye crafting matches");
            ItemStack dyed=dyeRecipe.get().assemble(grid,level.registryAccess());require(dyed.getItem() instanceof DyeableLeatherItem && ((DyeableLeatherItem)dyed.getItem()).hasCustomColor(dyed),"dye crafting produces colored glider");
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(Items.STONE));controller.tick(new TickEvent.PlayerTickEvent(TickEvent.Phase.END,p));require(GliderController.phase(p)==-1,"slot change cancels");
            var recipe=server.getRecipeManager().byKey(SuitcaseGlider.id("glider"));require(recipe.isPresent(),"crafting recipe loaded");require(recipe.get().getResultItem(server.registryAccess()).is(SuitcaseGlider.GLIDER.get()),"recipe outputs glider");
            require(new ItemStack(SuitcaseGlider.GLIDER.get()).getMaxDamage()==192,"durability");
            String message="SMOKE_OK: registry, recipe, dye, one-block closed, dangerous-drop deployment, descent clamp, flight, blocked attack, grounded stow, carry return, slot cancellation.";
            Files.writeString(Path.of("smoke-report.txt"),message);SuitcaseGlider.LOG.info(message);tested=server;stop=10;
        } catch(Exception ex) { throw new RuntimeException("Suitcase Glider server smoke failed",ex); }
    }
    @SubscribeEvent public void tick(TickEvent.ServerTickEvent event) { if(tested!=null && event.phase==TickEvent.Phase.END && --stop<=0){var s=tested;tested=null;s.halt(false);} }
}
