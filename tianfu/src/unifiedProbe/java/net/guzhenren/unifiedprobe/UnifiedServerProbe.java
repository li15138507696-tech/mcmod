package net.guzhenren.unifiedprobe;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.guzhenren.daotu.DaotuData;
import net.guzhenren.daotu.DaotuMainDao;
import net.guzhenren.daotu.DaotuOpening;
import net.guzhenren.daotu.DaotuRun;
import net.guzhenren.init.GuzhenrenModGameRules;
import net.guzhenren.init.GuzhenrenModMobEffects;
import net.guzhenren.item.YizhuanrendaoxiwangguItem;
import net.guzhenren.network.GuzhenrenModVariables;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.guzhenren.procedures.KE1Procedure;
import net.guzhenren.procedures.UIxiuliananjianProcedure;
import net.guzhenren.procedures.UikongqiaoquedingProcedure;
import net.guzhenren.procedures.UishuxingshijuetiProcedure;
import net.guzhenren.procedures.XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure;
import net.guzhenren.tianfu.TianfuApply;
import net.guzhenren.tianfu.TianfuAttributes;
import net.guzhenren.tianfu.TianfuData;
import net.guzhenren.tianfu.TianfuNames;
import net.guzhenren.tianfu.TianfuOpening;
import net.guzhenren.tianfu.TianfuTalents;
import net.guzhenren.tianfu.compat.Guzhenren98Compatibility;
import net.guzhenren.tianfu.compat.Guzhenren98Progression;
import net.guzhenren.tianfu.v2.TianfuSelection;
import net.guzhenren.tianfu.v2.TianfuV2Store;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 跨版本孤立专用服务端集成探针（仅用于 NeoForge 无头服务端 + 命名 FakePlayer 验证）。
 * 不宣称任何图形界面、客户端光影或真人联机结果。
 */
@Mod("unified_probe")
public final class UnifiedServerProbe {
    private static final Logger LOG = LoggerFactory.getLogger("unified_probe");
    private static final String TAG_UNDYING_READY = "tianfu_malice_undying_ready";

    private final List<Map<String, Object>> checks = new ArrayList<>();
    private final String expectedProfile = System.getProperty("unified.probe.expectedProfile", "").trim();
    private ServerLevel level;
    private int failures;

    public UnifiedServerProbe() {
        NeoForge.EVENT_BUS.addListener(this::started);
    }

    private void started(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        level = server.overworld();
        level.getGameRules().getRule(GuzhenrenModGameRules.XIUWEI).set(true, server);

        run("compat-profile", this::compatProfile);
        run("native-item-opening", this::opening);
        run("physique-name-mixin", this::physiqueNameMixin);
        run("native-clone-event", this::cloneEvent);
        run("reincarnation-both-mods", this::reincarnationBothMods);
        run("native-cultivation-pulse", this::cultivation);
        run("native-item-cooldown", this::cooldown);
        run("native-aptitude-confirmation", this::aptitudeConfirmations);
        run("native-recovery", this::nativeRecovery);
        run("native-breakthrough", this::breakthrough);
        run("confirmation-transaction", this::confirmation);
        run("stage1-malice-undying-priority", this::stage1MaliceUndyingPriority);
        run("stage1-permanent-max-health", this::stage1PermanentMaxHealth);
        run("stage1-maintain-effects-no-escalation", this::stage1MaintainEffectsNoEscalation);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completed", true);
        result.put("expectedProfile", expectedProfile);
        result.put("matchedProfile", Guzhenren98Compatibility.matchedProfile());
        result.put("passed", checks.size() - failures);
        result.put("failed", failures);
        result.put("checks", checks);
        result.put("evidence",
                "Isolated NeoForge 21.1.263 headless dedicated server with named FakePlayers; "
                        + "real bytecode Mixin routing and live procedures executed. "
                        + "No graphical client, Veil shader UI, or manual multiplayer acceptance.");
        try {
            Files.writeString(
                    Path.of("probe-result.json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(result),
                    StandardCharsets.UTF_8);
        } catch (Exception failure) {
            LOG.error("UNIFIED_PROBE_REPORT_WRITE_FAILED", failure);
        }
        LOG.info("UNIFIED_PROBE_COMPLETE passed={} failed={}", checks.size() - failures, failures);
    }

    private void run(String name, Checked test) {
        try {
            test.run();
        } catch (Throwable failure) {
            check(name + ":unexpected-exception", false, failure.toString());
            LOG.error("UNIFIED_PROBE_EXCEPTION " + name, failure);
        }
    }

    private void check(String name, boolean pass, Object actual) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("passed", pass);
        item.put("actual", String.valueOf(actual));
        checks.add(item);
        if (pass) {
            LOG.info("UNIFIED_PROBE_PASS {} actual={}", name, actual);
        } else {
            failures++;
            LOG.error("UNIFIED_PROBE_FAIL {} actual={}", name, actual);
        }
    }

