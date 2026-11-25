package net.guzhenren.tianfu;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UniversalState(int points, List<String> owned, boolean open) implements CustomPacketPayload {
    public static final Type<UniversalState> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("tianfu", "universal_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UniversalState> CODEC = StreamCodec.of(
        (b, p) -> { b.writeVarInt(p.points); b.writeCollection(p.owned, (out, id) -> out.writeUtf(id)); b.writeBoolean(p.open); },
        b -> {
            int points = b.readVarInt();
            List<String> ids = new ArrayList<>(b.readCollection(ArrayList::new, in -> in.readUtf()));
            return new UniversalState(points, ids, b.readBoolean());
        });
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(UniversalState p, IPayloadContext c) {
        c.enqueueWork(() -> net.guzhenren.tianfu.client.UniversalScreen.open(p));
    }
}
