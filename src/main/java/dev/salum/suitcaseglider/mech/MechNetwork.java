package dev.salum.suitcaseglider.mech;

import dev.salum.suitcaseglider.SuitcaseGlider;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Supplier;

public final class MechNetwork {
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(SuitcaseGlider.id("mech_controls"),()->"2","2"::equals,"2"::equals);
    public static void register(){
        CHANNEL.messageBuilder(Dismount.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((packet,buf)->{}).decoder(buf->new Dismount()).consumerMainThread(MechNetwork::handle).add();
        CHANNEL.messageBuilder(Motion.class,1,NetworkDirection.PLAY_TO_SERVER).encoder((p,b)->{b.writeFloat(p.forward);b.writeFloat(p.strafe);b.writeBoolean(p.jumpHeld);b.writeFloat(p.charge);b.writeBoolean(p.grounded);b.writeFloat(p.speed);}).decoder(b->new Motion(b.readFloat(),b.readFloat(),b.readBoolean(),b.readFloat(),b.readBoolean(),b.readFloat())).consumerMainThread(MechNetwork::motion).add();
    }
    public static void reportMotion(float forward,float strafe,boolean held,float charge,boolean grounded,float speed){CHANNEL.sendToServer(new Motion(forward,strafe,held,charge,grounded,speed));}
    private static void motion(Motion p,Supplier<NetworkEvent.Context> supplier){
        var context=supplier.get();var rider=context.getSender();
        if(rider!=null&&rider.getVehicle() instanceof MechAntEntity ant&&ant.getControllingPassenger()==rider&&Float.isFinite(p.forward+p.strafe+p.charge+p.speed))ant.riderMotion(p.forward,p.strafe,p.jumpHeld,p.charge,p.grounded,p.speed);
        context.setPacketHandled(true);
    }
    public static void requestDismount(){CHANNEL.sendToServer(new Dismount());}
    private static void handle(Dismount packet,Supplier<NetworkEvent.Context> supplier){
        var context=supplier.get();var player=context.getSender();
        if(player!=null&&player.getVehicle() instanceof MechAntEntity ant)ant.requestDismount(player);
        context.setPacketHandled(true);
    }
    private record Dismount(){}
    private record Motion(float forward,float strafe,boolean jumpHeld,float charge,boolean grounded,float speed){}
}
