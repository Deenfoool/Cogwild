package dev.salum.suitcaseglider.blast;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.*;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.*;

/** Invisible occupied cells of an assembled furnace. No item, inventory, or duplicate loot. */
public final class BlastPartBlock extends BlastModuleBlock {
    public static final IntegerProperty INDEX=IntegerProperty.create("cell",0,44);
    public BlastPartBlock(){super(moduleProperties().noOcclusion());registerDefaultState(defaultBlockState().setValue(INDEX,1));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState>b){super.createBlockStateDefinition(b);b.add(INDEX);}
    @Override public RenderShape getRenderShape(BlockState s){return RenderShape.INVISIBLE;}
    @Override public int getLightBlock(BlockState s,BlockGetter l,BlockPos p){return 0;}
    @Override public boolean propagatesSkylightDown(BlockState s,BlockGetter l,BlockPos p){return true;}
    @Override public boolean useShapeForLightOcclusion(BlockState s){return true;}
    @Override public VoxelShape getShape(BlockState s,BlockGetter l,BlockPos p,CollisionContext c){return s.getValue(INDEX)>=36?Block.box(0,0,0,16,8,16):Shapes.block();}
    public static BlockPos ownerPos(BlockState s,BlockPos p){return BlastBlueprint.root(p,s.getValue(FACING),s.getValue(INDEX));}
    public static BlastBlockEntity owner(BlockState s,Level l,BlockPos p){var root=ownerPos(s,p);return l.hasChunkAt(root)&&l.getBlockEntity(root)instanceof BlastBlockEntity b?b:null;}
    @Override public List<ItemStack> getDrops(BlockState s,LootParams.Builder builder){
        var origin=builder.getOptionalParameter(LootContextParams.ORIGIN);if(origin==null)return List.of();
        var b=owner(s,builder.getLevel(),BlockPos.containing(origin));var original=b==null?BlastBlueprint.expected(s.getValue(INDEX),s.getValue(FACING)):b.original(s.getValue(INDEX));
        return original.isAir()?List.of():List.of(new ItemStack(original.getBlock().asItem()));
    }
    @Override public ItemStack getCloneItemStack(BlockGetter l,BlockPos p,BlockState s){var o=BlastBlueprint.expected(s.getValue(INDEX),s.getValue(FACING));return new ItemStack(o.getBlock());}
    @Override public InteractionResult use(BlockState s,Level l,BlockPos p,Player player,InteractionHand hand,BlockHitResult h){var b=owner(s,l,p);return b==null?InteractionResult.PASS:b.getBlockState().use(l,player,hand,new BlockHitResult(h.getLocation(),h.getDirection(),b.getBlockPos(),false));}
    @Override public void onRemove(BlockState s,Level l,BlockPos p,BlockState n,boolean moved){if(!s.is(n.getBlock())&&!l.isClientSide&&!BlastBlockEntity.changing){var b=owner(s,l,p);if(b!=null)b.restore(p);}super.onRemove(s,l,p,n,moved);}
    @Override public void onPlace(BlockState s,Level l,BlockPos p,BlockState old,boolean moved){super.onPlace(s,l,p,old,moved);if(!l.isClientSide)l.scheduleTick(p,this,40);}
    @Override public void tick(BlockState s,ServerLevel l,BlockPos p,RandomSource random){
        var root=ownerPos(s,p);if(!l.hasChunkAt(root)){l.scheduleTick(p,this,40);return;}var b=owner(s,l,p);
        if(b==null||!b.formed()){BlastBlockEntity.changing=true;try{l.setBlock(p,b==null?BlastBlueprint.expected(s.getValue(INDEX),s.getValue(FACING)):b.original(s.getValue(INDEX)),3);}finally{BlastBlockEntity.changing=false;}}
        else l.scheduleTick(p,this,40);
    }
}
