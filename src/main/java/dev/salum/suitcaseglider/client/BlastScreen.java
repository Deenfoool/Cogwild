package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.blast.*;
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
public final class BlastScreen extends AbstractContainerScreen<BlastMenu> {
    private static final ResourceLocation TEXTURE=SuitcaseGlider.id("textures/gui/blast_furnace.png");
    public BlastScreen(BlastMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=176;imageHeight=166;inventoryLabelY=72;}
    @SubscribeEvent public static void setup(FMLClientSetupEvent e){e.enqueueWork(()->MenuScreens.register(BlastContent.MENU.get(),BlastScreen::new));}
    @Override protected void renderBg(GuiGraphics g,float p,int mx,int my){
        g.blit(TEXTURE,leftPos,topPos,0,0,176,166,176,166);
        int temperature=menu.furnace.data.get(3),energy=menu.furnace.data.get(4),progress=menu.furnace.data.get(0),duration=Math.max(1,menu.furnace.data.get(1));
        int h=Math.max(0,52*(temperature-20)/1380);if(h>0)g.fill(leftPos+10,topPos+69-h,leftPos+17,topPos+69,temperature>=menu.furnace.data.get(5)?0xffff9e32:0xffbb6030);
        h=52*energy/BlastBlockEntity.ENERGY_CAPACITY;if(h>0)g.fill(leftPos+153,topPos+69-h,leftPos+161,topPos+69,0xffc7a43b);
        int w=24*progress/duration;g.fill(leftPos+81,topPos+38,leftPos+105,topPos+42,0xff747474);if(w>0)g.fill(leftPos+81,topPos+38,leftPos+81+w,topPos+42,0xffd7b15b);
        int s=menu.furnace.data.get(2);g.fill(leftPos+89,topPos+23,leftPos+96,topPos+29,s==BlastBlockEntity.RUNNING?0xff7fb446:s==BlastBlockEntity.HEATING?0xffdf982f:s==BlastBlockEntity.NO_POWER||s==BlastBlockEntity.BLOCKED||s==BlastBlockEntity.UNLOADED?0xffb64b37:0xff706c60);
    }
    @Override public void render(GuiGraphics g,int mx,int my,float p){
        renderBackground(g);super.render(g,mx,my,p);renderTooltip(g,mx,my);
        if(isHovering(8,16,11,54,mx,my))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.blast.temperature",menu.furnace.data.get(3),menu.furnace.data.get(5)),mx,my);
        if(isHovering(151,16,12,54,mx,my))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.blast.energy",menu.furnace.data.get(4),BlastBlockEntity.ENERGY_CAPACITY,menu.furnace.data.get(6)),mx,my);
        if(isHovering(80,22,26,25,mx,my))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.blast.state."+menu.furnace.data.get(2)).append(" | "+menu.furnace.data.get(0)/20+" / "+menu.furnace.data.get(1)/20+" s"),mx,my);
        int[][] pos={{35,17},{35,53},{62,53},{116,17},{116,53}};String[] labels={"ore","coke","flux","iron","slag"};for(int i=0;i<5;i++)if(!menu.furnace.inventory.getStackInSlot(i).isEmpty())continue;else if(isHovering(pos[i][0],pos[i][1],16,16,mx,my))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.blast.slot."+labels[i]),mx,my);
    }
}
