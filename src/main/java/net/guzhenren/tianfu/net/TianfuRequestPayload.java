// src/main/java/net/guzhenren/tianfu/net/TianfuRequestPayload.java
package net.guzhenren.tianfu.net;

import net.guzhenren.tianfu.TianfuMod;
import net.guzhenren.tianfu.TianfuNet;
import net.guzhenren.tianfu.TianfuOpening;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：请求打开「天赋」合一界面。无负载，服务端按当前状态回发 {@link TianfuStatePayload}。
 *
 * <p>门禁：未以希望蛊开窍（本体 scoreboard {@code kaiqiao} == 0）时拒绝，仅回一条提示——
 * 希望蛊是天赋系统的唯一开窍开关。
 */
public record TianfuRequestPayload() implements CustomPacketPayload {
    public static final Type<TianfuRequestPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TianfuMod.MODID, "request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TianfuRequestPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
            }, buf -> new TianfuRequestPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Service-thread gate and migration; any failure is logged and reported without opening a partial UI. */
    public static void handle(TianfuRequestPayload payload, IPayloadContext context) {
        try {
            context.enqueueWork(() -> {
                ServerPlayer player = null;
                try {
                    if (!(context.player() instanceof ServerPlayer serverPlayer)) return;
                    player = serverPlayer;
                    if (!TianfuOpening.hasOpenedQiao(player)) {
                        player.displayClientMessage(Component.literal("需以希望蛊开窍后才能选择天赋"), true);
                        return;
                    }
                    TianfuNet.openSelection(player);
                } catch (Throwable e) {
                    TianfuMod.LOGGER.error("天赋请求失败", e);
                    if (player != null) {
                        player.displayClientMessage(Component.literal(
                                "天赋状态同步失败，请稍后按 K 重试；详情请查看服务端日志。"), false);
                    }
                }
            });
        } catch (Throwable e) {
            TianfuMod.LOGGER.error("天赋请求调度失败", e);
        }
    }
}
