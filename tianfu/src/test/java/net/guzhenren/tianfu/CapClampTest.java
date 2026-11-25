package net.guzhenren.tianfu;

import java.util.Set;

/**
 * 上限钳制测试（1.4.18 / CAP 数值钳制）。
 *
 * <p>覆盖 9 个 CAP 族里可纯 JVM 验证的部分：
 * <ul>
 *   <li>{@link TianfuTalents#limitFor}：乘算累积类的封顶算术（需要本体基值）；</li>
 *   <li>{@link TianfuTalents#attrs}：生命上限连乘封顶 ×4.0；</li>
 *   <li>{@link TianfuTalents#regenZy}：真元恢复合计封顶 +300%。</li>
 * </ul>
 *
 * <p>不覆盖：{@code applyOnce} 里的「快照基值 → 写入后钳回」流程需要 {@code PlayerVariables}
 * 的运行时实例（静态初始化依赖游戏注册表），无法在此环境构造；本测试只验证其算术与常量。
 */
public final class CapClampTest {
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

    private static boolean near(double a, double b) {
        return Math.abs(a - b) < 1e-9;
    }

    public static void main(String[] args) {
        System.out.println("=== 1. 气运上限 ×3.0 ===");
        check("基值 100 => 上限 300", near(TianfuTalents.limitFor("qiyun_shangxian", 100), 300), "");
        check("基值 0 => 不封顶（保留「零上限保持零」的自然行为）",
                TianfuTalents.limitFor("qiyun_shangxian", 0) == Double.MAX_VALUE, "");
        check("连乘 4.34× 会被钳回 3.0×",
                4.34 * 100 > TianfuTalents.limitFor("qiyun_shangxian", 100), "");

        System.out.println("=== 2. 魂魄上限 / 承载 ×3.0 ===");
        check("zuida_hunpo 基值 40 => 上限 120",
                near(TianfuTalents.limitFor("zuida_hunpo", 40), 120), "");
        check("hunpochengzaijixian 基值 40 => 上限 120",
                near(TianfuTalents.limitFor("hunpochengzaijixian", 40), 120), "");

        System.out.println("=== 3. 寿元 +300 ===");
        check("基值 200 => 上限 500", near(TianfuTalents.limitFor("shouyuan", 200), 500), "");
        check("基值 0 => 上限 300（纯加算，基值 0 也要生效）",
                near(TianfuTalents.limitFor("shouyuan", 0), 300), "");
        check("三条合计 +330 会被钳到 +300",
                330 > TianfuTalents.limitFor("shouyuan", 200) - 200, "");

        System.out.println("=== 4. 未受约束的字段不设上限 ===");
        check("daohen_jiandao 无上限",
                TianfuTalents.limitFor("daohen_jiandao", 500) == Double.MAX_VALUE, "");
        check("niantou_rongliang 无上限（念头容量走 TIER 族互斥，不设绝对上限）",
                TianfuTalents.limitFor("niantou_rongliang", 100) == Double.MAX_VALUE, "");

        System.out.println("=== 5. 生命上限总倍数封顶 ×4.0 ===");
        check("封顶常量 = 4.0", near(TianfuTalents.HEALTH_CAP, 4.0), "");
        // attrs() 只返回「扩展词条的裸连乘」；池内条目（不灭金身 ×2.0、筋骨强健 ×1.1）
        // 由 TianfuAttributes 单独合成，封顶也在那里施加。
        var single = TianfuTalents.attrs(Set.of("ji_wanshi_bumie"));
        check("单买 极·万世不灭 => attrs 裸值 ×2.5",
                near(single.healthMult(), 2.5), String.valueOf(single.healthMult()));
        var two = TianfuTalents.attrs(Set.of("xueqi_rulin", "ji_wanshi_bumie"));
        check("血气如林 × 万世不灭 => attrs 裸值 ×3.0（1.2×2.5）",
                near(two.healthMult(), 3.0), String.valueOf(two.healthMult()));
        check("attrs 不做封顶（封顶在 TianfuAttributes 合成后）",
                two.healthMult() > 1.0, "");
        // 复算 TianfuAttributes 的合成顺序：池内 2.0×1.1 = 2.2，再乘裸值 3.0 = 6.6，钳到 4.0
        double assembled = 2.0 * 1.10 * two.healthMult();
        check("池内 ×2.2 · 扩展 ×3.0 => 原始 6.6", near(assembled, 6.6),
                String.valueOf(assembled));
        check("合成后封顶 => ×4.0",
                near(Math.min(TianfuTalents.HEALTH_CAP, assembled), 4.0),
                String.valueOf(Math.min(TianfuTalents.HEALTH_CAP, assembled)));
        check("单条最强项（2.5）不会因封顶被削",
                2.5 <= TianfuTalents.HEALTH_CAP, "");
        check("空集合 => ×1.0", near(TianfuTalents.attrs(Set.of()).healthMult(), 1.0), "");

        System.out.println("=== 6. 真元恢复合计封顶 +300% ===");
        check("封顶常量 = 3.0", near(TianfuTalents.REGEN_ZY_CAP, 3.0), "");
        check("空集合 => +0", near(TianfuTalents.regenZy(Set.of()), 0.0), "");
        // 回流 .35 + 如泉 .80 + 不竭 1.50 = 2.65，未达上限
        var three = TianfuTalents.regenZy(Set.of("zhenyuan_huiliu", "zhenyuan_ruquan", "zhenyuan_bujie"));
        check("三条合计 +265% 未被削", near(three, 2.65), String.valueOf(three));

        System.out.println("=== 7. 上限提示覆盖全部 9 个 CAP 族 ===");
        String[][] cases = {
                {"tiepi_rougu", "50%"},
                {"shanbu_suifeng", "60%"},
                {"baoji_rulie", "50%"},
                {"jianbi_qingfeng", "4.5"},
                {"xueqi_rulin", "4.0"},
                {"qiyun_ruhong", "3.0"},
                {"shouyuan_ruhe", "+300"},
                {"hunpo_rugui", "3.0"},
                {"zhenyuan_bujie", "+300%"},
        };
        for (String[] c : cases) {
            String note = net.guzhenren.tianfu.v2.TianfuInterlock.capNote(c[0]);
            check("capNote(" + c[0] + ") 含 " + c[1], note.contains(c[1]),
                    note.replace('\n', '|'));
        }

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
