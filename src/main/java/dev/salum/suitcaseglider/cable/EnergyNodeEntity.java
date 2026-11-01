package dev.salum.suitcaseglider.cable;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public final class EnergyNodeEntity extends BlockEntity implements IEnergyStorage {
    public int stored,fuelTicks;
    private LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->this);
    public EnergyNodeEntity(BlockPos p,BlockState s){super(CableContent.TYPE.get(),p,s);}
    public void tickEnergy(){
        boolean burning=((EnergyNodeBlock)getBlockState().getBlock()).generator&&fuelTicks>0&&stored<=getMaxEnergyStored()-40;
        if(burning){fuelTicks--;stored+=40;setChanged();}
        if(getBlockState().getValue(EnergyNodeBlock.LIT)!=burning)level.setBlock(worldPosition,getBlockState().setValue(EnergyNodeBlock.LIT,burning),3);
        if(!((EnergyNodeBlock)getBlockState().getBlock()).generator){
            Direction face=getBlockState().getValue(EnergyNodeBlock.FACING).getOpposite();BlockPos p=worldPosition.relative(face);
            if(level.hasChunkAt(p)&&level.getBlockEntity(p)!=null&&!(level.getBlockEntity(p) instanceof EnergyNodeEntity))level.getBlockEntity(p).getCapability(ForgeCapabilities.ENERGY,face.getOpposite()).ifPresent(other->{
                if(other.canExtract()){int n=other.extractEnergy(Math.min(256,getMaxEnergyStored()-stored),false);receiveEnergy(n,false);}
                else if(other.canReceive()){int n=other.receiveEnergy(Math.min(256,stored),false);extractEnergy(n,false);}
            });
        }
    }
    @Override public int receiveEnergy(int n,boolean simulate){int value=Math.min(Math.max(0,n),getMaxEnergyStored()-stored);if(!simulate&&value>0){stored+=value;setChanged();}return value;}
    @Override public int extractEnergy(int n,boolean simulate){int value=Math.min(Math.max(0,n),stored);if(!simulate&&value>0){stored-=value;setChanged();}return value;}
    @Override public int getEnergyStored(){return stored;}
    @Override public int getMaxEnergyStored(){return 32000;}
    @Override public boolean canReceive(){return true;}
    @Override public boolean canExtract(){return true;}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ENERGY?energy.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();energy.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();energy=LazyOptional.of(()->this);}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.putInt("Energy",stored);t.putInt("Fuel",fuelTicks);}
    @Override public void load(CompoundTag t){super.load(t);stored=Math.max(0,Math.min(32000,t.getInt("Energy")));fuelTicks=Math.max(0,Math.min(64000,t.getInt("Fuel")));}
}
