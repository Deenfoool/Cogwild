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
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

public final class KilnBlockEntity extends BlockEntity implements MenuProvider {
    public static final int UNFORMED=0,STOPPED=1,WAITING=2,RUNNING=3,BLOCKED=4,PAUSED=5,UNLOADED=6,NO_FUEL=7;
    public boolean formed;
    private int progress,duration=1200,state=UNFORMED;
    private int heat,heatCapacity;
    public final FluidTank tank=new FluidTank(16000){@Override protected void onContentsChanged(){setChanged();}};
    private ItemStack reserved=ItemStack.EMPTY;
    public final ItemStackHandler inventory=new ItemStackHandler(3){
        @Override public boolean isItemValid(int slot,ItemStack stack){return slot==0&&accepts(stack)||slot==2&&fuelTicks(stack)>0;}
        @Override protected void onContentsChanged(int slot){setChanged();}
    };
    public final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->progress;case 1->duration;case 2->state;case 3->1;case 4->formed?1:0;case 5->tank.getFluidAmount();case 6->heat;case 7->heatCapacity;default->0;};}
        public void set(int i,int v){switch(i){case 0->progress=v;case 1->duration=v;case 2->state=v;case 3->{} case 4->formed=v!=0;case 5->tank.setFluid(v>0?new FluidStack(KilnFluids.SOURCE.get(),v):FluidStack.EMPTY);case 6->heat=v;case 7->heatCapacity=v;default->{}}}
        public int getCount(){return 8;}
    };
    public KilnBlockEntity(BlockPos p,BlockState s){super(KilnContent.TYPE.get(),p,s);}
    public boolean controller(){return getBlockState().is(ResourceContent.CONTROLLER.get());}
    public static boolean accepts(ItemStack s){return s.is(Items.COAL)||s.is(Items.COAL_BLOCK);}
    public static int fuelTicks(ItemStack stack){return stack.is(Items.COAL_BLOCK)?16000:stack.is(KilnContent.CHARCOAL_ITEM.get())?14400:0;}
    private int availableHeat(){return heat+inventory.getStackInSlot(2).getCount()*fuelTicks(inventory.getStackInSlot(2));}
    private boolean outputsFit(int count){return outputFits(count)&&tank.getFluidAmount()+250*count<=tank.getCapacity();}
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
            if(!outputsFit(count)){state=BLOCKED;light(false);return;}
            if(availableHeat()<count*1200){state=NO_FUEL;light(false);return;}
            reserved=inventory.extractItem(0,1,false);duration=count*1200;progress=0;setChanged();
        }
        if(!outputsFit(batchCount())){state=BLOCKED;light(false);return;}
        if(availableHeat()<duration-progress){state=NO_FUEL;light(false);return;}
        if(heat<=0){heatCapacity=fuelTicks(inventory.getStackInSlot(2));inventory.extractItem(2,1,false);heat=heatCapacity;}
        heat--;state=RUNNING;progress++;light(true);setChanged();
        if(progress>=duration){
            tank.fill(new FluidStack(KilnFluids.SOURCE.get(),250*batchCount()),IFluidHandler.FluidAction.EXECUTE);
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
        for(int i=0;i<3;i++){Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,inventory.getStackInSlot(i));inventory.setStackInSlot(i,ItemStack.EMPTY);}
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
        public int getSlots(){return input()?1:2;}
        private int index(int slot){return input()?0:slot==0?1:2;}
        public ItemStack getStackInSlot(int slot){var k=target();return slot>=0&&slot<getSlots()&&k!=null?k.inventory.getStackInSlot(index(slot)).copy():ItemStack.EMPTY;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){var k=target();return slot>=0&&slot<getSlots()&&(input()||slot==1)&&k!=null?k.inventory.insertItem(index(slot),stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){var k=target();return slot==0&&!input()&&k!=null?k.inventory.extractItem(1,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot>=0&&slot<getSlots()?64:0;}
        public boolean isItemValid(int slot,ItemStack stack){return input()?slot==0&&accepts(stack):slot==1&&fuelTicks(stack)>0;}
    };
    private final IFluidHandler fluidPort=new IFluidHandler(){
        public int getTanks(){return 1;}
        public FluidStack getFluidInTank(int index){var k=owner();return index==0&&k!=null?k.tank.getFluid().copy():FluidStack.EMPTY;}
        public int getTankCapacity(int index){return index==0?16000:0;}
        public boolean isFluidValid(int index,FluidStack fluid){return false;}
        public int fill(FluidStack fluid,FluidAction action){return 0;}
        public FluidStack drain(FluidStack fluid,FluidAction action){var k=owner();return k==null?FluidStack.EMPTY:k.tank.drain(fluid,action);}
        public FluidStack drain(int amount,FluidAction action){var k=owner();return k==null?FluidStack.EMPTY:k.tank.drain(amount,action);}
    };
    public IFluidHandler fluidHandler(){return fluidPort;}
    public CompoundTag carriedContents(){var t=new CompoundTag();if(!tank.isEmpty())t.put("Creosote",tank.writeToNBT(new CompoundTag()));if(heat>0){t.putInt("Heat",heat);t.putInt("HeatCapacity",heatCapacity);}return t;}
    private LazyOptional<IItemHandler> portCap=LazyOptional.of(()->port);
    private LazyOptional<IFluidHandler> fluidCap=LazyOptional.of(()->fluidPort);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){
        if(cap==ForgeCapabilities.FLUID_HANDLER&&(controller()||getBlockState().is(ResourceContent.OUTPUT.get())))return fluidCap.cast();
        return cap==ForgeCapabilities.ITEM_HANDLER&&!controller()?portCap.cast():super.getCapability(cap,side);
    }
    @Override public void invalidateCaps(){super.invalidateCaps();portCap.invalidate();fluidCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();portCap=LazyOptional.of(()->port);fluidCap=LazyOptional.of(()->fluidPort);}
    @Override protected void saveAdditional(CompoundTag t){super.saveAdditional(t);t.put("Inventory",inventory.serializeNBT());t.put("Creosote",tank.writeToNBT(new CompoundTag()));t.putInt("Heat",heat);t.putInt("HeatCapacity",heatCapacity);t.put("Reserved",reserved.save(new CompoundTag()));t.putBoolean("Formed",formed);t.putInt("Progress",progress);}
    @Override public void load(CompoundTag t){super.load(t);var stored=t.getCompound("Inventory").copy();stored.putInt("Size",3);inventory.deserializeNBT(stored);tank.readFromNBT(t.getCompound("Creosote"));if(!tank.isEmpty()&&tank.getFluid().getFluid()!=KilnFluids.SOURCE.get())tank.setFluid(FluidStack.EMPTY);heat=Math.max(0,Math.min(16000,t.getInt("Heat")));heatCapacity=Math.max(heat,Math.min(16000,t.getInt("HeatCapacity")));reserved=ItemStack.of(t.getCompound("Reserved"));if(!reserved.isEmpty()&&!accepts(reserved))reserved=ItemStack.EMPTY;duration=reserved.is(Items.COAL_BLOCK)?10800:1200;progress=Math.max(0,Math.min(duration-1,t.getInt("Progress")));formed=t.getBoolean("Formed");refreshState();}
    @Override public Component getDisplayName(){return Component.translatable("container.suitcaseglider.coke_oven");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new KilnMenu(id,inv,this);}
}
