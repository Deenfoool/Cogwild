package dev.salum.suitcaseglider.cable;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CableRoutesTest {
    private CableRoutes.Terrain flat(Set<BlockPos> blocked){return new CableRoutes.Terrain(){
        public CableRoutes.Surface surface(int x,int z,double near){return blocked.contains(new BlockPos(x,0,z))?null:new CableRoutes.Surface(new BlockPos(x,-1,z),0);}
        public boolean clear(Vec3 a,Vec3 b){return true;}
    };}
    @Test void floorDetoursAroundAnObstacleInsteadOfCuttingThroughIt(){
        var t=flat(Set.of(new BlockPos(2,0,0),new BlockPos(2,0,1)));var p=CableRoutes.floor(new Vec3(.5,.5,.5),new Vec3(4.5,.5,.5),t);
        assertFalse(p.isEmpty());assertTrue(p.stream().anyMatch(v->v.z<0||v.z>2));assertEquals(.045,p.get(1).y,1e-6);
    }
    @Test void unsupportedGapIsNotBridgedByAFloorCable(){
        var t=new CableRoutes.Terrain(){public CableRoutes.Surface surface(int x,int z,double near){return z==0&&(x==0||x==2)?new CableRoutes.Surface(new BlockPos(x,-1,z),0):null;}public boolean clear(Vec3 a,Vec3 b){return true;}};
        assertTrue(CableRoutes.floor(new Vec3(.5,.5,.5),new Vec3(2.5,.5,.5),t).isEmpty());
    }
    @Test void oneBlockStepUsesARiserRatherThanADiagonalThroughTheBlock(){
        var t=new CableRoutes.Terrain(){public CableRoutes.Surface surface(int x,int z,double near){if(z!=0||x<0||x>2)return null;double y=x<1?0:1;return y<=near+.01?new CableRoutes.Surface(new BlockPos(x,(int)y-1,z),y):null;}public boolean clear(Vec3 a,Vec3 b){return true;}};
        var p=CableRoutes.floor(new Vec3(.5,.5,.5),new Vec3(2.5,1.5,.5),t);assertFalse(p.isEmpty());boolean riser=false;
        for(int i=1;i<p.size();i++){var a=p.get(i-1);var b=p.get(i);if(Math.abs(a.y-b.y)>.9){assertEquals(a.x,b.x,1e-6);assertEquals(a.z,b.z,1e-6);riser=true;}}assertTrue(riser);
    }
    @Test void overheadCableSagsAndRejectsObstructions(){
        Vec3 a=new Vec3(0,3,0),b=new Vec3(8,3,0);var p=CableRoutes.air(a,b,flat(Set.of()));assertEquals(a,p.get(0));assertEquals(b,p.get(p.size()-1));assertTrue(p.stream().anyMatch(v->v.y<2.3));
        assertTrue(CableRoutes.air(a,b,new CableRoutes.Terrain(){public CableRoutes.Surface surface(int x,int z,double y){return null;}public boolean clear(Vec3 x,Vec3 y){return x.x<3||x.x>5;}}).isEmpty());
    }
    @Test void bothModesEnforceTheLengthLimit(){var t=flat(Set.of());assertTrue(CableRoutes.air(Vec3.ZERO,new Vec3(33,0,0),t).isEmpty());assertTrue(CableRoutes.floor(Vec3.ZERO,new Vec3(33,0,0),t).isEmpty());}
}
