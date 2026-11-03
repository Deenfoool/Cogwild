package dev.salum.suitcaseglider.machine;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public final class MachineMenu extends AbstractContainerMenu {
    public final MachineBlockEntity machine;
    public final MachineUiSpec layout;
    public MachineMenu(int id,Inventory inv,FriendlyByteBuf data){this(id,inv,read(inv,data));}
    private static MachineBlockEntity read(Inventory inv,FriendlyByteBuf data){var b=inv.player.level().getBlockEntity(data.readBlockPos());if(!(b instanceof MachineBlockEntity machine))throw new IllegalArgumentException("Missing machine container");return machine;}
    public MachineMenu(int id,Inventory inv,MachineBlockEntity b){
        super(MachineContent.MENU.get(),id);machine=b;layout=b.ui();
        for(int i=0;i<layout.visibleSlots();i++){final int storage=layout.storageSlot(i);addSlot(new SlotItemHandler(b.inventory,storage,layout.x(i),layout.y(i)){
            @Override public boolean mayPlace(ItemStack s){return storage<layout.inputs()&&super.mayPlace(s);}
        });}
        for(int y=0;y<3;y++)for(int x=0;x<9;x++)addSlot(new Slot(inv,9+x+y*9,8+x*18,84+y*18));
        for(int x=0;x<9;x++)addSlot(new Slot(inv,x,8+x*18,142));addDataSlots(b.data);
    }
    @Override public boolean stillValid(Player p){return !machine.isRemoved()&&p.level()==machine.getLevel()&&p.level().getBlockEntity(machine.getBlockPos())==machine&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos()))<=64;}
    @Override public ItemStack quickMoveStack(Player p,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var copy=s.copy();int count=layout.visibleSlots();
        if(index<count){if(!moveItemStackTo(s,count,count+36,true))return ItemStack.EMPTY;}
        else{
            boolean moved=layout.inputs()>0&&moveItemStackTo(s,0,layout.inputs(),false);
            if(!moved){if(index<count+27){if(!moveItemStackTo(s,count+27,count+36,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,count,count+27,false))return ItemStack.EMPTY;}
        }
        if(s.getCount()==copy.getCount())return ItemStack.EMPTY;if(s.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return copy;
    }
}
