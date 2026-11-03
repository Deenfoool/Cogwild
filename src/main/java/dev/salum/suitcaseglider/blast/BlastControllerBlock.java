package dev.salum.suitcaseglider.blast;

import dev.salum.suitcaseglider.kiln.KilnContent;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

public final class BlastControllerBlock extends BlastModuleBlock implements EntityBlock {
    public static final BooleanProperty FORMED=BooleanProperty.create("formed");
    public BlastControllerBlock(){super(moduleProperties().noOcclusion());registerDefaultState(defaultBlockState().setValue(FORMED,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(FORMED);}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new BlastBlockEntity(p,s);}
    @Override public RenderShape getRenderShape(BlockState s){return s.getValue(FORMED)?RenderShape.INVISIBLE:RenderShape.MODEL;}
    @Override public int getLightBlock(BlockState s,BlockGetter l,BlockPos p){return s.getValue(FORMED)?0:super.getLightBlock(s,l,p);}
    @Override public boolean propagatesSkylightDown(BlockState s,BlockGetter l,BlockPos p){return s.getValue(FORMED);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> t){return !l.isClientSide&&t==BlastContent.TYPE.get()?(w,p,state,be)->((BlastBlockEntity)be).tick():null;}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult h){
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(l.getBlockEntity(p)instanceof BlastBlockEntity b){
            if(s.getValue(FORMED)){if(player.isShiftKeyDown()&&player.getItemInHand(hand).is(KilnContent.KEY.get()))b.restore(null);else player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.assembled"),true);}
            else if(player.getItemInHand(hand).is(KilnContent.KEY.get()))b.form(player);
            else player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.need_key"),true);
        }return InteractionResult.CONSUME;
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moved){if(!s.is(n.getBlock())&&!l.isClientSide&&!BlastBlockEntity.changing&&l.getBlockEntity(p)instanceof BlastBlockEntity b)b.restore(p);super.onRemove(s,l,p,n,moved);}
}
