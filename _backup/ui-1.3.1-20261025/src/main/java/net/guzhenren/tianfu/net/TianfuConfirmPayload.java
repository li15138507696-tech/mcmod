// src/main/java/net/guzhenren/tianfu/net/TianfuConfirmPayload.java
package net.guzhenren.tianfu.net;

import java.util.Set;

import net.guzhenren.tianfu.TianfuApply;
import net.guzhenren.tianfu.TianfuData;
import net.guzhenren.tianfu.TianfuDifficulty;
import net.guzhenren.tianfu.TianfuMod;
import net.guzhenren.tianfu.TianfuNet;
import net.guzhenren.tianfu.TianfuPool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：最终确认本世天赋选择。无负载。
 *
 * <p>一经确认，本世的购买 / 撤销 / 难度全部冻结，直到寿元为0转世（{@code wipeAll} 清除标记）。
 * 门禁：必须已选开局难度（未选难度就锁定会让玩家永远无法购买）。
 */
public record TianfuConfirmPayload() implements CustomPacketPayload {
    public static final Type<TianfuConfirmPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TianfuMod.MODID, "confirm"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TianfuConfirmPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
            }, buf -> new TianfuConfirmPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** V2 契约：预检出身与硬代价后幂等结算再锁定；确认认可原生资质、已购SIJUE或历史机缘。 */
    public static void handle(TianfuConfirmPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }            net.guzhenren.tianfu.v2.TianfuV2Store.migrate(player);
            net.guzhenren.tianfu.v2.TianfuSelection.Snapshot snapshot = net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
            if (snapshot.confirmed()) {
                net.guzhenren.tianfu.TianfuNet.sendAll(player);
                return;
            }
            net.guzhenren.tianfu.TianfuApply.applyAllConfirmed(player, false);
            net.guzhenren.tianfu.TianfuNet.sendAll(player);
        });    }
}
