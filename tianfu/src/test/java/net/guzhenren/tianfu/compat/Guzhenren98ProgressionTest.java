package net.guzhenren.tianfu.compat;

import net.guzhenren.tianfu.compat.Guzhenren98Progression;

/** Scenario checks independent of Minecraft; real procedure/mixin execution needs runtime smoke. */
public final class Guzhenren98ProgressionTest {
    private static int assertions;
    private static void equal(double expected, double actual, String message) {
        assertions++;
        if (Math.abs(expected - actual) > 1e-9 || !Double.isFinite(actual))
            throw new AssertionError(message + ": expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
    private static int paidPulses(double bonus, double nativeGain) {
        double current = 0;
        int paid = 0;
        while (current < 400 && paid < 10000) {
            current = Guzhenren98Progression.cultivation(current, current + nativeGain, 400, bonus);
            paid++;
        }
        equal(400, current, "paid native pulses terminate at exact target");
        return paid;
    }
    public static void main(String[] args) {
        check(paidPulses(0, 2.5) == 160, "unmodified normal native cultivation costs 160 paid pulses");
        check(paidPulses(.2, 2.5) == 134, "+20% applies to all of each accepted 2.5 native pulse");
        check(paidPulses(-.4, 2.5) == 267, "-40% slows real paid pulses and still completes");
        equal(103, Guzhenren98Progression.cultivation(100, 102.5, 400, .2), "ordinary +20% native pulse");
        equal(118, Guzhenren98Progression.cultivation(100, 115, 400, .2), "tizhi2 +20% uses actual 15 gain");
        equal(100, Guzhenren98Progression.cultivation(100, 100, 400, .2), "no accepted native gain gives no bonus");
        equal(100, Guzhenren98Progression.cultivation(100, 102.5, 400, -2), "negative sums cannot reverse progress");
        equal(400, Guzhenren98Progression.cultivation(399, 401.5, 400, -.4), "negative bonus still completes when its scaled gain reaches target");
        equal(400, Guzhenren98Progression.align(400 - 1e-10, 400), "1412 near-target exact completion");
        equal(400, Guzhenren98Progression.align(405, 400), "1412 old overshoot recovery");
        equal(0, Guzhenren98Progression.align(Double.NaN, 400), "1412 NaN recovery");
        equal(0, Guzhenren98Progression.align(Double.POSITIVE_INFINITY, 400), "1412 infinity recovery");
        equal(300, Guzhenren98Progression.align(300, 0), "uninitialized targets are untouched");

        equal(54.5, Guzhenren98Progression.recovery(50, 53, 800, .5), "+50% on accepted 3 yuan gain is 4.5, no second baseline");
        equal(50.75, Guzhenren98Progression.recovery(50, 53, 800, -.75), "-75% on accepted native gain");
        equal(50, Guzhenren98Progression.recovery(50, 50, 800, 1), "blocked natural recovery remains blocked");
        equal(800, Guzhenren98Progression.recovery(900, 800, 800, 1), "native downward clamp remains exact");
        equal(800, Guzhenren98Progression.recovery(799, 802, 800, 1), "positive boosted recovery honors cap");
        equal(50, Guzhenren98Progression.recovery(50, 53, 800, -5), "negative recovery is never consumption");

        // Rank 2 / human qi 100 / yuan cap 800 / luck 10 has native chance 71%.
        double chance = Guzhenren98Progression.nativeBreakthroughChance(2, 100, 800, 10, false);
        equal(.71, chance, "native regular destiny probability fixture");
        // 命格 5 的人气系数**随本体版本变**：9.8 = 1.0E-3，10.4/10.20 = 7.5E-4。
        // 代码已改为从本体 class 字节探测，所以期望值必须按**探测到的系数**算，
        // 不能写死某个版本的数字（否则换个本体就红）。
        double k5 = Guzhenren98Progression.destinyFiveHumanQiCoefficient();
        System.out.println("  [探测] 命格5 人气系数 = " + k5
                + "（9.8 应 0.001，10.x 应 7.5E-4）");
        check(k5 == 0.001 || k5 == 0.00075, "命格5 系数只能是 1.0E-3（9.8）或 7.5E-4（10.x），实测=" + k5);
        double destiny5 = 1.0 - 2 * 0.1 + 100 * k5 - 2 * 2 * 0.1 + 800 * 0.0002 + 10 * 0.01;
        equal(destiny5, Guzhenren98Progression.nativeBreakthroughChance(2, 100, 800, 10, true),
                "destiny5 人气贡献按探测到的系数计算（实测 k=" + k5 + "）");
        // 1.4.18 口径改为「加性 + 95% 封顶」，不再按失败率乘算：
        // 旧口径 .71 with .3 -> 1-(1-.71)(1-.3) = .797；新口径 .71+.3 -> 封顶 .95。
        equal(.81, Guzhenren98Progression.breakthroughChance(chance, .1),
                "additive: 71% + 10 points = 81%");
        equal(.95, Guzhenren98Progression.breakthroughChance(chance, .3),
                "talent-boosted chance is capped at 95%, never 100%");
        equal(.21, Guzhenren98Progression.breakthroughChance(chance, -.5),
                "negative reduction still subtracts additively");
        int successful = 0;
        for (int i = 0; i < 10000; i++) {
            double roll = (i + .5) / 10000;
            if (Guzhenren98Progression.breakthroughRoll(roll, chance, .1) < chance) successful++;
        }
        check(successful == 8100, "native strict comparison yields requested risk distribution (81%)");
        check(Double.doubleToLongBits(Guzhenren98Progression.breakthroughRoll(.4242, chance, 0))
                        == Double.doubleToLongBits(.4242),
                "no talent preserves native random roll bit for bit");
        // P<=0（本体四转常态 -0.74）时天赋完全不生效：不得把「必失败」撬成「有 r 的概率」。
        equal(0, Guzhenren98Progression.breakthroughChance(-.74, .5),
                "talent never opens a native-impossible breakthrough");
        equal(1, Guzhenren98Progression.breakthroughChance(1.06, .5),
                "native already-certain breakthrough stays certain");
        equal(0, Guzhenren98Progression.breakthroughChance(.1, -4), "failure penalties clamp at zero");
        equal(.95, Guzhenren98Progression.breakthroughChance(.1, 2), "talents cannot exceed the 95% cap");

        check(Guzhenren98Progression.itemCooldownTicks(100, .25) == 80, "-.25 extra cooldown tick units yield80 real ticks");
        check(Guzhenren98Progression.itemCooldownTicks(100, -.1) == 112, "+.10 extra remainingtick units yield112 real ticks");
        check(Guzhenren98Progression.itemCooldownTicks(100, .15) == 87, "fractional extra nativeclock progress rounds completion upward");
        check(Guzhenren98Progression.itemCooldownTicks(0, .25) == 0, "no phantom cooldown on native clear");
        check(Guzhenren98Progression.itemCooldownTicks(100, 0) == 100, "no talent preserves native duration");
        System.out.println("PASS: " + assertions + " progression assertions");
    }
}
