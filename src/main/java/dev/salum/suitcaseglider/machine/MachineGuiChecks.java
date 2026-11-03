package dev.salum.suitcaseglider.machine;

import com.mojang.authlib.GameProfile;
import dev.salum.suitcaseglider.SuitcaseGlider;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class MachineGuiChecks {
    private static final List<String> checks=new ArrayList<>();
    private static void check(boolean ok,String name){checks.add(name+"="+ok);if(!ok)throw new IllegalStateException("MACHINE_GUI_FAILED "+name);}
    private static MachineBlockEntity place(net.minecraft.server.level.ServerLevel l,String kind,BlockPos p){
        var block=MachineContent.MACHINES.get(kind).get();var state=block.defaultBlockState();for(var cell:block.cells(p,state))l.removeBlock(cell,false);l.setBlockAndUpdate(p,state);block.setPlacedBy(l,p,state,null,new ItemStack(block));return (MachineBlockEntity)l.getBlockEntity(p);
    }
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(!Boolean.getBoolean("machines.guiSmoke"))return;
        var l=event.getServer().overworld();var player=FakePlayerFactory.get(l,new GameProfile(UUID.nameUUIDFromBytes("machine-ui-checks".getBytes()),"MachineGuiChecks"));int index=0;
        for(String kind:MachineCatalog.ALL.keySet()){
            var p=new BlockPos(100+index++*5,80,0);var b=place(l,kind,p);player.setPos(Vec3.atCenterOf(p));player.getInventory().clearContent();var menu=b.createMenu(10,player.getInventory(),player);
            check(menu.slots.size()==36+b.ui().visibleSlots(),kind+" exact inventory slots");check(menu.stillValid(player),kind+" local validity");
            var tag=b.saveWithoutMetadata();var restored=new MachineBlockEntity(p,b.getBlockState());restored.load(tag);check(restored.inventory.getSlots()==5,kind+" inventory NBT size");var legacy=tag.copy();legacy.remove("Inventory");restored.load(legacy);check(restored.inventory.getSlots()==5,kind+" pre-interface save migration");
            for(int i=b.ui().inputs();i<b.ui().visibleSlots();i++)check(!menu.slots.get(i).mayPlace(new ItemStack(Items.COBBLESTONE)),kind+" protected output "+i);
            if(b.ui().inputs()>0){
                player.getInventory().setItem(0,new ItemStack(Items.COBBLESTONE,7));check(!menu.quickMoveStack(player,b.ui().visibleSlots()+27).isEmpty()&&b.inventory.getStackInSlot(0).getCount()==7,kind+" shift-click input");
                check(!menu.quickMoveStack(player,0).isEmpty()&&b.inventory.getStackInSlot(0).isEmpty(),kind+" shift-click extract");
                var io=b.getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing items"));
                check(io.insertItem(0,new ItemStack(Items.GRAVEL),true).isEmpty()&&b.inventory.getStackInSlot(0).isEmpty(),kind+" simulated insert");
                io.insertItem(0,new ItemStack(Items.GRAVEL),false);check(io.extractItem(0,1,false).isEmpty(),kind+" automation cannot steal input");b.inventory.setStackInSlot(0,ItemStack.EMPTY);
                check(!io.insertItem(3,new ItemStack(Items.STONE),false).isEmpty(),kind+" automation cannot insert output");
            }
            player.setPos(0,80,0);check(!menu.stillValid(player),kind+" remote validity rejected");
        }
        var p=new BlockPos(100,80,10);var b=place(l,"crusher",p);b.inventory.setStackInSlot(0,new ItemStack(Items.COBBLESTONE,2));b.receiveCableEnergy(4000,false);for(int i=0;i<35;i++)b.tickEnergy();
        check(b.data.get(0)==35&&b.cableEnergy()==3720,"automatic recipe consumes exactly 8 FE per tick");
        var nbt=b.saveWithoutMetadata();var restored=new MachineBlockEntity(p,b.getBlockState());restored.load(nbt);l.setBlockEntity(restored);b=restored;
        check(b.data.get(0)==35&&b.inventory.getStackInSlot(0).getCount()==2,"inflight recipe survives NBT reload");
        for(int i=0;i<65;i++)b.tickEnergy();check(b.inventory.getStackInSlot(0).getCount()==1&&b.inventory.getStackInSlot(3).is(Items.GRAVEL)&&b.inventory.getStackInSlot(3).getCount()==1,"one cycle one input one output");
        b.inventory.setStackInSlot(3,new ItemStack(Items.GRAVEL,64));int energy=b.cableEnergy();for(int i=0;i<150;i++)b.tickEnergy();check(b.data.get(2)==MachineBlockEntity.BLOCKED&&b.cableEnergy()==energy&&b.inventory.getStackInSlot(0).getCount()==1,"full output pauses without consuming power or input");
        b.inventory.setStackInSlot(3,ItemStack.EMPTY);var emptyPower=b.saveWithoutMetadata();emptyPower.putInt("CableEnergy",0);b.load(emptyPower);for(int i=0;i<20;i++)b.tickEnergy();check(b.data.get(2)==MachineBlockEntity.NO_POWER&&b.data.get(0)==0&&b.inventory.getStackInSlot(0).getCount()==1,"no power pauses processing");
        b.receiveCableEnergy(4000,false);b.inventory.setStackInSlot(0,new ItemStack(Items.DIAMOND));b.tickEnergy();check(b.data.get(2)==MachineBlockEntity.NO_RECIPE&&b.cableEnergy()==4000,"unsupported recipe never consumes energy");
        var sep=place(l,"air_separator",p.offset(6,0,0));sep.inventory.setStackInSlot(0,new ItemStack(Items.GRAVEL));sep.inventory.setStackInSlot(4,new ItemStack(Items.SAND,64));sep.receiveCableEnergy(4000,false);for(int i=0;i<160;i++)sep.tickEnergy();check(sep.inventory.getStackInSlot(3).isEmpty()&&sep.inventory.getStackInSlot(0).getCount()==1&&sep.cableEnergy()==4000,"separator secondary output blocks atomically");
        sep.inventory.setStackInSlot(4,ItemStack.EMPTY);for(int i=0;i<140;i++)sep.tickEnergy();check(sep.inventory.getStackInSlot(0).isEmpty()&&sep.inventory.getStackInSlot(3).is(Items.FLINT)&&sep.inventory.getStackInSlot(4).is(Items.SAND),"separator produces both fractions automatically");
        var original=sep.saveWithoutMetadata();var copy=new MachineBlockEntity(sep.getBlockPos(),sep.getBlockState());copy.load(original);check(copy.inventory.getStackInSlot(3).is(Items.FLINT)&&copy.inventory.getStackInSlot(4).is(Items.SAND),"both fractions survive reload");
        for(var drop:l.getEntitiesOfClass(ItemEntity.class,new AABB(sep.getBlockPos()).inflate(5)))drop.discard();
        var block=(MachineBlock)sep.getBlockState().getBlock();l.destroyBlock(MachineBlock.other(sep.getBlockPos(),sep.getBlockState()),true);
        var drops=l.getEntitiesOfClass(ItemEntity.class,new AABB(sep.getBlockPos()).inflate(5));
        check(drops.stream().filter(e->e.getItem().is(Items.FLINT)).mapToInt(e->e.getItem().getCount()).sum()==1&&drops.stream().filter(e->e.getItem().is(Items.SAND)).mapToInt(e->e.getItem().getCount()).sum()==1,"breaking secondary drops inventory exactly once");
        check(drops.stream().filter(e->e.getItem().is(block.asItem())).mapToInt(e->e.getItem().getCount()).sum()==1,"breaking secondary drops one machine");
        Files.write(Path.of("machine-gui-report.txt"),checks);SuitcaseGlider.LOG.info("MACHINE_GUI_OK {} checks",checks.size());
    }
}
