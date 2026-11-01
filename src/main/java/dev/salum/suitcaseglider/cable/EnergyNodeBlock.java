package dev.salum.suitcaseglider.cable;

import net.minecraft.core.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.*;
import net.minecraftforge.common.ForgeHooks;

public final class EnergyNodeBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING=BlockStateProperties.FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public final boolean generator;
    public EnergyNodeBlock(boolean generator){super(Properties.of().strength(2,5).sound(SoundType.METAL).noOcclusion().lightLevel(s->s.getValue(LIT)?7:0));this.generator=generator;registerDefaultState(stateDefinition.any().setValue(FACING,Direction.UP).setValue(LIT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){var s=defaultBlockState().setValue(FACING,generator?c.getHorizontalDirection().getOpposite():c.getClickedFace());return generator||s.canSurvive(c.getLevel(),c.getClickedPos())?s:null;}
    @Override public boolean canSurvive(BlockState s,LevelReader l,BlockPos p){Direction d=s.getValue(FACING);return generator||l.getBlockState(p.relative(d.getOpposite())).isFaceSturdy(l,p.relative(d.getOpposite()),d);}
    @Override public BlockState updateShape(BlockState s,Direction d,BlockState n,LevelAccessor l,BlockPos p,BlockPos q){return !s.canSurvive(l,p)?Blocks.AIR.defaultBlockState():super.updateShape(s,d,n,l,p,q);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.MODEL;}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new EnergyNodeEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide?null:createTickerHelper(type,CableContent.TYPE.get(),(world,p,state,node)->node.tickEnergy());}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){
        if(generator)return Shapes.block();
        return switch(s.getValue(FACING)){case UP->Block.box(5,0,5,11,7,11);case DOWN->Block.box(5,9,5,11,16,11);case NORTH->Block.box(5,5,9,11,11,16);case SOUTH->Block.box(5,5,0,11,11,7);case EAST->Block.box(0,5,5,7,11,11);case WEST->Block.box(9,5,5,16,11,11);};
    }
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        var stack=player.getItemInHand(hand);if(stack.getItem() instanceof CableItem||stack.is(net.minecraft.world.item.Items.SHEARS))return InteractionResult.PASS;
        if(!l.isClientSide&&l.getBlockEntity(p) instanceof EnergyNodeEntity node){
            int burn=generator?ForgeHooks.getBurnTime(stack,null):0;
            if(burn>0&&node.fuelTicks+burn<=64000){node.fuelTicks+=burn;if(!player.isCreative()){var remainder=stack.getCraftingRemainingItem();stack.shrink(1);if(!remainder.isEmpty())player.getInventory().placeItemBackInInventory(remainder);}node.setChanged();}
            player.displayClientMessage(Component.translatable("message.suitcaseglider.energy",node.stored,node.fuelTicks),true);
        }return InteractionResult.sidedSuccess(l.isClientSide);
    }
}
