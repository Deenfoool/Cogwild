package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.kiln.*;
import net.minecraft.client.gui.GuiGraphics;
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
    private static final ResourceLocation TEXTURE=new ResourceLocation("minecraft","textures/gui/container/furnace.png");
    public KilnScreen(KilnMenu menu,Inventory inv,Component title){super(menu,inv,title);imageHeight=166;inventoryLabelY=72;}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(KilnContent.MENU.get(),KilnScreen::new));}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        // The coke oven has no separate fuel slot: cover the vanilla fuel well.
        g.fill(leftPos+55,topPos+52,leftPos+73,topPos+70,0xffc6c6c6);
        int duration=Math.max(1,menu.kiln.data.get(1)),progress=menu.kiln.data.get(0),w=Math.min(24,24*progress/duration);
        if(menu.kiln.data.get(2)==KilnBlockEntity.RUNNING)g.blit(TEXTURE,leftPos+56,topPos+36,176,0,14,14);
        if(w>0)g.blit(TEXTURE,leftPos+79,topPos+34,176,14,w+1,16);
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        if(isHovering(79,34,24,16,mouseX,mouseY)||isHovering(56,36,14,14,mouseX,mouseY))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.kiln.state."+menu.kiln.data.get(2)).append(" | "+menu.kiln.data.get(0)/20+" / "+Math.max(1,menu.kiln.data.get(1))/20+" s"),mouseX,mouseY);
    }
}
