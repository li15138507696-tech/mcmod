package net.guzhenren.tianfu;

import java.util.List;
import java.util.Map;
import java.util.Set;
import net.guzhenren.tianfu.compat.Guzhenren98Progression;
import net.guzhenren.tianfu.v2.TianfuEconomy;
import net.guzhenren.tianfu.v2.TianfuInterlock;
import net.guzhenren.tianfu.v2.TianfuSelection;

/**
 * 阶段 1 修复回归测试（#1 origin:* 拦截、#5 CROSS 双流派兼修、#8 突破概率边界）。
 */
public final class Stage1BugfixTest {
    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("  PASS  " + name);
        } else {
            fail++;
            System.out.println("  FAIL  " + name + " -> " + detail);
        }
    }

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    private static TianfuSelection.Snapshot snapshot(int points) {
        return new TianfuSelection.Snapshot(3, 1L, points, 0, false,
                new TianfuSelection.Origin("", ""), null,
                List.of(), Map.of(), 0, Set.of());
    }

    public static void main(String[] args) {
        System.out.println("=== 1. #1 禁止通过购买接口直接买 origin:* 出身条目 ===");
        var base = snapshot(10);
        for (String originId : List.of("origin:foundling", "origin:orphan", "origin:clan", "origin:scholar")) {
            var res = TianfuEconomy.buy(base, originId);
            check("buy(" + originId + ") 返回 UNKNOWN_ID",
                    !res.ok() && res.reason() == TianfuSelection.Rejection.UNKNOWN_ID,
                    res.ok() ? "ok" : String.valueOf(res.reason()));
            check("buy(" + originId + ") 零写入", res.snapshot() == base, "");
        }

        System.out.println("=== 2. #5 CROSS 组内双流派兼修不被 effective() 剥离 ===");
        TianfuInterlock.CostLookup lookup = id -> {
            var spec = TianfuPool.byId(id);
            if (spec != null) return spec.cost();
            var talent = TianfuTalents.byId(id);
            return talent != null ? talent.cost() : 0;
        };
        Set<String> dualPath = TianfuInterlock.effective(
                List.of("jiandao_qinhe", "leidao_qinhe"), lookup);
        check("剑道亲和 + 雷道亲和 同在 CROSS opposes 侧全部保留",
                dualPath.contains("jiandao_qinhe") && dualPath.contains("leidao_qinhe") && dualPath.size() == 2,
                String.valueOf(dualPath));

        Set<String> conflictBothSides = TianfuInterlock.effective(
                List.of("shen_wandao_guizong", "jiandao_qinhe", "leidao_qinhe"), lookup);
        check("同时存在主修强化(8点)与单流派(1点)时保留主修强化、剥离单流派",
                conflictBothSides.contains("shen_wandao_guizong")
                        && !conflictBothSides.contains("jiandao_qinhe")
                        && !conflictBothSides.contains("leidao_qinhe"),
                String.valueOf(conflictBothSides));

        System.out.println("=== 3. #8 突破概率边界修正 ===");
        double p98Boost = Guzhenren98Progression.breakthroughChance(0.98, 0.20);
        check("原版 0.98 + 正天赋 0.20 保持 0.98（不再倒扣到 0.95）",
                near(p98Boost, 0.98), String.valueOf(p98Boost));

        double pCertainNeg = Guzhenren98Progression.breakthroughChance(1.06, -0.30);
        check("原版 1.06 + 负天赋 -0.30 生效 => 0.70（不再无视负面）",
                near(pCertainNeg, 0.70), String.valueOf(pCertainNeg));

        double pCertainPos = Guzhenren98Progression.breakthroughChance(1.06, 0.50);
        check("原版 1.06 + 正天赋 0.50 保持 1.0",
                near(pCertainPos, 1.0), String.valueOf(pCertainPos));

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
