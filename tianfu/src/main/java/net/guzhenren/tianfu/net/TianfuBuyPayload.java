// src/main/java/net/guzhenren/tianfu/net/TianfuBuyPayload.java
package net.guzhenren.tianfu.net;

import net.guzhenren.tianfu.TianfuApply;
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
 * C2S：购买单个天赋条目。服务端权威校验（余额 / 槽位 / 去重）后扣点生效，
 * 成功即回发最新 {@link TianfuStatePayload} 刷新界面。
 *
 * <p><b>1.4.5</b>：失败时把具体原因回显给玩家。此前 {@code TianfuApply.buy} 只回
 * boolean，这里拿到 false 就什么都不做，于是「点数不够」「与已选互斥」「已锁定」
 * 三种情况在客户端表现完全一样——**点了没反应**。玩家会以为自己买上了
 * （典型：点了十绝体但没扣点，最终确认后仍是凡体，tizhi 还是 0）。
 */
public record TianfuBuyPayload(String id) implements CustomPacketPayload {
    public static final Type<TianfuBuyPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(TianfuMod.MODID, "buy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TianfuBuyPayload> STREAM_CODEC = StreamCodec.of(
            (buf, payload) -> buf.writeUtf(payload.id, 64),
            buf -> new TianfuBuyPayload(buf.readUtf(64)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** V2 契约：服务端线程只走Apply.buy统一V2事务，失败零写入，成功成对回发旧state和v2_state。未填充时拒绝执行，不写入玩家状态。 */
    public static void handle(TianfuBuyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof net.minecraft.server.level.ServerPlayer player)) {
                return;
            }
            var rejection = TianfuApply.buyReason(player, payload.id());
            if (rejection != net.guzhenren.tianfu.v2.TianfuSelection.Rejection.NONE) {
                player.displayClientMessage(Component.literal(reason(player, payload.id(), rejection)), true);
            }
            TianfuNet.sendAll(player);
        });
    }

    /** 把拒绝原因翻成玩家能懂的一句话（含点数差额，省得自己算）。 */
    private static String reason(ServerPlayer player, String id, Object rejection) {
        var snapshot = net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
        TianfuPool.Spec spec = TianfuPool.byId(id);
        String name = spec == null ? id : spec.name();
        int cost = spec == null ? 0 : spec.cost();
        int have = snapshot.points();
        return switch (rejection.toString()) {
            case "INSUFFICIENT_POINTS" -> "点数不足：" + name + " 需要 " + cost
                    + " 点，你现在只有 " + have + " 点";
            case "CONFLICT" -> name + " 与已选内容互斥："
                    + "体质（凡体/十绝体）只能选一个，资质档位（丁/丙/乙/甲）与十绝体只能选一个，"
                    + "空窍桎梏不能与资质档位同选";
            case "NEGATIVE_CAP" -> "负面补点已达上限 24 点，不能再选负面天赋";
            case "NO_MAIN_DAO" -> name + " 只强化「当前主修流派」：请先确定主修"
                    + "（道途里选，或 /tianfu maindao <流派>），主修未定或并列最高时不能购买";
            case "RETIRED" -> name + " 已于 1.4.18 下架，旧购买记录已按原价退还点数，不能再购买";
            case "LOCKED" -> "本世已最终确认，不能再改";
            case "NO_DIFFICULTY" -> "请先选择开局难度";
            case "UNKNOWN_ID" -> name + " 当前不可购买";
            case "STALE_REVISION" -> "选择已更新，请重试";
            default -> "无法购买 " + name + "（" + rejection + "）";
        };
    }
}
