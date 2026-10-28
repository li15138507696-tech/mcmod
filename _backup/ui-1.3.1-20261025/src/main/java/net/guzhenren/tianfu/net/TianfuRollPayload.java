// src/main/java/net/guzhenren/tianfu/net/TianfuRollPayload.java
package net.guzhenren.tianfu.net;

import java.util.List;
import java.util.Random;

import net.guzhenren.tianfu.TianfuData;
import net.guzhenren.tianfu.TianfuDifficulty;
import net.guzhenren.tianfu.TianfuMod;
import net.guzhenren.tianfu.TianfuNet;
import net.guzhenren.tianfu.TianfuOpening;
import net.guzhenren.tianfu.TianfuRoll;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * C2S：「命运由天」摇一次。首次摇与重摇共用本包。
 *
 * <ul>
 *   <li><b>首次</b>（待定区为空、难度未选）：按 {@code difficultyId} 定下难度、发放自由点，
 *       然后生成随机配置。</li>
 *   <li><b>重摇</b>（待定区非空）：不重复发点，用当前余额重新生成；{@code difficultyId} 被忽略。</li>
 * </ul>
 *
 * <p>门禁：已开窍；未最终确认；重摇次数未用尽（{@link TianfuRoll#MAX_ROLLS}）。
 * 已选过难度的存档<b>不能</b>再走随机 —— 随机与「自己买」是开局二选一的另一条路。
 *
 * <p>结果只写进 {@code rollpending} 待定区，<b>不</b>落 {@code purchased}：
 * 十绝体 / 机缘不可撤销，落库后重摇就没有退路了。
 */
public record TianfuRollPayload(String difficultyId) implements CustomPacketPayload {
    public static final Type<TianfuRollPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TianfuMod.MODID, "roll"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TianfuRollPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.difficultyId, 32),
            buf -> new TianfuRollPayload(buf.readUtf(32)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** V2 契约：保持旧随机接口，生成后按V2经济整批预检，不能绕过24限额与出身锁。未填充时拒绝执行，不写入玩家状态。 */
    public static void handle(TianfuRollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }            net.guzhenren.tianfu.v2.TianfuV2Store.migrate(player);
            net.guzhenren.tianfu.v2.TianfuSelection.Snapshot snapshot = net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
            java.util.List<String> plan = net.guzhenren.tianfu.v2.TianfuEconomy.planRoll(snapshot,
                    new java.util.Random(player.getRandom().nextLong()));
            net.guzhenren.tianfu.TianfuData.setRollPending(player, plan);
            net.guzhenren.tianfu.TianfuNet.sendAll(player);
        });    }
}
