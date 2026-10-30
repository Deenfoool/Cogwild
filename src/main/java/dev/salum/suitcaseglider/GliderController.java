package dev.salum.suitcaseglider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.*;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class GliderController {
    private static final java.lang.reflect.Field FLOATING_TICKS=net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(net.minecraft.server.network.ServerGamePacketListenerImpl.class,"f_9737_");
    public static final int NONE=-1, CARRY=0, DEPLOY=1, FLIGHT=2, STOW=3;
    public static final int DEPLOY_TICKS=28, STOW_TICKS=24;
    public static boolean selected(Player p) { return p.getMainHandItem().getItem() instanceof GliderItem; }
    public static int phase(Player p) { return p.getPersistentData().contains("sg_phase") ? p.getPersistentData().getInt("sg_phase") : NONE; }
    public static boolean active(Player p) { return FlightPolicy.airbornePhase(phase(p)); }
    private static void set(ServerPlayer p, int phase) {
        CompoundTag tag=p.getPersistentData();
        if (phase(p)==phase && tag.contains("sg_started")) return;
        tag.putInt("sg_phase",phase); tag.putLong("sg_started",p.level().getGameTime());
        GliderNetwork.broadcast(p,phase,p.level().getGameTime());
        if (phase==DEPLOY || phase==STOW) p.level().playSound(null,p.blockPosition(),SoundEvents.ARMOR_EQUIP_LEATHER,SoundSource.PLAYERS,.7f,phase==DEPLOY?1.1f:.8f);
    }
    public static double clearance(Player p) {
        Vec3 start=p.position().add(0,.05,0);
        BlockHitResult hit=p.level().clip(new ClipContext(start,start.add(0,-64,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p));
        return hit.getType()==HitResult.Type.MISS ? 64 : Math.max(0,p.getY()-hit.getLocation().y);
    }
    private static boolean safeBelow(Player p) {
        Vec3 start=p.position().add(0,.05,0);
        BlockHitResult hit=p.level().clip(new ClipContext(start,start.add(0,-64,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.ANY,p));
        return p.isInWaterOrBubble() || p.isInLava() || p.hasEffect(MobEffects.SLOW_FALLING)
                || p.level().getFluidState(hit.getBlockPos()).is(FluidTags.WATER)
                || (p.level().getBlockState(hit.getBlockPos()).is(Blocks.SLIME_BLOCK) && !p.isShiftKeyDown());
    }
    @SubscribeEvent public void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase!=TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!selected(p) || !p.isAlive() || p.isSpectator() || p.getAbilities().flying || p.getAbilities().instabuild || p.isPassenger() || p.isInWaterOrBubble() || p.isInLava()) { set(p,NONE); return; }
        int state=phase(p); long elapsed=p.level().getGameTime()-p.getPersistentData().getLong("sg_started");
        if (state==NONE) { set(p,CARRY);state=CARRY; }
        if (p.onGround()) {
            if (FlightPolicy.airbornePhase(state)) set(p,STOW);
            else if (state==STOW && elapsed>=STOW_TICKS) set(p,CARRY);
            return;
        }
        if (state==STOW) { set(p,CARRY);state=CARRY; }
        int jump=p.hasEffect(MobEffects.JUMP)?p.getEffect(MobEffects.JUMP).getAmplifier()+1:0;
        if (state==CARRY && FlightPolicy.dangerous(p.fallDistance,clearance(p),p.getDeltaMovement().y,false,safeBelow(p),false,jump)) { set(p,DEPLOY);state=DEPLOY;elapsed=0; }
        if (state==DEPLOY && elapsed>=DEPLOY_TICKS) { set(p,FLIGHT);state=FLIGHT; }
        if (FlightPolicy.airbornePhase(state)) {
            p.stopFallFlying(); physics(p,state==DEPLOY);
            // A glider is authorized server-side movement; slow descent must not accrue vanilla hover ticks.
            try { FLOATING_TICKS.setInt(p.connection,0); }
            catch(IllegalAccessException ex) { throw new IllegalStateException("Cannot reset glider floating counter",ex); }
            if (p.tickCount%20==0) p.getMainHandItem().hurtAndBreak(1,p,who->who.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        }
    }
    public static void physics(Player p, boolean deploying) {
        ItemStack stack=p.getMainHandItem(); int soaring=EnchantmentHelper.getItemEnchantmentLevel(SuitcaseGlider.SOARING.get(),stack);
        Vec3 v=p.getDeltaMovement(),look=p.getLookAngle();double len=Math.hypot(look.x,look.z);
        double x=v.x*.995+(len>.001?look.x/len*.012:0),z=v.z*.995+(len>.001?look.z/len*.012:0),speed=Math.hypot(x,z);
        if (speed>.45) { x*=.45/speed;z*=.45/speed; }
        p.setDeltaMovement(x,Math.max(-FlightPolicy.sink(soaring,deploying),Math.min(0,v.y)),z);
        p.fallDistance=0; p.hasImpulse=true;
    }
    @SubscribeEvent public void fall(LivingFallEvent e) { if (e.getEntity() instanceof Player p && selected(p) && active(p)) e.setCanceled(true); }
    @SubscribeEvent public void attack(AttackEntityEvent e) { if (active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent public void useItem(PlayerInteractEvent.RightClickItem e) { if (active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent public void useBlock(PlayerInteractEvent.RightClickBlock e) { if (active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent public void mine(PlayerInteractEvent.LeftClickBlock e) { if (active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent public void entity(PlayerInteractEvent.EntityInteract e) { if (active(e.getEntity())) e.setCanceled(true); }
    @SubscribeEvent public void tracking(PlayerEvent.StartTracking e) { if (e.getEntity() instanceof ServerPlayer who && e.getTarget() instanceof ServerPlayer target) GliderNetwork.sendTo(who,target); }
    @SubscribeEvent public void clone(PlayerEvent.Clone e) { e.getEntity().getPersistentData().remove("sg_phase");e.getEntity().getPersistentData().remove("sg_started"); }
}
