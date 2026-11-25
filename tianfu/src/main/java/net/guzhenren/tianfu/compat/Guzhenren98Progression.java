package net.guzhenren.tianfu.compat;

/** Numeric adaptation of accepted Guzhenren 9.8 / 10.x progression writes. */
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

    /**
     * Native rank-breakthrough score, shared by the four real attempt sites.
     *
     * <p><b>注意本体这个量不是概率，是一个 [−2, +2] 的线性分</b>（`UIxiuliananjianProcedure`
     * 用 `Math.random() < P` 判定且从不对 P 做 clamp）。常规配置下 1 转 +1.06、2 转 +0.66、
     * 3 转 +0.06、4 转 <b>−0.74</b>、5 转 −1.74 —— 三转起就跌破 0，四转之后本体判定「必失败」。
     *
     * <p><b>1.4.18 修正</b>：`destinyFive` 的人气系数**随本体版本变**——
     * <b>9.8 是 1.0E-3</b>、**10.4/10.20 是 7.5E-4**（两边常量池实测，
     * {@code docs/audit-1.4.18/_verify_against_98.py}）。
     * 所以不写死，改成从本体字节探测，见 {@link #destinyFiveHumanQiCoefficient()}。
     */
    public static double nativeBreakthroughChance(double rank, double humanQi, double yuanCap,
            double luck, boolean destinyFive) {
        return 1.0 - rank * 0.1 + humanQi * (destinyFive ? destinyFiveHumanQiCoefficient() : 0.0005)
                - rank * rank * 0.1 + yuanCap * 0.0002 + luck * 0.01;
    }

    /**
     * 命格 5 的人气系数，从本体 class 字节探测。
     *
     * <p>探测不到时回退 {@code 1.0E-3} —— 1.4.17 及更早一直用这个值，也是长期在 9.8 上
     * 验证过的值。懒初始化，避免类加载过早时读到不可用的 classloader。
     */
    public static double destinyFiveHumanQiCoefficient() {
        Double cached = destinyK;
        if (cached == null) {
            cached = probeDestinyFiveK();
            destinyK = cached;
        }
        return cached;
    }

    private static volatile Double destinyK;

    private static double probeDestinyFiveK() {
        try {
            if (Guzhenren98Compatibility.baseClassContainsDouble(
                    "net/guzhenren/procedures/UIxiuliananjianProcedure", 0.00075)) {
                return 0.00075;
            }
        } catch (Throwable ignored) {
            // 类读取失败：走回退
        }
        return 0.001;
    }

    /**
     * 天赋能把突破概率推到多高。
     *
     * <p>本体自己给的必成（`P ≥ 1`，如 1 转 +1.06）原样保留为 1.0；
     * 但只要概率是靠**天赋**加上去的，最终值就封顶 0.95，落实任务书
     * 「不允许多个天赋叠加后轻易达到 100%」——否则 2 转本体 P=0.66 时
     * 只需 Σr=0.50（厚积薄发 2 点 + 突破通天 3 点 = 5 点）就是 100%。
     */
    public static final double MAX_TALENT_CHANCE = 0.95;

    /**
     * 天赋对突破的实际生效概率（1.4.18 口径 B：加性修正，且不撬开「本体判定为不可能」的门）。
     *
     * <p>旧实现是「失败率乘算」`1-(1-P)(1-r)`，先把 P 钳到 [0,1] 再算 —— 当本体 P ≤ 0
     * （四转起常态）时结果恒等于 `r`，等于把「必失败」直接变成「有 r 的概率成功」，
     * 再叠加多个天赋就能买到 100% 必成。
     *
     * <p>新口径：
     * <ul>
     *   <li>`P ≤ 0` → 返回 `0.0`，<b>天赋完全不生效</b>（本体说不行就是不行）；</li>
     *   <li>`P ≥ 1` → 返回 `1.0`（本体本就必成，天赋无意义）；</li>
     *   <li>其余 → `clamp(P + reduction, 0, MAX_TALENT_CHANCE)`，即加性百分点修正。</li>
     * </ul>
     * 正 reduction 提高成功率，负 reduction 降低成功率（负面天赋仍然有效）。
     */
    public static double breakthroughChance(double nativeChance, double reduction) {
        if (!Double.isFinite(nativeChance) || !Double.isFinite(reduction)) return nativeChance;
        if (nativeChance <= 0.0) return 0.0;
        if (nativeChance >= 1.0) {
            return reduction < 0.0 ? Math.clamp(1.0 + reduction, 0.0, 1.0) : 1.0;
        }
        double upper = Math.max(MAX_TALENT_CHANCE, nativeChance);
        return Math.clamp(nativeChance + reduction, 0.0, upper);
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
