package dev.salum.suitcaseglider.blast;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class BlastBlockEntity extends BlockEntity {
    static boolean changing;
    private final BlockState[] originals=new BlockState[BlastBlueprint.CELLS];
    public BlastBlockEntity(BlockPos p,BlockState s){super(BlastContent.TYPE.get(),p,s);}
    public boolean formed(){return getBlockState().getValue(BlastControllerBlock.FORMED);}
    public Direction facing(){return getBlockState().getValue(BlastModuleBlock.FACING);}
    public BlockState original(int i){return originals[i]==null?BlastBlueprint.expected(i,facing()):originals[i];}
    public boolean form(Player player){
        if(level==null||level.isClientSide||formed())return false;
        for(int i=0;i<BlastBlueprint.CELLS;i++){
            var p=BlastBlueprint.at(worldPosition,facing(),i);var want=BlastBlueprint.expected(i,facing());
            if(level.isOutsideBuildHeight(p)||!level.getWorldBorder().isWithinBounds(p)||!level.hasChunkAt(p)){if(player!=null)player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.unloaded"),true);return false;}
            var s=level.getBlockState(p);
            if(!s.equals(want)||level.getBlockEntity(p)instanceof Container c&&!c.isEmpty()){
                if(player!=null)player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.mismatch",p.getX(),p.getY(),p.getZ(),want.getBlock().getName()),false);return false;
            }
        }
        for(int i=0;i<BlastBlueprint.CELLS;i++)originals[i]=level.getBlockState(BlastBlueprint.at(worldPosition,facing(),i));
        changing=true;try{
            for(int i=1;i<BlastBlueprint.CELLS;i++)level.setBlock(BlastBlueprint.at(worldPosition,facing(),i),BlastContent.PART.get().defaultBlockState().setValue(BlastModuleBlock.FACING,facing()).setValue(BlastPartBlock.INDEX,i),2);
            level.setBlock(worldPosition,getBlockState().setValue(BlastControllerBlock.FORMED,true),3);
        }finally{changing=false;}
        setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
        if(player!=null)player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.formed"),true);return true;
    }
    /** Restore loaded cells only. Unloaded proxy cells self-recover when loaded. */
    public void restore(BlockPos broken){
        if(level==null||level.isClientSide||!formed())return;var front=facing();
        changing=true;try{
            for(int i=1;i<BlastBlueprint.CELLS;i++){
                var p=BlastBlueprint.at(worldPosition,front,i);if(p.equals(broken)||!level.hasChunkAt(p))continue;
                var s=level.getBlockState(p);if(s.is(BlastContent.PART.get())&&s.getValue(BlastPartBlock.INDEX)==i&&s.getValue(BlastModuleBlock.FACING)==front)level.setBlock(p,original(i),3);
            }
            if(!worldPosition.equals(broken))level.setBlock(worldPosition,getBlockState().setValue(BlastControllerBlock.FORMED,false),3);
        }finally{changing=false;}setChanged();
    }
    public void tick(){if(!formed()||level==null||level.getGameTime()%40!=0)return;for(int i=1;i<BlastBlueprint.CELLS;i++){var p=BlastBlueprint.at(worldPosition,facing(),i);if(!level.hasChunkAt(p))continue;var s=level.getBlockState(p);if(!s.is(BlastContent.PART.get())||s.getValue(BlastPartBlock.INDEX)!=i||s.getValue(BlastModuleBlock.FACING)!=facing()){restore(p);return;}}}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);var list=new ListTag();for(int i=0;i<BlastBlueprint.CELLS;i++)list.add(NbtUtils.writeBlockState(original(i)));t.put("Construction",list);}
    @Override public void load(CompoundTag t){super.load(t);var list=t.getList("Construction",10);for(int i=0;i<originals.length;i++)originals[i]=i<list.size()?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),list.getCompound(i)):null;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){var other=BlastBlueprint.at(worldPosition,facing(),44);return new AABB(worldPosition,other.offset(1,1,1)).inflate(3);}
}
