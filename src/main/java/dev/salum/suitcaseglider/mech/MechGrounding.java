package dev.salum.suitcaseglider.mech;

import java.util.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Six world-space contact constraints. Animation supplies rhythm; collision shapes supply support. */
public final class MechGrounding {
    private static final Vector3f PIVOT=new Vector3f(0,22f/16,0);
    private static final class Foot {
        final MechLegIk.Leg leg;Vec3 point,previous,from,end;double floor,fromFloor,start,duration;float yaw;boolean valid,swing,authoredAir;
        Foot(MechLegIk.Leg leg){this.leg=leg;}
    }
    private record Hull(int bone,Vector3f vertex){}
    private record Landing(Vec3 point,double floor,boolean valid){}
    private record Plan(float pitch,float roll,float height,float low,float high,double gap,double cost){}
    public record Debug(int planted,double maxPlantError,double lowestSupport,double highestSupport,float height,float pitch,float roll,float speedFactor,int longestTransfer){}
    private final MechAntEntity entity;
    private final MechSurfaceProbe probe;
    private Foot[] feet;private List<Hull> hull;
    private boolean initialized,grounded;
    private Vec3 origin=Vec3.ZERO,motion=Vec3.ZERO;private float lastYaw;
    private float height,previousHeight,pitch,previousPitch,roll,previousRoll,speedFactor=1;
    private double diagnosticPeak;
    private Debug debug=new Debug(0,0,0,0,0,0,0,1,0);
    public MechGrounding(MechAntEntity entity){this.entity=entity;probe=new MechSurfaceProbe(entity);}
    public float speedFactor(){return speedFactor;}
    public Debug debug(){return debug;}
    private float scale(){return entity.isBaby()?.5f:1;}
    private void prepare(){
        if(feet!=null)return;var rig=MechRig.get();feet=new Foot[6];int i=0;
        for(String side:new String[]{"left","right"})for(int row=1;row<=3;row++)feet[i++]=new Foot(MechLegIk.leg(rig,side,row));
        var rest=rig.pose("idle",0,"idle",0,1);Map<String,Hull> cells=new HashMap<>();Map<String,Float> lows=new HashMap<>();
        for(var face:rig.faces){boolean leg=false;for(Foot f:feet)if(rig.descendant(face.bone(),f.leg.upper())){leg=true;break;}if(leg)continue;
            for(float[] v:face.vertices()){var p=rest.bones[face.bone()].transformPosition(new Vector3f(v));if(p.y>1.3f)continue;String key=Mth.floor(p.x*2)+","+Mth.floor(p.z*2);if(p.y<lows.getOrDefault(key,Float.MAX_VALUE)){lows.put(key,p.y);cells.put(key,new Hull(face.bone(),new Vector3f(v)));}}
        }hull=new ArrayList<>(cells.values());
    }
    private static Matrix4f body(float y,float pitch,float roll){return new Matrix4f().translation(0,y,0).translate(PIVOT).rotateX((float)Math.toRadians(pitch)).rotateZ((float)Math.toRadians(roll)).translate(-PIVOT.x,-PIVOT.y,-PIVOT.z);}
    private Vec3 toWorld(Vector3f local,Vec3 base,float yaw){return new Vec3(local.x*scale(),local.y*scale(),local.z*scale()).yRot((float)Math.toRadians(180-yaw)).add(base);}
    private Vector3f toModel(Vec3 world,Vec3 base,float yaw){Vec3 v=world.subtract(base).yRot((float)Math.toRadians(yaw-180)).scale(1/scale());return new Vector3f((float)v.x,(float)v.y,(float)v.z);}
    private Landing landing(Foot foot,Vector3f model,Vec3 base,float yaw,Vec3 direction){
        Vec3 world=toWorld(model,base,yaw);var surface=probe.at(world.x,world.z);
        if(direction.horizontalDistanceSqr()>.00001){Vec3 ahead=world.add(direction.scale(.24));var next=probe.at(ahead.x,ahead.z);
            if(next.valid()&&surface.valid()&&Math.abs(next.height()-surface.height())>.18){world=ahead;surface=next;}}
        if(surface.valid()&&(Math.abs(surface.height()-entity.getY())>.25||speedFactor<.9f)){
            // Tuck the stance inward on an edge, reserving leg reach for the vertical step.
            Vector3f narrow=toModel(world,base,yaw);var leg=foot.leg;
            narrow.x=Mth.lerp(.8f,narrow.x,leg.hip().x+leg.side()*.35f);
            Vec3 tucked=toWorld(narrow,base,yaw);var support=probe.at(tucked.x,tucked.z);
            if(support.valid()&&Math.abs(support.height()-surface.height())<.1){world=tucked;surface=support;}
            narrow.z=Mth.lerp(.65f,narrow.z,leg.hip().z+Mth.clamp(leg.ankle().z-leg.hip().z,-.45f,.45f));
            tucked=toWorld(narrow,base,yaw);support=probe.at(tucked.x,tucked.z);
            if(support.valid()&&Math.abs(support.height()-surface.height())<.1){world=tucked;surface=support;}
        }
        if(!surface.valid())return new Landing(world,0,false);
        return new Landing(new Vec3(world.x,surface.height()+.125*scale(),world.z),surface.height(),true);
    }
    private void start(Foot f,Landing end,double now,double ticks){
        if(!end.valid)return;f.from=f.point;f.fromFloor=f.floor;f.end=end.point;f.floor=end.floor;f.start=now;f.duration=Math.max(ticks,Math.abs(end.floor-f.fromFloor)>.15?Math.max(8,14/Math.sqrt(MechGait.TEMPO)):Math.max(2,6/MechGait.TEMPO));f.swing=true;f.valid=true;
    }
    private void swing(Foot f,double now){
        double u=Mth.clamp((now-f.start)/f.duration,0,1),s=u*u*(3-2*u),rise=f.floor-f.fromFloor,y;
        double h;
        if(rise>.15){h=smooth((u-.3)/.5);double top=Math.max(f.from.y,f.end.y)+.18*scale();y=u<.4?mix(f.from.y,top,smooth(u/.4)):u<.76?top:mix(top,f.end.y,smooth((u-.76)/.24));}
        else if(rise<-.15){h=smooth(u/.7);double top=f.from.y+.14*scale();y=u<.25?mix(f.from.y,top,smooth(u/.25)):u<.58?top:mix(top,f.end.y,smooth((u-.58)/.42));}
        else{h=s;y=mix(f.from.y,f.end.y,s)+.15*scale()*Math.sin(Math.PI*u);}
        f.point=new Vec3(mix(f.from.x,f.end.x,h),y,mix(f.from.z,f.end.z,h));
        if(u>=1)f.point=f.end;
    }
    private static double smooth(double t){t=Mth.clamp(t,0,1);return t*t*(3-2*t);}
    private static double mix(double a,double b,double u){return a+(b-a)*u;}
    public void tick(){
        prepare();probe.reset();Vec3 pos=entity.position(),change=pos.subtract(origin);float yaw=entity.getYRot();
        previousHeight=height;previousPitch=pitch;previousRoll=roll;
        boolean air=entity.clip().equals("jump_release")||entity.clip().equals("jump_air")||(!entity.onGround()&&Math.abs(change.y)>.36);
        if(air){grounded=false;initialized=false;height*=.7f;pitch*=.7f;roll*=.7f;speedFactor=1;origin=pos;lastYaw=yaw;return;}
        var raw=entity.rawRigPose(0);boolean reset=!initialized||change.horizontalDistance()>.75||Math.abs(change.y)>1.25||Math.abs(Mth.wrapDegrees(yaw-lastYaw))>45;
        if(!reset&&grounded)height-=(float)(change.y/scale());
        motion=reset?Vec3.ZERO:new Vec3(change.x,0,change.z);
        Vec3 direction=motion.horizontalDistanceSqr()>.000001?motion.normalize():entity.movementIntent();
        boolean resting=MechGait.resting(entity.clip())&&motion.horizontalDistanceSqr()<.00000225&&Math.abs(change.y)<.015&&Math.abs(Mth.wrapDegrees(yaw-lastYaw))<.15;
        var rig=MechRig.get();double time=entity.animationTime(0),length=rig.duration(entity.clip()),rate=Math.max(.2,entity.gaitRate());
        if(reset){height=previousHeight=0;pitch=previousPitch=roll=previousRoll=0;
            for(Foot f:feet){Landing l=landing(f,raw.bonePosition(rig.bones.get(f.leg.foot()).name()),pos,yaw,Vec3.ZERO);f.point=f.previous=f.from=f.end=l.point;f.floor=f.fromFloor=l.floor;f.valid=l.valid;f.swing=f.authoredAir=false;f.yaw=yaw;}initialized=true;
        }
        int airborne=0;for(Foot f:feet)if(f.swing)airborne++;
        for(Foot f:feet){f.previous=f.point;float phase=(float)((time/length+f.leg.phase())%1);if(phase<0)phase++;
            boolean sourceAir=!resting&&(MechGait.loop(entity.clip())?phase<.26:raw.bones[f.leg.foot()].m31()>f.leg.ankle().y+.025);
            double remaining=MechGait.loop(entity.clip())?Math.max(.02,(.26-phase)*length):.25;
            boolean wasSwing=f.swing;
            if(sourceAir&&!f.authoredAir&&!f.swing&&airborne<3){
                double ticks=remaining/rate*20;var future=rig.pose(entity.clip(),time+remaining,entity.clip(),time,1);
                Vec3 forecast=pos.add(motion.scale(ticks));Landing end=landing(f,future.bonePosition(rig.bones.get(f.leg.foot()).name()),forecast,yaw,direction);start(f,end,entity.tickCount,ticks);
            }
            f.authoredAir=sourceAir&&(f.swing||f.authoredAir);
            if(f.swing)swing(f,entity.tickCount);else if(f.valid){var below=probe.at(f.point.x,f.point.z);if(!below.valid()||Math.abs(below.height()-f.floor)>=.15)f.valid=false;else{f.floor=below.height();f.point=new Vec3(f.point.x,f.floor+.125*scale(),f.point.z);}}
            if(f.swing&&!wasSwing)airborne++;else if(!f.swing&&wasSwing)airborne--;
        }
        // A terrain change or a turn may require a recovery step before the authored swing window.
        if(airborne<3){Foot candidate=null;Landing target=null;double best=.2;
            for(Foot f:feet)if(!f.swing){
                // Idle motion belongs to the chassis. Existing, valid world contacts stay planted.
                if(resting&&f.valid)continue;
                Landing next=landing(f,raw.bonePosition(rig.bones.get(f.leg.foot()).name()),pos.add(motion.scale(8/MechGait.TEMPO)),yaw,direction);
                if(!next.valid)continue;double score=f.valid?new Vec3(next.point.x-f.point.x,0,next.point.z-f.point.z).length():1;
                if(f.valid&&direction.lengthSqr()>.001&&Math.abs(next.floor-f.floor)>.18)score+=.8;
                Vector3f h=body(height,pitch,roll).transformPosition(raw.bones[f.leg.upper()].getTranslation(new Vector3f()));
                double reach=h.distance(toModel(f.point,pos,yaw))/(f.leg.l1()+f.leg.l2());
                if(f.valid&&reach>.88)score+=Math.max(0,reach-.88)*12;
                if(score>best){best=score;candidate=f;target=next;}}
            if(candidate!=null)start(candidate,target,entity.tickCount,8/MechGait.TEMPO);
        }
        double min=Double.POSITIVE_INFINITY,max=Double.NEGATIVE_INFINITY;int valid=0;
        for(Foot f:feet)if(f.valid){valid++;min=Math.min(min,Math.min(f.floor,f.swing?f.fromFloor:f.floor));max=Math.max(max,Math.max(f.floor,f.swing?f.fromFloor:f.floor));}
        if(valid<3){grounded=false;initialized=false;height*=.8f;pitch*=.8f;roll*=.8f;speedFactor=1;origin=pos;lastYaw=yaw;return;}
        grounded=true;speedFactor=(float)(1-.55*Mth.clamp((max-min)/.75,0,1));
        Plan plan=plan(raw,pos,yaw,min,max);pitch=plan.pitch;roll=plan.roll;
        height=(float)mix(height,plan.height,.55);
        if(plan.gap<.001)height=Mth.clamp(height,plan.low,plan.high);
        origin=pos;lastYaw=yaw;
        // A completed transfer becomes a contact only once the chassis makes it reachable.
        Matrix4f chassis=body(height,pitch,roll);
        for(Foot f:feet)if(f.valid&&f.swing&&entity.tickCount>=f.start+f.duration){
            Vector3f hip=chassis.transformPosition(raw.bones[f.leg.upper()].getTranslation(new Vector3f()));
            float reach=hip.distance(toModel(f.end,pos,yaw));
            if(reach<f.leg.l1()+f.leg.l2()-.002f&&reach>Math.abs(f.leg.l1()-f.leg.l2())+.002f){f.swing=false;f.yaw=yaw;}
            else if(entity.tickCount>f.start+f.duration+3){
                Landing replacement=landing(f,raw.bonePosition(rig.bones.get(f.leg.foot()).name()),pos,yaw,direction);
                if(replacement.valid&&replacement.point.distanceTo(f.end)>.12)start(f,replacement,entity.tickCount,8/MechGait.TEMPO);
            }
        }
        var evaluated=apply(entity.rawRigPose(0),1,false);int planted=0;double error=0,plantedMin=Double.POSITIVE_INFINITY,plantedMax=Double.NEGATIVE_INFINITY;
        for(Foot f:feet)if(f.valid&&!f.swing){planted++;plantedMin=Math.min(plantedMin,f.floor);plantedMax=Math.max(plantedMax,f.floor);Vec3 actual=toWorld(evaluated.bones[f.leg.foot()].getTranslation(new Vector3f()),pos,yaw);error=Math.max(error,actual.distanceTo(f.point));}
        int longest=0;for(Foot f:feet)if(f.swing)longest=Math.max(longest,(int)(entity.tickCount-f.start));
        debug=new Debug(planted,error,plantedMin,plantedMax,height,pitch,roll,speedFactor,longest);
        if(Boolean.getBoolean("mechant.terrainQa")&&error>.02&&(entity.tickCount%20==0||error>diagnosticPeak+.005)){
            diagnosticPeak=Math.max(diagnosticPeak,error);
            StringBuilder details=new StringBuilder("gap="+plan.gap+" low="+plan.low+" high="+plan.high);
            for(Foot f:feet)if(f.valid&&!f.swing){Vector3f h=evaluated.bones[f.leg.upper()].getTranslation(new Vector3f());Vec3 actual=toWorld(evaluated.bones[f.leg.foot()].getTranslation(new Vector3f()),pos,yaw);details.append(" leg").append(f.leg.side()).append(':').append(f.leg.row()).append(" err=").append(actual.distanceTo(f.point)).append(" hip=").append(h).append(" target=").append(toModel(f.point,pos,yaw));}
            dev.salum.suitcaseglider.SuitcaseGlider.LOG.info("MECH_CONTACT_DIAGNOSTIC z={} {}",pos.z,details);
        }
    }
    private Plan plan(MechRig.Pose raw,Vec3 pos,float yaw,double min,double max){
        double[][] normal=new double[3][4];normal[0][0]=normal[1][1]=.015;normal[2][2]=.0001;
        for(Foot f:feet)if(f.valid){Vector3f p=toModel(f.swing?f.end:f.point,pos,yaw);double w=f.swing?.22:1,yy=(f.floor-pos.y)/scale();double[] v={p.x,p.z,1};for(int r=0;r<3;r++){for(int c=0;c<3;c++)normal[r][c]+=w*v[r]*v[c];normal[r][3]+=w*v[r]*yy;}}
        for(int c=0;c<3;c++){int row=c;for(int r=c+1;r<3;r++)if(Math.abs(normal[r][c])>Math.abs(normal[row][c]))row=r;double[] swap=normal[c];normal[c]=normal[row];normal[row]=swap;double div=normal[c][c];if(Math.abs(div)<1e-9)continue;for(int k=c;k<4;k++)normal[c][k]/=div;for(int r=0;r<3;r++)if(r!=c){double factor=normal[r][c];for(int k=c;k<4;k++)normal[r][k]-=factor*normal[c][k];}}
        float desiredPitch=Mth.clamp((float)Math.toDegrees(Math.atan(-normal[1][3])),-28,28),desiredRoll=Mth.clamp((float)Math.toDegrees(Math.atan(normal[0][3])),-14,14);
        float nextRoll=roll+Mth.clamp(desiredRoll-roll,-3,3),wantedHeight=(float)(normal[2][3]-.14*Mth.clamp((max-min)/scale(),0,1));Plan best=null;
        for(float pp:new float[]{desiredPitch,pitch,desiredPitch-5,desiredPitch+5,desiredPitch-10,desiredPitch+10,desiredPitch-15,desiredPitch+15,desiredPitch-20,desiredPitch+20}){
            pp=Mth.clamp(pp,-32,32);for(float candidateRoll:new float[]{nextRoll,roll}){
            Matrix4f rotation=body(0,pp,candidateRoll);float low=-4,high=4,airLow=-4,airHigh=4;double horizontalGap=0,airHorizontal=0;
            for(Foot f:feet)if(f.valid){Vector3f h=rotation.transformPosition(raw.bones[f.leg.upper()].getTranslation(new Vector3f())),t=toModel(f.point,pos,yaw);double dx=h.x-t.x,dz=h.z-t.z,L=f.leg.l1()+f.leg.l2()-.004,dd=dx*dx+dz*dz;double horizontal=Math.max(0,Math.sqrt(dd)-L),vertical=Math.sqrt(Math.max(0,L*L-dd));float ll=(float)(t.y-h.y-vertical),hh=(float)(t.y-h.y+vertical);
                if(f.swing){airLow=Math.max(airLow,ll);airHigh=Math.min(airHigh,hh);airHorizontal=Math.max(airHorizontal,horizontal);}
                else{low=Math.max(low,ll);high=Math.min(high,hh);horizontalGap=Math.max(horizontalGap,horizontal);}}

            for(Hull sample:hull){Vector3f p=rotation.transformPosition(raw.bones[sample.bone].transformPosition(new Vector3f(sample.vertex)));Vec3 world=toWorld(p,pos,yaw);var floor=probe.at(world.x,world.z);if(floor.valid())low=Math.max(low,(float)((floor.height()+.025-pos.y)/scale()-p.y));}
            double gap=Math.max(0,low-high)+horizontalGap;float allLow=Math.max(low,airLow),allHigh=Math.min(high,airHigh);
            float hh=allLow<=allHigh?Mth.clamp(wantedHeight,allLow,allHigh):(allLow+allHigh)*.5f;
            hh=Mth.clamp(hh,Math.min(low,high),Math.max(low,high));
            double airGap=Math.max(0,airLow-hh)+Math.max(0,hh-airHigh)+airHorizontal;
            double cost=gap*100000+airGap*1000+Math.abs(hh-wantedHeight)*.3+Math.abs(pp-desiredPitch)*.02+Math.abs(pp-pitch)*.025+Math.abs(candidateRoll-desiredRoll)*.03;
            if(best==null||cost<best.cost)best=new Plan(pp,candidateRoll,hh,low,high,gap,cost);
            }
        }return best;
    }
    public MechRig.Pose apply(MechRig.Pose pose,float partial,boolean render){
        if(feet==null)return pose;float f=render?partial:1,hh=Mth.lerp(f,previousHeight,height),pp=Mth.lerp(f,previousPitch,pitch),rr=Mth.lerp(f,previousRoll,roll);
        Matrix4f delta=body(hh,pp,rr);pose.transform(delta);if(!initialized||!grounded)return pose;
        Vec3 pos=render?new Vec3(Mth.lerp(f,entity.xOld,entity.getX()),Mth.lerp(f,entity.yOld,entity.getY()),Mth.lerp(f,entity.zOld,entity.getZ())):entity.position();
        float yaw=render?Mth.rotLerp(f,entity.yRotO,entity.getYRot()):entity.getYRot();
        for(Foot foot:feet)if(foot.valid){var leg=foot.leg;Vec3 point=foot.previous.lerp(foot.point,f);Vector3f target=toModel(point,pos,yaw),hip=pose.bones[leg.upper()].getTranslation(new Vector3f()),pole=new Vector3f(leg.knee()).sub(leg.hip());
            delta.transformDirection(pole);float ratio=hip.distance(target)/(leg.l1()+leg.l2()),blend=(float)smooth((.95-ratio)/.25);pole.lerp(new Vector3f(leg.side()*1.25f,.75f,(leg.knee().z-leg.hip().z)*.5f),blend);
            MechLegIk.Solution solution=MechLegIk.solve(leg,hip,target,pole);double best=clearance(hip,solution,pos,yaw),bestCost=Math.max(0,-best)*10000;
            if(best<-.015){Vector3f axis=new Vector3f(target).sub(hip).normalize();for(float angle:new float[]{-30,30,-60,60,-90,90,135,-135,180}){
                Vector3f p=new Quaternionf().rotationAxis((float)Math.toRadians(angle),axis).transform(new Vector3f(pole));var candidate=MechLegIk.solve(leg,hip,target,p);if(leg.side()*(candidate.knee().x-hip.x)<.12)continue;
                double clear=clearance(hip,candidate,pos,yaw),cost=Math.max(0,-clear)*10000+Math.abs(angle)*.0002;
                if(cost<bestCost){bestCost=cost;solution=candidate;}
            }}
            float footYaw=foot.swing?0:(float)Math.toRadians(Mth.wrapDegrees(yaw-foot.yaw));MechLegIk.apply(pose,leg,hip,solution,footYaw);
        }return pose;
    }
    private double clearance(Vector3f hip,MechLegIk.Solution solution,Vec3 pos,float yaw){
        double min=Double.POSITIVE_INFINITY;Vector3f[] points={hip,solution.knee(),solution.ankle()};
        for(int s=0;s<2;s++)for(int i=0;i<=4;i++){float u=i/4f;Vector3f p=new Vector3f(points[s]).lerp(points[s+1],u);Vec3 world=toWorld(p,pos,yaw);var floor=probe.at(world.x,world.z);if(floor.valid())min=Math.min(min,world.y-floor.height()-(.09+.04*Math.sin(Math.PI*u))*scale());}
        return min;
    }
}
