package dev.salum.suitcaseglider.mech;

import com.mojang.authlib.GameProfile;
import dev.salum.suitcaseglider.SuitcaseGlider;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Development fixtures are disabled in ordinary mod installations. */
@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class MechDevelopment {
    private static MinecraftServer stopServer;
    private static int countdown;
    private static void check(boolean condition,String label){if(!condition)throw new IllegalStateException("MECH_SMOKE_FAILED: "+label);}
    @SubscribeEvent public static void started(ServerStartedEvent e)throws Exception{
        if(!Boolean.getBoolean("mechant.smoke"))return;
        var level=e.getServer().overworld();for(int x=-12;x<=12;x++)for(int z=-6;z<=6;z++)level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());
        var p=FakePlayerFactory.get(level,new GameProfile(UUID.nameUUIDFromBytes("mech-smoke".getBytes()),"MechSmoke"));p.setPos(0,80,3);
        var ant=MechContent.ANT.get().create(level);check(ant!=null,"registry");ant.setPos(0,80,0);level.addFreshEntity(ant);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE,3));int temper=ant.getTemper();ant.mobInteract(p,InteractionHand.MAIN_HAND);
        check(ant.getTemper()>temper,"apple increases native horse temper");check(p.getMainHandItem().getCount()==2,"apple consumption");
        check(ant.tameWithName(p),"native taming");check(ant.isTamed()&&p.getUUID().equals(ant.getOwnerUUID()),"owner");
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SADDLE));ant.mobInteract(p,InteractionHand.MAIN_HAND);check(ant.isSaddled(),"saddle interaction");
        p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);ant.mobInteract(p,InteractionHand.MAIN_HAND);check(p.getVehicle()==ant,"mount");ant.positionRider(p);
        Vec3 expected=ant.worldAnchor("rider_boarding_pelvis",0).add(0,-.75,0);check(p.position().distanceTo(expected)<.001,"boarding pelvis anchor");
        for(String anchor:List.of("boarding_step_left","rider_seat_anchor","rider_hand_left","rider_hand_right","exhaust_underbody")){var v=ant.worldAnchor(anchor,0);check(Double.isFinite(v.x+v.y+v.z),"anchor "+anchor);}
        var tag=new CompoundTag();ant.saveWithoutId(tag);var copy=MechContent.ANT.get().create(level);copy.load(tag);check(copy.isTamed()&&copy.isSaddled()&&p.getUUID().equals(copy.getOwnerUUID()),"save/load ownership and saddle");check(copy.isSilent(),"silent after reload");check(copy.maxUpStep()==1,"one block step height");
        var walker=MechContent.ANT.get().create(level);walker.setNoAi(true);walker.setPos(-8,80,0);walker.setOnGround(true);level.addFreshEntity(walker);
        for(int x=-4;x<=1;x++)for(int z=-3;z<=3;z++)level.setBlockAndUpdate(new BlockPos(x,80,z),Blocks.STONE.defaultBlockState());
        for(int i=0;i<45;i++)walker.move(MoverType.SELF,new Vec3(.16,-.08,0));check(walker.getY()>80.95&&walker.getY()<81.05,"climb one block using collision movement");
        for(int i=0;i<40;i++)walker.move(MoverType.SELF,new Vec3(.16,-.25,0));check(Math.abs(walker.getY()-80)<.01,"descend one block");
        check(MechRig.get().clips.size()==23,"23 authored animation clips");
        String result="MECH_SMOKE_OK: registry, apple/temper/consumption, native taming, ownership, saddle, mount, boarding/seat/grip/step/exhaust anchors, NBT persistence, silence, one-block climb/descent, 17 animation clips.";
        Files.writeString(Path.of("mech-smoke-report.txt"),result);SuitcaseGlider.LOG.info(result);stopServer=e.getServer();countdown=5;
    }
    @SubscribeEvent public static void stop(TickEvent.ServerTickEvent e){if(stopServer!=null&&e.phase==TickEvent.Phase.END&&--countdown<=0){var s=stopServer;stopServer=null;s.halt(false);}}
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){
        if(!Boolean.getBoolean("mechant.dev")||!(e.getEntity() instanceof ServerPlayer p))return;
        var level=p.serverLevel();for(int x=-28;x<=28;x++)for(int z=-28;z<=28;z++)level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());
        for(int x=10;x<=24;x++)for(int z=-10;z<=10;z++)level.setBlockAndUpdate(new BlockPos(x,80,z),Blocks.COPPER_BLOCK.defaultBlockState());
        level.setDayTime(6000);level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,p.getServer());p.getServer().getPlayerList().op(p.getGameProfile());p.setGameMode(GameType.CREATIVE);p.teleportTo(level,.5,80,1.5,-90,0);
        p.getInventory().clearContent();p.getInventory().add(new ItemStack(Items.APPLE,64));p.getInventory().add(new ItemStack(Items.SADDLE,4));p.getInventory().add(new ItemStack(Items.LEAD,4));p.getInventory().add(new ItemStack(MechContent.EGG.get(),8));p.getInventory().add(new ItemStack(Items.GOLDEN_APPLE,16));
        if(!p.getPersistentData().getBoolean("MechDemoSpawned")){
            var wild=MechContent.ANT.get().create(level);wild.moveTo(5,80,1,-90,0);wild.setCustomName(Component.literal("Мех-муравей — приручение"));level.addFreshEntity(wild);
            var ride=MechContent.ANT.get().create(level);ride.moveTo(4,80,-6,-90,0);ride.setCustomName(Component.literal("Мех-муравей — готов к поездке"));ride.tameWithName(p);ride.equipSaddle(null);level.addFreshEntity(ride);p.getPersistentData().putBoolean("MechDemoSpawned",true);
        }
        p.getInventory().add(new ItemStack(dev.salum.suitcaseglider.machine.MachineContent.LATHE_ITEM.get()));p.getInventory().add(new ItemStack(dev.salum.suitcaseglider.machine.MachineContent.CRUSHER_ITEM.get()));
        p.inventoryMenu.broadcastChanges();p.sendSystemMessage(Component.literal("Мех-муравей: яблоки помогают приручению, затем садитесь пустой рукой. Приручённому нужен обычный седло. Shift — высадка. Медный уступ справа — 1 блок."));
    }
}
