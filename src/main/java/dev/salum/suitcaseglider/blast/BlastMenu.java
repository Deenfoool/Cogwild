package dev.salum.suitcaseglider.blast;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class BlastMenu extends AbstractContainerMenu {
    public final BlastBlockEntity furnace;
    public BlastMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,(BlastBlockEntity)inv.player.level().getBlockEntity(data.readBlockPos()));}
    public BlastMenu(int id,Inventory inv,BlastBlockEntity b){
        super(BlastContent.MENU.get(),id);furnace=b;
        int[][] positions={{35,17},{35,53},{62,53},{116,17},{116,53}};
        for(int i=0;i<5;i++){final int index=i;addSlot(new SlotItemHandler(b.inventory,i,positions[i][0],positions[i][1]){@Override public boolean mayPlace(ItemStack s){return index<3&&super.mayPlace(s);}@Override public void setChanged(){super.setChanged();b.contentsChanged(index);}});}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,x+y*9+9,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));addDataSlots(b.data);
    }
    @Override public boolean stillValid(Player p){return !furnace.isRemoved()&&furnace.formed()&&p.level()==furnace.getLevel()&&p.level().getBlockEntity(furnace.getBlockPos())==furnace&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(furnace.getBlockPos()))<=64;}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();
        if(index<5){if(!moveItemStackTo(s,5,41,true))return ItemStack.EMPTY;}
        else{boolean input=false;for(int i=0;i<3;i++)if(furnace.accepts(i,s)){input=true;moveItemStackTo(s,i,i+1,false);if(s.isEmpty())break;}if(!input){if(index<32){if(!moveItemStackTo(s,32,41,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,5,32,false))return ItemStack.EMPTY;}}
        if(s.getCount()==copy.getCount())return ItemStack.EMPTY;if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);furnace.setChanged();return copy;
    }
}
