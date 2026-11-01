package dev.salum.suitcaseglider.cable;

import dev.salum.suitcaseglider.SuitcaseGlider;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.*;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class CableNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(SuitcaseGlider.id("cables"),()->"1","1"::equals,"1"::equals);
    public record Picture(boolean floor,List<Vec3> points){}
    public record Snapshot(String dimension,List<Picture> links){}
    public static void register(){CHANNEL.messageBuilder(Snapshot.class,0,NetworkDirection.PLAY_TO_CLIENT).encoder(CableNetwork::encode).decoder(CableNetwork::decode).consumerMainThread((p,s)->{DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->dev.salum.suitcaseglider.client.CableRenderer.accept(p));s.get().setPacketHandled(true);}).add();}
    private static void encode(Snapshot p,FriendlyByteBuf b){b.writeUtf(p.dimension);b.writeVarInt(p.links.size());for(var e:p.links){b.writeBoolean(e.floor);b.writeVarInt(e.points.size());for(Vec3 v:e.points){b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z);}}}
    private static Snapshot decode(FriendlyByteBuf b){String dim=b.readUtf();int count=b.readVarInt();if(count<0||count>512)throw new IllegalArgumentException("Cable count");var edges=new ArrayList<Picture>();for(int i=0;i<count;i++){boolean floor=b.readBoolean();int n=b.readVarInt();if(n<2||n>1024)throw new IllegalArgumentException("Cable points");var points=new ArrayList<Vec3>();for(int j=0;j<n;j++){Vec3 v=new Vec3(b.readDouble(),b.readDouble(),b.readDouble());if(!Double.isFinite(v.x+v.y+v.z))throw new IllegalArgumentException("Cable vertex");points.add(v);}edges.add(new Picture(floor,List.copyOf(points)));}return new Snapshot(dim,List.copyOf(edges));}
    public static void send(ServerPlayer p,CableWorld world){
        var links=world.links.stream().filter(e->e.points().stream().anyMatch(v->v.distanceToSqr(p.position())<112*112)).sorted(Comparator.comparingDouble(e->e.points().get(0).distanceToSqr(p.position()))).limit(512).map(e->new Picture(e.floor(),e.points())).toList();
        CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Snapshot(p.serverLevel().dimension().location().toString(),links));
    }
    public static void broadcast(ServerLevel l){for(ServerPlayer p:l.players())send(p,CableWorld.get(l));}
}
