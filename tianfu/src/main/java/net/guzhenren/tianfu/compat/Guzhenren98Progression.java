package net.guzhenren.tianfu.compat;

/** Numeric adaptation of accepted Guzhenren 9.8 progression writes. */
public final class Guzhenren98Progression {
    private Guzhenren98Progression() {}

    /** Preserve the 1.4.12 finite-value and exact-completion recovery. */
    public static double align(double current, double target) {
        if (!Double.isFinite(target) || target <= 0.0) return current;
        if (!Double.isFinite(current)) return 0.0;
        double tolerance = Math.max(1.0e-8, Math.ulp(target) * 8.0);
        return current >= target - tolerance ? target : current;
    }

    /** Called only where the native procedure actually grants a paid cultivation pulse. */
    public static double cultivation(double before, double nativeAfter, double target, double bonus) {
        double aligned = align(before, target);
        if (!Double.isFinite(before) || aligned != before) return aligned;
        if (!Double.isFinite(nativeAfter)) return align(nativeAfter, target);
        double delta = nativeAfter - before;
        if (delta <= 0.0) return align(nativeAfter, target);
        double rate = Double.isFinite(bonus) ? Math.max(0.0, 1.0 + bonus) : 1.0;
        double result = before + delta * rate;
        if (Double.isFinite(target) && target > 0.0) result = Math.clamp(result, 0.0, target);
        return align(result, target);
    }

    /** Scale a positive native recovery assignment; downward native clamps pass through. */
    public static double recovery(double before, double nativeAfter, double cap, double bonus) {
        if (!Double.isFinite(before) || !Double.isFinite(nativeAfter) || nativeAfter <= before
                || !Double.isFinite(bonus) || bonus == 0.0) return nativeAfter;
        double result = before + (nativeAfter - before) * Math.max(0.0, 1.0 + bonus);
        return Double.isFinite(cap) && cap >= 0.0 ? Math.min(cap, result) : result;
    }

    /** Native rank-breakthrough probability, shared by the four real attempt sites. */
    public static double nativeBreakthroughChance(double rank, double humanQi, double yuanCap,
            double luck, boolean destinyFive) {
        return 1.0 - rank * 0.1 + humanQi * (destinyFive ? 0.001 : 0.0005)
                - rank * rank * 0.1 + yuanCap * 0.0002 + luck * 0.01;
    }

    /** Positive reduction lowers failure risk; negative reduction increases failure risk. */
    public static double breakthroughChance(double nativeChance, double reduction) {
        if (!Double.isFinite(nativeChance) || !Double.isFinite(reduction)) return nativeChance;
        double chance = Math.clamp(nativeChance, 0.0, 1.0);
        return Math.clamp(1.0 - (1.0 - chance) * Math.max(0.0, 1.0 - reduction), 0.0, 1.0);
    }

    /** Keep the original comparison and both native outcome branches intact. */
    public static double breakthroughRoll(double roll, double nativeChance, double reduction) {
        if (reduction == 0.0 || !Double.isFinite(nativeChance) || !Double.isFinite(reduction)) return roll;
        return roll + nativeChance - breakthroughChance(nativeChance, reduction);
    }

    /** Extra cooldown ticks consumed per game tick, preserving existing GU_CD data units. */
    public static int itemCooldownTicks(int nativeTicks, double extraTicksPerTick) {
        if (nativeTicks <= 0 || !Double.isFinite(extraTicksPerTick) || extraTicksPerTick == 0.0) return nativeTicks;
        double rate = 1.0 + extraTicksPerTick;
        if (rate <= 0.0) return nativeTicks; // Current data's only negative value is -0.10.
        double ticks = Math.ceil(nativeTicks / rate);
        return (int)Math.min(Integer.MAX_VALUE, Math.max(1.0, ticks));
    }
}
