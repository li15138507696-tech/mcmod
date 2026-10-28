// src/main/java/net/guzhenren/tianfu/net/TianfuRollAcceptPayload.java
package net.guzhenren.tianfu.net;

import java.util.List;

import net.guzhenren.tianfu.TianfuApply;
import net.guzhenren.tianfu.TianfuData;
import net.guzhenren.tianfu.TianfuMod;
import net.guzhenren.tianfu.TianfuNet;
import net.guzhenren.tianfu.TianfuRoll;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：接受「命运由天」的随机结果 —— 落库 + 结算 + <b>本世立即锁定</b>。
 *
 * <p>与手动点「最终确认」等价（随机后不允许手动增删）：走同一条结算链路
 * {@code applyRoll → markDeferred → applyAllConfirmed → markConfirmed → markSettled}。
 *
 * <p>服务端兜底校验（包可被伪造）：
 * <ol>
 *   <li>待定区非空、本世未确认；</li>
 *   <li>结果必须含 1 个资质（十绝体或机缘）—— 否则确认门禁过不了，会永久卡死；</li>
 *   <li>净成本不得超过当前余额。</li>
 * </ol>
 */
public record TianfuRollAcceptPayload() implements CustomPacketPayload {
    public static final Type<TianfuRollAcceptPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TianfuMod.MODID, "roll_accept"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TianfuRollAcceptPayload> STREAM_CODEC =
            StreamCodec.of((buf, payload) -> {
            }, buf -> new TianfuRollAcceptPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** V2 契约：服务端重新验证整批和出身，使用Economy.acceptRoll原子提交且确认幂等，禁止只信netCost。未填充时拒绝执行，不写入玩家状态。 */
    public static void handle(TianfuRollAcceptPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }            net.guzhenren.tianfu.v2.TianfuV2Store.migrate(player);
            java.util.List<String> pending = net.guzhenren.tianfu.TianfuData.rollPending(player);
            if (!pending.isEmpty()) {
                net.guzhenren.tianfu.TianfuData.applyRoll(player, pending, 0);
                net.guzhenren.tianfu.TianfuData.clearRollPending(player);
            }
            net.guzhenren.tianfu.TianfuNet.sendAll(player);
        });    }
}
