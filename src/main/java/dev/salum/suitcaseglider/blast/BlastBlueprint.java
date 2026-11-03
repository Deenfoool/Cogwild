package dev.salum.suitcaseglider.blast;

import net.minecraft.core.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** Coordinates are right/back/up from the front-left controller, in block units. */
public final class BlastBlueprint {
    public static final int CELLS=45;
    public static int index(int x,int y,int z){return y*9+z*3+x;}
    public static BlockPos at(BlockPos root,Direction front,int index){return root.relative(front.getClockWise(),index%3).relative(front.getOpposite(),index/3%3).above(index/9);}
    public static BlockPos root(BlockPos pos,Direction front,int index){return pos.relative(front.getClockWise(),-index%3).relative(front.getOpposite(),-(index/3%3)).below(index/9);}
    public static BlockState expected(int i,Direction facing){
        int x=i%3,z=i/3%3,y=i/9;
        if(y==4||(y==1||y==2)&&x==1&&z==1)return Blocks.AIR.defaultBlockState();
        Block b;
        if(y==0)b=x==0&&z==0?BlastContent.CONTROLLER.get():x==2&&z==1?BlastContent.BLOWER.get():BlastContent.HEARTH.get();
        else if(y==3){if(x==1&&z==1)return Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING,Direction.DOWN);b=x==1&&z==0?BlastContent.HOPPER.get():x==2&&z==2?BlastContent.DUCT.get():BlastContent.SHAFT.get();}
        else b=x==2&&z==2?BlastContent.PREHEATER.get():x==2&&(z==1||y==1&&z==0)?BlastContent.DUCT.get():BlastContent.SHAFT.get();
        return b.defaultBlockState().setValue(BlastModuleBlock.FACING,facing);
    }
}
