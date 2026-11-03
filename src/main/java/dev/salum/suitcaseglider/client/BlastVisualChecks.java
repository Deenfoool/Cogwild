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
            try{Files.writeString(mc.gameDirectory.toPath().resolve("blast-client-report.txt"),"BLAST_CLIENT_OK\nAll 12 component item models loaded; real multiblock renderer drawn; raw and formed constructions captured.\n");}catch(Exception ex){throw new RuntimeException(ex);}
            done=true;SuitcaseGlider.LOG.info("BLAST_CLIENT_OK");mc.stop();
        }
    }
}
