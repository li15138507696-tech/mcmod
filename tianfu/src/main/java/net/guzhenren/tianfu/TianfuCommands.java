package net.guzhenren.tianfu;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * {@code /tianfu vfx on|off}：按玩家开关天赋特效。
 * {@code /tianfu open}：不按键直接打开天赋界面（并打印诊断信息）。
 *
 * <p>特效全部由服务端 {@code ServerLevel.sendParticles} 广播，客户端拦不住，
 * 所以开关只能做在服务端、存在玩家自己身上（{@link TianfuData#vfxEnabled}）。
 * 关掉后不再发包，既清净也省性能。
 *
 * <p>为什么不做成客户端配置：粒子是服务端广播给<b>所有</b>附近玩家的，
 * 客户端只能隐藏自己屏幕上已有的粒子，管不了别人；而且服主也可能想统一控制。
 *
 * <p><b>{@code open} 子命令是给「按 K 没反应」这类反馈准备的</b>：
 * 它绕过按键与客户端 tick 监听，直接从服务端发状态包。
 * 如果它能打开界面，就证明问题在客户端按键层（键位冲突 / 监听没挂）；
 * 如果它也打不开，问题在服务端门禁或界面本身。同时把关键状态打进聊天栏，
 * 玩家截个图就能定位。
 */
public final class TianfuCommands {
    private TianfuCommands() {
    }

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("tianfu")
                // 控制台没有玩家，getPlayerOrException 会抛异常刷屏
                .requires(CommandSourceStack::isPlayer)
                .then(Commands.literal("vfx")
                        .then(Commands.literal("on").executes(ctx -> set(ctx, true)))
                        .then(Commands.literal("off").executes(ctx -> set(ctx, false)))
                        // 不带参数 = 切换
                        .executes(ctx -> set(ctx,
                                !TianfuData.vfxEnabled(ctx.getSource().getPlayerOrException()))))
                .then(Commands.literal("特效")
                        .executes(ctx -> set(ctx,
                                !TianfuData.vfxEnabled(ctx.getSource().getPlayerOrException()))))
                // 不按键打开界面 + 诊断
                .then(Commands.literal("open").executes(TianfuCommands::open))
                .then(Commands.literal("打开").executes(TianfuCommands::open)));
    }

    /**
     * 直接打开天赋界面，并把诊断状态打到聊天栏。
     *
     * <p>诊断项：开窍标记 / 空窍 / **体质 tizhi** / 已结算 / 难度 / 点数 / **已购列表**。
     * 「按 K 没反应」的反馈里，这几项能直接分开四类原因：
     * 未开窍、开窍了但没结算、结算了但没选难度、以及界面侧问题。
     *
     * <p><b>1.4.5</b> 补了 {@code tizhi} 与已购列表：只报「空窍 5」不够用——
     * 「买了十绝体却还是凡体」这类反馈必须看到 {@code tizhi}（0=凡人，1–10=十绝体，
     * 17–53=凡体，54=杀神）和 {@code purchased} 里到底有没有 {@code sijue_N}。
     */
    private static int open(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();

        int kaiqiao = TianfuOpening.kaiqiaoScoreOf(player);
        boolean applied = TianfuData.isApplied(player);
        int difficulty = TianfuData.difficulty(player);
        int points = TianfuData.points(player);
        var pv = player.getData(
                net.guzhenren.network.GuzhenrenModVariables.PLAYER_VARIABLES);
        double kongqiao = pv.kongqiao;
        int tizhi = (int) Math.round(pv.tizhi);

        player.displayClientMessage(Component.literal(
                "§6[天赋] §7诊断 §8| §7开窍标记(kaiqiao) §f" + kaiqiao
                        + " §8| §7空窍 §f" + String.format("%.0f", kongqiao)
                        + " §8| §7体质tizhi §f" + tizhi + "§8(" + physiqueName(tizhi) + "§8)"
                        + " §8| §7已结算 §f" + applied
                        + " §8| §7难度 §f" + difficulty
                        + " §8| §7点数 §f" + points), false);

        var snapshot = net.guzhenren.tianfu.v2.TianfuV2Store.read(player);
        player.displayClientMessage(Component.literal(
                "§6[天赋] §7已购 §8(" + snapshot.purchased().size() + ")§f "
                        + (snapshot.purchased().isEmpty() ? "（空）"
                                : String.join("§7, §f", snapshot.purchased()))), false);

        if (kaiqiao == 0) {
            player.displayClientMessage(Component.literal(
                    "§c[天赋] 尚未以希望蛊开窍——开窍标记为 0，界面被门禁拦下。"
                            + "请先右键使用希望蛊。"), false);
            return 0;
        }

        // 门禁已过：直接发状态包，客户端 TianfuClientHandler 会建界面
        TianfuNet.openSelection(player);
        player.displayClientMessage(Component.literal(
                "§b[天赋] 已请求打开天赋界面。若这里能开、按 K 却不行，"
                        + "说明是客户端按键层问题（键位冲突或监听未挂）。"), false);
        return 1;
    }

    /** tizhi 数值 → 人话（0 凡人 / 1–10 十绝体 / 17–53 凡体 / 54 杀神）。 */
    private static String physiqueName(int tizhi) {
        if (tizhi == 0) return "凡人";
        if (tizhi == 54) return "杀神";
        if (tizhi >= 1 && tizhi <= 10) return "十绝体";
        if (tizhi >= 17 && tizhi <= 53) return "凡体";
        return "未知";
    }

    private static int set(CommandContext<CommandSourceStack> ctx, boolean on)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        TianfuData.setVfxEnabled(player, on);
        player.displayClientMessage(Component.literal(on
                ? "§b[天赋] 天赋特效已开启"
                : "§7[天赋] 天赋特效已关闭（体质形态 / 词条光环 / 购买爆发都不再播放）"), false);
        return 1;
    }
}
