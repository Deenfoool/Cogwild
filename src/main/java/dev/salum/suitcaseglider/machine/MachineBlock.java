package dev.salum.suitcaseglider.machine;

import net.minecraft.core.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Model-sized footprint, one render origin and one item drop. */
public final class MachineBlock extends BaseEntityBlock {
    private static final ThreadLocal<Boolean> REMOVING_PAIR=ThreadLocal.withInitial(()->false);
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SECONDARY=BooleanProperty.create("secondary"),POWERED=BlockStateProperties.POWERED;
    public static final IntegerProperty SEGMENT=IntegerProperty.create("segment",0,2),LAYER=IntegerProperty.create("layer",0,2),DEPTH=IntegerProperty.create("depth",0,1);
    public final String kind;
    public MachineCatalog.Spec spec(){return MachineCatalog.get(kind);}
    public static boolean primary(BlockState s){return !s.getValue(SECONDARY)&&s.getValue(LAYER)==0;}
    private static int segment(BlockState s){return s.getValue(SECONDARY)&&s.getValue(SEGMENT)==0&&s.getValue(LAYER)==0&&s.getValue(DEPTH)==0?1:s.getValue(SEGMENT);}
    public MachineBlock(String kind){super(Properties.of().strength(3,6).sound(SoundType.METAL).noOcclusion());this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(SECONDARY,false).setValue(POWERED,false).setValue(SEGMENT,0).setValue(LAYER,0).setValue(DEPTH,0));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,SECONDARY,POWERED,SEGMENT,LAYER,DEPTH);}
    public static BlockPos other(BlockPos pos,BlockState state){return primary(state)?pos.relative(state.getValue(FACING).getClockWise()):base(pos,state);}
    public static BlockPos base(BlockPos pos,BlockState state){return pos.below(state.getValue(LAYER)).relative(state.getValue(FACING).getCounterClockWise(),segment(state)).relative(state.getValue(FACING),state.getValue(DEPTH));}
    public java.util.List<BlockPos> cells(BlockPos base,BlockState state){
        var cells=new java.util.ArrayList<BlockPos>();
        for(int y=0;y<spec().layers();y++)for(int x=0;x<spec().width();x++)for(int z=0;z<spec().depth();z++)cells.add(base.relative(state.getValue(FACING).getClockWise(),x).relative(state.getValue(FACING).getOpposite(),z).above(y));
        return cells;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var state=defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());
        for(var p:cells(c.getClickedPos(),state))if(!c.getLevel().isInWorldBounds(p)||!c.getLevel().getWorldBorder().isWithinBounds(p)||!c.getLevel().getBlockState(p).canBeReplaced(c))return null;
        return state;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){
        if(level.isClientSide)return;
        for(int y=0;y<spec().layers();y++)for(int x=0;x<spec().width();x++)for(int z=0;z<spec().depth();z++)if(x!=0||y!=0||z!=0){
            var part=state.setValue(SEGMENT,x).setValue(LAYER,y).setValue(DEPTH,z).setValue(SECONDARY,true);
            level.setBlock(pos.relative(state.getValue(FACING).getClockWise(),x).relative(state.getValue(FACING).getOpposite(),z).above(y),part,3);
        }updatePower(level,pos,state);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return primary(state)?new MachineBlockEntity(pos,state):null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide||!primary(s)?null:createTickerHelper(type,MachineContent.TYPE.get(),(world,p,state,machine)->machine.tickEnergy());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(0,0,0,16,Math.min(16,Math.max(.1,spec().height()-16*state.getValue(LAYER))),16);}
    @Override public BlockState rotate(BlockState state,Rotation r){return state.setValue(FACING,r.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror m){return state.rotate(m.getRotation(state.getValue(FACING)));}
    private boolean matching(Level level,BlockPos p,BlockPos origin,BlockState state){var part=level.getBlockState(p);return part.is(this)&&part.getValue(FACING)==state.getValue(FACING)&&base(p,part).equals(origin);}
    private void removeParts(Level level,BlockPos pos,BlockState state,boolean drop){
        BlockPos origin=base(pos,state);REMOVING_PAIR.set(true);
        try{for(var p:cells(origin,state))if(!p.equals(pos)&&matching(level,p,origin,state))level.destroyBlock(p,drop&&primary(level.getBlockState(p)));}
        finally{REMOVING_PAIR.set(false);}
    }
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()&&!REMOVING_PAIR.get())removeParts(level,pos,state,false);
        super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){
        if(!state.is(next.getBlock())&&!level.isClientSide&&!REMOVING_PAIR.get())removeParts(level,pos,state,!primary(state));
        super.onRemove(state,level,pos,next,moved);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,BlockPos from,boolean moving){if(!level.isClientSide)updatePower(level,base(pos,state),level.getBlockState(base(pos,state)));}
    public void updatePower(Level level,BlockPos pos,BlockState state){
        if(!state.is(this)||!primary(state))return;
        boolean powered=cells(pos,state).stream().anyMatch(level::hasNeighborSignal)||(level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&machine.cableEnergy()>=8);
        if(state.getValue(POWERED)==powered)return;
        if(level.getBlockEntity(pos) instanceof MachineBlockEntity machine)machine.powerChanged(powered);
        level.setBlock(pos,state.setValue(POWERED,powered),3);
        for(var p:cells(pos,state))if(!p.equals(pos)&&matching(level,p,pos,state))level.setBlock(p,level.getBlockState(p).setValue(POWERED,powered),3);
    }
}
