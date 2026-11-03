package dev.salum.suitcaseglider.kiln;

import com.mojang.authlib.GameProfile;
import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.resource.*;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class KilnDevelopment {
    private static MinecraftServer stop;private static int countdown;private static final List<String> report=new ArrayList<>();
    private static void check(boolean pass,String name){if(!pass)throw new IllegalStateException("KILN_SMOKE_FAILED "+name);report.add(name+"=true");}
    public static KilnBlockEntity build(ServerLevel level,BlockPos controller,Direction facing){
        level.setBlockAndUpdate(controller,ResourceContent.CONTROLLER.get().defaultBlockState().setValue(KilnComponentBlock.FACING,facing));
        var k=(KilnBlockEntity)level.getBlockEntity(controller);
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++){
            if(x==1&&y==1&&z==0)continue;
            var block=x==1&&y==1&&z==1?Blocks.AIR:x==1&&z==0?(y==2?ResourceContent.INPUT.get():ResourceContent.OUTPUT.get()):ResourceContent.BRICKS.get();
            var state=block.defaultBlockState();if(state.hasProperty(KilnComponentBlock.FACING))state=state.setValue(KilnComponentBlock.FACING,facing);
            level.setBlockAndUpdate(k.cell(x,y,z),state);
        }return k;
    }
    private static ServerPlayer player(ServerLevel l,String name){var p=new ServerPlayer(l.getServer(),l,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes()),name));p.connection=new ServerGamePacketListenerImpl(l.getServer(),new Connection(PacketFlow.SERVERBOUND),p);return p;}
    @SubscribeEvent public static void started(ServerStartedEvent event)throws Exception{
        if(Boolean.getBoolean("cogwild.kilnPersistence")){
            var level=event.getServer().overworld();var at=new BlockPos(64,80,0);level.getChunkAt(at);
            var k=(KilnBlockEntity)level.getBlockEntity(at);check(k!=null,"controller restored after real server restart");
            for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++)level.getChunkAt(k.cell(x,y,z));
            check(k.formed&&!k.enabled&&k.inspect().valid(),"formed structure and manual pause restored");
            check(k.data.get(0)==61&&k.inventory.getStackInSlot(0).getCount()==6&&k.inventory.getStackInSlot(1).getCount()==3,"progress and both slots restored");
            check(k.saveWithoutMetadata().getCompound("Reserved").getByte("Count")==1,"reserved coal restored once");
            k.toggle();for(int i=61;i<1200;i++)k.tick();check(k.inventory.getStackInSlot(1).getCount()==4&&k.inventory.getStackInSlot(0).getCount()==6,"restarted cycle finishes without consuming input twice");
            k.toggle();Files.write(Path.of("kiln-persistence-report.txt"),report);SuitcaseGlider.LOG.info("KILN_PERSISTENCE_OK {} checks",report.size());stop=event.getServer();countdown=5;return;
        }
        if(!Boolean.getBoolean("cogwild.kilnSmoke"))return;
        var server=event.getServer();var level=server.overworld();int index=0;
        check(KilnContent.KEY.get().getDefaultInstance().getMaxStackSize()==1,"assembly key is reusable single-stack tool");
        check(server.getRecipeManager().byKey(SuitcaseGlider.id("guild_assembly_key")).isPresent(),"assembly key recipe exists");
        for(Direction facing:Direction.Plane.HORIZONTAL){
            var pos=new BlockPos(index++*12,80,0);var k=build(level,pos,facing);var p=player(level,"KilnCheck"+index);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            check(k.inspect().valid(),"complete structure "+facing);
            var hit=new BlockHitResult(Vec3.atCenterOf(pos),facing,pos,false);
            p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);k.getBlockState().use(level,p,InteractionHand.MAIN_HAND,hit);
            check(!k.formed&&!(p.containerMenu instanceof KilnMenu),"bare hand cannot form "+facing);
            level.setBlockAndUpdate(k.cell(1,1,1),Blocks.STONE.defaultBlockState());p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(KilnContent.KEY.get()));
            k.getBlockState().use(level,p,InteractionHand.MAIN_HAND,hit);check(!k.formed,"solid centre rejected "+facing);
            level.setBlockAndUpdate(k.cell(1,1,1),Blocks.AIR.defaultBlockState());var hatch=k.cell(1,2,0);level.setBlockAndUpdate(hatch,level.getBlockState(hatch).setValue(KilnComponentBlock.FACING,facing.getOpposite()));
            check(!k.inspect().valid(),"wrong hatch facing rejected "+facing);level.setBlockAndUpdate(hatch,level.getBlockState(hatch).setValue(KilnComponentBlock.FACING,facing));
            k.getBlockState().use(level,p,InteractionHand.MAIN_HAND,hit);check(k.formed,"key forms structure "+facing);check(p.getMainHandItem().getCount()==1,"key is not consumed "+facing);
            for(Item held:List.of(Items.AIR,Items.STONE,Items.APPLE)){
                p.setItemInHand(InteractionHand.MAIN_HAND,held==Items.AIR?ItemStack.EMPTY:new ItemStack(held));k.getBlockState().use(level,p,InteractionHand.MAIN_HAND,hit);
                check(p.containerMenu instanceof KilnMenu,"menu opens with "+held+" "+facing);p.closeContainer();
            }
            p.getInventory().clearContent();var menu=new KilnMenu(5,p.getInventory(),k);
            check(!menu.slots.get(1).mayPlace(new ItemStack(Items.COAL)),"menu output rejects insertion "+facing);
            p.getInventory().setItem(9,new ItemStack(Items.COAL,5));menu.quickMoveStack(p,2);
            check(k.inventory.getStackInSlot(0).getCount()==5&&p.getInventory().getItem(9).isEmpty(),"shift-click loads coal "+facing);
            menu.quickMoveStack(p,0);check(k.inventory.getStackInSlot(0).isEmpty(),"shift-click returns unreserved input "+facing);
            p.getInventory().clearContent();k.inventory.setStackInSlot(0,new ItemStack(Items.COAL,63));p.getInventory().setItem(9,new ItemStack(Items.COAL,5));menu.quickMoveStack(p,2);
            check(k.inventory.getStackInSlot(0).getCount()==64&&p.getInventory().getItem(9).getCount()==4,"partial shift-click conserves item count "+facing);
            k.inventory.setStackInSlot(0,ItemStack.EMPTY);p.getInventory().clearContent();
            k.inventory.setStackInSlot(1,new ItemStack(ResourceContent.COKE.get(),5));menu.quickMoveStack(p,1);
            check(k.inventory.getStackInSlot(1).isEmpty()&&p.getInventory().items.stream().filter(s->s.is(ResourceContent.COKE.get())).mapToInt(ItemStack::getCount).sum()==5,"shift-click extracts coke exactly once "+facing);
            check(!menu.clickMenuButton(p,9)&&!k.enabled,"unknown menu button rejected "+facing);p.getInventory().clearContent();
            check(!k.getCapability(ForgeCapabilities.ENERGY).isPresent(),"no electrical requirement "+facing);
            var input=level.getBlockEntity(hatch).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing item port"));
            var output=level.getBlockEntity(pos.below()).getCapability(ForgeCapabilities.ITEM_HANDLER).orElseThrow(()->new IllegalStateException("Missing item port"));
            check(input.insertItem(0,new ItemStack(Items.COAL,2),false).isEmpty(),"input port accepts coal "+facing);
            check(!input.insertItem(0,new ItemStack(Items.DIRT),false).isEmpty(),"input rejects unrelated item "+facing);
            check(input.extractItem(0,1,false).isEmpty()&&!output.insertItem(0,new ItemStack(ResourceContent.COKE.get()),false).isEmpty(),"port directions enforced "+facing);
            k.tick();check(k.inventory.getStackInSlot(0).getCount()==2&&k.data.get(0)==0,"manual start required "+facing);
            k.toggle();k.tick();check(k.data.get(0)==1&&k.inventory.getStackInSlot(0).getCount()==1,"coal reserved once "+facing);
            check(k.getBlockState().getValue(KilnComponentBlock.LIT),"working light "+facing);
            for(int i=0;i<49;i++)k.tick();int progress=k.data.get(0);k.toggle();for(int i=0;i<10;i++)k.tick();check(k.data.get(0)==progress,"manual pause preserves progress "+facing);
            k.toggle();var redstone=k.cell(0,0,0).below();level.setBlockAndUpdate(redstone,Blocks.REDSTONE_BLOCK.defaultBlockState());k.tick();check(k.data.get(0)==progress+1,"redstone does not control the oven "+facing);
            var tag=k.saveWithoutMetadata();var reloaded=new KilnBlockEntity(pos,k.getBlockState());reloaded.load(tag);level.setBlockEntity(reloaded);k=reloaded;
            check(k.formed&&k.enabled&&k.data.get(0)==progress+1&&k.inventory.getStackInSlot(0).getCount()==1,"NBT restores reserved operation "+facing);
            var wall=k.cell(0,1,1);level.setBlockAndUpdate(wall,Blocks.AIR.defaultBlockState());int frozen=k.data.get(0);k.tick();check(!k.formed&&k.data.get(0)==frozen,"broken shell pauses without loss "+facing);
            level.setBlockAndUpdate(wall,ResourceContent.BRICKS.get().defaultBlockState());check(k.form(p),"key reforms repaired shell "+facing);
            k.inventory.setStackInSlot(1,new ItemStack(ResourceContent.COKE.get(),64));k.tick();check(k.data.get(0)==frozen,"full output freezes active cycle "+facing);
            k.inventory.setStackInSlot(1,ItemStack.EMPTY);for(int i=k.data.get(0);i<1200;i++)k.tick();
            check(k.inventory.getStackInSlot(1).getCount()==1&&k.inventory.getStackInSlot(0).getCount()==1,"one reserved coal yields exactly one coke "+facing);
            check(output.extractItem(0,64,false).getCount()==1,"output port extracts result "+facing);
            k.toggle();k.inventory.setStackInSlot(0,new ItemStack(Items.COAL_BLOCK));k.inventory.setStackInSlot(1,new ItemStack(ResourceContent.COKE.get(),56));k.toggle();k.tick();
            check(k.inventory.getStackInSlot(0).is(Items.COAL_BLOCK)&&k.data.get(0)==0,"nine coke capacity required before reserving block "+facing);
            k.inventory.setStackInSlot(1,new ItemStack(ResourceContent.COKE.get(),55));k.tick();check(k.data.get(1)==10800,"coal block takes nine normal cycle times "+facing);
            for(int i=1;i<10800;i++)k.tick();check(k.inventory.getStackInSlot(1).getCount()==64&&k.inventory.getStackInSlot(0).isEmpty(),"coal block produces nine coke "+facing);
            k.inventory.setStackInSlot(1,ItemStack.EMPTY);k.inventory.setStackInSlot(0,new ItemStack(Items.COAL,3));k.tick();
            check(k.inventory.getStackInSlot(0).getCount()==2,"destruction fixture has one reserved coal "+facing);
            for(var e:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4)))e.discard();level.destroyBlock(pos,false);
            int coal=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4)).stream().filter(e->e.getItem().is(Items.COAL)).mapToInt(e->e.getItem().getCount()).sum();
            check(coal==3,"controller destruction returns pending and unreserved input exactly once "+facing);
        }
        var fixture=build(level,new BlockPos(64,80,0),Direction.NORTH);var fixturePlayer=player(level,"KilnPersist");fixture.form(fixturePlayer);fixture.inventory.setStackInSlot(0,new ItemStack(Items.COAL,7));fixture.toggle();for(int i=0;i<61;i++)fixture.tick();fixture.toggle();fixture.inventory.setStackInSlot(1,new ItemStack(ResourceContent.COKE.get(),3));
        check(fixture.data.get(0)==61&&!fixture.enabled,"paused fixture prepared for real server restart");
        Files.write(Path.of("kiln-smoke-report.txt"),report);SuitcaseGlider.LOG.info("KILN_SMOKE_OK {} checks",report.size());stop=server;countdown=5;
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(stop!=null&&e.phase==TickEvent.Phase.END&&--countdown<=0){var server=stop;stop=null;server.halt(false);}}
}
