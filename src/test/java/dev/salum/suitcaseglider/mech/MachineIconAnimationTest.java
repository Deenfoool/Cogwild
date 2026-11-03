package dev.salum.suitcaseglider.mech;
import dev.salum.suitcaseglider.machine.MachineIconAnimation;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MachineIconAnimationTest {
    @Test void bothMechanismsAnimateAndWrapWithoutDependingOnFrameRate(){
        for(boolean fan:new boolean[]{false,true}){
            var clock=new MachineIconAnimation(fan);java.util.Set<Integer> frames=new java.util.HashSet<>();
            for(int i=0;i<180;i++){clock.tick(true);for(float p:new float[]{0,.25f,.5f,.75f}){int frame=clock.frame(true,p);assertTrue(frame>=0&&frame<24);frames.add(frame);}}
            assertEquals(24,frames.size());
        }
    }
    @Test void idleAndPowerOrOutputPausesFreezeLastVisibleFrameAndResume(){
        for(boolean fan:new boolean[]{false,true}){
            var clock=new MachineIconAnimation(fan);for(int i=0;i<100;i++){clock.tick(false);assertEquals(0,clock.frame(false,.7f));}
            for(int i=0;i<7;i++)clock.tick(true);int moving=clock.frame(true,.5f);assertNotEquals(0,moving);
            for(int i=0;i<100;i++){clock.tick(false);assertEquals(moving,clock.frame(false,i/100f));}
            clock.tick(true);assertNotEquals(moving,clock.frame(true,.5f));
        }
    }
}
