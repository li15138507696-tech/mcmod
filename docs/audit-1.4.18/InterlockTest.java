import java.util.List;
import java.util.Set;

import net.guzhenren.tianfu.v2.TianfuInterlock;

/**
 * TianfuInterlock 纯 Java 语义测试（不依赖 Minecraft）。
 *
 * <p>编译：javac -d &lt;out&gt; -cp &lt;classes&gt; InterlockTest.java
 * <p>运行：java -cp &lt;out&gt;;&lt;classes&gt; InterlockTest
 */
public final class InterlockTest {
    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("  PASS  " + name);
        } else {
            fail++;
            System.out.println("  FAIL  " + name + "   -> " + detail);
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 1. TIER：同流派只可选最高级（剑道） ===");
        check("持 亲和 时买 大成 => 允许，并替换掉 亲和",
                TianfuInterlock.conflict(List.of("jiandao_qinhe"), "jiandao_dacheng") == null
                        && TianfuInterlock.replacedBy(List.of("jiandao_qinhe"), "jiandao_dacheng")
                                .equals(List.of("jiandao_qinhe")),
                String.valueOf(TianfuInterlock.replacedBy(List.of("jiandao_qinhe"), "jiandao_dacheng")));

        check("持 大成 时买 亲和 => 拒绝（只升不降）",
                TianfuInterlock.conflict(List.of("jiandao_dacheng"), "jiandao_qinhe") != null,
                String.valueOf(TianfuInterlock.conflict(List.of("jiandao_dacheng"), "jiandao_qinhe")));

        check("持 剑道亲和 时买 雷道亲和 => 允许（双流派兼修，跨组规则不误杀）",
                TianfuInterlock.conflict(List.of("jiandao_qinhe"), "leidao_qinhe") == null,
                String.valueOf(TianfuInterlock.conflict(List.of("jiandao_qinhe"), "leidao_qinhe")));
        check("持 剑道亲和+雷道亲和 时买 金道亲和 => 允许（三流派）",
                TianfuInterlock.conflict(List.of("jiandao_qinhe", "leidao_qinhe"), "jindao_qinhe") == null, "");

        System.out.println("=== 2. 修炼族：只留最高级 ===");
        check("持 修炼I 时买 修炼III => 允许并替换",
                TianfuInterlock.conflict(List.of("v2_mind_xiulian_1"), "v2_mind_xiulian_3") == null
                        && TianfuInterlock.replacedBy(List.of("v2_mind_xiulian_1"), "v2_mind_xiulian_3")
                                .contains("v2_mind_xiulian_1"),
                "");
        check("持 神·修炼无极(1.50) 时买 苦修不辍 => 拒绝（只升不降）",
                TianfuInterlock.conflict(List.of("shen_xiulian_wuji"), "kuxiu_buchuo") != null, "");
        check("持 苦修不辍 时买 神·修炼无极 => 允许",
                TianfuInterlock.conflict(List.of("kuxiu_buchuo"), "shen_xiulian_wuji") == null, "");
        check("旧档同时持 修炼I+III => 效果侧只留 III",
                TianfuInterlock.effective(List.of("v2_mind_xiulian_1", "v2_mind_xiulian_3"),
                        id -> 0).equals(Set.of("v2_mind_xiulian_3")),
                String.valueOf(TianfuInterlock.effective(
                        List.of("v2_mind_xiulian_1", "v2_mind_xiulian_3"), id -> 0)));

        System.out.println("=== 3. 主修强化 与 单流派道痕 互斥 ===");
        check("持 剑道亲和 时买 神·万道归宗 => 拒绝",
                TianfuInterlock.conflict(List.of("jiandao_qinhe"), "shen_wandao_guizong") != null, "");
        check("持 神·万道归宗 时买 剑道大成 => 拒绝",
                TianfuInterlock.conflict(List.of("shen_wandao_guizong"), "jiandao_dacheng") != null, "");
        check("持 神·万道归宗(1500) 时买 极·众道至尊(3000) => 允许并替换（同族升级）",
                TianfuInterlock.conflict(List.of("shen_wandao_guizong"), "ji_zhongdao_zhizun") == null
                        && TianfuInterlock.replacedBy(List.of("shen_wandao_guizong"), "ji_zhongdao_zhizun")
                                .contains("shen_wandao_guizong"),
                String.valueOf(TianfuInterlock.replacedBy(List.of("shen_wandao_guizong"), "ji_zhongdao_zhizun")));
        check("持 极·众道至尊 时买 神·万道归宗 => 拒绝（只升不降）",
                TianfuInterlock.conflict(List.of("ji_zhongdao_zhizun"), "shen_wandao_guizong") != null, "");
        check("持 特殊流派（骨道）时买 神·万道归宗 => 允许（不在 15 主修内）",
                TianfuInterlock.conflict(List.of("gudao_qinhe"), "shen_wandao_guizong") == null, "");
        check("旧档 万道归宗+剑道亲和 => 效果侧保留价格更高者（万道归宗）",
                TianfuInterlock.effective(List.of("jiandao_qinhe", "shen_wandao_guizong"),
                        id -> "shen_wandao_guizong".equals(id) ? 12 : 1)
                        .equals(Set.of("shen_wandao_guizong")),
                "");

        System.out.println("=== 4. 负面同属性互斥 ===");
        check("持 天弃之命 时买 气运稀薄 => 拒绝",
                TianfuInterlock.conflict(List.of("v2_luck_forfeit"), "qiyun_xibo") != null, "");
        check("持 气运稀薄 时买 气运断绝 => 拒绝",
                TianfuInterlock.conflict(List.of("qiyun_xibo"), "v2_qiyun_duanjue") != null, "");
        check("持 肉体残缺 时买 肉体孱弱 => 拒绝",
                TianfuInterlock.conflict(List.of("v2_routi_canque"), "routi_chanruo") != null, "");
        check("持 心肺有损 时买 修行迟滞 => 拒绝（修炼负面同族）",
                TianfuInterlock.conflict(List.of("v2_xinzhi_yousun"), "xiuxing_chizhi") != null, "");

        System.out.println("=== 5. CAP 不禁选 ===");
        check("持 铁皮肉骨 时买 坚甲如岩 => 允许（CAP 只封顶不拦购）",
                TianfuInterlock.conflict(List.of("tiepi_rougu"), "jianjia_ruyan") == null, "");
        check("CAP 族不过滤效果（两条都保留）",
                TianfuInterlock.effective(List.of("tiepi_rougu", "jianjia_ruyan"), id -> 0)
                        .equals(Set.of("tiepi_rougu", "jianjia_ruyan")), "");

        System.out.println("=== 6. 流派联动打包 ===");
        check("持 冰雪道体 时买 神·流派兼济 => 拒绝",
                TianfuInterlock.conflict(List.of("bingxue_daoti"), "shen_liupai_jianji") != null, "");
        check("持 魂道融彻 时买 一掷乾坤 => 拒绝（同属 liupai_bundle）",
                TianfuInterlock.conflict(List.of("hundao_ronghe"), "qiyun_yizhi") != null, "应拒绝");

        System.out.println("=== 7. 无族条目不受影响 ===");
        check("买 万法皆通（无族）=> 允许",
                TianfuInterlock.conflict(List.of(), "wanfa_jietong") == null, "");
        check("买 无族条目 后 与任何族不冲突",
                TianfuInterlock.conflict(List.of("wanfa_jietong"), "jiandao_qinhe") == null, "");
        check("known() 对无族条目返回 false",
                !TianfuInterlock.known("wanfa_jietong"), "");
        check("known() 对族内条目返回 true",
                TianfuInterlock.known("jiandao_dacheng"), "");

        System.out.println("=== 8. effective() 热路径开销（TianfuApply.effective 每 tick 会调用）===");
        {
            java.util.List<String> typical = new java.util.ArrayList<>();
            // 典型一次开局：约 20 条已购，其中含族内条目但无同族重复
            for (String id : new String[]{
                    "jiandao_qinhe", "v2_mind_xiulian_2", "shen_bumie_jinshen",
                    "baoji_rushen", "jianjia_rushen", "xixue_rushi", "zhenyuan_bujie",
                    "niantou_bujie", "huixue_rusheng", "jingli_ruhai", "hunpo_rugui",
                    "shouyuan_ruhe", "qiyun_ruhong", "shen_zhanshen_linshi", "lijin_ruquan",
                    "quansheng_ruhong", "canshen_rulang", "wanjun_lili", "shengji_rumu",
                    "jixing_ruying"}) {
                typical.add(id);
            }
            Set<String> frozen = new java.util.LinkedHashSet<>(typical);
            long before = allocBytes();
            long start = System.nanoTime();
            int n = 200_000;
            int sink = 0;
            for (int i = 0; i < n; i++) {
                sink += TianfuInterlock.effective(frozen, id -> 0).size();
            }
            long nanos = System.nanoTime() - start;
            long allocated = allocBytes() - before;
            check("典型 20 条已购的 effective() 不改变集合大小",
                    sink == n * typical.size(), "sink=" + sink);
            System.out.printf("        200k 次调用：%d ms，平均 %.0f ns/次，%.1f B/次%n",
                    nanos / 1_000_000, nanos / (double) n, allocated / (double) n);

            // 旧档重复：应只保留最高级
            Set<String> legacy = new java.util.LinkedHashSet<>(
                    List.of("v2_mind_xiulian_1", "v2_mind_xiulian_3", "jiandao_qinhe"));
            check("旧档重复仍只留最高级（热路径优化未破坏语义）",
                    TianfuInterlock.effective(legacy, id -> 0).equals(Set.of("v2_mind_xiulian_3", "jiandao_qinhe")),
                    String.valueOf(TianfuInterlock.effective(legacy, id -> 0)));
        }

        System.out.println("=== 9. 上限提示只报告真实存在的上限 ===");
        {
            String red = TianfuInterlock.capNote("jianjia_rushen");
            String dodge = TianfuInterlock.capNote("shanbu_suifeng");
            String crit = TianfuInterlock.capNote("baoji_rushen");
            check("减伤条目的上限提示含 50%", red.contains("50%"), red);
            check("闪避条目的上限提示含 60%", dodge.contains("60%"), dodge);
            check("暴击如神同时在「暴击率」与「暴伤」两族，提示两行",
                    crit.contains("50%") && crit.contains("4.5") && crit.contains("\n"),
                    crit.replace('\n', '|'));
            check("神·天机神算三族命中（暴击率/暴伤/闪避）",
                    TianfuInterlock.capNote("shen_tianji_suan").split("\n").length >= 3,
                    TianfuInterlock.capNote("shen_tianji_suan").replace('\n', '|'));
            // 1.4.18 起这 4 族也有真实上限（乘算累积类，按本体基值封顶），提示必须出现
            check("生命上限族有上限提示", TianfuInterlock.capNote("xueqi_rulin").contains("4.0"),
                    TianfuInterlock.capNote("xueqi_rulin"));
            check("气运上限族有上限提示", TianfuInterlock.capNote("qiyun_ruhong").contains("3.0"),
                    TianfuInterlock.capNote("qiyun_ruhong"));
            check("魂魄上限族有上限提示", TianfuInterlock.capNote("hunpo_rugui").contains("3.0"),
                    TianfuInterlock.capNote("hunpo_rugui"));
            check("寿元族有上限提示", TianfuInterlock.capNote("shouyuan_ruhe").contains("+300"),
                    TianfuInterlock.capNote("shouyuan_ruhe"));
            check("真元恢复族有上限提示", TianfuInterlock.capNote("zhenyuan_bujie").contains("+300%"),
                    TianfuInterlock.capNote("zhenyuan_bujie"));
            check("不属于 CAP 族的条目无提示", TianfuInterlock.capNote("jiandao_qinhe").isEmpty(),
                    TianfuInterlock.capNote("jiandao_qinhe"));
        }

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }

    private static long allocBytes() {
        try {
            com.sun.management.ThreadMXBean bean =
                    (com.sun.management.ThreadMXBean) java.lang.management.ManagementFactory.getThreadMXBean();
            return bean.getThreadAllocatedBytes(Thread.currentThread().getId());
        } catch (Throwable t) {
            return 0L;
        }
    }
}
