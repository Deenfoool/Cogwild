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
        addSlot(new SlotItemHandler(k.inventory,2,56,53){@Override public void setChanged(){super.setChanged();k.setChanged();}});
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,8+col*18,84+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,8+col*18,142));addDataSlots(k.data);
    }
    @Override public boolean stillValid(Player p){return !kiln.isRemoved()&&p.level()==kiln.getLevel()&&p.level().getBlockEntity(kiln.getBlockPos())==kiln&&p.distanceToSqr(kiln.getBlockPos().getX()+.5,kiln.getBlockPos().getY()+.5,kiln.getBlockPos().getZ()+.5)<=64;}
    @Override public boolean clickMenuButton(Player p,int button){
        if(button!=1||p.level().isClientSide||!stillValid(p)||!getCarried().is(net.minecraft.world.item.Items.BUCKET)||kiln.tank.getFluidAmount()<1000)return false;
        var filled=new ItemStack(KilnFluids.BUCKET.get());
        kiln.tank.drain(1000,net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if(getCarried().getCount()==1)setCarried(filled);else{getCarried().shrink(1);p.getInventory().placeItemBackInInventory(filled);}
        kiln.setChanged();broadcastChanges();return true;
    }
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var original=stack.copy();
        if(index<3){if(!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
        else if(KilnBlockEntity.fuelTicks(stack)>0&&kiln.inventory.getStackInSlot(2).isEmpty()){if(!moveItemStackTo(stack,2,3,false))return ItemStack.EMPTY;}
        else if(KilnBlockEntity.accepts(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(KilnBlockEntity.fuelTicks(stack)>0){if(!moveItemStackTo(stack,2,3,false))return ItemStack.EMPTY;}
        else if(index<30){if(!moveItemStackTo(stack,30,39,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,3,30,false))return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==original.getCount())return ItemStack.EMPTY;slot.onTake(p,stack);kiln.setChanged();return original;
    }
}
