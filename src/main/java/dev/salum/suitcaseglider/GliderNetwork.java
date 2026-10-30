package dev.salum.suitcaseglider;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;

public final class GliderNetwork {
    public record State(int phase, long started) {}
    public record Packet(UUID player, int phase, long started) {
        static void encode(Packet p, FriendlyByteBuf b) { b.writeUUID(p.player); b.writeVarInt(p.phase); b.writeLong(p.started); }
        static Packet decode(FriendlyByteBuf b) { return new Packet(b.readUUID(), b.readVarInt(), b.readLong()); }
        static void handle(Packet p, Supplier<NetworkEvent.Context> context) {
            context.get().enqueueWork(() -> CLIENT.put(p.player, new State(p.phase, p.started)));
            context.get().setPacketHandled(true);
        }
    }
    public static final Map<UUID, State> CLIENT = new ConcurrentHashMap<>();
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(SuitcaseGlider.id("state"), () -> "1", "1"::equals, "1"::equals);
    public static void register() { CHANNEL.messageBuilder(Packet.class, 0, NetworkDirection.PLAY_TO_CLIENT).encoder(Packet::encode).decoder(Packet::decode).consumerMainThread(Packet::handle).add(); }
    public static void broadcast(ServerPlayer p, int phase, long started) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> p), new Packet(p.getUUID(), phase, started));
    }
    public static void sendTo(ServerPlayer observer, ServerPlayer p) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> observer), new Packet(p.getUUID(), GliderController.phase(p), p.getPersistentData().getLong("sg_started")));
    }
}
