package dev.salum.suitcaseglider.kiln;

import dev.salum.suitcaseglider.resource.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.*;

public final class KilnBlockEntity extends BlockEntity implements MenuProvider {
    public static final int UNFORMED=0,STOPPED=1,WAITING=2,RUNNING=3,BLOCKED=4,PAUSED=5,UNLOADED=6;
    public boolean formed;
    private int progress,duration=1200,state=UNFORMED;
    private ItemStack reserved=ItemStack.EMPTY;
    public final ItemStackHandler inventory=new ItemStackHandler(2){
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0&&accepts(stack);}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    public final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->progress;case 1->duration;case 2->state;case 3->1;case 4->formed?1:0;default->0;};}
        public void set(int i,int v){switch(i){case 0->progress=v;case 1->duration=v;case 2->state=v;case 3->{} case 4->formed=v!=0;default->{}}}
        public int getCount(){return 5;}
    };
    public KilnBlockEntity(BlockPos p,BlockState s){super(KilnContent.TYPE.get(),p,s);}
    public boolean controller(){return getBlockState().is(ResourceContent.CONTROLLER.get());}
    public static boolean accepts(ItemStack s){return s.is(Items.COAL)||s.is(Items.COAL_BLOCK);}
    public Direction facing(){return getBlockState().getValue(KilnComponentBlock.FACING);}
    public BlockPos cell(int x,int y,int z){return worldPosition.relative(facing().getClockWise(),x-1).above(y-1).relative(facing().getOpposite(),z);}
    public record Inspection(boolean loaded,List<BlockPos> wrong){public boolean valid(){return loaded&&wrong.isEmpty();}}
    public Inspection inspect(){
        if(level==null)return new Inspection(false,List.of());
        List<BlockPos> wrong=new ArrayList<>();
        for(int x=0;x<3;x++)for(int y=0;y<3;y++)for(int z=0;z<3;z++){
            var p=cell(x,y,z);if(!level.hasChunkAt(p))return new Inspection(false,List.of());
            var s=level.getBlockState(p);
            if(x==1&&y==1&&z==1){if(!s.isAir())wrong.add(p);continue;}
            Block expected=x==1&&z==0?(y==0?ResourceContent.OUTPUT.get():y==1?ResourceContent.CONTROLLER.get():ResourceContent.INPUT.get()):ResourceContent.BRICKS.get();
            if(!s.is(expected)||(expected instanceof KilnComponentBlock&&s.getValue(KilnComponentBlock.FACING)!=facing()))wrong.add(p);
        }
        return new Inspection(true,wrong);
    }
    public boolean form(Player player){
        var check=inspect();
        if(!check.valid()){
            player.displayClientMessage(Component.translatable(check.loaded()?"message.suitcaseglider.kiln.invalid":"message.suitcaseglider.kiln.unloaded",check.wrong().size()),false);
            for(var p:check.wrong().stream().limit(6).toList()){
                player.displayClientMessage(Component.translatable("message.suitcaseglider.kiln.cell",p.getX(),p.getY(),p.getZ()),false);
                if(level instanceof ServerLevel server)server.sendParticles(new DustParticleOptions(new org.joml.Vector3f(1,.15f,.08f),1),p.getX()+.5,p.getY()+.5,p.getZ()+.5,12,.35,.35,.35,0);
            }
            return false;
        }
        formed=true;refreshState();setChanged();player.displayClientMessage(Component.translatable("message.suitcaseglider.kiln.formed"),true);return true;
    }
    public boolean outputFits(int count){var out=inventory.getStackInSlot(1);return (out.isEmpty()||out.is(ResourceContent.COKE.get())&&!out.hasTag())&&out.getCount()+count<=64;}
    private int batchCount(){return reserved.is(Items.COAL_BLOCK)?9:1;}
    private void refreshState(){state=!formed?UNFORMED:reserved.isEmpty()?WAITING:RUNNING;}
    public void tick(){
        if(!controller()||level==null||level.isClientSide)return;
        if(formed){var check=inspect();if(!check.loaded()){state=UNLOADED;light(false);return;}if(!check.valid()){formed=false;setChanged();}}
        refreshState();
        if(!formed){light(false);return;}
        if(reserved.isEmpty()){
            var input=inventory.getStackInSlot(0);if(!accepts(input)){state=WAITING;light(false);return;}
            int count=input.is(Items.COAL_BLOCK)?9:1;
            if(!outputFits(count)){state=BLOCKED;light(false);return;}
            reserved=inventory.extractItem(0,1,false);duration=count*1200;progress=0;setChanged();
        }
        if(!outputFits(batchCount())){state=BLOCKED;light(false);return;}
        state=RUNNING;progress++;light(true);setChanged();
        if(progress>=duration){
            int count=inventory.getStackInSlot(1).getCount()+batchCount();inventory.setStackInSlot(1,new ItemStack(ResourceContent.COKE.get(),count));reserved=ItemStack.EMPTY;progress=0;
        }
    }
    private void light(boolean active){
        for(var p:List.of(worldPosition,worldPosition.above(),worldPosition.below())){
            if(!level.hasChunkAt(p))continue;var s=level.getBlockState(p);
            if(s.getBlock() instanceof KilnComponentBlock&&s.getValue(KilnComponentBlock.FACING)==facing()&&s.getValue(KilnComponentBlock.LIT)!=active)level.setBlock(p,s.setValue(KilnComponentBlock.LIT,active),3);
        }
    }
    public void dropContents(){
        if(level==null||level.isClientSide||!controller())return;
        for(int i=0;i<2;i++){Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,inventory.getStackInSlot(i));inventory.setStackInSlot(i,ItemStack.EMPTY);}
        Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,reserved);reserved=ItemStack.EMPTY;progress=0;setChanged();
    }
    private KilnBlockEntity owner(){
        if(controller())return this;
        var p=getBlockState().is(ResourceContent.INPUT.get())?worldPosition.below():worldPosition.above();
        if(level!=null&&level.hasChunkAt(p)&&level.getBlockEntity(p) instanceof KilnBlockEntity k&&k.controller()&&k.facing()==facing())return k;
        return null;
    }
    private final IItemHandler port=new IItemHandler(){
        private KilnBlockEntity target(){var k=owner();return k!=null&&k.formed&&k.inspect().valid()?k:null;}
        private boolean input(){return getBlockState().is(ResourceContent.INPUT.get());}
        public int getSlots(){return 1;}
        public ItemStack getStackInSlot(int slot){var k=target();return slot==0&&k!=null?k.inventory.getStackInSlot(input()?0:1).copy():ItemStack.EMPTY;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){var k=target();return slot==0&&input()&&k!=null?k.inventory.insertItem(0,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){var k=target();return slot==0&&!input()&&k!=null?k.inventory.extractItem(1,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot==0?64:0;}
        public boolean isItemValid(int slot,ItemStack stack){return slot==0&&input()&&accepts(stack);}
    };
    private LazyOptional<IItemHandler> portCap=LazyOptional.of(()->port);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ITEM_HANDLER&&!controller()?portCap.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();portCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();portCap=LazyOptional.of(()->port);}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.put("Inventory",inventory.serializeNBT());t.put("Reserved",reserved.save(new CompoundTag()));t.putBoolean("Formed",formed);t.putInt("Progress",progress);}
    @Override public void load(CompoundTag t){super.load(t);inventory.deserializeNBT(t.getCompound("Inventory"));reserved=ItemStack.of(t.getCompound("Reserved"));if(!reserved.isEmpty()&&!accepts(reserved))reserved=ItemStack.EMPTY;duration=reserved.is(Items.COAL_BLOCK)?10800:1200;progress=Math.max(0,Math.min(duration-1,t.getInt("Progress")));formed=t.getBoolean("Formed");refreshState();}
    @Override public Component getDisplayName(){return Component.translatable("container.suitcaseglider.coke_oven");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new KilnMenu(id,inv,this);}
}
