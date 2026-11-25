package net.guzhenren.tianfu;

import java.util.List;
import java.util.Set;

/** Pure catalog and conservative import policy shared by the server and tests. */
public final class UniversalRules {
    public record Talent(String id, String name, int cost, String attribute, double amount) { }
    public static final List<Talent> CATALOG = List.of(
        new Talent("jingu_qiangjian", "筋骨强健", 1, "health", 0.10),
        new Talent("shenfa_qingjie", "身法轻捷", 1, "speed", 0.05),
        new Talent("xueqi_rulin", "血气如林", 2, "health", 0.20),
        new Talent("lijin_ruquan", "力进如泉", 2, "attack", 0.08),
        new Talent("quanru_jifeng", "拳如疾风", 2, "attack_speed", 0.10),
        new Talent("songyue_jianli", "耸岳坚立", 2, "knockback", 0.40));
    private UniversalRules() { }
    public static String effectText(Talent t) { return t.attribute().equals("knockback") ? "+0.4" : "+" + Math.round(t.amount() * 100) + "%"; }
    public static String attributeName(Talent t) { return switch (t.attribute()) {
        case "health" -> "生命"; case "speed" -> "移速"; case "attack" -> "攻击";
        case "attack_speed" -> "攻速"; case "knockback" -> "抗击退"; default -> "";
    }; }
    public static Talent find(String id) { return CATALOG.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null); }
    public static List<String> importOwned(boolean confirmed, List<String> ids) {
        return confirmed ? ids.stream().filter(id -> find(id) != null).distinct().toList() : List.of();
    }
    public static int initialPoints(boolean hasLegacy, boolean validDifficulty, boolean hasLegacySelections, int legacyPoints) {
        return hasLegacy && (validDifficulty || hasLegacySelections || legacyPoints > 0) ? Math.max(0, legacyPoints) : 8;
    }
    public static boolean repair1419(boolean legacyPresent, boolean snapshot, boolean initialized,
            boolean hasOwned, int points, boolean validDifficulty, boolean selections, int oldPoints) {
        return legacyPresent && snapshot && initialized && !hasOwned && points == 0
                && !validDifficulty && !selections && oldPoints == 0;
    }
    public static boolean mayImport(boolean markerExists) { return !markerExists; }
    public static double amount(String attr, Set<String> owned) {
        double factor = 1;
        for (Talent talent : CATALOG) if (talent.attribute().equals(attr) && owned.contains(talent.id())) factor *= 1 + talent.amount();
        return factor - 1;
    }
}
