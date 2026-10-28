// src/main/java/net/guzhenren/tianfu/TianfuTianyiCompat.java
package net.guzhenren.tianfu;

import net.neoforged.fml.ModList;

/**
 * 与「天意」（tianyi）的共存说明。
 *
 * <p>天意启动时会把 {@code tianfu}/{@code daotu}/{@code shenshang} 判为旧附属并打 ERROR，
 * 但那只是 {@code ModList.isLoaded} + {@code LOGGER.error}，没有任何禁用逻辑——
 * 三者可以共存（详见 docs/2026-10-27-天意兼容性审计.md）。
 *
 * <p>唯一的真实共享写点是本体的 {@code shouyuan}：天意的 WorldServer 会写寿元，
 * 而天命「命烛残尽」要求本世寿元不得增加。这里只做一次性说明，不改行为。
 */
public final class TianfuTianyiCompat {
    private static boolean explained;

    private TianfuTianyiCompat() {
    }

    public static boolean tianyiLoaded() {
        return ModList.get().isLoaded("tianyi");
    }

    /** 命烛残尽首次封顶时调用一次，说明为什么世界意志给的寿元不会生效。 */
    public static void explainLifespanCapOnce() {
        if (explained || !tianyiLoaded()) {
            return;
        }
        explained = true;
        TianfuMod.LOGGER.info(
                "[天命] 命烛残尽生效：本世寿元封顶优先于天意世界意志的寿元扶持（设计如此，非故障）");
    }
}