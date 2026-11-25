package net.guzhenren.tianfu.compat;

/** Final-balance rules: never manufacture lifespan to evade native death/reset. */
public final class Guzhenren98Settlement {
    private Guzhenren98Settlement() { }

    public static double lifespanAfterHardCost(double afterTalents, boolean lifespanScar) {
        return afterTalents - (lifespanScar ? 400.0 : 0.0);
    }

    public static boolean validLifespan(double lifespan) {
        return Double.isFinite(lifespan) && lifespan > 0.0;
    }

    public static int finalAptitude(int selected) {
        return switch (selected) {
            case 2 -> 2;
            case 4 -> 1;
            case 6 -> 5;
            case 8 -> 8;
            default -> throw new IllegalArgumentException("不支持的本体空窍档位: " + selected);
        };
    }
}
