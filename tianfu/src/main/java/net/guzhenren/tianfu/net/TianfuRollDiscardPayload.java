// src/main/java/net/guzhenren/tianfu/net/TianfuRollDiscardPayload.java
package net.guzhenren.tianfu.net;

import net.guzhenren.tianfu.TianfuData;
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
 * C2S：放弃当前未接受的随机候选。
 *
 * <p>候选只存在 {@code rollpending} 待定区，放弃不扣点、不返还已消耗的摇取次数；
 * 没有候选时是幂等的空操作。存在的意义是让「有未接受候选就不能关界面」不会变成永久锁死。
 */
public record TianfuRollDiscardPayload() implements CustomPacketPayload {
    public static final Type<TianfuRollDiscardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TianfuMod.MODID, "roll_discard"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TianfuRollDiscardPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
            }, buf -> new TianfuRollDiscardPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TianfuRollDiscardPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            if (!TianfuOpening.hasOpenedQiao(player)) {
                player.displayClientMessage(Component.literal("需以希望蛊开窍后才能放弃随机候选"), false);
                return;
            }
            if (TianfuData.isConfirmed(player) || TianfuData.isSettled(player)) {
                TianfuNet.sendAll(player);
                return;
            }
            if (!TianfuData.rollPending(player).isEmpty()) {
                TianfuData.clearRollPending(player);
                player.displayClientMessage(Component.literal("已放弃本次随机候选；不返还摇取次数，不扣点。"), false);
            }
            TianfuNet.sendAll(player);
        });
    }
}
