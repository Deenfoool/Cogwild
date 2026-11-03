package dev.salum.suitcaseglider.blast;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class BlastBlockEntity extends BlockEntity implements MenuProvider,IEnergyStorage {
    static boolean changing;
    public static final int ENERGY_CAPACITY=32000,AMBIENT=20;
    public static final int UNFORMED=0,WAITING=1,HEATING=2,RUNNING=3,NO_POWER=4,BLOCKED=5,UNLOADED=6;
    private int energy,temperature=AMBIENT,progress,duration=1200,requiredTemperature=1200,energyPerTick=32,state=UNFORMED;
    private ItemStack[] reserved={ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};
    private ItemStack pendingIron=ItemStack.EMPTY,pendingSlag=ItemStack.EMPTY;
    private long lastFeedTick=Long.MIN_VALUE/2;
    private boolean allLoaded,loadInventory;
    private final int[] inputCounts=new int[3];
    public final ItemStackHandler inventory=new ItemStackHandler(5){
        @Override public boolean isItemValid(int slot,ItemStack stack){return accepts(slot,stack);}
        @Override protected void onContentsChanged(int slot){contentsChanged(slot);}
    };
    public void contentsChanged(int slot){setChanged();if(slot<3){int count=inventory.getStackInSlot(slot).getCount(),old=inputCounts[slot];inputCounts[slot]=count;if(!loadInventory&&count>old&&level!=null&&!level.isClientSide){lastFeedTick=level.getGameTime();sync();}}}
    public final ContainerData data=new ContainerData(){
        public int get(int i){return switch(i){case 0->progress;case 1->duration;case 2->state;case 3->temperature;case 4->energy;case 5->requiredTemperature;case 6->energyPerTick;default->0;};}
        public void set(int i,int v){switch(i){case 0->progress=v;case 1->duration=v;case 2->state=v;case 3->temperature=v;case 4->energy=v;case 5->requiredTemperature=v;case 6->energyPerTick=v;default->{}}}
        public int getCount(){return 7;}
    };
    private final BlockState[] originals=new BlockState[BlastBlueprint.CELLS];
    public BlastBlockEntity(BlockPos p,BlockState s){super(BlastContent.TYPE.get(),p,s);}
    public boolean formed(){return getBlockState().getValue(BlastControllerBlock.FORMED);}
    public Direction facing(){return getBlockState().getValue(BlastModuleBlock.FACING);}
    public int energy(){return energy;}
    public int temperature(){return temperature;}
    public int state(){return state;}
    public boolean active(){return state==HEATING||state==RUNNING;}
    public boolean hot(){return temperature>=600;}
    public double feedSeconds(float partial){return level==null?100:(level.getGameTime()+partial-lastFeedTick)/20;}
    public boolean hasJob(){return !reserved[0].isEmpty();}
    public boolean accepts(int slot,ItemStack stack){return slot>=0&&slot<3&&level!=null&&level.getRecipeManager().getAllRecipesFor(BlastContent.RECIPE_TYPE.get()).stream().anyMatch(r->r.accepts(slot,stack));}
    public Vec3 modelPoint(double x,double y,double z){double a=Math.toRadians(180-facing().toYRot()),u=x/16-.5,v=z/16-.5;return new Vec3(worldPosition.getX()+.5+u*Math.cos(a)+v*Math.sin(a),worldPosition.getY()+y/16,worldPosition.getZ()+.5-u*Math.sin(a)+v*Math.cos(a));}
    public Vec3 powerSocket(){return modelPoint(46.9,11,26.7);}
    public BlockState original(int i){return originals[i]==null?BlastBlueprint.expected(i,facing()):originals[i];}
    public boolean form(Player player){
        if(level==null||level.isClientSide||formed())return false;
        for(int i=0;i<BlastBlueprint.CELLS;i++){
            var p=BlastBlueprint.at(worldPosition,facing(),i);var want=BlastBlueprint.expected(i,facing());
            if(level.isOutsideBuildHeight(p)||!level.getWorldBorder().isWithinBounds(p)||!level.hasChunkAt(p)){if(player!=null)player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.unloaded"),true);return false;}
            var s=level.getBlockState(p);
            if(!s.equals(want)||level.getBlockEntity(p)instanceof Container c&&!c.isEmpty()){
                if(player!=null)player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.mismatch",p.getX(),p.getY(),p.getZ(),want.getBlock().getName()),false);return false;
            }
        }
        for(int i=0;i<BlastBlueprint.CELLS;i++)originals[i]=level.getBlockState(BlastBlueprint.at(worldPosition,facing(),i));
        changing=true;try{
            for(int i=1;i<BlastBlueprint.CELLS;i++)level.setBlock(BlastBlueprint.at(worldPosition,facing(),i),BlastContent.PART.get().defaultBlockState().setValue(BlastModuleBlock.FACING,facing()).setValue(BlastPartBlock.INDEX,i),2);
            level.setBlock(worldPosition,getBlockState().setValue(BlastControllerBlock.FORMED,true),3);
        }finally{changing=false;}
        allLoaded=true;ensurePorts();setChanged();sync();
        if(player!=null)player.displayClientMessage(Component.translatable("message.suitcaseglider.blast.formed"),true);return true;
    }
    /** Restore loaded cells only. Unloaded proxy cells self-recover when loaded. */
    public void restore(BlockPos broken){
        if(level==null||level.isClientSide||!formed())return;var front=facing();
        changing=true;try{
            for(int i=1;i<BlastBlueprint.CELLS;i++){
                var p=BlastBlueprint.at(worldPosition,front,i);if(p.equals(broken)||!level.hasChunkAt(p))continue;
                var s=level.getBlockState(p);if(s.is(BlastContent.PART.get())&&s.getValue(BlastPartBlock.INDEX)==i&&s.getValue(BlastModuleBlock.FACING)==front)level.setBlock(p,original(i),3);
            }
            if(!worldPosition.equals(broken))level.setBlock(worldPosition,getBlockState().setValue(BlastControllerBlock.FORMED,false).setValue(BlastControllerBlock.LIT,false),3);
        }finally{changing=false;}allLoaded=false;state=UNFORMED;setChanged();sync();
    }
    private void ensurePorts(){for(int i:new int[]{1,5,31,40}){var p=BlastBlueprint.at(worldPosition,facing(),i);if(level.hasChunkAt(p)&&level.getBlockState(p).is(BlastContent.PART.get())&&!(level.getBlockEntity(p)instanceof BlastPortBlockEntity))level.setBlockEntity(new BlastPortBlockEntity(p,level.getBlockState(p)));}}
    /** Never force-load a chunk to process, transfer items or receive energy. */
    public boolean operable(){
        if(level==null||!formed())return false;
        for(int i=1;i<BlastBlueprint.CELLS;i++){var p=BlastBlueprint.at(worldPosition,facing(),i);if(!level.hasChunkAt(p))return false;var s=level.getBlockState(p);if(!s.is(BlastContent.PART.get())||s.getValue(BlastPartBlock.INDEX)!=i||s.getValue(BlastModuleBlock.FACING)!=facing())return false;}
        return true;
    }
    private boolean outputFits(int slot,ItemStack result){var out=inventory.getStackInSlot(slot);return !result.isEmpty()&&(out.isEmpty()||ItemStack.isSameItemSameTags(out,result))&&out.getCount()+result.getCount()<=Math.min(64,result.getMaxStackSize());}
    private void merge(int slot,ItemStack result){var out=inventory.getStackInSlot(slot).copy();if(out.isEmpty())out=result.copy();else out.grow(result.getCount());inventory.setStackInSlot(slot,out);}
    private void reserve(BlastRecipe r){
        loadInventory=true;try{for(int i=0;i<3;i++)reserved[i]=inventory.extractItem(i,1,false);}finally{loadInventory=false;}
        pendingIron=r.iron().copy();pendingSlag=r.slag().copy();duration=r.duration();requiredTemperature=r.temperature();energyPerTick=r.energy();progress=0;setChanged();
    }
    private void process(){
        if(!hasJob()){
            var charge=new SimpleContainer(inventory.getStackInSlot(0),inventory.getStackInSlot(1),inventory.getStackInSlot(2));
            var found=level.getRecipeManager().getRecipeFor(BlastContent.RECIPE_TYPE.get(),charge,level);
            if(found.isEmpty()){state=WAITING;return;}var r=found.get();
            if(!outputFits(3,r.iron())||!outputFits(4,r.slag())){state=BLOCKED;return;}
            if(energy<r.energy()){state=NO_POWER;return;}reserve(r);
        }
        if(!outputFits(3,pendingIron)||!outputFits(4,pendingSlag)){state=BLOCKED;return;}
        if(energy<energyPerTick){state=NO_POWER;return;}
        energy-=energyPerTick;
        if(temperature<requiredTemperature){temperature=Math.min(requiredTemperature,temperature+2);state=HEATING;return;}
        state=RUNNING;progress++;
        if(progress>=duration){merge(3,pendingIron);merge(4,pendingSlag);reserved=new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};pendingIron=ItemStack.EMPTY;pendingSlag=ItemStack.EMPTY;progress=0;}
    }
    public void tick(){
        if(level==null||level.isClientSide)return;int oldState=state;
        if(!formed()){state=UNFORMED;allLoaded=false;}
        else{
            allLoaded=true;
            for(int i=1;i<BlastBlueprint.CELLS;i++){
                var p=BlastBlueprint.at(worldPosition,facing(),i);if(!level.hasChunkAt(p)){allLoaded=false;continue;}
                var s=level.getBlockState(p);if(!s.is(BlastContent.PART.get())||s.getValue(BlastPartBlock.INDEX)!=i||s.getValue(BlastModuleBlock.FACING)!=facing()){restore(p);return;}
            }
            if(!allLoaded)state=UNLOADED;else{ensurePorts();process();}
        }
        if(!active()&&temperature>AMBIENT&&level.getGameTime()%20==0)temperature--;
        boolean lit=formed()&&hot();if(getBlockState().getValue(BlastControllerBlock.LIT)!=lit)level.setBlock(worldPosition,getBlockState().setValue(BlastControllerBlock.LIT,lit),3);
        if(active()&&level.getGameTime()%10==0&&level instanceof ServerLevel server){var p=modelPoint(45,58.2,43);server.sendParticles(ParticleTypes.SMOKE,p.x,p.y,p.z,3,.045,.07,.045,.015);}
        if(active()||temperature>AMBIENT)setChanged();
        if(oldState!=state||level.getGameTime()%10==0)sync();
    }
    public void dropContents(){
        if(level==null||level.isClientSide)return;
        loadInventory=true;try{for(int i=0;i<5;i++){Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,inventory.getStackInSlot(i));inventory.setStackInSlot(i,ItemStack.EMPTY);}for(var s:reserved)Containers.dropItemStack(level,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,s);}finally{loadInventory=false;}
        reserved=new ItemStack[]{ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};pendingIron=ItemStack.EMPTY;pendingSlag=ItemStack.EMPTY;progress=0;setChanged();
    }
    private void sync(){if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),2);}
    public int receiveEnergy(int n,boolean simulate){if(!operable())return 0;int moved=Math.min(Math.max(0,n),ENERGY_CAPACITY-energy);if(!simulate&&moved>0){energy+=moved;setChanged();}return moved;}
    public int extractEnergy(int n,boolean simulate){return 0;}
    public int getEnergyStored(){return energy;}
    public int getMaxEnergyStored(){return ENERGY_CAPACITY;}
    public boolean canExtract(){return false;}
    public boolean canReceive(){return operable();}
    private LazyOptional<IEnergyStorage> energyCap=LazyOptional.of(()->this);
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,Direction side){return cap==ForgeCapabilities.ENERGY&&formed()?energyCap.cast():super.getCapability(cap,side);}
    @Override public void invalidateCaps(){super.invalidateCaps();energyCap.invalidate();}
    @Override public void reviveCaps(){super.reviveCaps();energyCap=LazyOptional.of(()->this);}
    private void writeVisual(CompoundTag t){t.putInt("Energy",energy);t.putInt("Temperature",temperature);t.putInt("Progress",progress);t.putInt("Duration",duration);t.putInt("RequiredTemperature",requiredTemperature);t.putInt("EnergyPerTick",energyPerTick);t.putInt("State",state);t.putLong("LastFeed",lastFeedTick);}
    @Override protected void saveAdditional(CompoundTag t){
        super.saveAdditional(t);var list=new ListTag();for(int i=0;i<BlastBlueprint.CELLS;i++)list.add(NbtUtils.writeBlockState(original(i)));t.put("Construction",list);t.put("Inventory",inventory.serializeNBT());
        var charge=new ListTag();for(var s:reserved)charge.add(s.save(new CompoundTag()));t.put("Charge",charge);t.put("PendingIron",pendingIron.save(new CompoundTag()));t.put("PendingSlag",pendingSlag.save(new CompoundTag()));writeVisual(t);
    }
    @Override public void load(CompoundTag t){
        super.load(t);energy=Math.max(0,Math.min(ENERGY_CAPACITY,t.getInt("Energy")));temperature=Math.max(AMBIENT,Math.min(1400,t.getInt("Temperature")));duration=Math.max(1,Math.min(32000,t.contains("Duration")?t.getInt("Duration"):1200));progress=Math.max(0,Math.min(duration-1,t.getInt("Progress")));requiredTemperature=Math.max(21,Math.min(1400,t.contains("RequiredTemperature")?t.getInt("RequiredTemperature"):1200));energyPerTick=Math.max(1,Math.min(1000,t.contains("EnergyPerTick")?t.getInt("EnergyPerTick"):32));state=t.getInt("State");lastFeedTick=t.contains("LastFeed")?t.getLong("LastFeed"):Long.MIN_VALUE/2;
        if(t.getBoolean("VisualOnly"))return;
        var list=t.getList("Construction",10);for(int i=0;i<originals.length;i++)originals[i]=i<list.size()?NbtUtils.readBlockState(net.minecraft.core.registries.BuiltInRegistries.BLOCK.asLookup(),list.getCompound(i)):null;
        loadInventory=true;try{var inv=t.getCompound("Inventory").copy();inv.putInt("Size",5);inventory.deserializeNBT(inv);}finally{loadInventory=false;}
        var charge=t.getList("Charge",10);for(int i=0;i<3;i++)reserved[i]=i<charge.size()?ItemStack.of(charge.getCompound(i)):ItemStack.EMPTY;
        pendingIron=upgradeMetal(ItemStack.of(t.getCompound("PendingIron")));pendingSlag=ItemStack.of(t.getCompound("PendingSlag"));allLoaded=false;
        inventory.setStackInSlot(3,upgradeMetal(inventory.getStackInSlot(3)));
    }
    /** Upgrade 0.10.0 work in progress and stored output without restarting or duplicating a charge. */
    private static ItemStack upgradeMetal(ItemStack s){if(!s.is(BlastContent.PIG_IRON.get()))return s;var steel=new ItemStack(BlastContent.STEEL.get(),s.getCount());if(s.hasTag())steel.setTag(s.getTag().copy());return steel;}
    @Override public CompoundTag getUpdateTag(){var t=new CompoundTag();t.putBoolean("VisualOnly",true);writeVisual(t);return t;}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public AABB getRenderBoundingBox(){var other=BlastBlueprint.at(worldPosition,facing(),44);return new AABB(worldPosition,other.offset(1,1,1)).inflate(3);}
    @Override public Component getDisplayName(){return Component.translatable("container.suitcaseglider.blast_furnace");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new BlastMenu(id,inv,this);}
}
