package dev.salum.suitcaseglider.client;
import dev.salum.suitcaseglider.SuitcaseGlider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,value=Dist.CLIENT)
public final class MechDevClient {
    private static int ticks;private static boolean connected;
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent e){
        if(!Boolean.getBoolean("mechant.devclient")||e.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();if(++ticks>80&&!connected&&mc.screen instanceof TitleScreen){connected=true;mc.createWorldOpenFlows().loadLevel(new TitleScreen(),"Mech-Ant-Test");}
    }
}
