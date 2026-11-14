// src/main/java/net/guzhenren/tianfu/v2/TianfuOriginTable.java
package net.guzhenren.tianfu.v2;

import java.util.List;
import java.util.Map;

import net.guzhenren.tianfu.TianfuTalents;

/** 出身纯数据。空字符串表示无出身，不是可购买的第六地域或第五类型。 */
public final class TianfuOriginTable {
    private TianfuOriginTable() { }

    /** @param daohen 精确本体字段到加值；@param lianguBonus 0.10 表示额外 +10%。 */
    public record Region(String id, String name, int cost, Map<String, Double> daohen, double lianguBonus) { }

    /**
     * @param cost 正值扣点、负值给点；其它百分比用小数，确认前不得生效。
     * @param yuanshiBonus 确认时一次性发放的元石比例，0.15 表示 +15%（写 yuanshishuliang）。
     * @param fx 出身自带的天赋原语：与词条共用同一套通道（sum / attributes / applyOnce），
     *           所以这里只写原语，不在适配层为每个出身另写分支。
     * @param desc 界面展示文本；换行用 {@code \n}。
     */
    public record Background(String id, String name, int cost, double luckCapBonus,
                             double lianguBonus, double yuanshiBonus,
                             List<TianfuTalents.Fx> fx, String desc) { }

    /** 中州仅非汇总 44 项；不能统一字段前缀，不顺带添加 liupai 或 daohen_zong。 */
    public static final Map<String, Double> CENTRAL_DAOHEN = Map.ofEntries(
            Map.entry("dahen_liandao", 30.0),
            Map.entry("dahen_nudao", 30.0),
            Map.entry("dahen_qidao", 30.0),
            Map.entry("dahen_rendao", 30.0),
            Map.entry("dahen_tiandao", 30.0),
            Map.entry("dahen_xingdao", 30.0),
            Map.entry("dahen_zhendao", 30.0),
            Map.entry("dahen_zhidao", 30.0),
            Map.entry("dahen_zhoudao", 30.0),
            Map.entry("daohen_andao", 30.0),
            Map.entry("daohen_bianhuadao", 30.0),
            Map.entry("daohen_bingdao", 30.0),
            Map.entry("daohen_bingxuedao", 30.0),
            Map.entry("daohen_dandao", 30.0),
            Map.entry("daohen_daodao", 30.0),
            Map.entry("daohen_dudao", 30.0),
            Map.entry("daohen_fengdao", 30.0),
            Map.entry("daohen_guangdao", 30.0),
            Map.entry("daohen_gudao", 30.0),
            Map.entry("daohen_huadao", 30.0),
            Map.entry("daohen_huandao", 30.0),
            Map.entry("daohen_hundao", 30.0),
            Map.entry("daohen_jiandao", 30.0),
            Map.entry("daohen_jindao", 30.0),
            Map.entry("daohen_jindao2", 30.0),
            Map.entry("daohen_leidao", 30.0),
            Map.entry("daohen_lidao", 30.0),
            Map.entry("daohen_lvdao", 30.0),
            Map.entry("daohen_mengdao", 30.0),
            Map.entry("daohen_mudao", 30.0),
            Map.entry("daohen_shidao", 30.0),
            Map.entry("daohen_shuidao", 30.0),
            Map.entry("daohen_toudao", 30.0),
            Map.entry("daohen_tudao", 30.0),
            Map.entry("daohen_xindao", 30.0),
            Map.entry("daohen_xudao", 30.0),
            Map.entry("daohen_xuedao", 30.0),
            Map.entry("daohen_yandao", 30.0),
            Map.entry("daohen_yindao", 30.0),
            Map.entry("daohen_yingdao", 30.0),
            Map.entry("daohen_yudao", 30.0),
            Map.entry("daohen_yuedao", 30.0),
            Map.entry("daohen_yundao", 30.0),
            Map.entry("daohen_yundao2", 30.0)
    );

