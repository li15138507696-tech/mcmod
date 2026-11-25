package net.guzhenren.tianfu;

/**
 * 宿慧上限 + 描述一致性测试（1.4.18 / P1-6）。
 *
 * <p>任务书 四.5 要求「宿慧的转世成长必须有合理上限」。
 * 旧实现 {@code 0.05 * max(1, zhuanshu)} 无 cap：本体把转数卡在 5 时恰好 +25%，
 * 但其它模组/命令抬高转数后会线性无界。1.4.18 起硬封顶 {@code SUHUI_CAP = 0.25}。
 *
 * <p>同时校验若干条「描述与实现不符」已按实际行为改写（P1-8 抽样）。
 */
public final class SuhuiAndDescTest {
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
        System.out.println("=== 1. 宿慧上限 ===");
        check("转数 1 => +5%", near(TianfuNativeProgression.suhuiBonus(1), 0.05),
                String.valueOf(TianfuNativeProgression.suhuiBonus(1)));
        check("转数 2 => +10%", near(TianfuNativeProgression.suhuiBonus(2), 0.10), "");
        check("转数 5（本体上限）=> +25%", near(TianfuNativeProgression.suhuiBonus(5), 0.25), "");
        check("转数 6 => 仍 +25%（封顶生效）",
                near(TianfuNativeProgression.suhuiBonus(6), 0.25),
                String.valueOf(TianfuNativeProgression.suhuiBonus(6)));
        check("转数 1000 => 仍 +25%（不再线性无界）",
                near(TianfuNativeProgression.suhuiBonus(1000), 0.25),
                String.valueOf(TianfuNativeProgression.suhuiBonus(1000)));
        check("转数 0 => 按 1 转算 +5%",
                near(TianfuNativeProgression.suhuiBonus(0), 0.05), "");
        check("转数 负数 => 按 1 转算 +5%",
                near(TianfuNativeProgression.suhuiBonus(-3), 0.05), "");
        check("上限常量 = 0.25", near(TianfuNativeProgression.SUHUI_CAP, 0.25), "");

        System.out.println("=== 2. 描述不再含已删除/写错的声明 ===");
        var suhui = TianfuPool.byId("suhui");
        check("宿慧描述不再提「道痕获取」",
                suhui != null && !suhui.desc().contains("道痕获取"),
                suhui == null ? "null" : suhui.desc());
        check("宿慧描述写明上限 +25%",
                suhui != null && suhui.desc().contains("上限"),
                suhui == null ? "null" : suhui.desc());

        var suiyue = TianfuPool.byId("suiyue_chendian");
        check("岁月沉淀描述不再说「已耗寿元越多」",
                suiyue != null && !suiyue.desc().contains("已耗寿元"),
                suiyue == null ? "null" : suiyue.desc());
        check("岁月沉淀描述写明「固定值」",
                suiyue != null && suiyue.desc().contains("固定"),
                suiyue == null ? "null" : suiyue.desc());

        var jianbi = TianfuPool.byId("jianbi_qingfeng");
        check("剑指偏锋不再要求「配合暴击精通」",
                jianbi != null && !jianbi.desc().contains("需配合"),
                jianbi == null ? "null" : jianbi.desc());

        var shiyuan = TianfuPool.byId("shiyuan_zhixing");
        check("噬元之性改为实际判定的 texiao",
                shiyuan != null && shiyuan.desc().contains("texiao"),
                shiyuan == null ? "null" : shiyuan.desc());

        System.out.println("=== 3. 道痕天赋描述改用「熟练度 +N」 ===");
        int checked = 0;
        boolean anyOld = false;
        boolean anyNew = false;
        for (TianfuTalents.Talent talent : TianfuTalents.all()) {
            for (TianfuTalents.Fx fx : talent.fx()) {
                if (fx.op() != TianfuTalents.Op.DAOHEN) {
                    continue;
                }
                checked++;
                String desc = talent.desc();
                if (desc.contains("熟练度效率")) {
                    anyOld = true;
                }
                if (desc.contains("熟练度 +" + (long) (fx.a() / 10.0))) {
                    anyNew = true;
                }
            }
        }
        check("扫描 " + checked + " 条道痕描述", checked == 47, String.valueOf(checked));
        check("不再残留「熟练度效率」这个假单位", !anyOld, "");
        check("至少已改用「熟练度 +N」，且 N = 道痕/10", anyNew, "");

        System.out.println("=== 4. 冷却单位改为真实百分比 ===");
        var cd1 = TianfuTalents.byId("v2_mind_cooldown_1");
        check("蛊冷却 I 描述不再写「每 tick」",
                cd1 != null && !cd1.desc().contains("每 tick"),
                cd1 == null ? "null" : cd1.desc());
        check("蛊冷却 I 描述写明 −13%",
                cd1 != null && cd1.desc().contains("13%"),
                cd1 == null ? "null" : cd1.desc());
        var ningse = TianfuTalents.byId("v2_gu_cd_ningse");
        check("催蛊凝涩描述改写为 +11.1%",
                ningse != null && ningse.desc().contains("11.1%"),
                ningse == null ? "null" : ningse.desc());

        System.out.println("=== 5. 反伤条目补充真实限制 ===");
        var rushan = TianfuTalents.byId("fanshang_rushan");
        check("反伤如山写明「同类只取最强」",
                rushan != null && rushan.desc().contains("同类只取最强"),
                rushan == null ? "null" : rushan.desc());
        check("反伤如山写明「仅对玩家来源」",
                rushan != null && rushan.desc().contains("仅对玩家来源"),
                rushan == null ? "null" : rushan.desc());

        System.out.println("=== 6. 已删除条目已从目录移除 ===");
        check("五行道藏已从目录移除", TianfuTalents.byId("wuxing_daocang") == null, "");
        check("总痕储备已从目录移除", TianfuTalents.byId("zonghen_chubei") == null, "");

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
