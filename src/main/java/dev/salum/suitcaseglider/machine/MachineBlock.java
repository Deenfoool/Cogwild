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

/** Two-cell footprint, one render origin and one item drop. */
public final class MachineBlock extends BaseEntityBlock {
    private static final ThreadLocal<Boolean> REMOVING_PAIR=ThreadLocal.withInitial(()->false);
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty SECONDARY=BooleanProperty.create("secondary"),POWERED=BlockStateProperties.POWERED;
    public final String kind;
    public MachineBlock(String kind){super(Properties.of().strength(3,6).sound(SoundType.METAL).noOcclusion());this.kind=kind;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(SECONDARY,false).setValue(POWERED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,SECONDARY,POWERED);}
    public static BlockPos other(BlockPos pos,BlockState state){Direction right=state.getValue(FACING).getClockWise();return pos.relative(state.getValue(SECONDARY)?right.getOpposite():right);}
    public static BlockPos base(BlockPos pos,BlockState state){return state.getValue(SECONDARY)?other(pos,state):pos;}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){
        var state=defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());var second=other(c.getClickedPos(),state);
        if(!c.getLevel().getBlockState(second).canBeReplaced(c)||!c.getLevel().getWorldBorder().isWithinBounds(second))return null;
        if(!c.getLevel().getBlockState(c.getClickedPos().above()).canBeReplaced(c)||!c.getLevel().getBlockState(second.above()).canBeReplaced(c))return null;
        return state;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity placer,ItemStack stack){if(!level.isClientSide){level.setBlock(other(pos,state),state.setValue(SECONDARY,true),3);updatePower(level,pos,state);}}
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return state.getValue(SECONDARY)?null:new MachineBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide||s.getValue(SECONDARY)?null:createTickerHelper(type,MachineContent.TYPE.get(),(world,p,state,machine)->machine.tickEnergy());}
    @Override public RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
    @Override public VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context){return Block.box(0,0,0,16,kind.equals("crusher")&&state.getValue(SECONDARY)?26:kind.equals("lathe")?22:18,16);}
    @Override public BlockState rotate(BlockState state,Rotation r){return state.setValue(FACING,r.rotate(state.getValue(FACING)));}
    @Override public BlockState mirror(BlockState state,Mirror m){return state.rotate(m.getRotation(state.getValue(FACING)));}
    private boolean matching(Level level,BlockPos pos,BlockState state){var other=level.getBlockState(pos);return other.is(this)&&other.getValue(FACING)==state.getValue(FACING)&&other.getValue(SECONDARY)!=state.getValue(SECONDARY);}
    @Override public void playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player){
        if(!level.isClientSide&&player.isCreative()&&matching(level,other(pos,state),state)){
            REMOVING_PAIR.set(true);try{level.destroyBlock(other(pos,state),false);}finally{REMOVING_PAIR.set(false);}
        }
        super.playerWillDestroy(level,pos,state,player);
    }
    @Override public void onRemove(BlockState state,Level level,BlockPos pos,BlockState next,boolean moved){
        if(!state.is(next.getBlock())&&!level.isClientSide&&!REMOVING_PAIR.get()&&matching(level,other(pos,state),state)){
            REMOVING_PAIR.set(true);try{level.destroyBlock(other(pos,state),state.getValue(SECONDARY));}finally{REMOVING_PAIR.set(false);}
        }
        super.onRemove(state,level,pos,next,moved);
    }
    @Override public void neighborChanged(BlockState state,Level level,BlockPos pos,Block source,BlockPos from,boolean moving){if(!level.isClientSide)updatePower(level,base(pos,state),level.getBlockState(base(pos,state)));}
    public void updatePower(Level level,BlockPos pos,BlockState state){
        if(!state.is(this)||state.getValue(SECONDARY))return;
        BlockPos second=other(pos,state);boolean powered=level.hasNeighborSignal(pos)||level.hasNeighborSignal(second)||(level.getBlockEntity(pos) instanceof MachineBlockEntity machine&&machine.cableEnergy()>=8);
        if(state.getValue(POWERED)==powered)return;
        if(level.getBlockEntity(pos) instanceof MachineBlockEntity machine)machine.powerChanged(powered);
        level.setBlock(pos,state.setValue(POWERED,powered),3);
        if(matching(level,second,state))level.setBlock(second,level.getBlockState(second).setValue(POWERED,powered),3);
    }
}