    private void eq(String name, double actual, double expected) {
        check(name, Double.compare(actual, expected) == 0, actual + " expected=" + expected);
    }

    private FakePlayer player(String name) {
        FakePlayer p = FakePlayerFactory.get(
                level,
                new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        resetPlayerState(p);
        return p;
    }

    private TrackingFakePlayer trackingPlayer(String name) {
        TrackingFakePlayer p = new TrackingFakePlayer(
                level,
                new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        resetPlayerState(p);
        return p;
    }

    private FakePlayer freshSameUuidClone(FakePlayer original) {
        FakePlayer clone = new FakePlayer(level, original.getGameProfile());
        clone.getPersistentData().remove(TianfuData.ROOT);
        clone.getPersistentData().remove(TianfuData.GENERATION_KEY);
        clone.getPersistentData().remove(DaotuData.ROOT);
        clone.getPersistentData().remove(TAG_UNDYING_READY);
        clone.removeAllEffects();
        TianfuAttributes.sync(clone);
        clone.setHealth(clone.getMaxHealth());
        clone.setPos(0.5, 80, 0.5);
        clone.setDeltaMovement(0, 0, 0);
        return clone;
    }

    private void resetPlayerState(FakePlayer p) {
        p.getPersistentData().remove(TianfuData.ROOT);
        p.getPersistentData().remove(TianfuData.GENERATION_KEY);
        p.getPersistentData().remove(DaotuData.ROOT);
        p.getPersistentData().remove(TAG_UNDYING_READY);
        p.removeAllEffects();
        TianfuOpening.resetKaiqiao(p);
        TianfuAttributes.sync(p);
        p.setHealth(p.getMaxHealth());
        p.setPos(0.5, 80, 0.5);
        p.setDeltaMovement(0, 0, 0);
    }

    private PlayerVariables vars(FakePlayer p) {
        return p.getData(GuzhenrenModVariables.PLAYER_VARIABLES);
    }

    private Item hope() {
        return BuiltInRegistries.ITEM.stream()
                .filter(item -> item instanceof YizhuanrendaoxiwangguItem)
                .findFirst()
                .orElseThrow();
    }

    /** Seed selection state through the real V2 store. */
    private void selection(FakePlayer p, List<String> ids, boolean confirmed) {
        var before = TianfuV2Store.read(p);
        Map<String, TianfuSelection.Receipt> receipts = new LinkedHashMap<>();
        for (String id : ids) {
            receipts.put(id, new TianfuSelection.Receipt(id, 0, 0, false));
        }
        var next = new TianfuSelection.Snapshot(
                TianfuSelection.SCHEMA,
                before.revision() + 1,
                100,
                0,
                confirmed,
                new TianfuSelection.Origin("", ""),
                null,
                List.copyOf(ids),
                Map.copyOf(receipts),
                0,
                Set.of());
        if (!TianfuV2Store.commit(p, new TianfuSelection.Change(before.revision(), next))) {
            throw new IllegalStateException("fixture commit failed");
        }
        TianfuData.markApplied(p);
        TianfuData.markDeferred(p);
        TianfuData.setVfxEnabled(p, false);
    }

    /**
     * 通过反射调用当前运行时本体的 JingJieBaoCunProcedure.execute，
     * 兼容 9.8/10.x (LevelAccessor, Entity) 与 11.22+ (LevelAccessor, double, double, double, Entity)。
     */
    private int invokeJingJieBaoCun(ServerLevel world, FakePlayer p) throws Exception {
        Class<?> cls = Class.forName("net.guzhenren.procedures.JingJieBaoCunProcedure");
        try {
            Method m5 = cls.getMethod(
                    "execute",
                    LevelAccessor.class,
                    double.class,
                    double.class,
                    double.class,
                    Entity.class);
            m5.invoke(null, world, p.getX(), p.getY(), p.getZ(), p);
            return 5;
        } catch (NoSuchMethodException ignored) {
            Method m2 = cls.getMethod("execute", LevelAccessor.class, Entity.class);
            m2.invoke(null, world, p);
            return 2;
        }
    }

    private void compatProfile() throws Exception {
        String actualProfile = Guzhenren98Compatibility.matchedProfile();
        if (!expectedProfile.isEmpty()) {
            check("compat-matched-profile", expectedProfile.equals(actualProfile),
                    actualProfile + " expected=" + expectedProfile);
            double expectedDestinyK = "9.8".equals(expectedProfile) ? 0.001 : 0.00075;
            eq("compat-destiny-five-coeff", Guzhenren98Progression.destinyFiveHumanQiCoefficient(), expectedDestinyK);
        } else {
            check("compat-matched-profile-nonempty",
                    actualProfile != null && !actualProfile.isEmpty() && !"未知".equals(actualProfile),
                    actualProfile);
        }
        check("native-shiqiao-is-boolean",
                PlayerVariables.class.getField("shiqiao").getType() == boolean.class,
                PlayerVariables.class.getField("shiqiao").getType());
        check("fake-player-has-real-server-cooldowns",
                player("up_schema").getCooldowns() instanceof ServerItemCooldowns,
                player("up_schema").getCooldowns().getClass());
    }

    private void opening() {
        FakePlayer p = player("up_open");
        PlayerVariables v = vars(p);
        v.zhuanshu = 0;
        v.jieduan = 0;
        v.kongqiao = 0;
        ItemStack stack = new ItemStack(hope(), 2);
        p.setItemInHand(InteractionHand.MAIN_HAND, stack);
        stack.getItem().use(level, p, InteractionHand.MAIN_HAND);
        eq("hope-use-consumes-one", p.getMainHandItem().getCount(), 1);
        eq("hope-use-native-zhuanshu", v.zhuanshu, 1);
        eq("hope-use-native-jieduan", v.jieduan, 1);
        eq("hope-use-kaiqiao", TianfuOpening.kaiqiaoScoreOf(p), 1);
        check("hope-use-own-applied", TianfuData.isApplied(p), TianfuData.isApplied(p));
        stack.getItem().use(level, p, InteractionHand.MAIN_HAND);
        eq("repeat-hope-use-no-second-consumption", p.getMainHandItem().getCount(), 1);
    }

    private void physiqueNameMixin() {
        FakePlayer p = player("up_physique_name");
        vars(p).tizhi = 19;
        String actual = UishuxingshijuetiProcedure.execute(p);
        String expected = "体质:" + TianfuNames.bodyName(19);
        check("physique-name-mixin-overrides-19", expected.equals(actual), actual + " expected=" + expected);
    }

    private void cloneEvent() {
        FakePlayer old = player("up_clone_old");
        FakePlayer fresh = freshSameUuidClone(old);
        PlayerVariables v = vars(old);
        v.zhuanshu = 3;
        v.jieduan = 2;
        v.kongqiao = 5;
        v.tizhi = 19;
        v.shiqiao = true;
        v.shouyuan = 80;
        v.zhenyuan = 321;
        v.zuida_zhenyuan = 500;
        v.gushi_xiulian_dangqian = 123;
        v.gushi_xiulian_jindu = 400;
        old.getPersistentData().putInt(TianfuData.GENERATION_KEY, 7);
        TianfuData.addPoints(old, 17);
        TianfuData.markApplied(old);
        CompoundTag own = old.getPersistentData().getCompound(TianfuData.ROOT).copy();
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(fresh, old, true));
        PlayerVariables c = vars(fresh);
        check("clone-native-new-attachment", c != v, c != v);
        eq("clone-native-rank", c.zhuanshu, 3);
        eq("clone-native-progress", c.gushi_xiulian_dangqian, 123);
        check("clone-native-boolean-shiqiao", c.shiqiao, c.shiqiao);
        eq("clone-own-generation", TianfuData.generation(fresh), 7);
        check("clone-own-root", fresh.getPersistentData().getCompound(TianfuData.ROOT).equals(own),
                fresh.getPersistentData().getCompound(TianfuData.ROOT));
        fresh.getPersistentData().getCompound(TianfuData.ROOT).putInt("points", 99);
        eq("clone-own-root-is-independent", TianfuData.points(old), 17);
    }

    private void reincarnationBothMods() throws Exception {
        FakePlayer p = player("up_reincarnation");
        PlayerVariables v = vars(p);
        v.zhuanshu = 2;
        v.jieduan = 3;
        v.shouyuan = 80;
        v.kongqiao = 5;
        v.tizhi = 19;
        v.shiqiao = true;
        v.daohen_jiandao = 300.0;

        TianfuData.addPoints(p, 21);
        p.getPersistentData().putInt(TianfuData.GENERATION_KEY, 4);

        DaotuRun.beginRun(p, 3);
        DaotuRun.setDraws(p, 4);
        DaotuRun.addLedger(p, "daohen_jiandao", 250.0);
        DaotuMainDao.set(p, "jiandao");
        DaotuData.markApplied(p);

        int arity = invokeJingJieBaoCun(level, p);
        if (!expectedProfile.isEmpty()) {
            int expectedArity = "11.22".equals(expectedProfile) ? 5 : 2;
            eq("reincarnation-procedure-arity", arity, expectedArity);
        }
        eq("ordinary-death-keeps-rank", vars(p).zhuanshu, 2);
        eq("ordinary-death-keeps-tianfu-points", TianfuData.points(p), 21);
        eq("ordinary-death-keeps-tianfu-gen", TianfuData.generation(p), 4);
        eq("ordinary-death-keeps-daotu-runId", DaotuRun.runId(p), 3);
        eq("ordinary-death-keeps-daotu-draws", DaotuRun.draws(p), 4);
        eq("ordinary-death-keeps-daohen", vars(p).daohen_jiandao, 300.0);

        v.shouyuan = 0;
        p.addEffect(new MobEffectInstance(GuzhenrenModMobEffects.FUHUO, 100, 0));
        invokeJingJieBaoCun(level, p);
        eq("fuhuo-keeps-rank", vars(p).zhuanshu, 2);
        eq("fuhuo-keeps-tianfu-gen", TianfuData.generation(p), 4);
        eq("fuhuo-keeps-tianfu-points", TianfuData.points(p), 21);
        eq("fuhuo-keeps-daotu-runId", DaotuRun.runId(p), 3);
        eq("fuhuo-keeps-daotu-draws", DaotuRun.draws(p), 4);

        p.removeEffect(GuzhenrenModMobEffects.FUHUO);
        invokeJingJieBaoCun(level, p);
        eq("reincarnation-reset-rank-zero", vars(p).zhuanshu, 0);
        check("reincarnation-reset-shiqiao-false", !vars(p).shiqiao, vars(p).shiqiao);
        eq("reincarnation-reset-kaiqiao-zero", TianfuOpening.kaiqiaoScoreOf(p), 0);
        eq("reincarnation-tianfu-gen-incremented", TianfuData.generation(p), 5);
        eq("reincarnation-tianfu-points-zero", TianfuData.points(p), 0);
        check("reincarnation-tianfu-unconfirmed", !TianfuData.isConfirmed(p), TianfuData.isConfirmed(p));
        eq("reincarnation-daotu-runId-incremented", DaotuRun.runId(p), 4);
        eq("reincarnation-daotu-draws-zero", DaotuRun.draws(p), 0);
        check("reincarnation-daotu-ledger-cleared", DaotuRun.ledger(p).isEmpty(), DaotuRun.ledger(p));
        eq("reincarnation-native-daohen-cleared", vars(p).daohen_jiandao, 0.0);
        check("reincarnation-daotu-mainDao-preserved", "jiandao".equals(DaotuMainDao.of(p)), DaotuMainDao.of(p));

        FakePlayer dtOnly = player("up_daotu_reinc_only");
        vars(dtOnly).daohen_jiandao = 300.0;
        DaotuRun.beginRun(dtOnly, 1);
        DaotuRun.addLedger(dtOnly, "daohen_jiandao", 250.0);
        DaotuMainDao.set(dtOnly, "jiandao");
        DaotuOpening.onReincarnation(dtOnly);
        eq("daotu-standalone-reinc-daohen-reverted", vars(dtOnly).daohen_jiandao, 50.0);
        eq("daotu-standalone-reinc-runId", DaotuRun.runId(dtOnly), 2);

        invokeJingJieBaoCun(level, p);
        eq("repeat-ordinary-death-no-second-gen-increment", TianfuData.generation(p), 5);
    }

    private void pulse(FakePlayer p, int duration, boolean blocked) {
        p.removeAllEffects();
        PlayerVariables v = vars(p);
        v.zhuanshu = 1;
        v.tizhi = 0;
        v.zhongzu = 0;
        v.mingge = 0;
        v.shouyuan = 100;
        v.zishenshijianjiasu = 1;
        v.zhenyuan = 100;
        v.gushi_xiulian_dangqian = 0;
        v.gushi_xiulian_jindu = 400;
        if (duration > 0) {
            p.addEffect(new MobEffectInstance(GuzhenrenModMobEffects.XIULIANJINGJIE, duration, 0, false, false));
        }
        if (blocked) {
            p.addEffect(new MobEffectInstance(GuzhenrenModMobEffects.LENGQUE, 100, 0, false, false));
        }
        XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure.execute(level, p.getX(), p.getY(), p.getZ(), p);
    }

    private void cultivation() {
        FakePlayer baseline = player("up_pulse0");
        FakePlayer fast = player("up_pulse1");
        selection(baseline, List.of(), true);
        selection(fast, List.of("v2_mind_xiulian_1"), true);
        pulse(baseline, 2, false);
        pulse(fast, 2, false);
        eq("cultivation-pulse-baseline-2.5", vars(baseline).gushi_xiulian_dangqian, 2.5);
        eq("cultivation-pulse-20percent-3", vars(fast).gushi_xiulian_dangqian, 3.0);
        eq("cultivation-baseline-cost-ten", vars(baseline).zhenyuan, 90);
        eq("cultivation-fast-cost-ten", vars(fast).zhenyuan, 90);
        pulse(fast, 15, false);
        eq("cultivation-duration-gate-no-progress", vars(fast).gushi_xiulian_dangqian, 0);
        eq("cultivation-duration-gate-no-cost", vars(fast).zhenyuan, 100);
        pulse(fast, 2, true);
        eq("cultivation-lengque-gate-no-progress", vars(fast).gushi_xiulian_dangqian, 0);
        eq("cultivation-lengque-gate-no-cost", vars(fast).zhenyuan, 100);
        pulse(fast, 0, false);
        eq("cultivation-absent-effect-no-progress", vars(fast).gushi_xiulian_dangqian, 0);
    }

    private int cooldownDuration(FakePlayer p, Item item) {
        p.getCooldowns().removeCooldown(item);
        p.getCooldowns().addCooldown(item, 100);
        int ticks = 0;
        while (p.getCooldowns().isOnCooldown(item) && ticks < 200) {
            p.getCooldowns().tick();
            ticks++;
        }
        return ticks;
    }

    private void cooldown() {
        FakePlayer base = player("up_cd0");
        FakePlayer fast = player("up_cd1");
        FakePlayer unconfirmed = player("up_cd2");
        selection(base, List.of(), true);
        selection(fast, List.of("v2_mind_cooldown_1"), true);
        selection(unconfirmed, List.of("v2_mind_cooldown_1"), false);
        eq("item-cooldown-baseline", cooldownDuration(base, hope()), 100);
        eq("item-cooldown-shortened", cooldownDuration(fast, hope()), 87);
        eq("vanilla-item-cooldown-unchanged", cooldownDuration(fast, Items.STICK), 100);
        eq("unconfirmed-item-cooldown-unchanged", cooldownDuration(unconfirmed, hope()), 100);
    }

    private void aptitudeConfirmations() {
        String[] ids = {"v2_apt_jia", "v2_apt_yi", "v2_apt_bing", "v2_apt_ding"};
        int[] grades = {2, 1, 5, 8};
        int[][] caps = {{800, 990}, {600, 790}, {400, 590}, {200, 390}};
        for (int i = 0; i < ids.length; i++) {
            TrackingFakePlayer p = trackingPlayer("up_apt_" + i);
            PlayerVariables v = vars(p);
            v.zhuanshu = 0;
            v.jieduan = 0;
            v.kongqiao = 0;
            v.zhenyuan = 0;
            v.zuida_zhenyuan = 0;
            v.shouyuan = 1000;
            selection(p, List.of(ids[i]), false);
            level.getScoreboard().getOrCreatePlayerScore(p, level.getScoreboard().getObjective("kaiqiao")).set(1);
            TianfuSelection.Rejection result = TianfuApply.confirmFinal(p);
            check("aptitude-" + i + "-confirmed", result == TianfuSelection.Rejection.NONE, result);
            eq("aptitude-" + i + "-final-grade", v.kongqiao, grades[i]);
            check("aptitude-" + i + "-cap-range",
                    v.zuida_zhenyuan >= caps[i][0] && v.zuida_zhenyuan <= caps[i][1],
                    v.zuida_zhenyuan);
            eq("aptitude-" + i + "-current-yuan-initialized", v.zhenyuan, v.zuida_zhenyuan);
            check("aptitude-" + i + "-settled",
                    TianfuData.isConfirmed(p) && TianfuData.isSettled(p),
                    TianfuData.isConfirmed(p));
            eq("aptitude-" + i + "-tx-no-close-container", p.closeCount, 0);
            v.kongqiao = 0;
            UikongqiaoquedingProcedure.execute(level, p);
            eq("aptitude-" + i + "-unscoped-close-preserved", p.closeCount, 1);
        }
    }

    private void nativeRecovery() {
        FakePlayer baseline = player("up_rec0");
        FakePlayer boosted = player("up_rec1");
        recoveryFixture(vars(baseline));
        recoveryFixture(vars(boosted));
        selection(baseline, List.of(), true);
        selection(boosted, List.of("zhenyuan_huiliu", "v2_mind_recovery_1"), true);
        KE1Procedure.execute(level, 0.5, 80, 0.5, baseline);
        KE1Procedure.execute(level, 0.5, 80, 0.5, boosted);
        eq("ke1-yuan-baseline", vars(baseline).zhenyuan, 101.5);
        eq("ke1-thought-baseline", vars(baseline).niantou, 0.2);
        eq("ke1-yuan-boosted", vars(boosted).zhenyuan, 102.025);
        eq("ke1-thought-boosted", vars(boosted).niantou, 0.24);
    }

    private void recoveryFixture(PlayerVariables v) {
        v.zhuanshu = 1;
        v.jieduan = 1;
        v.kongqiao = 1;
        v.shiqiao = false;
        v.tizhi = 0;
        v.zhongzu = 0;
        v.shouyuan = 10000;
        v.shouyuan_ke = 0;
        v.shouyuan_miao = 0;
        v.shouyuan_fen = 0;
        v.zishenshijianjiasu = 1;
        v.zhenyuan = 100;
        v.zuida_zhenyuan = 500;
        v.niantou = 0;
        v.niantou_zhida = 100;
        v.niantou_rongliang = 100;
        v.qiyun = 0;
        v.qiyun_shangxian = 10;
    }

    private void breakthrough() {
        FakePlayer gated = player("up_bt_gated");
        PlayerVariables gv = vars(gated);
        gv.shiqiao = true;
        gv.kongqiao = 0;
        gv.gushi_xiulian_dangqian = 10;
        gv.gushi_xiulian_jindu = 20;
        UIxiuliananjianProcedure.execute(level, 0.5, 80, 0.5, gated);
        eq("breakthrough-target-gated-no-state-change", gv.gushi_xiulian_dangqian, 10);

        int trials = 128;
        int baseSuccess = 0;
        int boostedSuccess = 0;
        for (int i = 0; i < trials; i++) {
            FakePlayer base = player("up_bt_a_" + i);
            FakePlayer boosted = player("up_bt_b_" + i);
            breakthroughFixture(vars(base));
            breakthroughFixture(vars(boosted));
            selection(base, List.of(), true);
            selection(boosted, List.of("tupo_tongtian"), true);
            UIxiuliananjianProcedure.execute(level, 0.5, 80, 0.5, base);
            UIxiuliananjianProcedure.execute(level, 0.5, 80, 0.5, boosted);
            if (vars(base).zhuanshu == 3) {
                baseSuccess++;
            }
            if (vars(boosted).zhuanshu == 3) {
                boostedSuccess++;
            }
        }
        check("breakthrough-baseline-near-50pct", baseSuccess >= 40 && baseSuccess <= 88, baseSuccess + "/" + trials);
        check("breakthrough-tupo-tongtian-near-80pct", boostedSuccess >= 84 && boostedSuccess <= 122,
                boostedSuccess + "/" + trials);
        check("breakthrough-bonus-increases-successes", boostedSuccess - baseSuccess >= 18,
                (boostedSuccess - baseSuccess) + " additional successes");
    }

    private void breakthroughFixture(PlayerVariables v) {
        v.shiqiao = false;
        v.kongqiao = 1;
        v.mingge = 5;
        v.zhuanshu = 2;
        v.jieduan = 6;
        v.zhongzu = 0;
        v.gushi_xiulian_dangqian = 400;
        v.gushi_xiulian_jindu = 400;
        v.renqi = 0;
        v.zuida_zhenyuan = 500;
        v.qiyun = 0;
        v.hunpo = 1000;
    }

    private void confirmation() {
        FakePlayer unsafe = player("up_life0");
        PlayerVariables v = vars(unsafe);
        v.shouyuan = 100;
        v.kongqiao = 5;
        v.zuida_zhenyuan = 500;
        selection(unsafe, List.of("v2_apt_bing", "v2_shouyuan_kuwei"), false);
        level.getScoreboard().getOrCreatePlayerScore(unsafe, level.getScoreboard().getObjective("kaiqiao")).set(1);
        CompoundTag beforeVars = v.serializeNBT(unsafe.registryAccess()).copy();
        CompoundTag beforeRoot = unsafe.getPersistentData().getCompound(TianfuData.ROOT).copy();
        var rejected = TianfuApply.confirmFinal(unsafe);
        check("lethal-100-minus250-rejected", rejected == TianfuSelection.Rejection.UNSAFE_LIFESPAN, rejected);
        check("lethal-confirm-native-rollback", v.serializeNBT(unsafe.registryAccess()).equals(beforeVars), v.shouyuan);
        check("lethal-confirm-own-rollback",
                unsafe.getPersistentData().getCompound(TianfuData.ROOT).equals(beforeRoot),
                unsafe.getPersistentData().getCompound(TianfuData.ROOT));
        check("lethal-confirm-unlocked",
                !TianfuData.isConfirmed(unsafe) && !TianfuData.isSettled(unsafe),
                TianfuData.isConfirmed(unsafe));

        FakePlayer capBoundary = player("up_life_cap100");
        PlayerVariables cb = vars(capBoundary);
        cb.shouyuan = 100;
        cb.kongqiao = 5;
        cb.zuida_zhenyuan = 500;
        selection(capBoundary, List.of("v2_apt_bing", "shouyuan_ruhe", "chuntian_yishou", "shouyuan_mianchang", "v2_lifespan_scar"), false);
        level.getScoreboard().getOrCreatePlayerScore(capBoundary, level.getScoreboard().getObjective("kaiqiao")).set(1);
        var capRejected = TianfuApply.confirmFinal(capBoundary);
        check("lifespan-cap300-boundary-100-rejected",
                capRejected == TianfuSelection.Rejection.UNSAFE_LIFESPAN, capRejected);
        eq("lifespan-cap300-boundary-rollback-100", cb.shouyuan, 100);

        FakePlayer safe = player("up_life1");
        PlayerVariables s = vars(safe);
        s.shouyuan = 130;
        s.kongqiao = 5;
        s.zuida_zhenyuan = 500;
        selection(safe, List.of("v2_apt_bing", "shouyuan_ruhe", "chuntian_yishou", "shouyuan_mianchang", "v2_lifespan_scar"), false);
        level.getScoreboard().getOrCreatePlayerScore(safe, level.getScoreboard().getObjective("kaiqiao")).set(1);
        var accepted = TianfuApply.confirmFinal(safe);
        check("nonlethal-combined-lifespan-confirmed", accepted == TianfuSelection.Rejection.NONE, accepted);
        eq("nonlethal-combined-lifespan-30", s.shouyuan, 30);
        check("nonlethal-confirm-locks-and-settles",
                TianfuData.isConfirmed(safe) && TianfuData.isSettled(safe),
                TianfuData.isConfirmed(safe));
        TianfuApply.confirmFinal(safe);
        eq("repeat-confirm-no-double-charge", s.shouyuan, 30);
    }

    private void stage1MaliceUndyingPriority() {
        FakePlayer p = player("up_undying");
        selection(p, List.of("malice_undying"), true);
        PlayerVariables v = vars(p);
        v.zhuanshu = 2;
        v.shouyuan = 0;
        p.getPersistentData().putInt(TianfuData.GENERATION_KEY, 2);
        p.setHealth(0.0F);

        LivingDeathEvent ev1 = new LivingDeathEvent(p, level.damageSources().generic());
        NeoForge.EVENT_BUS.post(ev1);
        check("undying-first-death-canceled", ev1.isCanceled(), ev1.isCanceled());
        check("undying-heals-at-least-10hp", p.getHealth() >= 10.0F, p.getHealth());
        eq("undying-preempts-reincarnation-gen", TianfuData.generation(p), 2);
        eq("undying-preempts-reincarnation-rank", vars(p).zhuanshu, 2);

        p.setHealth(0.0F);
        LivingDeathEvent ev2 = new LivingDeathEvent(p, level.damageSources().generic());
        NeoForge.EVENT_BUS.post(ev2);
        check("undying-second-death-respects-cooldown", !ev2.isCanceled(), ev2.isCanceled());
    }

    private void stage1PermanentMaxHealth() {
        FakePlayer orig = player("up_hp_orig");
        vars(orig).shouyuan = 80.0;
        selection(orig, List.of("shen_bumie_jinshen"), true);
        TianfuAttributes.sync(orig);
        eq("perm-hp-max-40", orig.getMaxHealth(), 40.0);

        ListTag saved = orig.getAttributes().save();
        FakePlayer loaded = player("up_hp_loaded");
        loaded.getAttributes().load(saved);
        eq("perm-hp-persisted-after-nbt-load", loaded.getMaxHealth(), 40.0);

        FakePlayer respawned = freshSameUuidClone(orig);
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(respawned, orig, true));
        NeoForge.EVENT_BUS.post(new PlayerEvent.PlayerRespawnEvent(respawned, false));
        eq("perm-hp-respawn-max-40", respawned.getMaxHealth(), 40.0);
        eq("perm-hp-respawn-health-40", respawned.getHealth(), 40.0);
    }

