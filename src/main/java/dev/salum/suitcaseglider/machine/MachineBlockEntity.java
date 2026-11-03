package dev.salum.suitcaseglider.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public final class MachineBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int CAPACITY=4000,WAITING=0,RUNNING=1,NO_POWER=2,BLOCKED=3,NO_RECIPE=4,UTILITY=5;
    private int progress,duration=100,processState,energyPerTick=8;
    private String recipeId="";
    private MachineRecipe cachedRecipe;
    private boolean loading,inputsChanged=true;
    private MachineUiSpec layout;
    public MachineUiSpec ui(){if(layout==null)layout=MachineUiSpec.get(kind());return layout;}
    public boolean processing(){return processState==RUNNING;}
    public final net.minecraftforge.items.ItemStackHandler inventory=new net.minecraftforge.items.ItemStackHandler(5){
        @Override public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack s){return slot<ui().inputs();}
        @Override protected void onContentsChanged(int slot){if(!loading){if(slot<3)inputsChanged=true;setChanged();}}
    };
    private final net.minecraftforge.items.IItemHandler automation=new net.minecraftforge.items.IItemHandler(){
        public int getSlots(){return 5;}
        public net.minecraft.world.item.ItemStack getStackInSlot(int slot){return inventory.getStackInSlot(slot);}
        public net.minecraft.world.item.ItemStack insertItem(int slot,net.minecraft.world.item.ItemStack stack,boolean simulate){return slot<ui().inputs()?inventory.insertItem(slot,stack,simulate):stack;}
        public net.minecraft.world.item.ItemStack extractItem(int slot,int count,boolean simulate){return slot>=3&&slot<3+ui().outputs()?inventory.extractItem(slot,count,simulate):net.minecraft.world.item.ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot<ui().inputs()||slot>=3&&slot<3+ui().outputs()?64:0;}
        public boolean isItemValid(int slot,net.minecraft.world.item.ItemStack stack){return slot<ui().inputs();}
    };
    private LazyOptional<net.minecraftforge.items.IItemHandler> itemCap=LazyOptional.of(()->automation);
    public final net.minecraft.world.inventory.ContainerData data=new net.minecraft.world.inventory.ContainerData(){
        public int get(int i){return switch(i){case 0->progress;case 1->duration;case 2->processState;case 3->energy;case 4->energyPerTick;default->0;};}
        public void set(int i,int value){switch(i){case 0->progress=value;case 1->duration=value;case 2->processState=value;case 3->energy=value;case 4->energyPerTick=value;}}
        public int getCount(){return 5;}
    };
    @Override public net.minecraft.network.chat.Component getDisplayName(){return getBlockState().getBlock().getName();}
    @Override public MachineMenu createMenu(int id,net.minecraft.world.entity.player.Inventory inv,net.minecraft.world.entity.player.Player player){return new MachineMenu(id,inv,this);}
    private net.minecraft.world.SimpleContainer charge(){return new net.minecraft.world.SimpleContainer(inventory.getStackInSlot(0),inventory.getStackInSlot(1),inventory.getStackInSlot(2));}
    private boolean outputFits(MachineRecipe recipe){for(int i=0;i<recipe.results().size();i++){var result=recipe.results().get(i);var out=inventory.getStackInSlot(3+i);if(!out.isEmpty()&&!net.minecraft.world.item.ItemStack.isSameItemSameTags(out,result)||out.getCount()+result.getCount()>result.getMaxStackSize())return false;}return true;}
    private void process(){
        if(ui().inputs()==0){processState=UTILITY;return;}
        if(inventory.getStackInSlot(0).isEmpty()&&inventory.getStackInSlot(1).isEmpty()&&inventory.getStackInSlot(2).isEmpty()){
            if(progress!=0||!recipeId.isEmpty())setChanged();progress=0;recipeId="";cachedRecipe=null;processState=WAITING;return;
        }
        var charge=charge();
        if(cachedRecipe!=null){var live=level.getRecipeManager().byKey(cachedRecipe.id()).orElse(null);if(live!=cachedRecipe||!cachedRecipe.matches(charge,level))cachedRecipe=null;}
        if(cachedRecipe==null&&(inputsChanged||level.getGameTime()%20==0)){
            cachedRecipe=level.getRecipeManager().getAllRecipesFor(MachineContent.PROCESSING_TYPE.get()).stream().filter(r->r.machine().equals(kind())&&r.matches(charge,level)).findFirst().orElse(null);inputsChanged=false;
        }
        if(cachedRecipe==null){if(!recipeId.isEmpty()){recipeId="";progress=0;setChanged();}processState=java.util.stream.IntStream.range(0,ui().inputs()).anyMatch(i->!inventory.getStackInSlot(i).isEmpty())?NO_RECIPE:WAITING;return;}
        var r=cachedRecipe;if(!recipeId.equals(r.id().toString())){recipeId=r.id().toString();progress=0;}
        duration=r.duration();energyPerTick=r.energy();
        if(!outputFits(r)){processState=BLOCKED;return;}
        if(energy<energyPerTick){processState=NO_POWER;return;}
        energy-=energyPerTick;processState=RUNNING;progress++;setChanged();
        if(progress>=duration){
            for(int i=0;i<r.inputs().size();i++)inventory.extractItem(i,1,false);
            for(int i=0;i<r.results().size();i++){var out=inventory.getStackInSlot(3+i).copy();if(out.isEmpty())out=r.results().get(i).copy();else out.grow(r.results().get(i).getCount());inventory.setStackInSlot(3+i,out);}
            progress=0;
        }
    }
    public void dropContents(){if(level==null||level.isClientSide)return;for(int i=0;i<inventory.getSlots();i++){net.minecraft.world.Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,inventory.getStackInSlot(i));inventory.setStackInSlot(i,net.minecraft.world.item.ItemStack.EMPTY);}}

    private long powerChange=Long.MIN_VALUE/2;
    private int energy;
    private final IEnergyStorage storage=new IEnergyStorage(){
        public int receiveEnergy(int n,boolean simulate){return receiveCableEnergy(n,simulate);}
        public int extractEnergy(int n,boolean simulate){return 0;}
        public int getEnergyStored(){return energy;}
        public int getMaxEnergyStored(){return 4000;}
        public boolean canExtract(){return false;}
        public boolean canReceive(){return true;}
    };
    private LazyOptional<IEnergyStorage> energyCap=LazyOptional.of(()->storage);
    public int cableEnergy(){return energy;}
    public int receiveCableEnergy(int n,boolean simulate){int moved=Math.min(Math.max(0,n),4000-energy);if(!simulate&&moved>0){energy+=moved;setChanged();}return moved;}
    public void tickEnergy(){
        if(level==null||level.isClientSide)return;
        int before=processState;process();((MachineBlock)getBlockState().getBlock()).updatePower(level,worldPosition,getBlockState());
        if(before!=processState){setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ENERGY?energyCap.cast():cap==ForgeCapabilities.ITEM_HANDLER?itemCap.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();itemCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();energyCap=LazyOptional.of(()->storage);itemCap=LazyOptional.of(()->automation);}
    public MachineBlockEntity(BlockPos pos,BlockState state){super(MachineContent.TYPE.get(),pos,state);}
    public String kind(){return ((MachineBlock)getBlockState().getBlock()).kind;}
    public boolean powered(){return getBlockState().getValue(MachineBlock.POWERED);}
    public double seconds(float partial){return level==null?0:(level.getGameTime()+partial)/20;}
    public double transitionTime(float partial){return level==null?100:(level.getGameTime()+partial-powerChange)/20;}
    public void powerChanged(boolean powered){powerChange=level.getGameTime();setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    @Override protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.putLong("PowerChange",powerChange);tag.putInt("CableEnergy",energy);tag.put("Inventory",inventory.serializeNBT());tag.putInt("Progress",progress);tag.putString("Recipe",recipeId);tag.putInt("Duration",duration);tag.putInt("ProcessState",processState);}
    @Override public void load(CompoundTag tag){super.load(tag);energy=Math.max(0,Math.min(4000,tag.getInt("CableEnergy")));powerChange=tag.contains("PowerChange")?tag.getLong("PowerChange"):Long.MIN_VALUE/2;loading=true;try{if(tag.contains("Inventory"))inventory.deserializeNBT(tag.getCompound("Inventory"));}finally{loading=false;}progress=Math.max(0,Math.min(32000,tag.getInt("Progress")));duration=Math.max(1,Math.min(32000,tag.getInt("Duration")));recipeId=tag.getString("Recipe");processState=tag.getInt("ProcessState");cachedRecipe=null;inputsChanged=true;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){return new AABB(worldPosition).inflate(2);}
}
