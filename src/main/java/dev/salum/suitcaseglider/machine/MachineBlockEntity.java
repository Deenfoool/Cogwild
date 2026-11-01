package dev.salum.suitcaseglider.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public final class MachineBlockEntity extends BlockEntity {
    private long powerChange=Long.MIN_VALUE/2;
    private int energy;
    private final IEnergyStorage storage=new IEnergyStorage(){
        public int receiveEnergy(int n,boolean simulate){return receiveCableEnergy(n,simulate);}
        public int extractEnergy(int n,boolean simulate){return 0;}
        public int getEnergyStored(){return energy;}
        public int getMaxEnergyStored(){return 4000;}
        public boolean canExtract(){return false;}
        public boolean canReceive(){return true;}
    };
    private LazyOptional<IEnergyStorage> energyCap=LazyOptional.of(()->storage);
    public int cableEnergy(){return energy;}
    public int receiveCableEnergy(int n,boolean simulate){int moved=Math.min(Math.max(0,n),4000-energy);if(!simulate&&moved>0){energy+=moved;setChanged();}return moved;}
    public void tickEnergy(){((MachineBlock)getBlockState().getBlock()).updatePower(level,worldPosition,getBlockState());if(energy>=8){energy-=8;setChanged();}}
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ENERGY?energyCap.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();energyCap=LazyOptional.of(()->storage);}
    public MachineBlockEntity(BlockPos pos,BlockState state){super(MachineContent.TYPE.get(),pos,state);}
    public String kind(){return ((MachineBlock)getBlockState().getBlock()).kind;}
    public boolean powered(){return getBlockState().getValue(MachineBlock.POWERED);}
    public double seconds(float partial){return level==null?0:(level.getGameTime()+partial)/20;}
    public double transitionTime(float partial){return level==null?100:(level.getGameTime()+partial-powerChange)/20;}
    public void powerChanged(boolean powered){powerChange=level.getGameTime();setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("PowerChange",powerChange);tag.putInt("CableEnergy",energy);}
    @Override public void load(CompoundTag tag){super.load(tag);energy=Math.max(0,Math.min(4000,tag.getInt("CableEnergy")));powerChange=tag.contains("PowerChange")?tag.getLong("PowerChange"):Long.MIN_VALUE/2;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(2);}
}
