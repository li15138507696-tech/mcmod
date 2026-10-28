// src/main/java/net/guzhenren/tianfu/net/TianfuDifficultyPayload.java
package net.guzhenren.tianfu.net;

import net.guzhenren.tianfu.TianfuData;
import net.guzhenren.tianfu.TianfuDifficulty;
import net.guzhenren.tianfu.TianfuMod;
import net.guzhenren.tianfu.TianfuNet;
import net.guzhenren.tianfu.TianfuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：选择开局难度。仅可选择一次，选定即发放自由点。
 */
public record TianfuDifficultyPayload(String id) implements CustomPacketPayload {
    public static final Type<TianfuDifficultyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TianfuMod.MODID, "difficulty"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TianfuDifficultyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.id, 32),
            buf -> new TianfuDifficultyPayload(buf.readUtf(32)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** V2 契约：服务端开窍门禁和确认锁后一次发放8或6或4或2，已选老档不重发，成对同步状态。未填充时拒绝执行，不写入玩家状态。 */
    public static void handle(TianfuDifficultyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }            if (!net.guzhenren.tianfu.TianfuOpening.hasOpenedQiao(player)) {
                return;
            }
            if (net.guzhenren.tianfu.TianfuData.difficulty(player) != net.guzhenren.tianfu.TianfuDifficulty.NOT_CHOSEN) {
                net.guzhenren.tianfu.TianfuNet.sendAll(player);
                return;
            }
            net.guzhenren.tianfu.TianfuDifficulty.Option option = net.guzhenren.tianfu.TianfuDifficulty.byId(payload.id());
            if (option == null) {
                net.guzhenren.tianfu.TianfuNet.sendAll(player);
                return;
            }
            net.guzhenren.tianfu.v2.TianfuV2Store.migrate(player);
            net.guzhenren.tianfu.TianfuData.setDifficulty(player, net.guzhenren.tianfu.TianfuDifficulty.OPTIONS.indexOf(option));
            net.guzhenren.tianfu.TianfuData.addPoints(player, option.points());
            net.guzhenren.tianfu.TianfuNet.sendAll(player);
        });    }
}
