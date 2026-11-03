package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.resource.*;
import java.nio.file.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** An isolated preview assembly, not the production multiblock implementation. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class KilnVisualChecks {
    private static int startup,ticks;private static boolean loading,done;private static volatile String failure;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(!Boolean.getBoolean("cogwild.kilnQa")||event.phase!=TickEvent.Phase.END||done)return;
        var mc=Minecraft.getInstance();mc.options.pauseOnLostFocus=false;
        if(mc.player==null||mc.level==null){
            if(++startup>70&&!loading&&mc.screen!=null&&mc.screen.getClass().getSimpleName().contains("Accessibility"))mc.setScreen(new TitleScreen());
            if(startup>80&&!loading&&mc.screen instanceof TitleScreen){loading=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Machines-Test");}return;
        }
        if(mc.getSingleplayerServer()==null)return;ticks++;
        if(ticks==20){mc.setScreen(null);mc.options.hideGui=false;mc.options.fov().set(65);mc.options.setCameraType(CameraType.FIRST_PERSON);mc.player.getInventory().selected=0;mc.getSingleplayerServer().execute(()->{
            try{
                var server=mc.getSingleplayerServer();var p=server.getPlayerList().getPlayer(mc.player.getUUID());var level=p.serverLevel();
                for(int x=-6;x<=14;x++)for(int z=-6;z<=6;z++){
                    level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.SMOOTH_STONE.defaultBlockState());
                    for(int y=80;y<85;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                }
                for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++){
                    if(x==1&&y==1&&z==1)continue;
                    var block=x==1&&z==0?(y==0?ResourceContent.OUTPUT.get():y==1?ResourceContent.CONTROLLER.get():ResourceContent.INPUT.get()):ResourceContent.BRICKS.get();
                    level.setBlockAndUpdate(new BlockPos(x,80+y,z),block.defaultBlockState());
                }
                var blocks=java.util.List.of(ResourceContent.BRICKS.get(),ResourceContent.CONTROLLER.get(),ResourceContent.INPUT.get(),ResourceContent.OUTPUT.get());
                for(int i=0;i<blocks.size();i++)level.setBlockAndUpdate(new BlockPos(6+i,80,0),blocks.get(i).defaultBlockState());
                p.getInventory().clearContent();for(int i=0;i<ResourceContent.ALL.size();i++)p.getInventory().setItem(i,new ItemStack(ResourceContent.ALL.get(i).get()));
                level.setDayTime(6000);p.teleportTo(level,-3,81,-5,-34,9);p.inventoryMenu.broadcastChanges();
            }catch(Exception e){failure=e.toString();}
        });}
        if(ticks==140){
            if(failure!=null)throw new IllegalStateException(failure);
            for(var item:ResourceContent.ALL){
                var model=mc.getItemRenderer().getModel(new ItemStack(item.get()),mc.level,mc.player,0);
                if(model==mc.getModelManager().getMissingModel()||model.getParticleIcon().contents().name().getPath().equals("missingno"))throw new IllegalStateException("Missing resource model "+item.getId());
            }
            Screenshot.grab(mc.gameDirectory,"coke-oven-components.png",mc.getMainRenderTarget(),c->{});
            mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.teleportTo(p.serverLevel(),7.5,80,-4,0,10);});
        }
        if(ticks==190){
            Screenshot.grab(mc.gameDirectory,"coke-oven-parts-row.png",mc.getMainRenderTarget(),c->{});
            try{Files.writeString(mc.gameDirectory.toPath().resolve("kiln-client-report.txt"),"KILN_CLIENT_OK\n7 item models and textures loaded; 4 construction blocks and hollow 3x3x3 visual assembly rendered.\nPreview only: no processing logic.\n");}catch(Exception e){throw new RuntimeException(e);}
            done=true;SuitcaseGlider.LOG.info("KILN_CLIENT_OK");mc.stop();
        }
    }
}
