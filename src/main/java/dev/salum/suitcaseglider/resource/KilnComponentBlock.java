package dev.salum.suitcaseglider.resource;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.NetworkHooks;
import dev.salum.suitcaseglider.kiln.*;

/** Individually placeable construction parts; the multiblock controller comes next. */
public final class KilnComponentBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING=HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT=BlockStateProperties.LIT;
    public KilnComponentBlock(){super(Properties.of().strength(7,12).sound(SoundType.STONE).requiresCorrectToolForDrops().lightLevel(s->s.getValue(LIT)?7:0));registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> b){b.add(FACING,LIT);}
    @Override public BlockState getStateForPlacement(BlockPlaceContext c){return defaultBlockState().setValue(FACING,c.getHorizontalDirection().getOpposite());}
    @Override public BlockState rotate(BlockState s,Rotation r){return s.setValue(FACING,r.rotate(s.getValue(FACING)));}
    @Override public BlockState mirror(BlockState s,Mirror m){return s.rotate(m.getRotation(s.getValue(FACING)));}
    @Override public BlockEntity newBlockEntity(BlockPos p,BlockState s){return new KilnBlockEntity(p,s);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level l,BlockState s,BlockEntityType<T> type){return l.isClientSide||type!=KilnContent.TYPE.get()?null:(world,p,state,be)->((KilnBlockEntity)be).tick();}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult hit){
        if(s.is(ResourceContent.OUTPUT.get())&&player.getItemInHand(hand).is(net.minecraft.world.item.Items.BUCKET)){
            if(l.isClientSide)return InteractionResult.SUCCESS;
            if(l.getBlockEntity(p) instanceof KilnBlockEntity k&&net.minecraftforge.fluids.FluidUtil.interactWithFluidHandler(player,hand,k.fluidHandler()))return InteractionResult.CONSUME;
        }
        if(!s.is(ResourceContent.CONTROLLER.get()))return InteractionResult.PASS;
        if(l.isClientSide)return InteractionResult.SUCCESS;
        if(l.getBlockEntity(p) instanceof KilnBlockEntity kiln){
            if(!kiln.formed){if(player.getItemInHand(hand).is(KilnContent.KEY.get()))kiln.form(player);else player.displayClientMessage(Component.translatable("message.suitcaseglider.kiln.need_key"),true);}
            else if(player instanceof ServerPlayer server)NetworkHooks.openScreen(server,kiln,p);
        }
        return InteractionResult.CONSUME;
    }
    @Override public java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState s,net.minecraft.world.level.storage.loot.LootParams.Builder builder){
        var drops=super.getDrops(s,builder);var be=builder.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY);
        if(be instanceof KilnBlockEntity k&&k.controller()){
            var stored=k.carriedContents();if(!stored.isEmpty())for(var stack:drops)if(stack.is(ResourceContent.CONTROLLER_ITEM.get()))stack.getOrCreateTag().put("BlockEntityTag",stored.copy());
        }return drops;
    }
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState next,boolean moved){if(!s.is(next.getBlock())&&l.getBlockEntity(p) instanceof KilnBlockEntity kiln)kiln.dropContents();super.onRemove(s,l,p,next,moved);}
}
