// src/main/java/net/guzhenren/tianfu/v2/TianfuInterlock.java
package net.guzhenren.tianfu.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 天赋互斥族表（1.4.18 新增）。
 *
 * <p>纯函数、无 Minecraft 依赖、无副作用；服务端 {@link TianfuEconomy#buy} 是唯一权威判定点，
 * 客户端只镜像同一套规则做置灰。
 *
 * <p>三类规则：
 * <ul>
 *   <li>{@link Kind#TIER}：同族**按 members 顺序由弱到强**，只允许持有最高级；
 *       买更高级时由调用方退掉旧条目的成交价（沿用 1.2.x 的同族替换语义）。</li>
 *   <li>{@link Kind#EXCLUSIVE}：同族**只能持有任意一条**，无强弱关系。</li>
 *   <li>{@link Kind#CAP}：族内允许叠加，但数值总和需封顶。**购买时不禁选**，
 *       此处只提供族名与成员，供 UI 提示「已达上限」与后续数值钳制使用。</li>
 * </ul>
 *
 * <p>一个 id 可以同时属于多个族（例：{@code shen_wandao_guizong} 既在 {@code daohen_main}
 * 里与 {@code ji_zhongdao_zhizun} 分档，又在 {@code main_vs_single} 里与全部单流派道痕互斥）。
 */
public final class TianfuInterlock {
    private TianfuInterlock() { }

    public enum Kind { TIER, EXCLUSIVE, CAP, CROSS }

    /**
     * @param members 顺序对 {@link Kind#TIER} 有意义：由弱到强。
     * @param opposes 仅 {@link Kind#CROSS} 使用：members 与 opposes 两组**跨组**互斥，
     *                但**组内**可以自由组合（例如「双流派兼修」必须允许）。
     */
    public record Family(String id, Kind kind, String label, List<String> members, List<String> opposes) {
        int indexOf(String talentId) {
            return members.indexOf(talentId);
        }

        boolean contains(String talentId) {
            return members.contains(talentId) || opposes.contains(talentId);
        }
    }

    private static final List<Family> ALL = new ArrayList<>();
    private static final Map<String, List<Family>> BY_ID = new LinkedHashMap<>();

    private static void fam(String id, Kind kind, String label, String... members) {
        Family family = new Family(id, kind, label, List.of(members), List.of());
        ALL.add(family);
        for (String member : members) {
            BY_ID.computeIfAbsent(member, key -> new ArrayList<>()).add(family);
        }
    }

    /** 跨组互斥：members 与 opposes 之间不能同时持有，组内不受限。 */
    private static void cross(String id, String label, List<String> members, List<String> opposes) {
        Family family = new Family(id, Kind.CROSS, label, List.copyOf(members), List.copyOf(opposes));
        ALL.add(family);
        for (String member : members) {
            BY_ID.computeIfAbsent(member, key -> new ArrayList<>()).add(family);
        }
        for (String member : opposes) {
            BY_ID.computeIfAbsent(member, key -> new ArrayList<>()).add(family);
        }
    }

    static {
        // ------------------------------------------------------------ TIER：同类只留最高级
        // 修炼速度：按 XIULIAN 实际数值由弱到强排列（suhui 为 0.05×转数，本体卡 5 转即 ≤0.25）
        fam("xiulian", Kind.TIER, "修炼速度",
                "suhui", "suiyue_chendian",
                "v2_mind_xiulian_1", "kuxiu_buchuo",
                "v2_mind_xiulian_2", "dazuo_wangwo",
                "v2_mind_xiulian_3", "xiulian_rushen", "shen_xiulian_wuji");
        fam("tupo", Kind.TIER, "突破成功率", "houji_bofa", "tupo_tongtian");
        fam("liangu", Kind.TIER, "炼蛊进度",
                "v2_mind_liangu_1", "v2_mind_liangu_2",
                "liangu_rushen", "liangu_bujie", "shen_liangu_tianshu");
        fam("cooldown", Kind.TIER, "蛊虫冷却",
                "v2_gu_cd_ningse", "v2_mind_cooldown_1", "v2_mind_cooldown_2", "gudao_dup");
        fam("capacity", Kind.TIER, "念头容量",
                "v2_mind_capacity_1", "v2_mind_capacity_2", "v2_mind_capacity_3",
                "niantou_kuozhan", "qiqiao_linglong", "shen_hunxi_zhuzai");
        fam("recovery", Kind.TIER, "念头恢复",
                "v2_mind_recovery_1", "v2_mind_recovery_2",
                "niantou_zisheng", "niantou_ruyuan", "niantou_bujie");
        fam("jingli", Kind.TIER, "精力上限", "jingli_shengheng", "jingli_ruhai");

        // 道痕：只有剑/雷/力三路有两个档位，其余 41 路单条无需族
        fam("daohen:jiandao", Kind.TIER, "剑道道痕", "jiandao_qinhe", "jiandao_dacheng");
        fam("daohen:leidao", Kind.TIER, "雷道道痕", "leidao_qinhe", "leidao_dacheng");
        fam("daohen:lidao", Kind.TIER, "力道道痕", "lidao_qinhe", "lidao_dacheng");

        // 主修强化两极：+1500(8 点) → +3000(16 点)
        fam("daohen_main", Kind.TIER, "主修道痕强化",
                "shen_wandao_guizong", "ji_zhongdao_zhizun");

        // ------------------------------------------------------------ EXCLUSIVE：只能持一个
        // 任务书 5.2：两个主修强化天赋与**所有普通主修道痕天赋**互斥。
        //
        // 这里必须用「跨组」而不是「大互斥团」：两组之间不能同时持有，但**组内必须能自由组合**
        // ——否则连「剑道亲和 + 雷道亲和」的双流派兼修都会被误杀（任务书的测试矩阵明确要求
        // 双流派兼修可用）。单流派之间的互斥只发生在**同一个流派**，那由上面的
        // daohen:jiandao / leidao / lidao 三个 TIER 族负责。
        cross("main_vs_single", "主修强化与单流派道痕",
                // 组 A：主修强化
                List.of("shen_wandao_guizong", "ji_zhongdao_zhizun"),
                // 组 B：15 项主修流的 18 条道痕天赋（剑/雷/力各两条，其余 12 流各一条）
                List.of("jiandao_qinhe", "jiandao_dacheng",
                        "jindao_qinhe", "shuidao_qinhe", "xuedao_qinhe",
                        "lidao_qinhe", "lidao_dacheng",
                        "mudao_qinhe", "yudao_qinhe", "fengdao_qinhe", "bingdao_qinhe",
                        "jindao2_jinjie", "hundao_qinhe",
                        "leidao_qinhe", "leidao_dacheng",
                        "yandao_qinhe", "lvdao_qinhe", "tudao_qinhe"));

        // 负面：同一属性只允许选一条（任务书 七.4）
        fam("neg_health", Kind.EXCLUSIVE, "生命上限负面", "v2_routi_canque", "routi_chanruo");
        fam("neg_zhenyuan", Kind.EXCLUSIVE, "真元恢复负面", "v2_zhenyuan_kubai", "zhenyuan_kujie");
        fam("neg_xiulian", Kind.EXCLUSIVE, "修炼突破负面", "v2_xinzhi_yousun", "xiuxing_chizhi");
        fam("neg_shouyuan", Kind.EXCLUSIVE, "寿元负面", "v2_shouyuan_kuwei", "shouyuan_duancu");
        // 天弃之命把气运每 tick 归零，另两条的边际代价会变成 0 → 必须同族
        fam("neg_qiyun", Kind.EXCLUSIVE, "气运负面",
                "v2_luck_forfeit", "v2_qiyun_duanjue", "qiyun_xibo");
        fam("neg_niantou", Kind.EXCLUSIVE, "念头负面", "nianfu_xiayai", "niantou_ganhe");

        // ------------------------------------------------------------ CAP：可叠但封顶（购买不禁选）
        fam("cap_reduction", Kind.CAP, "非护甲减伤",
                "tiepi_rougu", "ruwei_dingshen", "jianjia_ruyan", "jianjia_rushen",
                "shen_busi_bumie", "shen_jingang_buhuai", "ji_wanshi_bumie");
        // 神·天机神算 同时给暴击率 +50%、暴伤 +240%、闪避 +24%，所以三族都要在
        fam("cap_dodge", Kind.CAP, "闪避",
                "shanbu_suifeng", "shenfa_ruyan", "shen_tianji_suan");
        fam("cap_crit_rate", Kind.CAP, "暴击率",
                "baoji_rulie", "baoji_rushen", "shen_tianji_suan");
        fam("cap_crit_dmg", Kind.CAP, "暴击伤害",
                "baoji_rushen", "jianbi_qingfeng", "shen_tianji_suan");
        fam("cap_health", Kind.CAP, "生命上限",
                "xueqi_rulin", "shen_bumie_jinshen", "ji_wanshi_bumie", "jingu_qiangjian");
        fam("cap_qiyun", Kind.CAP, "气运上限",
                "qiyun_weiying", "qiyun_yingmen", "qiyun_ruhong", "qiyun_yizhi",
                "shen_qiyun_zhizi", "fuze_mianchang", "qiyun_weiguang");
        fam("cap_shouyuan", Kind.CAP, "寿元",
                "chuntian_yishou", "shouyuan_ruhe", "shouyuan_mianchang");
        fam("cap_zhenyuan_regen", Kind.CAP, "真元恢复",
                "zhenyuan_huiliu", "zhenyuan_ruquan", "zhenyuan_bujie");
        fam("cap_hunsoul", Kind.CAP, "魂魄上限",
                "hunpo_lianxi", "hunzai_kuanhou", "hunpo_rugui", "shen_hunxi_zhuzai");
    }

    // ------------------------------------------------------------------ 查询

    public static List<Family> familiesOf(String talentId) {
        List<Family> families = BY_ID.get(talentId);
        return families == null ? List.of() : List.copyOf(families);
    }

    /** 该 id 是否被任何族覆盖（用于 UI 决定是否显示互斥提示）。 */
    public static boolean known(String talentId) {
        return BY_ID.containsKey(talentId);
    }

    public static List<Family> all() {
        return List.copyOf(ALL);
    }

    /**
     * 候选条目把哪些已购条目「替换」掉（仅 TIER 升级时非空）。
     *
     * <p>返回的 id 需要由调用方按成交价退款并移出 purchased。
     */
    public static List<String> replacedBy(List<String> purchased, String candidate) {
        List<String> replaced = new ArrayList<>();
        for (Family family : familiesOf(candidate)) {
            if (family.kind() != Kind.TIER) {
                continue;
            }
            int candidateIndex = family.indexOf(candidate);
            if (candidateIndex < 0) {
                continue;
            }
            for (String owned : purchased) {
                int ownedIndex = family.indexOf(owned);
                if (ownedIndex >= 0 && ownedIndex < candidateIndex && !replaced.contains(owned)) {
                    replaced.add(owned);
                }
            }
        }
        return replaced;
    }

    /**
     * 购买冲突判定。
     *
     * @return {@code null} 表示可买；否则返回人类可读的拒绝理由。
     */
    public static String conflict(List<String> purchased, String candidate) {
        List<String> replaced = replacedBy(purchased, candidate);
        for (Family family : familiesOf(candidate)) {
            if (family.kind() == Kind.CAP) {
                continue;
            }
            if (family.kind() == Kind.CROSS) {
                // 跨组互斥：只在「候选属于组 A 而已购有组 B」或反之时冲突，组内自由。
                List<String> otherSide = family.members().contains(candidate)
                        ? family.opposes()
                        : family.members();
                for (String owned : purchased) {
                    if (!owned.equals(candidate) && otherSide.contains(owned)) {
                        return "与已购的「" + owned + "」在「" + family.label() + "」上互斥";
                    }
                }
                continue;
            }
            int candidateIndex = family.indexOf(candidate);
            for (String owned : purchased) {
                if (owned.equals(candidate) || replaced.contains(owned)) {
                    continue;
                }
                int ownedIndex = family.indexOf(owned);
                if (ownedIndex < 0) {
                    continue;
                }
                if (family.kind() == Kind.EXCLUSIVE) {
                    return "与已购的「" + owned + "」同属「" + family.label() + "」，只能选择一个";
                }
                // TIER：只升不降
                if (candidateIndex <= ownedIndex) {
                    return "「" + family.label() + "」已持有更高等级，只能选更高档";
                }
            }
        }
        return null;
    }

    /**
     * 效果侧过滤：同族只保留生效的那一条。
     *
     * <p>用途是兼容旧存档——历史档里可能已同时持有多条同族天赋（当时没有互斥规则）。
     * 规则：
     * <ul>
     *   <li>TIER：保留下标最高的那条；</li>
     *   <li>EXCLUSIVE / CROSS：保留「目录价最高」的一条，同价时保留购买顺序靠前的；</li>
     *   <li>CAP：不过滤（叠加但由数值上限钳制）。</li>
     * </ul>
     * 被过滤掉的条目**仍保留在账上**（可撤销、不可再购），只是不产生效果。
     */
    public static Set<String> effective(Collection<String> ids, CostLookup costs) {
        // 单遍分桶：只遍历「确实属于某个族」的 id，避免此前对 26 个族各扫一遍的
        // 26×N 分配与比较。绝大多数 id 不属于任何族（187 条里只有 101 条被覆盖）。
        Map<Family, List<String>> buckets = null;
        for (String id : ids) {
            List<Family> families = BY_ID.get(id);
            if (families == null) {
                continue;
            }
            for (Family family : families) {
                if (family.kind() == Kind.CAP) {
                    continue;
                }
                if (buckets == null) {
                    buckets = new LinkedHashMap<>();
                }
                buckets.computeIfAbsent(family, key -> new ArrayList<>(2)).add(id);
            }
        }
        // 快路径：没有任何族出现真实冲突 → 原样返回，零拷贝零过滤（正常新档走这里）。
        if (buckets == null || !hasConflict(buckets)) {
            return ids instanceof Set<String> set ? set : new LinkedHashSet<>(ids);
        }
        Set<String> active = new LinkedHashSet<>(ids);
        for (Map.Entry<Family, List<String>> entry : buckets.entrySet()) {
            Family family = entry.getKey();
            List<String> present = entry.getValue();
            if (present.size() <= 1) {
                continue;
            }
            if (family.kind() == Kind.CROSS) {
                List<String> groupA = new ArrayList<>();
                List<String> groupB = new ArrayList<>();
                for (String id : present) {
                    if (family.members().contains(id)) {
                        groupA.add(id);
                    }
                    if (family.opposes().contains(id)) {
                        groupB.add(id);
                    }
                }
                if (groupA.isEmpty() || groupB.isEmpty()) {
                    // 双流派兼修等：只占 CROSS 的同一侧（如全部在 opposes），完全允许共存。
                    continue;
                }
                int maxCostA = Integer.MIN_VALUE;
                for (String id : groupA) {
                    maxCostA = Math.max(maxCostA, costs.costOf(id));
                }
                int maxCostB = Integer.MIN_VALUE;
                for (String id : groupB) {
                    maxCostB = Math.max(maxCostB, costs.costOf(id));
                }
                List<String> drop = maxCostB > maxCostA ? groupA : groupB;
                for (String id : drop) {
                    active.remove(id);
                }
                continue;
            }
            String keep;
            if (family.kind() == Kind.TIER) {
                keep = present.get(0);
                for (String id : present) {
                    if (family.indexOf(id) > family.indexOf(keep)) {
                        keep = id;
                    }
                }
            } else {
                keep = present.get(0);
                int best = costs.costOf(keep);
                for (String id : present) {
                    int cost = costs.costOf(id);
                    if (cost > best) {
                        best = cost;
                        keep = id;
                    }
                }
            }
            for (String id : present) {
                if (!id.equals(keep)) {
                    active.remove(id);
                }
            }
        }
        return active;
    }

    private static boolean hasConflict(Map<Family, List<String>> buckets) {
        for (Map.Entry<Family, List<String>> entry : buckets.entrySet()) {
            Family family = entry.getKey();
            List<String> present = entry.getValue();
            if (present.size() <= 1) {
                continue;
            }
            if (family.kind() == Kind.CROSS) {
                boolean hasMember = false;
                boolean hasOppose = false;
                for (String id : present) {
                    if (family.members().contains(id)) hasMember = true;
                    if (family.opposes().contains(id)) hasOppose = true;
                    if (hasMember && hasOppose) return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    /** 目录价查询回调；由调用方注入，避免本类反向依赖目录表。 */
    public interface CostLookup {
        int costOf(String id);
    }

    // ------------------------------------------------------------------ 上限说明

    /**
     * {@link Kind#CAP} 各族的真实上限文案（**只写代码里真的存在的上限**）。
     *
     * <p>这里是「描述必须与实现一致」的落点：UI 按族动态显示，而不是在 30 多条
     * 天赋描述里各抄一遍（抄漏抄错都会立刻变成假描述）。
     *
     * <p>全部 9 个 CAP 族都有真实上限（1.4.18）。
     *
     * <p>前 4 条是「汇总值直接封顶」；后 5 条是「乘算/加算累积在同一个本体字段」，
     * 封顶必须先知道本体基值，实现放在 {@code TianfuTalents.limitFor} /
     * {@code TianfuTalents.attrs} / {@code TianfuTalents.regenZy}。
     */
    private static final Map<String, String> CAP_NOTES = Map.ofEntries(
            Map.entry("cap_reduction", "非护甲减伤合计上限 50%（超出不生效）"),
            Map.entry("cap_dodge", "闪避合计上限 60%（超出不生效）"),
            Map.entry("cap_crit_rate", "暴击率合计上限 50%（超出不生效）"),
            Map.entry("cap_crit_dmg", "暴击伤害合计上限 ×4.5（超出不生效）"),
            Map.entry("cap_health", "生命上限合计上限 ×4.0（超出不生效）"),
            Map.entry("cap_qiyun", "气运上限合计上限 ×3.0（超出不生效；本体上限为 0 时不生效）"),
            Map.entry("cap_shouyuan", "寿元加成合计上限 +300（超出不生效）"),
            Map.entry("cap_hunsoul", "魂魄上限与承载极限合计上限 ×3.0（超出不生效）"),
            Map.entry("cap_zhenyuan_regen", "真元恢复合计上限 +300%（超出不生效）"));

    /**
     * 该条目的上限提示；没有真实上限时返回空串。
     *
     * <p>一个条目可能属于多个 CAP 族（如 {@code shen_tianji_suan} 同时含暴击率/暴伤/闪避），
     * 用 {@code \n} 连接全部提示。
     */
    public static String capNote(String talentId) {
        List<Family> families = BY_ID.get(talentId);
        if (families == null || families.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Family family : families) {
            if (family.kind() != Kind.CAP) {
                continue;
            }
            String note = CAP_NOTES.get(family.id());
            if (note == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(note);
        }
        return sb.toString();
    }
}
