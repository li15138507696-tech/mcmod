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
        equal(.76, Guzhenren98Progression.nativeBreakthroughChance(2, 100, 800, 10, true), "destiny5 doubles human qi contribution only");
        equal(.797, Guzhenren98Progression.breakthroughChance(chance, .3), "-30% failure risk changes 29% failure to20.3%");
        equal(.565, Guzhenren98Progression.breakthroughChance(chance, -.5), "+50% failure risk changes29% to43.5%");
        int successful = 0;
        for (int i = 0; i < 10000; i++) {
            double roll = (i + .5) / 10000;
            if (Guzhenren98Progression.breakthroughRoll(roll, chance, .3) < chance) successful++;
            check(Double.doubleToLongBits(roll) == Double.doubleToLongBits(Guzhenren98Progression.breakthroughRoll(roll, chance, 0)), "no talent preserves native random roll bit for bit");
        }
        check(successful == 7970, "native strict comparison yields requested risk distribution");
        equal(0, Guzhenren98Progression.breakthroughChance(.1, -4), "failure penalties clamp probability, not arbitrary talent limits");
        equal(1, Guzhenren98Progression.breakthroughChance(.1, 2), "reductions beyond100% cannot exceed certainty");

        check(Guzhenren98Progression.itemCooldownTicks(100, .25) == 80, "-.25 extra cooldown tick units yield80 real ticks");
        check(Guzhenren98Progression.itemCooldownTicks(100, -.1) == 112, "+.10 extra remainingtick units yield112 real ticks");
        check(Guzhenren98Progression.itemCooldownTicks(100, .15) == 87, "fractional extra nativeclock progress rounds completion upward");
        check(Guzhenren98Progression.itemCooldownTicks(0, .25) == 0, "no phantom cooldown on native clear");
        check(Guzhenren98Progression.itemCooldownTicks(100, 0) == 100, "no talent preserves native duration");
        System.out.println("PASS: " + assertions + " progression assertions");
    }
}
