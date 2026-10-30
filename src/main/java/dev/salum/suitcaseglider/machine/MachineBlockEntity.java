package dev.salum.suitcaseglider.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class MachineBlockEntity extends BlockEntity {
    private long powerChange=Long.MIN_VALUE/2;
    public MachineBlockEntity(BlockPos pos,BlockState state){super(MachineContent.TYPE.get(),pos,state);}
    public String kind(){return ((MachineBlock)getBlockState().getBlock()).kind;}
    public boolean powered(){return getBlockState().getValue(MachineBlock.POWERED);}
    public double seconds(float partial){return level==null?0:(level.getGameTime()+partial)/20;}
    public double transitionTime(float partial){return level==null?100:(level.getGameTime()+partial-powerChange)/20;}
    public void powerChanged(boolean powered){powerChange=level.getGameTime();setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("PowerChange",powerChange);}
    @Override public void load(CompoundTag tag){super.load(tag);powerChange=tag.contains("PowerChange")?tag.getLong("PowerChange"):Long.MIN_VALUE/2;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(2);}
}
