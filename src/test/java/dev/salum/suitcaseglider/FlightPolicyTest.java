package dev.salum.suitcaseglider;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class FlightPolicyTest {
    @Test void shortJumpDoesNotOpen(){assertFalse(FlightPolicy.dangerous(.2,1,-.2,false,false,false,0));}
    @Test void threeBlocksRemainSafe(){assertFalse(FlightPolicy.dangerous(0,3,-.2,false,false,false,0));}
    @Test void fourBlockFallOpens(){assertTrue(FlightPolicy.dangerous(.1,4,-.2,false,false,false,0));}
    @Test void upwardMovementCannotDeploy(){assertFalse(FlightPolicy.dangerous(0,30,.2,false,false,false,0));}
    @Test void landingDoesNotDeploy(){assertFalse(FlightPolicy.dangerous(10,0,-.3,true,false,false,0));}
    @Test void waterOrSlowFallDoesNotDeploy(){assertFalse(FlightPolicy.dangerous(4,20,-.3,false,true,false,0));}
    @Test void creativeFlightDoesNotDeploy(){assertFalse(FlightPolicy.dangerous(4,20,-.3,false,false,true,0));}
    @Test void jumpBoostExtendsSafeHeight(){assertFalse(FlightPolicy.dangerous(0,4,-.3,false,false,false,2));}
    @Test void soaringOnlyReducesDescent(){assertTrue(FlightPolicy.sink(3,false)>0);assertTrue(FlightPolicy.sink(3,false)<FlightPolicy.sink(0,false));}
    @Test void deploymentHasGentlerDescent(){assertTrue(FlightPolicy.sink(0,true)<FlightPolicy.sink(0,false));}
    @Test void onlyAirbornePhasesBlockCombat(){assertFalse(FlightPolicy.airbornePhase(0));assertTrue(FlightPolicy.airbornePhase(1));assertTrue(FlightPolicy.airbornePhase(2));assertFalse(FlightPolicy.airbornePhase(3));}
}
