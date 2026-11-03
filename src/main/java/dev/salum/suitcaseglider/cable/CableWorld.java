package dev.salum.suitcaseglider.cable;

import dev.salum.suitcaseglider.SuitcaseGlider;
import dev.salum.suitcaseglider.machine.*;
import dev.salum.suitcaseglider.blast.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.server.level.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid=SuitcaseGlider.ID)
public final class CableWorld extends SavedData {
    public record Link(UUID id,BlockPos a,BlockPos b,boolean floor,int cost,List<Vec3> points){}
    public final List<Link> links=new ArrayList<>();
    public static CableWorld get(ServerLevel l){return l.getDataStorage().computeIfAbsent(CableWorld::load,CableWorld::new,"cogwild_cables");}
    public static BlockPos endpoint(Level level,BlockPos p){
        var s=level.getBlockState(p);
        if(s.getBlock() instanceof MachineBlock)return MachineBlock.base(p,s);
        if(s.is(BlastContent.CONTROLLER.get())&&s.getValue(BlastControllerBlock.FORMED))return p.immutable();
        if(s.is(BlastContent.PART.get())){var owner=BlastPartBlock.owner(s,level,p);if(owner!=null&&owner.formed())return owner.getBlockPos();}
        return s.getBlock() instanceof EnergyNodeBlock?p.immutable():null;
    }
    public static Vec3 anchor(Level level,BlockPos p){
        var s=level.getBlockState(p);
        if(level.getBlockEntity(p)instanceof BlastBlockEntity b){var d=b.facing().getClockWise();return b.powerSocket().add(d.getStepX()*.23,0,d.getStepZ()*.23);}
        if(s.getBlock() instanceof MachineBlock machine){
            var port=machine.spec().power();double x=port[0],y=port[1],z=port[2];
            double r=Math.toRadians(180-s.getValue(MachineBlock.FACING).toYRot());
            return new Vec3(p.getX()+.5+x*Math.cos(r)+z*Math.sin(r),p.getY()+y,p.getZ()+.5-x*Math.sin(r)+z*Math.cos(r));
        }
        Direction d=s.getValue(EnergyNodeBlock.FACING);
        if(((EnergyNodeBlock)s.getBlock()).generator)return Vec3.atCenterOf(p).add(d.getStepX()*.59,0,d.getStepZ()*.59);
        return Vec3.atCenterOf(p).add(d.getStepX()*-.06,d.getStepY()*-.06,d.getStepZ()*-.06);
    }
    public static CableRoutes.Terrain terrain(Level l,BlockPos a,BlockPos b){
        Set<BlockPos> ignore=new HashSet<>(List.of(a,b));
        for(BlockPos p:List.of(a,b)){var s=l.getBlockState(p);if(s.getBlock() instanceof MachineBlock machine)ignore.addAll(machine.cells(MachineBlock.base(p,s),s));}
        for(BlockPos p:List.of(a,b))if(l.getBlockEntity(p)instanceof BlastBlockEntity furnace)for(int i=0;i<BlastBlueprint.CELLS;i++)ignore.add(BlastBlueprint.at(p,furnace.facing(),i));
        return new CableRoutes.Terrain(){
            public CableRoutes.Surface surface(int x,int z,double nearY){
                for(int y=(int)Math.floor(nearY);y>=Math.floor(nearY)-8;y--){
                    BlockPos p=new BlockPos(x,y,z);if(!l.hasChunkAt(p)||!l.isInWorldBounds(p))return null;
                    var state=l.getBlockState(p);if(ignore.contains(p)||state.getBlock() instanceof MachineBlock||state.getBlock() instanceof EnergyNodeBlock)continue;
                    var shape=state.getCollisionShape(l,p);if(shape.isEmpty())continue;
                    var hit=shape.clip(new Vec3(x+.5,y+1.01,z+.5),new Vec3(x+.5,y-.01,z+.5),p);if(hit==null)continue;
                    double top=hit.getLocation().y;if(top>nearY+.01)continue;
                    if(!l.getFluidState(p.above()).isEmpty())continue;
                    var point=new Vec3(x+.5,top+.045,z+.5);if(clear(point,point.add(0,.08,0)))return new CableRoutes.Surface(p,top);
                }return null;
            }
            public boolean clear(Vec3 from,Vec3 to){
                // Sample at <1/8 block; use each block's real collision shape, not a solid-cube assumption.
                int n=Math.max(1,(int)Math.ceil(from.distanceTo(to)*12));
                for(int i=0;i<=n;i++){
                    Vec3 point=from.lerp(to,(double)i/n);BlockPos pos=BlockPos.containing(point);
                    if(!l.hasChunkAt(pos)||!l.isInWorldBounds(pos))return false;
                    if(ignore.contains(pos)){
                        if(l.getBlockState(pos).getBlock() instanceof MachineBlock)continue;
                        if(l.getBlockState(pos).is(BlastContent.PART.get())||l.getBlockState(pos).is(BlastContent.CONTROLLER.get())){
                            // Only the short socket stub may enter the multiblock. Floor routes must go around its base.
                            Vec3 sa=socket(l,a),sb=socket(l,b);
                            if(point.distanceToSqr(sa)<.35*.35||point.distanceToSqr(sb)<.35*.35)continue;
                        }
                        var block=l.getBlockState(pos).getBlock();
                        if(block instanceof EnergyNodeBlock node&&!node.generator)continue;
                        if(point.distanceToSqr(anchor(l,a))<.25*.25||point.distanceToSqr(anchor(l,b))<.25*.25)continue;
                    }
                    var shape=l.getBlockState(pos).getCollisionShape(l,pos);
                    if(!shape.isEmpty()&&!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(shape.move(pos.getX(),pos.getY(),pos.getZ()),net.minecraft.world.phys.shapes.Shapes.create(new AABB(point.add(-.018,-.018,-.018),point.add(.018,.018,.018))),net.minecraft.world.phys.shapes.BooleanOp.AND))continue;
                    if(!shape.isEmpty())return false;
                }return true;
            }
        };
    }
    public static List<Vec3> plan(Level l,BlockPos a,BlockPos b,boolean floor){
        var terrain=terrain(l,a,b);var route=floor?CableRoutes.floor(anchor(l,a),anchor(l,b),terrain):CableRoutes.air(anchor(l,a),anchor(l,b),terrain);
        if(route.isEmpty())return route;var points=new ArrayList<Vec3>();points.add(socket(l,a));points.addAll(route);points.add(socket(l,b));
        return CableRoutes.length(points)<=CableRoutes.MAX_LENGTH?List.copyOf(points):List.of();
    }
    private static Vec3 socket(Level l,BlockPos p){
        var s=l.getBlockState(p);Vec3 port=anchor(l,p);
        if(l.getBlockEntity(p)instanceof BlastBlockEntity b)return b.powerSocket();
        if(s.getBlock() instanceof MachineBlock m){Direction back=s.getValue(MachineBlock.FACING).getOpposite();double inset=m.kind.equals("lathe")?.1475:.10375;return port.add(-back.getStepX()*inset,0,-back.getStepZ()*inset);}
        return port;
    }
    public List<Vec3> route(ServerLevel l,BlockPos a,BlockPos b,boolean floor){return plan(l,a,b,floor);}
    public boolean available(BlockPos a,BlockPos b){return links.size()<4096&&!a.equals(b)&&links.stream().noneMatch(e->e.a.equals(a)&&e.b.equals(b)||e.a.equals(b)&&e.b.equals(a))&&degree(a)<8&&degree(b)<8;}
    private long degree(BlockPos p){return links.stream().filter(e->e.a.equals(p)||e.b.equals(p)).count();}
    public void add(Link link){links.add(link);setDirty();}
    public void remove(ServerLevel level,Link link,boolean refund){
        if(!links.remove(link))return;setDirty();
        if(refund){Vec3 p=link.points.get(0);int remaining=link.cost;while(remaining>0){int n=Math.min(64,remaining);level.addFreshEntity(new ItemEntity(level,p.x,p.y,p.z,new ItemStack(CableContent.CABLE.get(),n)));remaining-=n;}}
    }
    private boolean loaded(ServerLevel l,Link e){return e.points.stream().allMatch(p->l.hasChunkAt(BlockPos.containing(p)))&&l.hasChunkAt(e.a)&&l.hasChunkAt(e.b);}
    public void validate(ServerLevel l){
        for(Link e:List.copyOf(links)){
            // Unloading a chunk never destroys a connection or forces the chunk to load.
            if(!loaded(l,e))continue;
            if(endpoint(l,e.a)==null||endpoint(l,e.b)==null){remove(l,e,true);continue;}
            if(e.points.get(0).distanceToSqr(socket(l,e.a))>.0001||e.points.get(e.points.size()-1).distanceToSqr(socket(l,e.b))>.0001){remove(l,e,true);continue;}
            var terrain=terrain(l,e.a,e.b);boolean valid=CableRoutes.valid(e.points,terrain);
            if(valid&&e.floor)for(int i=1;i<e.points.size();i++){
                if(i==1||i==e.points.size()-1)continue; // Connector stubs may be above the floor.
                Vec3 a=e.points.get(i-1),b=e.points.get(i);if(Math.abs(a.y-b.y)>.01||a.distanceTo(b)<.05)continue;
                Vec3 mid=a.lerp(b,.5);var support=terrain.surface((int)Math.floor(mid.x),(int)Math.floor(mid.z),mid.y);
                if(support==null||Math.abs(support.y()+.045-mid.y)>.10){valid=false;break;}
            }
            if(!valid)remove(l,e,true);
        }
    }
    public void tick(ServerLevel l){
        if(l.getGameTime()%20==0)validate(l);
        var adjacency=new HashMap<BlockPos,Set<BlockPos>>();
        for(Link e:links)if(loaded(l,e)){adjacency.computeIfAbsent(e.a,k->new HashSet<>()).add(e.b);adjacency.computeIfAbsent(e.b,k->new HashSet<>()).add(e.a);}
        var visited=new HashSet<BlockPos>();
        for(BlockPos root:adjacency.keySet()){
            if(!visited.add(root))continue;var component=new ArrayList<BlockPos>();var queue=new ArrayDeque<BlockPos>();queue.add(root);
            while(!queue.isEmpty()){BlockPos p=queue.remove();component.add(p);for(BlockPos q:adjacency.getOrDefault(p,Set.of()))if(visited.add(q))queue.add(q);}
            var sources=new ArrayList<EnergyNodeEntity>();var machines=new ArrayList<IEnergyStorage>();
            for(BlockPos p:component){BlockEntity be=l.getBlockEntity(p);if(be instanceof EnergyNodeEntity node)sources.add(node);else if(be!=null)be.getCapability(ForgeCapabilities.ENERGY).ifPresent(cap->{if(cap.canReceive()&&!cap.canExtract())machines.add(cap);});}
            // Rotate the first receiver to prevent a low-power network starving the same machine every tick.
            if(!machines.isEmpty())Collections.rotate(machines,(int)(l.getGameTime()%machines.size()));
            int budget=256;
            for(var machine:machines)for(var source:sources){int wanted=Math.min(budget,machine.receiveEnergy(source.extractEnergy(budget,true),true));if(wanted>0){int moved=source.extractEnergy(wanted,false);machine.receiveEnergy(moved,false);budget-=moved;}}
            // Relays buffer power at the far end for adjacent Forge Energy consumers.
            for(var sink:sources)for(var source:sources)if(source!=sink&&source.stored>sink.stored+1&&budget>0){int n=Math.min(budget,(source.stored-sink.stored)/2);n=sink.receiveEnergy(n,true);sink.receiveEnergy(source.extractEnergy(n,false),false);budget-=n;}
        }
        if(l.getGameTime()%40==0)for(ServerPlayer p:l.players())CableNetwork.send(p,this);
    }
    @Override public CompoundTag save(CompoundTag tag){ListTag list=new ListTag();for(Link e:links){CompoundTag n=new CompoundTag();n.putUUID("Id",e.id);n.putLong("A",e.a.asLong());n.putLong("B",e.b.asLong());n.putBoolean("Floor",e.floor);n.putInt("Cost",e.cost);ListTag points=new ListTag();for(Vec3 p:e.points){CompoundTag v=new CompoundTag();v.putDouble("X",p.x);v.putDouble("Y",p.y);v.putDouble("Z",p.z);points.add(v);}n.put("Points",points);list.add(n);}tag.put("Links",list);return tag;}
    public static CableWorld load(CompoundTag tag){var world=new CableWorld();for(Tag value:tag.getList("Links",Tag.TAG_COMPOUND)){var n=(CompoundTag)value;var points=new ArrayList<Vec3>();for(Tag entry:n.getList("Points",Tag.TAG_COMPOUND)){var p=(CompoundTag)entry;points.add(new Vec3(p.getDouble("X"),p.getDouble("Y"),p.getDouble("Z")));}if(points.size()>=2&&points.size()<=1024&&n.hasUUID("Id"))world.links.add(new Link(n.getUUID("Id"),BlockPos.of(n.getLong("A")),BlockPos.of(n.getLong("B")),n.getBoolean("Floor"),Math.max(1,Math.min(32,n.getInt("Cost"))),List.copyOf(points)));}return world;}
    @SubscribeEvent public static void tickLevel(TickEvent.LevelTickEvent e){if(e.phase==TickEvent.Phase.END&&e.level instanceof ServerLevel l)get(l).tick(l);}
    @SubscribeEvent public static void cut(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getLevel() instanceof ServerLevel l)||!e.getItemStack().is(net.minecraft.world.item.Items.SHEARS)||!e.getEntity().mayBuild())return;
        BlockPos p=endpoint(l,e.getPos());if(p==null)return;var world=get(l);
        var match=world.links.stream().filter(link->link.a.equals(p)||link.b.equals(p)).min(Comparator.comparingDouble(link->{Vec3 from=link.a.equals(p)?link.points.get(1):link.points.get(link.points.size()-2);return from.distanceToSqr(e.getHitVec().getLocation());}));
        if(match.isPresent()){world.remove(l,match.get(),!e.getEntity().isCreative());if(!e.getEntity().isCreative())e.getItemStack().hurtAndBreak(1,e.getEntity(),player->player.broadcastBreakEvent(e.getHand()));CableNetwork.broadcast(l);e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}
    }
}
