package net.guzhenren.tianfu;

import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;

/**
 * 主修流派（1.4.18 / P1-1）契约测试。
 *
 * <p>放在 {@code src/test} 下是因为 {@link TianfuTalents} 会加载 Minecraft 类，
 * 必须在带本体/NeoForge classpath 的环境里跑（见
 * {@code docs/audit-1.4.18/_run_jvm_tests.py}）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>两个主修强化天赋的条目契约（id / 名称 / 价格 / 单一 {@code DAOHEN_MAIN} 原语）；</li>
 *   <li>{@link TianfuMainDao#KEYS} 是 15 项且与目录里的天赋对得上；</li>
 *   <li>{@code uniqueHighest} 的<b>并列必须返回空串</b>（1.4.18 修正的剑道默认 bug）；</li>
 *   <li>全 0 返回空串；唯一最高返回该流派；29 项特殊流派不参与识别。</li>
 * </ul>
 */
public final class MainDaoContractTest {
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

    public static void main(String[] args) {
        System.out.println("=== 1. 主修强化天赋条目契约 ===");
        check("神·主道归宗 需要主修", TianfuTalents.requiresMainDao("shen_wandao_guizong"), "");
        check("极·主道至尊 需要主修", TianfuTalents.requiresMainDao("ji_zhongdao_zhizun"), "");
        check("剑道亲和 不需要主修", !TianfuTalents.requiresMainDao("jiandao_qinhe"), "");
        check("五行道藏已删除且不是主修条目",
                TianfuTalents.byId("wuxing_daocang") == null && !TianfuTalents.requiresMainDao("wuxing_daocang"), "");

        var guizong = TianfuTalents.byId("shen_wandao_guizong");
        var zhizun = TianfuTalents.byId("ji_zhongdao_zhizun");
        check("神·主道归宗：价格 8、仅 1 条 DAOHEN_MAIN 1500",
                guizong != null && guizong.cost() == 8 && guizong.fx().size() == 1
                        && guizong.fx().get(0).op() == TianfuTalents.Op.DAOHEN_MAIN
                        && guizong.fx().get(0).a() == 1500.0,
                guizong == null ? "null" : guizong.cost() + " / " + guizong.fx());
        check("极·主道至尊：价格 16、仅 1 条 DAOHEN_MAIN 3000（无修炼/突破）",
                zhizun != null && zhizun.cost() == 16 && zhizun.fx().size() == 1
                        && zhizun.fx().get(0).op() == TianfuTalents.Op.DAOHEN_MAIN
                        && zhizun.fx().get(0).a() == 3000.0,
                zhizun == null ? "null" : zhizun.cost() + " / " + zhizun.fx());
        check("名称已改为「神·主道归宗」/「极·主道至尊」",
                "神·主道归宗".equals(guizong == null ? null : guizong.name())
                        && "极·主道至尊".equals(zhizun == null ? null : zhizun.name()), "");

        System.out.println("=== 2. KEYS 与目录一致性 ===");
        check("主修候选恰好 15 项", TianfuMainDao.KEYS.size() == 15, String.valueOf(TianfuMainDao.KEYS.size()));
        check("15 项互不重复",
                TianfuMainDao.KEYS.stream().distinct().count() == 15, "");
        boolean allHaveFields = true;
        for (String key : TianfuMainDao.KEYS) {
            try {
                PlayerVariables.class.getField("daohen_" + key);
                PlayerVariables.class.getField("liupai_" + key);
            } catch (NoSuchFieldException e) {
                allHaveFields = false;
                System.out.println("        缺字段: " + key + " -> " + e.getMessage());
            }
        }
        check("15 项在 PlayerVariables 里两侧字段都存在（daohen_ + liupai_）", allHaveFields, "");
        boolean specialExcluded = true;
        for (String special : new String[]{"andao", "gudao", "qidao", "zhidao", "zhoudao", "tiandao"}) {
            if (TianfuMainDao.KEYS.contains(special)) {
                specialExcluded = false;
            }
        }
        check("29 项特殊流派不在主修候选内", specialExcluded, "");

        System.out.println("=== 3. uniqueHighest：并列/全 0/唯一（注入访问器，不需要游戏环境）===");
        {
            // 全 0
            check("全 0 => 空串",
                    TianfuMainDao.uniqueHighest(key -> 0.0).isEmpty(), "");
            // 唯一最高
            check("唯一最高（水道 50）=> shuidao",
                    "shuidao".equals(TianfuMainDao.uniqueHighest(
                            key -> "shuidao".equals(key) ? 50 : 0)), "");
            // 并列最高：旧实现会静默判成剑道
            check("并列最高（剑道 50 = 水道 50）=> 空串（不再默认剑道）",
                    TianfuMainDao.uniqueHighest(
                            key -> "jiandao".equals(key) || "shuidao".equals(key) ? 50 : 0).isEmpty(), "");
            check("三者并列 => 空串",
                    TianfuMainDao.uniqueHighest(
                            key -> "jiandao".equals(key) || "shuidao".equals(key) || "tudao".equals(key)
                                    ? 50 : 0).isEmpty(), "");
            // 破并列
            check("破并列（水道 51 最高）=> shuidao",
                    "shuidao".equals(TianfuMainDao.uniqueHighest(
                            key -> "shuidao".equals(key) ? 51
                                    : "jiandao".equals(key) || "tudao".equals(key) ? 50 : 0)), "");
            // 特殊流派：不在 KEYS 里，访问器根本不会被查询
            check("特殊流派即使 999 也不参与识别（KEYS 之外不查询）",
                    TianfuMainDao.uniqueHighest(
                            key -> "gudao".equals(key) || "andao".equals(key) ? 999 : 0).isEmpty(), "");
            // 负数
            check("负熟练度不被当作最高",
                    TianfuMainDao.uniqueHighest(key -> -5).isEmpty(), "");
            // NaN / Infinity
            check("NaN 与 Infinity 被忽略",
                    TianfuMainDao.uniqueHighest(
                            key -> "leidao".equals(key) ? Double.NaN
                                    : "yandao".equals(key) ? Double.POSITIVE_INFINITY : 0).isEmpty(), "");
            // 15 项里最靠后的一项也能被选中（不依赖顺序早退）
            check("最后一项（tudao）可被选中",
                    "tudao".equals(TianfuMainDao.uniqueHighest(
                            key -> "tudao".equals(key) ? 7 : 0)), "");
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
