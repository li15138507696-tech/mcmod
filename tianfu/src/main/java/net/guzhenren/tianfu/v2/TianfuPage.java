// src/main/java/net/guzhenren/tianfu/v2/TianfuPage.java
package net.guzhenren.tianfu.v2;

/** 八页唯一枚举；序号仅用于显示，不写入旧 Kind 或玩家 NBT。 */
public enum TianfuPage {
    DIFFICULTY("difficulty", "难度"), ORIGIN("origin", "出身"),
    APTITUDE("aptitude", "资质"), LIUPAI("liupai", "流派"),
    PHYSIQUE("physique", "体质"),
    /**
     * 负面条目独立成页（1.4.15）。
     *
     * <p>原先 20 条负面只是体质页里的一个分组，与正面、十绝体混排，玩家补点时得在
     * 一长串正面里翻。现在单独一页，页内再按扣点档位（−2 / −5 / −8）分组。
     * 只改展示归属，不动价格、互斥与结算——那些仍走 {@code TianfuPool} / {@code TianfuEconomy}。
     */
    NEGATIVE("negative", "负面"),
    MIND("mind", "心智"), QIYUN("qiyun", "气运");

    public final String id;
    public final String label;

    TianfuPage(String id, String label) {
        this.id = id;
        this.label = label;
    }
}
