package net.guzhenren.tianfu;

import java.util.Map;
import java.util.WeakHashMap;

import net.guzhenren.network.GuzhenrenModVariables;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Cleans this mod only after native 9.8 actually completes a reincarnation reset. */
public final class TianfuReincarnation {
    private TianfuReincarnation() { }
    private static final ThreadLocal<Map<Entity, TianfuResetState>> BEFORE_RESET =
            ThreadLocal.withInitial(WeakHashMap::new);

    private record TianfuResetState(int oldPhysique, boolean flyGranted, boolean vfxEnabled) { }

    /** Capture only: no generation, score, effects or ledger changes at HEAD. */
    public static void capture(Entity entity) {
        if (entity instanceof ServerPlayer player) {
            var vars = player.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
            BEFORE_RESET.get().put(player, new TianfuResetState((int) Math.round(vars.tizhi),
                    TianfuData.getBoolean(player, "fly_granted", false), TianfuData.vfxEnabled(player)));
        }
    }

    public static void complete(Entity entity) {
        if (!(entity instanceof ServerPlayer player)) return;
        TianfuResetState before = BEFORE_RESET.get().remove(player);
        if (before == null) return;
        try {
            TianfuLiupai.revoke(player, before.oldPhysique());
            TianfuTalents.revokeEffects(player, java.util.Set.of());
        } catch (RuntimeException | LinkageError failure) {
            TianfuMod.LOGGER.error("[天命] 本体转世已完成，旧常驻效果回收异常", failure);
        }
        TianfuData.incrementGeneration(player);
        TianfuData.wipeAll(player);
        TianfuCombat.clearUndyingCooldown(player);
        TianfuEffects.forget(player.getUUID());
        TianfuData.setVfxEnabled(player, before.vfxEnabled());
        try {
            TianfuAttributes.sync(player);
        } catch (RuntimeException | LinkageError failure) {
            TianfuMod.LOGGER.error("[天命] 本体转世后属性修正重建失败", failure);
        }
        if (before.flyGranted() && !player.isCreative() && !player.isSpectator()
                && player.getAbilities().mayfly) {
            player.getAbilities().mayfly = false;
            player.getAbilities().flying = false;
            player.onUpdateAbilities();
        }
        // SxProcedure has already reset kaiqiao to zero. No native fields are restored here.
        player.displayClientMessage(Component.literal(
                "§6[天命] 本体转世已完成：天赋点、已购天赋与开局难度已重置；请重新使用希望蛊开窍"), false);
    }


    public static void release(Entity entity) {
        Map<Entity, TianfuResetState> snapshots = BEFORE_RESET.get();
        snapshots.remove(entity);
        if (snapshots.isEmpty()) BEFORE_RESET.remove();
    }
}


