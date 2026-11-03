package net.guzhenren.tianfu.compat;

/** Boundary scenarios from native 9.8's shouyuan<=0 death/reset rule. */
public final class Guzhenren98SettlementTest {
    private static int checked;
    private static void check(boolean condition, String name) {
        checked++;
        if (!condition) throw new AssertionError(name);
    }

    public static void main(String[] args) {
        check(Guzhenren98Settlement.validLifespan(100), "ordinary living character");
        check(Guzhenren98Settlement.validLifespan(1), "one remaining lifespan is legitimate");
        check(!Guzhenren98Settlement.validLifespan(0), "native zero threshold");
        check(!Guzhenren98Settlement.validLifespan(-1), "native negative threshold");
        check(!Guzhenren98Settlement.validLifespan(100 - 250), "lifespan wither alone must be rejected");
        check(!Guzhenren98Settlement.validLifespan(100 - 100), "short lifespan alone reaches zero");
        double allPositive = 100 + 200 + 80 + 50;
        double finalBalance = Guzhenren98Settlement.lifespanAfterHardCost(allPositive, true);
        check(finalBalance == 30, "positive lifespan offsets the single 400 scar");
        check(Guzhenren98Settlement.validLifespan(finalBalance), "validate after all positive bonuses");
        check(Guzhenren98Settlement.lifespanAfterHardCost(400, true) == 0, "exact scar boundary");
        check(!Guzhenren98Settlement.validLifespan(Guzhenren98Settlement.lifespanAfterHardCost(400, true)), "no keepalive clamp");
        check(Guzhenren98Settlement.lifespanAfterHardCost(-150, false) == -150, "never erase a native negative balance");
        check(Guzhenren98Settlement.lifespanAfterHardCost(200, false) == 200, "no scar means no extra deduction");
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            check(!Guzhenren98Settlement.validLifespan(invalid), "nonfinite lifespan cannot commit");
        check(Guzhenren98Settlement.finalAptitude(2) == 2, "native Jia final grade");
        check(Guzhenren98Settlement.finalAptitude(4) == 1, "native Yi KE1 conversion");
        check(Guzhenren98Settlement.finalAptitude(6) == 5, "native Bing KE1 conversion");
        check(Guzhenren98Settlement.finalAptitude(8) == 8, "native Ding keeps grade");
        boolean rejected = false;
        try { Guzhenren98Settlement.finalAptitude(3); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "ordinary aptitude path must not enter delayed random physique branch");
        System.out.println("PASS: " + checked + " native settlement boundary scenarios");
    }
}
