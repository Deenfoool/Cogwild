package dev.salum.suitcaseglider.blast;

import net.minecraft.core.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;

/** Loaded port forwards each operation to its live owner; it never owns inventory or energy. */
public final class BlastPortBlockEntity extends BlockEntity {
    public static boolean port(int cell){return cell==1||cell==5||cell==31||cell==40;}
    public BlastPortBlockEntity(BlockPos p,BlockState s){super(BlastContent.PORT_TYPE.get(),p,s);}
    private BlastBlockEntity owner(){return level==null?null:BlastPartBlock.owner(getBlockState(),level,worldPosition);}
    private int cell(){return getBlockState().getValue(BlastPartBlock.INDEX);}
    private boolean input(){return cell()==31||cell()==40;}
    private final IItemHandler items=new IItemHandler(){
        private BlastBlockEntity target(){var b=owner();return b!=null&&b.operable()?b:null;}
        public int getSlots(){return input()?3:2;}
        private int index(int slot){return input()?slot:slot+3;}
        private boolean valid(int s){return s>=0&&s<getSlots();}
        public ItemStack getStackInSlot(int s){var b=target();return !valid(s)||b==null?ItemStack.EMPTY:b.inventory.getStackInSlot(index(s)).copy();}
        public ItemStack insertItem(int s,ItemStack stack,boolean simulate){var b=target();return !input()||!valid(s)||b==null?stack:b.inventory.insertItem(s,stack,simulate);}
        public ItemStack extractItem(int s,int n,boolean simulate){var b=target();return input()||!valid(s)||b==null?ItemStack.EMPTY:b.inventory.extractItem(index(s),n,simulate);}
        public int getSlotLimit(int s){return valid(s)?64:0;}
        public boolean isItemValid(int s,ItemStack stack){var b=target();return input()&&valid(s)&&b!=null&&b.accepts(s,stack);}
    };
    private final IEnergyStorage power=new IEnergyStorage(){
        public int receiveEnergy(int n,boolean simulate){var b=owner();return b==null?0:b.receiveEnergy(n,simulate);}
        public int extractEnergy(int n,boolean simulate){return 0;}
        public int getEnergyStored(){var b=owner();return b==null?0:b.energy();}
        public int getMaxEnergyStored(){return BlastBlockEntity.ENERGY_CAPACITY;}
        public boolean canReceive(){var b=owner();return b!=null&&b.operable();}
        public boolean canExtract(){return false;}
    };
    private LazyOptional<IItemHandler> itemCap=LazyOptional.of(()->items);
    private LazyOptional<IEnergyStorage> powerCap=LazyOptional.of(()->power);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        var b=owner();if(b==null||!b.operable())return super.getCapability(cap,side);
        if(cell()==5&&cap==ForgeCapabilities.ENERGY&&(side==null||side==b.facing().getClockWise()))return powerCap.cast();
        if(cell()!=5&&cap==ForgeCapabilities.ITEM_HANDLER&&(cell()!=40||side==null||side==Direction.UP))return itemCap.cast();
        return super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();itemCap.invalidate();powerCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();itemCap=LazyOptional.of(()->items);powerCap=LazyOptional.of(()->power);}
}