    public static final List<Region> REGIONS = List.of(
            new Region("beiyuan", "北原", 0, Map.of("daohen_lidao", 200.0), 0),
            new Region("nanjiang", "南疆", 0, Map.of("daohen_dudao", 200.0), 0.10),
            new Region("ximo", "西漠", 0, Map.of("daohen_jindao", 200.0), 0),
            new Region("donghai", "东海", 0, Map.of("daohen_shuidao", 200.0), 0),
            new Region("zhongzhou", "中州", 0, CENTRAL_DAOHEN, 0));

    /** 出身：负成本给点、正成本扣点；气运上限与元石是专用通道，其余一律走 {@link #fx}。 */
    public static final List<Background> BACKGROUNDS = List.of(
            new Background("foundling", "弃婴", -3, -0.30, 0, 0,
                    List.of(fx(TianfuTalents.Op.CRIT_DAMAGE, 0.30)),
                    "气运上限 −30%\n暴击伤害 +30%\n无人认领，命硬"),
            new Background("orphan", "亡族遗孤", -2, -0.20, 0, 0,
                    List.of(fx(TianfuTalents.Op.DAOHEN_ALL, 150)),
                    "气运上限 −20%\n44 项非汇总道痕各 +150\n血债在后，道在前"),
            new Background("clan", "家族子弟", -1, -0.10, 0, 0.20,
                    List.of(),
                    "气运上限 −10%\n确认时元石 +20%\n家里给本钱，也给你枷锁"),
            new Background("branch", "世家旁支", 0, -0.05, 0.08, 0,
                    List.of(),
                    "气运上限 −5%\n炼蛊进度 +8%\n沾着姓，进不了堂"),
            new Background("wanderer", "散修", 1, 0.15, 0.10, 0,
                    List.of(),
                    "气运上限 +15%\n炼蛊进度 +10%\n无门无派，天地皆路"),
            new Background("merchant", "商贾", 1, 0.10, 0, 0.15,
                    List.of(),
                    "气运上限 +10%\n确认时元石 +15%\n本钱是算盘打出来的"),
            new Background("hunter", "猎户", 1, 0, 0, 0,
                    List.of(fx(TianfuTalents.Op.CRIT_CHANCE, 0.06),
                            fx(TianfuTalents.Op.DODGE, 0.04)),
                    "暴击率 +6%\n闪避 +4%\n山里长大，知道要害在哪"),
            new Background("apprentice", "蛊师学徒", 2, 0, 0.20, 0,
                    List.of(fx(TianfuTalents.Op.GU_CD, 0.15)),
                    "炼蛊进度 +20%\n蛊冷却每 tick −0.15\n师父领进门"),
            new Background("herbalist", "药童", 2, 0, 0, 0,
                    List.of(fx(TianfuTalents.Op.REGEN_HP_5S, 0.02),
                            fx(TianfuTalents.Op.XIULIAN, 0.10)),
                    "每 5 秒回复 2% 生命\n修炼效率 +10%\n药炉边的日子不白过"),
            new Background("tribal", "部族勇士", 2, 0, 0, 0,
                    List.of(fx(TianfuTalents.Op.HEALTH_MULT, 0.10),
                            fx(TianfuTalents.Op.KNOCKBACK_FLAT, 0.20)),
                    "生命上限 +10%\n击退抗性 +0.2\n先挨打，再还手"),
            new Background("scholar", "书痴", 2, 0, 0.05, 0,
                    List.of(fx(TianfuTalents.Op.XIULIAN, 0.15),
                            fx(TianfuTalents.Op.BREAKTHROUGH, 0.10)),
                    "修炼效率 +15%\n突破成功率 +10%\n炼蛊进度 +5%\n纸上也有道")
    );

    private static TianfuTalents.Fx fx(TianfuTalents.Op op, double a) {
        return new TianfuTalents.Fx(op, null, a, 0.0);
    }
}
