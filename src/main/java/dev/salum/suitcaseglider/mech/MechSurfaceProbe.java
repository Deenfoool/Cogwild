package dev.salum.suitcaseglider.mech;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Queries exposed collision-shape tops; stairs/slabs are not approximated as full blocks. */
public final class MechSurfaceProbe {
    public record Surface(boolean valid,double height){}
    private static final Surface NONE=new Surface(false,0);
    private final MechAntEntity entity;
    private final Map<BlockPos,List<AABB>> boxes=new HashMap<>();
    private final Map<Long,Surface> surfaces=new HashMap<>();
    private double top,bottom;
    public MechSurfaceProbe(MechAntEntity entity){this.entity=entity;}
    public void reset(){boxes.clear();surfaces.clear();top=entity.getY()+1.35;bottom=entity.getY()-1.65;}
    public Surface at(double x,double z){
        int qx=Mth.floor(x*64),qz=Mth.floor(z*64);long key=((long)qx<<32)^(qz&0xffffffffL);
        return surfaces.computeIfAbsent(key,k->query((qx+.5)/64.0,(qz+.5)/64.0));
    }
    private Surface query(double x,double z){
        List<AABB> column=new ArrayList<>();int bx=Mth.floor(x),bz=Mth.floor(z);
        for(int y=Math.max(entity.level().getMinBuildHeight(),Mth.floor(bottom)-1);y<=Math.min(entity.level().getMaxBuildHeight()-1,Mth.floor(top)+1);y++){
            BlockPos pos=new BlockPos(bx,y,bz);
            column.addAll(boxes.computeIfAbsent(pos,p->entity.level().getBlockState(p).getCollisionShape(entity.level(),p,CollisionContext.of(entity)).toAabbs().stream().map(a->a.move(p.getX(),p.getY(),p.getZ())).toList()));
        }
        return exposedTop(column,x,z,bottom,top);
    }
    public static Surface exposedTop(List<AABB> boxes,double x,double z,double bottom,double top){
        double height=Double.NEGATIVE_INFINITY;
        for(AABB b:boxes){
            if(x<b.minX||x>b.maxX||z<b.minZ||z>b.maxZ||b.maxY<bottom||b.maxY>top)continue;
            boolean covered=false;
            for(AABB other:boxes)if(x>=other.minX&&x<=other.maxX&&z>=other.minZ&&z<=other.maxZ&&b.maxY+.005>other.minY&&b.maxY+.005<other.maxY){covered=true;break;}
            if(!covered)height=Math.max(height,b.maxY);
        }
        return Double.isFinite(height)?new Surface(true,height):NONE;
    }
}
