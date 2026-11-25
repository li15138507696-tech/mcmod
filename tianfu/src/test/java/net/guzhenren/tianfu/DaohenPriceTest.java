package net.guzhenren.tianfu;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/**
 * 道痕天赋价格不变量测试（1.4.18 / P1-3）。
 *
 * <p>换算标准由用户确定：<b>1 点 = 200 道痕，向上取整</b>。
 * 即 {@code cost == ceil(amount / 200)}：
 * <pre>
 *   +200  -> 1
 *   +500  -> 2.5 -> 3
 *   +800  -> 4
 *   +1200 -> 6
 * </pre>
 * 这正好复现任务书给定的梯度（1 / 3 / 4 / 6），也说明旧价（1 / 2 / 3 / 5）是按旧的
 * 品质锚（黄1/玄2/地3/天5）而不是按道痕量定的。
 *
 * <p>主修强化 {@code DAOHEN_MAIN} 走任务书指定价：
 * 1500 -> 8（恰好等于 ceil(7.5)）；3000 -> 16（任务书指定，高于线性值 15，属刻意溢价）。
 */
public final class DaohenPriceTest {
    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("  PASS  " + name);
        } else {
            fail++;
            System.out.println("  FAIL  " + name + "  -> " + detail);
        }
    }

    private static int expected(double amount) {
        return (int) Math.ceil(amount / 200.0);
    }

    public static void main(String[] args) {
        List<String> violations = new ArrayList<>();
        TreeMap<Integer, Integer> tierCount = new TreeMap<>();
        int single = 0;

        for (TianfuTalents.Talent talent : TianfuTalents.all()) {
            for (TianfuTalents.Fx fx : talent.fx()) {
                if (fx.op() != TianfuTalents.Op.DAOHEN) {
                    continue;
                }
                single++;
                int want = expected(fx.a());
                tierCount.merge(want, 1, Integer::sum);
                if (talent.cost() != want) {
                    violations.add(talent.id() + " 道痕+" + (long) fx.a()
                            + " 价=" + talent.cost() + " 应为 " + want);
                }
            }
        }

        System.out.println("=== 1. 单调条道痕天赋：cost == ceil(道痕/200) ===");
        check("扫描到 " + single + " 条道痕天赋", single > 0, String.valueOf(single));
        check("全部满足 1 点=200 道痕（向上取整）", violations.isEmpty(), String.valueOf(violations));

        System.out.println("=== 2. 四档价格分布 ===");
        for (var e : tierCount.entrySet()) {
            System.out.println("        " + e.getKey() + " 点档：" + e.getValue() + " 条");
        }
        check("+200 档 = 1 点", tierCount.getOrDefault(1, 0) > 0, "");
        check("+500 档 = 3 点（不是旧的 2）", tierCount.getOrDefault(3, 0) > 0, "");
        check("+800 档 = 4 点（不是旧的 3）", tierCount.getOrDefault(4, 0) > 0, "");
        check("+1200 档 = 6 点（不是旧的 5）", tierCount.getOrDefault(6, 0) > 0, "");
        check("不存在 2 点档（旧的 +500 错价）", !tierCount.containsKey(2), "");
        check("不存在 5 点档（旧的 +1200 错价）", !tierCount.containsKey(5), "");

        System.out.println("=== 3. 主修强化天赋的指定价 ===");
        var guizong = TianfuTalents.byId("shen_wandao_guizong");
        var zhizun = TianfuTalents.byId("ji_zhongdao_zhizun");
        check("主道归宗 1500 道痕 => 8 点（= ceil(7.5)，与线性一致）",
                guizong != null && guizong.cost() == 8 && expected(1500) == 8,
                String.valueOf(guizong == null ? null : guizong.cost()));
        check("主道至尊 3000 道痕 => 16 点（任务书指定，高于线性 15 的溢价）",
                zhizun != null && zhizun.cost() == 16 && expected(3000) == 15,
                String.valueOf(zhizun == null ? null : zhizun.cost()));

        System.out.println("=== 4. 已删除条目不再存在 ===");
        check("五行道藏已彻底删除", TianfuTalents.byId("wuxing_daocang") == null && TianfuPool.byId("wuxing_daocang") == null, "");
        check("总痕储备已彻底删除", TianfuTalents.byId("zonghen_chubei") == null && TianfuPool.byId("zonghen_chubei") == null, "");

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
