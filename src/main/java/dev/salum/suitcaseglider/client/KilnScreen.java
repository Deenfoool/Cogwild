package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.kiln.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID,bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class KilnScreen extends AbstractContainerScreen<KilnMenu> {
    private static final ResourceLocation TEXTURE=SuitcaseGlider.id("textures/gui/coke_oven.png");
    private Button start;
    public KilnScreen(KilnMenu menu,Inventory inv,Component title){super(menu,inv,title);imageHeight=190;inventoryLabelY=96;}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(KilnContent.MENU.get(),KilnScreen::new));}
    @Override protected void init(){super.init();start=addRenderableWidget(Button.builder(label(),b->minecraft.gameMode.handleInventoryButtonClick(menu.containerId,0)).bounds(leftPos+52,topPos+72,72,20).build());}
    private Component label(){return Component.translatable(menu.kiln.data.get(3)!=0?"gui.suitcaseglider.kiln.stop":"gui.suitcaseglider.kiln.start");}
    @Override protected void containerTick(){super.containerTick();start.setMessage(label());start.active=menu.kiln.data.get(4)!=0;}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int duration=Math.max(1,menu.kiln.data.get(1)),progress=menu.kiln.data.get(0),w=Math.min(24,24*progress/duration);
        g.fill(leftPos+76,topPos+44,leftPos+100,topPos+50,0xff626665);if(w>0)g.fill(leftPos+76,topPos+44,leftPos+76+w,topPos+50,0xffd79a37);
    }
    @Override protected void renderLabels(GuiGraphics g,int mouseX,int mouseY){
        super.renderLabels(g,mouseX,mouseY);
        var status=Component.translatable("gui.suitcaseglider.kiln.state."+menu.kiln.data.get(2));g.drawString(font,status,8,22,0xff394341,false);
        g.drawCenteredString(font,Component.literal(menu.kiln.data.get(0)/20+" / "+Math.max(1,menu.kiln.data.get(1))/20+" s"),88,60,0xff4d4540);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);}
}
