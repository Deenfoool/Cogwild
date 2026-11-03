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
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->{MenuScreens.register(KilnContent.MENU.get(),KilnScreen::new);net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(KilnFluids.SOURCE.get(),net.minecraft.client.renderer.RenderType.translucent());net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(KilnFluids.FLOWING.get(),net.minecraft.client.renderer.RenderType.translucent());});}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(TEXTURE,leftPos,topPos,0,0,imageWidth,imageHeight);
        int duration=Math.max(1,menu.kiln.data.get(1)),progress=menu.kiln.data.get(0),w=Math.min(24,24*progress/duration);
        if(menu.kiln.data.get(2)==KilnBlockEntity.RUNNING)g.blit(TEXTURE,leftPos+56,topPos+36,176,0,14,14);
        if(w>0)g.blit(TEXTURE,leftPos+79,topPos+34,176,14,w+1,16);
        g.fill(leftPos+148,topPos+16,leftPos+164,topPos+70,0xff4a4036);
        g.fill(leftPos+149,topPos+17,leftPos+163,topPos+69,0xff211e18);
        int amount=menu.kiln.data.get(5),height=amount==0?0:Math.max(1,52*amount/16000);
        if(height>0){g.fill(leftPos+149,topPos+69-height,leftPos+163,topPos+69,0xff79461d);g.fill(leftPos+150,topPos+69-height,leftPos+152,topPos+69,0xffa37138);}
        for(int i=1;i<4;i++)g.fill(leftPos+159,topPos+17+i*13,leftPos+163,topPos+18+i*13,0xffb3a48b);
    }

    @Override public boolean mouseClicked(double x,double y,int button){
        if((button==0||button==1)&&isHovering(148,16,16,54,x,y)&&menu.getCarried().is(net.minecraft.world.item.Items.BUCKET)){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true;}
        return super.mouseClicked(x,y,button);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        if(isHovering(148,16,16,54,mouseX,mouseY))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.kiln.creosote",menu.kiln.data.get(5),16000),mouseX,mouseY);
        if(isHovering(79,34,24,16,mouseX,mouseY)||isHovering(56,36,14,14,mouseX,mouseY))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.kiln.state."+menu.kiln.data.get(2)).append(" | "+menu.kiln.data.get(0)/20+" / "+Math.max(1,menu.kiln.data.get(1))/20+" s"),mouseX,mouseY);
    }
}
