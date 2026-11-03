package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.blast.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Separate test world; does not touch the player's regular save. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class BlastVisualChecks {
    private static int startup,ticks;private static boolean loading,done;private static volatile String failure;
    static boolean rendered;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("cogwild.blastQa")||e.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){
            if(++startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());
            if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Machines-Test");}return;
        }
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){mc.setScreen(null);mc.options.hideGui=true;mc.options.fov().set(55);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.getSingleplayerServer().execute(()->{
            try{
                var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayer(mc.player.getUUID());var l=p.serverLevel();
                l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false,server);l.setWeatherParameters(6000,0,false,false);
                for(int x=-5;x<=16;x++)for(int z=-6;z<=8;z++){
                    l.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.SMOOTH_STONE.defaultBlockState());
                    for(int y=80;y<88;y++)l.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                }
                BlastDevelopment.build(l,new BlockPos(0,80,0),Direction.NORTH);
                var b=BlastDevelopment.build(l,new BlockPos(6,80,0),Direction.NORTH);if(!b.form(p))throw new IllegalStateException("Furnace QA formation failed");
                p.teleportTo(l,11,82,-11,27,6);p.setNoGravity(true);
            }catch(Exception ex){failure=ex.toString();}
        });}
        if(ticks==140){
            if(failure!=null)throw new IllegalStateException(failure);
            for(var item:BlastContent.ITEMS){var model=mc.getItemRenderer().getModel(new ItemStack(item.get()),mc.level,mc.player,0);if(model==mc.getModelManager().getMissingModel()||model.getParticleIcon().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing blast item model "+item.getId());}
            if(!rendered)throw new IllegalStateException("Furnace renderer never drawn");
            Screenshot.grab(mc.gameDirectory,"blast-furnace-assembly.png",mc.getMainRenderTarget(),c->{});
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.teleportTo(p.serverLevel(),12,82,-8,28,8);});
        }
        if(ticks==200){
            Screenshot.grab(mc.gameDirectory,"blast-furnace-formed.png",mc.getMainRenderTarget(),c->{});
            mc.getSingleplayerServer().execute(()->{
                try{
                    var l=mc.getSingleplayerServer().overworld();var b=(BlastBlockEntity)l.getBlockEntity(new BlockPos(6,80,0));
                    b.inventory.setStackInSlot(0,new ItemStack(net.minecraft.world.item.Items.RAW_IRON,3));b.inventory.setStackInSlot(1,new ItemStack(dev.salum.suitcaseglider.resource.ResourceContent.COKE.get(),3));b.inventory.setStackInSlot(2,new ItemStack(BlastContent.FLUX.get(),3));
                    // Load a hot saved fixture; real warmup is covered by dedicated-server tests.
                    var fixture=b.saveWithoutMetadata();fixture.putInt("Temperature",1200);b.load(fixture);b.receiveEnergy(32,false);b.tick();
                    fixture=b.saveWithoutMetadata();fixture.putInt("Progress",210);b.load(fixture);b.receiveEnergy(32,false);b.tick();
                    var gen=new BlockPos(12,80,2);l.setBlockAndUpdate(gen,dev.salum.suitcaseglider.cable.CableContent.GENERATOR.get().defaultBlockState().setValue(dev.salum.suitcaseglider.cable.EnergyNodeBlock.FACING,Direction.WEST));
                    var source=(dev.salum.suitcaseglider.cable.EnergyNodeEntity)l.getBlockEntity(gen);source.stored=32000;source.fuelTicks=32000;
                    var cables=dev.salum.suitcaseglider.cable.CableWorld.get(l);for(var link:java.util.List.copyOf(cables.links))if(link.a().equals(gen)||link.b().equals(gen)||link.a().equals(b.getBlockPos())||link.b().equals(b.getBlockPos()))cables.remove(l,link,false);
                    var path=cables.route(l,gen,b.getBlockPos(),false);if(path.isEmpty())throw new IllegalStateException("QA power cable route failed");cables.add(new dev.salum.suitcaseglider.cable.CableWorld.Link(java.util.UUID.randomUUID(),gen,b.getBlockPos(),false,3,path));dev.salum.suitcaseglider.cable.CableNetwork.broadcast(l);
                    if(!b.active()||!b.hot()||b.data.get(0)<100)throw new IllegalStateException("Production fixture failed");
                }catch(Exception ex){failure=ex.toString();}
            });
        }
        if(ticks==240){if(failure!=null)throw new IllegalStateException(failure);var b=(BlastBlockEntity)mc.level.getBlockEntity(new BlockPos(6,80,0));if(!b.active()||!b.hot())throw new IllegalStateException("Visual state not synchronized");Screenshot.grab(mc.gameDirectory,"blast-furnace-processing.png",mc.getMainRenderTarget(),c->{});mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.teleportTo(p.serverLevel(),9,81,-2,35,10);var b2=(BlastBlockEntity)p.serverLevel().getBlockEntity(new BlockPos(6,80,0));net.minecraftforge.network.NetworkHooks.openScreen(p,b2,b2.getBlockPos());
        });}
        if(ticks==280){
            if(!(mc.screen instanceof BlastScreen)||!(mc.player.containerMenu instanceof BlastMenu m)||m.furnace.data.get(3)<1200||m.furnace.data.get(0)<=0)throw new IllegalStateException("Blast menu or live data sync failed");
            if(mc.screen.children().stream().anyMatch(w->w instanceof net.minecraft.client.gui.components.Button))throw new IllegalStateException("Unexpected manual start button");
            Screenshot.grab(mc.gameDirectory,"blast-furnace-gui.png",mc.getMainRenderTarget(),c->{});
            try{Files.writeString(mc.gameDirectory.toPath().resolve("blast-client-report.txt"),"BLAST_CLIENT_OK\nAll "+BlastContent.ITEMS.size()+" component/material item models loaded; live heat, progress and energy synchronized; processing animation and compact automatic GUI captured.\n");}catch(Exception ex){throw new RuntimeException(ex);}
            done=true;SuitcaseGlider.LOG.info("BLAST_CLIENT_OK");mc.level.disconnect();mc.clearLevel(new TitleScreen());mc.stop();
        }
    }
}
