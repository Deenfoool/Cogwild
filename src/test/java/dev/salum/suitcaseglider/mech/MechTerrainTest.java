package dev.salum.suitcaseglider.mech;
import java.util.List;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MechTerrainTest {
    @Test void speedMatchesAuthoredStanceStroke(){
        assertEquals(MechGait.TEMPO*.375/(1.12*.74),MechGait.FORWARD,1e-6);
        assertEquals(MechGait.TEMPO*.325/(1.36*.74),MechGait.BACKWARD,1e-6);
        for(double friction:new double[]{.4,.6,.8,.98})for(double input:new double[]{1,.25}){
            double speed=MechGait.nativeSpeed(MechGait.BACKWARD,friction,input);
            double steady=speed*.216/Math.pow(friction,3)*input/(1-.91*friction)*20;
            assertEquals(MechGait.BACKWARD,steady,1e-6);
        }
    }
    @Test void collisionSupportDistinguishesStepsAndCoveredWallTops(){
        var stair=List.of(new AABB(0,0,0,1,.5,1),new AABB(0,.5,.5,1,1,1));
        assertEquals(.5,MechSurfaceProbe.exposedTop(stair,.5,.25,-1,1.35).height());
        assertEquals(1,MechSurfaceProbe.exposedTop(stair,.5,.75,-1,1.35).height());
        assertFalse(MechSurfaceProbe.exposedTop(List.of(new AABB(0,0,0,1,1,1),new AABB(0,1,0,1,2,1)),.5,.5,-1,1.35).valid());
    }
    @Test void twoLinkSolverPreservesBoneLengthsAndHitsReachableTargets(){
        var rig=MechRig.get();for(String side:new String[]{"left","right"})for(int row=1;row<=3;row++){
            var leg=MechLegIk.leg(rig,side,row);var hip=leg.hip();
            for(float raised:new float[]{0,.5f,1}){
                var target=new Vector3f(leg.ankle()).add(0,raised,0);var s=MechLegIk.solve(leg,hip,target,new Vector3f(leg.knee()).sub(hip));
                assertEquals(leg.l1(),hip.distance(s.knee()),1e-5);assertEquals(leg.l2(),s.knee().distance(s.ankle()),1e-5);assertTrue(s.ankle().distance(target)<.002);
                var pose=rig.pose("idle",0,"idle",0,1);MechLegIk.apply(pose,leg,hip,s,0);
                assertTrue(pose.bones[leg.foot()].getTranslation(new Vector3f()).distance(target)<.002);
            }
            for(var target:new Vector3f[]{new Vector3f(hip),new Vector3f(hip).add(10,0,0)}){
                var s=MechLegIk.solve(leg,hip,target,new Vector3f(1,0,0));assertTrue(Float.isFinite(s.knee().length()));
                assertEquals(leg.l1(),hip.distance(s.knee()),1e-4);assertEquals(leg.l2(),s.knee().distance(s.ankle()),1e-4);
            }
        }
    }
}
