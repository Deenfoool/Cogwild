package dev.salum.suitcaseglider.client;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.*;
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
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final ResourceLocation texture;
    private final MachineIconAnimation iconClock;
    private int displayedFrame;
    private final ResourceLocation motionTexture;
    @Override protected void containerTick(){super.containerTick();iconClock.tick(menu.machine.processing());}
    int animationFrame(){return displayedFrame;}

    public MachineScreen(MachineMenu m,Inventory inv,Component title){super(m,inv,title);imageWidth=176;imageHeight=166;inventoryLabelY=73;titleLabelY=6;texture=SuitcaseGlider.id("textures/gui/machines/"+m.machine.kind()+".png");
        iconClock=new MachineIconAnimation(m.machine.kind().equals("air_separator"));
        motionTexture=m.machine.kind().equals("crusher")||m.machine.kind().equals("air_separator")?SuitcaseGlider.id("textures/gui/machines/"+m.machine.kind()+"_motion.png"):null;
    }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){event.enqueueWork(()->MenuScreens.register(MachineContent.MENU.get(),MachineScreen::new));}
    @Override protected void renderBg(GuiGraphics g,float partial,int mouseX,int mouseY){
        g.blit(texture,leftPos,topPos,0,0,176,166,176,166);
        int energy=menu.machine.data.get(3),h=44*Math.max(0,Math.min(MachineBlockEntity.CAPACITY,energy))/MachineBlockEntity.CAPACITY;
        if(h>0){g.fill(leftPos+11,topPos+68-h,leftPos+18,topPos+68,0xffd89022);for(int y=topPos+68-h;y<topPos+68;y+=4)g.fill(leftPos+11,y,leftPos+18,Math.min(y+1,topPos+68),0xfff5ba39);}
        if(motionTexture!=null){displayedFrame=iconClock.frame(menu.machine.processing(),partial);g.blit(motionTexture,leftPos+85,topPos+20,0,displayedFrame*32,32,32,32,32*MachineIconAnimation.FRAMES);}
        int state=menu.machine.data.get(2);g.fill(leftPos+158,topPos+7,leftPos+162,topPos+11,state==MachineBlockEntity.RUNNING?0xff7cdb36:state==MachineBlockEntity.BLOCKED||state==MachineBlockEntity.NO_RECIPE?0xffd69731:energy==0?0xff555b50:0xff75a84b);
        if(menu.layout.inputs()>0){int width=29*Math.max(0,menu.machine.data.get(0))/Math.max(1,menu.machine.data.get(1));if(width>0)g.fill(leftPos+86,topPos+54,leftPos+86+Math.min(width,29),topPos+57,0xffffdb48);}
        else{g.drawString(font,Component.translatable("gui.suitcaseglider.machine.buffer"),leftPos+34,topPos+29,0xff444749,false);g.drawString(font,energy+" / "+MachineBlockEntity.CAPACITY+" FE",leftPos+34,topPos+43,0xff444749,false);}
        var status=Component.translatable("gui.suitcaseglider.machine.state."+state);g.drawString(font,status,leftPos+28,topPos+63,0xff54534c,false);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        renderBackground(g);super.render(g,mouseX,mouseY,partial);renderTooltip(g,mouseX,mouseY);
        if(isHovering(9,20,11,49,mouseX,mouseY))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.machine.energy",menu.machine.data.get(3),MachineBlockEntity.CAPACITY),mouseX,mouseY);
        if(menu.layout.inputs()>0&&isHovering(82,23,39,36,mouseX,mouseY))g.renderTooltip(font,Component.translatable("gui.suitcaseglider.machine.progress",menu.machine.data.get(0)/20.0,menu.machine.data.get(1)/20.0,menu.machine.data.get(4)),mouseX,mouseY);
        for(int i=0;i<menu.layout.visibleSlots();i++)if(!menu.getSlot(i).hasItem()&&isHovering(menu.layout.x(i),menu.layout.y(i),16,16,mouseX,mouseY)){
            String key=i<menu.layout.inputs()?"input":menu.machine.kind().equals("air_separator")?(i==menu.layout.inputs()?"heavy":"light"):"output";
            g.renderTooltip(font,Component.translatable("gui.suitcaseglider.machine.slot."+key),mouseX,mouseY);
        }
        if(isHovering(28,62,130,9,mouseX,mouseY)&&menu.machine.data.get(2)==MachineBlockEntity.UTILITY)g.renderTooltip(font,Component.translatable("gui.suitcaseglider.machine.utility_hint"),mouseX,mouseY);
    }
}
