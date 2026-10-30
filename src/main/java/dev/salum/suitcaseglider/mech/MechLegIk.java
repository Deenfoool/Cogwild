package dev.salum.suitcaseglider.mech;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class MechLegIk {
    public record Leg(int upper,int shin,int foot,int side,int row,float phase,Vector3f hip,Vector3f knee,Vector3f ankle,float l1,float l2){}
    public record Solution(Vector3f knee,Vector3f ankle,float error){}
    public static Leg leg(MechRig rig,String side,int row){
        String suffix=side+"_"+row;Vector3f h=rig.origin("leg_"+suffix),k=rig.origin("shin_"+suffix),a=rig.origin("foot_"+suffix);
        return new Leg(rig.boneIndex("leg_"+suffix),rig.boneIndex("shin_"+suffix),rig.boneIndex("foot_"+suffix),side.equals("left")?-1:1,row,((row+(side.equals("left")?0:1))%2)*.5f,h,k,a,h.distance(k),k.distance(a));
    }
    public static Solution solve(Leg leg,Vector3f hip,Vector3f target,Vector3f pole){
        Vector3f axis=new Vector3f(target).sub(hip);float wanted=axis.length(),distance=Math.max(Math.abs(leg.l1-leg.l2)+.001f,Math.min(wanted,leg.l1+leg.l2-.001f));if(wanted<.00001f)axis.set(0,-1,0);else axis.div(wanted);
        Vector3f ankle=new Vector3f(hip).fma(distance,axis),bend=new Vector3f(pole).fma(-pole.dot(axis),axis);
        if(bend.lengthSquared()<.00001f)bend.set(leg.side,0,0).fma(-leg.side*axis.x,axis);if(bend.lengthSquared()<.00001f)bend.set(0,1,0).fma(-axis.y,axis);bend.normalize();
        float along=(leg.l1*leg.l1-leg.l2*leg.l2+distance*distance)/(2*distance),height=(float)Math.sqrt(Math.max(0,leg.l1*leg.l1-along*along));
        return new Solution(new Vector3f(hip).fma(along,axis).fma(height,bend),ankle,Math.max(0,wanted-distance));
    }
    public static void apply(MechRig.Pose pose,Leg leg,Vector3f hip,Solution result,float footYaw){
        Quaternionf upper=new Quaternionf().rotationTo(new Vector3f(leg.knee).sub(leg.hip).normalize(),new Vector3f(result.knee).sub(hip).normalize());
        Quaternionf shin=new Quaternionf().rotationTo(new Vector3f(leg.ankle).sub(leg.knee).normalize(),new Vector3f(result.ankle).sub(result.knee).normalize());
        pose.replaceSubtree(leg.upper,new Matrix4f().translation(hip).rotate(upper));
        pose.replaceSubtree(leg.shin,new Matrix4f().translation(result.knee).rotate(shin));
        pose.replaceSubtree(leg.foot,new Matrix4f().translation(result.ankle).rotateY(footYaw));
    }
}
