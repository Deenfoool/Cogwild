package dev.salum.suitcaseglider.mech;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MechRigTest {
    @Test void allClipsHaveFinitePosesAndRequiredAnchors(){
        var rig=MechRig.get();assertEquals(23,rig.clips.size());
        for(var entry:rig.clips.entrySet())for(int i=0;i<=8;i++){
            var pose=rig.pose(entry.getKey(),entry.getValue().length()*i/8,entry.getKey(),0,1);
            for(var matrix:pose.bones){float[] values=new float[16];matrix.get(values);for(float value:values)assertTrue(Float.isFinite(value),entry.getKey());}
            for(String name:new String[]{"rider_seat_anchor","boarding_step_left","rider_hand_left","rider_hand_right","exhaust_underbody","rider_boarding_pelvis"})assertTrue(Float.isFinite(pose.anchor(name).length()));
        }
    }
    @Test void gripsFollowControlLeverInput(){
        var rig=MechRig.get();var rest=rig.pose("ridden_idle",0,"ridden_idle",0,1);var pushed=rig.pose("ridden_idle",0,"ridden_idle",0,1,1,1);
        assertTrue(rest.anchor("rider_hand_left").distance(pushed.anchor("rider_hand_left"))>.02);
        assertTrue(rest.anchor("rider_seat_anchor").distance(pushed.anchor("rider_seat_anchor"))<.00001);
    }
    @Test void boardingPathReachesSeat(){
        var rig=MechRig.get();var start=rig.pose("mount",0,"mount",0,1);var end=rig.pose("mount",3.2,"mount",0,1);
        assertTrue(start.anchor("rider_boarding_pelvis").x<-.7);
        assertTrue(end.anchor("rider_boarding_pelvis").distance(end.anchor("rider_seat_anchor"))<.2);
    }
    @Test void chargingLowersSeatWithoutLiftingTheFeet(){
        var rig=MechRig.get();var neutral=rig.pose("jump_charge",0,"jump_charge",0,1);var loaded=rig.pose("jump_charge",.65,"jump_charge",0,1);
        assertTrue(neutral.anchor("rider_seat_anchor").y-loaded.anchor("rider_seat_anchor").y>.25);
        for(String side:new String[]{"left","right"})for(int i=1;i<=3;i++)assertTrue(neutral.bonePosition("foot_"+side+"_"+i).distance(loaded.bonePosition("foot_"+side+"_"+i))<.00001);
    }
    @Test void mountedForwardAndReverseActuallyMoveLegBones(){
        var rig=MechRig.get();for(String name:new String[]{"ridden_walk","ridden_walk_backward"}){
            var a=rig.pose(name,0,name,0,1);var b=rig.pose(name,.2,name,0,1);
            assertTrue(a.bonePosition("foot_left_1").distance(b.bonePosition("foot_left_1"))>.02,name);
        }
    }
}
