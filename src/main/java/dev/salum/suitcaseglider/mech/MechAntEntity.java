package dev.salum.suitcaseglider.mech;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.horse.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Horse AI, food, temper, ownership, inventory, saddle, leash and ridden controls are inherited. */
public final class MechAntEntity extends Horse {
    private static final EntityDataAccessor<String> CLIP=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> PREVIOUS=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Long> START=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> OLD_TIME=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> THROTTLE=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> STEER=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> CHARGING=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> CHARGE=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GAIT_CLOCK=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> GAIT_RATE=SynchedEntityData.defineId(MechAntEntity.class,EntityDataSerializers.FLOAT);
    private final MechGrounding grounding=new MechGrounding(this);
    private boolean allowDismount;
    private boolean wasMoving;
    private boolean wasBackward;
    private float priorYaw;
    private double priorY;
    private double lastGroundY;
    private boolean previouslyGrounded;
    private double priorX,priorZ;
    private int movingGrace;
    private long lastRiderReport=Long.MIN_VALUE/2;
    private float reportedForward,reportedStrafe,reportedSpeed;
    private boolean reportedGrounded,clientJumpHeld,jumpActive,sawAirborne;
    private long jumpStarted;
    public MechAntEntity(EntityType<? extends Horse> type,Level level){super(type,level);setMaxUpStep(1);setPersistenceRequired();matchWalkAttribute();}
    private void matchWalkAttribute(){getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED).setBaseValue(MechGait.nativeSpeed(MechGait.FORWARD,.6,1));}
    @Override protected void randomizeAttributes(net.minecraft.util.RandomSource random){super.randomizeAttributes(random);matchWalkAttribute();}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(CLIP,"idle");entityData.define(PREVIOUS,"idle");entityData.define(START,0L);entityData.define(OLD_TIME,0f);entityData.define(THROTTLE,0f);entityData.define(STEER,0f);entityData.define(CHARGING,false);entityData.define(CHARGE,0f);entityData.define(GAIT_CLOCK,0f);entityData.define(GAIT_RATE,1f);}
    @Override public boolean isSilent(){return true;}
    @Override public void playSound(SoundEvent sound,float volume,float pitch){}
    @Override public void playSound(SoundEvent sound){}
    public String clip(){return entityData.get(CLIP);}
    public double elapsed(float partial){return Math.max(0,(level().getGameTime()+partial-entityData.get(START))/20.0);}
    public float gaitRate(){return entityData.get(GAIT_RATE);}
    public MechGrounding.Debug terrainDebug(){return grounding.debug();}
    public double animationTime(float partial){double t=elapsed(partial);if(MechGait.loop(clip()))return entityData.get(GAIT_CLOCK)+partial*gaitRate()/20;if(clip().equals("jump_charge"))return entityData.get(CHARGE)*MechRig.get().duration("jump_charge");if(clip().startsWith("start_")||clip().startsWith("stop_"))return t*MechGait.TEMPO;return clip().startsWith("step_")?t*MechRig.get().duration(clip())/1.65:t;}
    public MechRig.Pose rawRigPose(float partial){float blend=Mth.clamp((float)elapsed(partial)/.18f,0,1);return MechRig.get().pose(clip(),animationTime(partial),entityData.get(PREVIOUS),entityData.get(OLD_TIME)+elapsed(partial),blend,entityData.get(THROTTLE),entityData.get(STEER));}
    public MechRig.Pose rigPose(float partial){return grounding.apply(rawRigPose(partial),partial,true);}
    public void animate(String clip){
        if(level().isClientSide||clip().equals(clip))return;String old=clip();float oldTime=(float)animationTime(0);entityData.set(PREVIOUS,old);entityData.set(OLD_TIME,oldTime);
        if(MechGait.loop(clip)){float start=MechGait.loop(old)?oldTime/(float)MechRig.get().duration(old)*(float)MechRig.get().duration(clip):old.startsWith("start_")?(MechGait.reverse(clip)?.36f:.3f):0;entityData.set(GAIT_CLOCK,start);}
        entityData.set(CLIP,clip);entityData.set(START,level().getGameTime());
    }
    public boolean boarding(){return clip().equals("mount")||clip().equals("dismount");}
    @Override public boolean isImmobile(){return boarding()||super.isImmobile();}
    @Override protected Vec3 getRiddenInput(Player rider,Vec3 input){
        if(boarding()||(level().isClientSide?clientJumpHeld:entityData.get(CHARGING)))return Vec3.ZERO;
        Vec3 v=super.getRiddenInput(rider,input);double maximum=rider.zza<0?.25:1;return v.horizontalDistance()>maximum?v.scale(maximum/v.horizontalDistance()):v;
    }
    @Override protected float getRiddenSpeed(Player rider){
        var pos=getBlockPosBelowThatAffectsMyMovement();double friction=Mth.clamp(level().getBlockState(pos).getFriction(level(),pos,this),.2f,.98f);
        boolean reverse=rider.zza<-.01f;return (float)(MechGait.nativeSpeed(reverse?MechGait.BACKWARD:MechGait.FORWARD,friction,reverse?.25:1)*grounding.speedFactor());
    }
    Vec3 movementIntent(){
        float forward=reportedForward,side=reportedStrafe;
        if(level().getGameTime()-lastRiderReport>8&&getControllingPassenger() instanceof Player player){forward=player.zza;side=player.xxa;}
        Vec3 v=new Vec3(side,0,forward);return v.lengthSqr()<.0001?Vec3.ZERO:v.normalize().yRot((float)Math.toRadians(-getYRot()));
    }
    @Override protected boolean canPerformRearing(){return false;}
    public void predictJumpHeld(boolean held){clientJumpHeld=held;}
    public float jumpCharge(){return entityData.get(CHARGE);}
    public void riderMotion(float forward,float strafe,boolean held,float charge,boolean grounded,float speed){
        if(level().isClientSide||!isVehicle())return;
        lastRiderReport=level().getGameTime();reportedForward=Mth.clamp(forward,-1,1);reportedStrafe=Mth.clamp(strafe,-1,1);reportedGrounded=grounded;reportedSpeed=Mth.clamp(speed,0,1);
        if(boarding())return;
        boolean old=entityData.get(CHARGING);
        if(held&&grounded&&!jumpActive&&!clip().equals("jump_land")){
            if(!old){entityData.set(CHARGE,0f);wasMoving=false;movingGrace=0;}
            entityData.set(CHARGING,true);entityData.set(CHARGE,Math.max(entityData.get(CHARGE),Mth.clamp(charge,.05f,1)));animate("jump_charge");
        }else if(!held&&old){entityData.set(CHARGING,false);beginJump();}
    }
    private void beginJump(){
        if(level().isClientSide||boarding()||!isSaddled()||jumpActive)return;
        entityData.set(CHARGING,false);jumpActive=true;sawAirborne=false;wasMoving=false;movingGrace=0;jumpStarted=level().getGameTime();animate("jump_release");
    }
    @Override public void onPlayerJump(int power){if(!boarding())super.onPlayerJump(power);}
    @Override public void handleStartJump(int power){if(boarding())return;super.handleStartJump(power);beginJump();}
    @Override protected void doPlayerRide(Player player){if(!isVehicle())animate("mount");super.doPlayerRide(player);}
    public boolean deferDismount(Player player){
        // Client cancellation also cancelled the server's final passenger packet, leaving a ghost rider.
        if(level().isClientSide||allowDismount||!isTamed()||!isAlive()||!player.isAlive()||!onGround())return false;
        if(clip().equals("mount")&&elapsed(0)<1.75)return false;
        entityData.set(CHARGING,false);jumpActive=false;
        if(!clip().equals("dismount"))animate("dismount");return true;
    }
    public void requestDismount(Player player){
        if(level().isClientSide||player.getVehicle()!=this)return;
        entityData.set(CHARGING,false);jumpActive=false;
        if(deferDismount(player))return;
        allowDismount=true;try{player.stopRiding();}finally{allowDismount=false;}
        if(!isVehicle())animate("idle");
    }
    @Override protected void positionRider(Entity passenger,Entity.MoveFunction move){
        if(!hasPassenger(passenger))return;String anchor=boarding()?"rider_boarding_pelvis":"rider_seat_anchor";
        Vec3 local=localAnchor(anchor,0).add(0,-.75*(isBaby()?.5:1),0);Vec3 p=position().add(local);move.accept(passenger,p.x,p.y,p.z);
        if(passenger instanceof LivingEntity living){living.yBodyRot=getYRot();living.yBodyRotO=getYRot();}
    }
    public Vec3 localAnchor(String name,float partial){Vector3f p=grounding.apply(rawRigPose(partial),1,false).anchor(name);float scale=isBaby()?.5f:1;return new Vec3(p.x*scale,p.y*scale,p.z*scale).yRot((float)Math.toRadians(180-getYRot()));}
    public Vec3 worldAnchor(String name,float partial){return position().add(localAnchor(name,partial));}
    @Override public AgeableMob getBreedOffspring(ServerLevel level,AgeableMob other){var child=MechContent.ANT.get().create(level);if(child!=null&&other instanceof Horse horse)setOffspringAttributes(child,horse);return child;}
    @Override public void tick(){
        super.tick();setMaxUpStep(1);yBodyRot=getYRot();yBodyRotO=yRotO;
        if(level().isClientSide){grounding.tick();if(tickCount%3==0&&isAlive()){Vec3 p=worldAnchor("exhaust_underbody",0);Vec3 back=new Vec3(0,.03,.07).yRot((float)Math.toRadians(180-getYRot()));for(int i=0;i<2;i++)level().addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE,p.x+random.nextGaussian()*.025,p.y,p.z+random.nextGaussian()*.025,back.x,.04+random.nextDouble()*.025,back.z);}return;}
        try {
        double dy=getY()-priorY,dx=getX()-priorX,dz=getZ()-priorZ;priorY=getY();priorX=getX();priorZ=getZ();float turn=Mth.wrapDegrees(getYRot()-priorYaw);priorYaw=getYRot();
        boolean fresh=isVehicle()&&level().getGameTime()-lastRiderReport<8;
        double speed=Math.sqrt(dx*dx+dz*dz)*20;boolean reverse=fresh?reportedForward<-.01:MechGait.reverse(clip());
        float rate=(float)Mth.clamp(speed*MechGait.TEMPO/(reverse?MechGait.BACKWARD:MechGait.FORWARD),0,1.2*MechGait.TEMPO);
        if(clip().startsWith("pivot_"))rate=Math.max(rate,Mth.clamp(Math.abs(turn)/3,.2f,1.5f));
        entityData.set(GAIT_RATE,rate);if(MechGait.loop(clip()))entityData.set(GAIT_CLOCK,entityData.get(GAIT_CLOCK)+rate/20);
        double drop=lastGroundY-getY();boolean landed=onGround()&&!previouslyGrounded;previouslyGrounded=onGround();if(onGround())lastGroundY=getY();
        if(getControllingPassenger() instanceof Player player&&!boarding()){entityData.set(THROTTLE,fresh?reportedForward:player.zza);entityData.set(STEER,Mth.clamp((fresh?reportedStrafe:player.xxa)+turn/6,-1,1));}else{entityData.set(THROTTLE,0f);entityData.set(STEER,0f);}
        if(boarding()){
            if(elapsed(0)<3.2)return;
            if(clip().equals("dismount")){allowDismount=true;try{for(Entity rider:java.util.List.copyOf(getPassengers()))rider.stopRiding();}finally{allowDismount=false;}}
            animate(isVehicle()?"ridden_idle":"idle");return;
        }
        if(jumpActive){
            boolean ground=fresh?reportedGrounded:onGround();
            if(!ground||dy>.06)sawAirborne=true;
            long age=level().getGameTime()-jumpStarted;
            if(sawAirborne&&ground&&age>3){jumpActive=false;animate("jump_land");return;}
            if(!sawAirborne&&age>12){jumpActive=false;animate("jump_land");return;}
            if(age>=5)animate("jump_air");return;
        }
        if(clip().equals("jump_land")&&elapsed(0)<MechRig.get().duration("jump_land"))return;
        if(entityData.get(CHARGING)){
            if(fresh&&isVehicle()){animate("jump_charge");return;}
            entityData.set(CHARGING,false);
        }
        String now=clip();double duration=now.startsWith("step_")?1.65:MechRig.get().duration(now)/((now.startsWith("start_")||now.startsWith("stop_"))?MechGait.TEMPO:1);
        if((now.startsWith("start_")||now.startsWith("stop_")||now.startsWith("step_"))&&elapsed(0)<duration)return;
        double distance=dx*dx+dz*dz;
        if((distance>.00000025&&distance<4)||getDeltaMovement().horizontalDistanceSqr()>.00000025||(fresh&&reportedSpeed>.0005))movingGrace=4;else movingGrace=Math.max(0,movingGrace-1);
        boolean moving=movingGrace>0;
        double along=dx*-Math.sin(Math.toRadians(getYRot()))+dz*Math.cos(Math.toRadians(getYRot()));
        boolean backwards=isVehicle()&&(fresh?reportedForward<-.05:getControllingPassenger() instanceof Player rider&&rider.zza<-.05||along<-.01);
        // Collision stepping moves the entity; six independent terrain contacts now place the model.
        if(moving&&!wasMoving){animate(backwards?"start_backward":"start_move");wasMoving=true;wasBackward=backwards;return;}
        if(!moving&&wasMoving){animate(wasBackward?"stop_backward":"stop_move");wasMoving=false;return;}
        if(moving){animate(backwards?(isVehicle()?"ridden_walk_backward":"walk_backward"):Math.abs(turn)>1.5?(turn>0?"turn_right":"turn_left"):(isVehicle()?"ridden_walk":"walk"));wasBackward=backwards;}
        else animate(Math.abs(turn)>2?(turn>0?"pivot_right":"pivot_left"):isVehicle()?"ridden_idle":"idle");
        } finally {grounding.tick();}
    }
    @Override public void addAdditionalSaveData(CompoundTag nbt){super.addAdditionalSaveData(nbt);nbt.putString("MechRigVersion","three-section-1");}
    @Override public void readAdditionalSaveData(CompoundTag nbt){super.readAdditionalSaveData(nbt);setMaxUpStep(1);matchWalkAttribute();}
}
