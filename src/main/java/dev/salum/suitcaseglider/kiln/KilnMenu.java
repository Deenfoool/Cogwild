package dev.salum.suitcaseglider.kiln;

import dev.salum.suitcaseglider.resource.ResourceContent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class KilnMenu extends AbstractContainerMenu {
    public final KilnBlockEntity kiln;
    public KilnMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,(KilnBlockEntity)inv.player.level().getBlockEntity(data.readBlockPos()));}
    public KilnMenu(int id,Inventory inv,KilnBlockEntity k){
        super(KilnContent.MENU.get(),id);kiln=k;
        addSlot(new SlotItemHandler(k.inventory,0,56,17){@Override public void setChanged(){super.setChanged();k.setChanged();}});
        addSlot(new SlotItemHandler(k.inventory,1,116,35){@Override public boolean mayPlace(ItemStack stack){return false;}@Override public void setChanged(){super.setChanged();k.setChanged();}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,142));addDataSlots(k.data);
    }
    @Override public boolean stillValid(Player p){return !kiln.isRemoved()&&p.level()==kiln.getLevel()&&p.level().getBlockEntity(kiln.getBlockPos())==kiln&&p.distanceToSqr(kiln.getBlockPos().getX()+.5,kiln.getBlockPos().getY()+.5,kiln.getBlockPos().getZ()+.5)<=64;}
    @Override public boolean clickMenuButton(Player p,int button){return false;}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(index<2){if(!moveItemStackTo(stack,2,38,true))return ItemStack.EMPTY;}
        else if(KilnBlockEntity.accepts(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(index<29){if(!moveItemStackTo(stack,29,38,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,2,29,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==original.getCount())return ItemStack.EMPTY;slot.onTake(p,stack);kiln.setChanged();return original;
    }
}