    private void stage1MaintainEffectsNoEscalation() {
        FakePlayer p = player("up_eff");
        p.removeAllEffects();
        p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 3));
        TianfuTalents.maintainEffects(p, Set.of("wanjun_lili"));
        MobEffectInstance cur = p.getEffect(MobEffects.DAMAGE_BOOST);
        check("maintain-effects-keeps-higher-amp-3", cur != null && cur.getAmplifier() == 3,
                cur == null ? "null" : cur.getAmplifier());
        check("maintain-effects-keeps-finite-duration-40", cur != null && cur.getDuration() == 40,
                cur == null ? "null" : cur.getDuration());

        p.removeEffect(MobEffects.DAMAGE_BOOST);
        TianfuTalents.maintainEffects(p, Set.of("wanjun_lili"));
        MobEffectInstance restored = p.getEffect(MobEffects.DAMAGE_BOOST);
        check("maintain-effects-restores-amp-1", restored != null && restored.getAmplifier() == 1,
                restored == null ? "null" : restored.getAmplifier());
        check("maintain-effects-restores-infinite-duration", restored != null && restored.getDuration() == -1,
                restored == null ? "null" : restored.getDuration());
    }

    @FunctionalInterface
    private interface Checked {
        void run() throws Exception;
    }

    private static final class TrackingFakePlayer extends FakePlayer {
        int closeCount;

        TrackingFakePlayer(ServerLevel level, GameProfile profile) {
            super(level, profile);
        }

        @Override
        public void closeContainer() {
            closeCount++;
            super.closeContainer();
        }
    }
}
