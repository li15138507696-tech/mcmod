package net.guzhenren.tianfu98test;

import com.google.gson.GsonBuilder;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.guzhenren.init.GuzhenrenModGameRules;
import net.guzhenren.init.GuzhenrenModMobEffects;
import net.guzhenren.item.YizhuanrendaoxiwangguItem;
import net.guzhenren.network.GuzhenrenModVariables;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.guzhenren.procedures.JingJieBaoCunProcedure;
import net.guzhenren.procedures.KE1Procedure;
import net.guzhenren.procedures.UIxiuliananjianProcedure;
import net.guzhenren.procedures.UikongqiaoquedingProcedure;
import net.guzhenren.procedures.XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure;
import net.guzhenren.tianfu.TianfuApply;
import net.guzhenren.tianfu.TianfuData;
import net.guzhenren.tianfu.TianfuOpening;
import net.guzhenren.tianfu.v2.TianfuSelection;
import net.guzhenren.tianfu.v2.TianfuV2Store;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Isolated server integration only: no client, GUI, real player, or human input claims. */
@Mod("tianfu98_probe")
public final class G98ServerProbe {
    private static final Logger LOG = LoggerFactory.getLogger("tianfu98_probe");
    private final List<Map<String, Object>> checks = new ArrayList<>();
    private ServerLevel level;
    private int failures;

    public G98ServerProbe() { NeoForge.EVENT_BUS.addListener(this::started); }

    private void started(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        level = server.overworld();
        level.getGameRules().getRule(GuzhenrenModGameRules.XIUWEI).set(true, server);
        run("native-schema", this::schema);
        run("native-item-opening", this::opening);
        run("native-clone-event", this::cloneEvent);
        run("native-death-reset", this::deathBranches);
        run("native-cultivation-pulse", this::cultivation);
        run("native-item-cooldown", this::cooldown);
        run("native-aptitude-confirmation", this::aptitudeConfirmations);
        run("native-recovery", this::nativeRecovery);
        run("native-breakthrough-target-load", this::breakthroughTargetLoad);
        run("native-breakthrough-outcome", this::breakthroughOutcome);
        run("confirmation-transaction", this::confirmation);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("completed", true);
        result.put("passed", checks.size() - failures);
        result.put("failed", failures);
        result.put("checks", checks);
        result.put("evidence", "NeoForge server with named FakePlayers; native target procedures and transformed item/cooldown methods invoked. No GUI, network request, real-player, or visual acceptance.");
        try {
            Files.writeString(Path.of("probe-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(result), StandardCharsets.UTF_8);
        } catch (Exception failure) {
            LOG.error("G98_PROBE_REPORT_WRITE_FAILED", failure);
        }
        LOG.info("G98_PROBE_COMPLETE passed={} failed={}", checks.size() - failures, failures);
    }

    private void run(String name, Checked test) {
        try { test.run(); } catch (Throwable failure) { check(name + ":unexpected-exception", false, failure.toString()); LOG.error("G98_PROBE_EXCEPTION " + name, failure); }
    }
    private void check(String name, boolean pass, Object actual) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("name", name); item.put("passed", pass); item.put("actual", String.valueOf(actual)); checks.add(item);
        if (pass) LOG.info("G98_PROBE_PASS {} actual={}", name, actual);
        else { failures++; LOG.error("G98_PROBE_FAIL {} actual={}", name, actual); }
    }
    private void eq(String name, double actual, double expected) { check(name, Double.compare(actual, expected) == 0, actual + " expected=" + expected); }
    private FakePlayer player(String name) {
        FakePlayer p = FakePlayerFactory.get(level, new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)), name));
        p.getPersistentData().remove(TianfuData.ROOT); p.getPersistentData().remove(TianfuData.GENERATION_KEY);
        p.setPos(0.5, 80, 0.5); p.setDeltaMovement(0,0,0);
        TianfuOpening.resetKaiqiao(p);
        return p;
    }
    private TrackingFakePlayer trackingPlayer(String name) {
        TrackingFakePlayer p=new TrackingFakePlayer(level,new GameProfile(UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8)),name));
        p.getPersistentData().remove(TianfuData.ROOT); p.getPersistentData().remove(TianfuData.GENERATION_KEY);
        p.setPos(0.5,80,0.5); p.setDeltaMovement(0,0,0); TianfuOpening.resetKaiqiao(p);
        return p;
    }
    private PlayerVariables vars(FakePlayer p) { return p.getData(GuzhenrenModVariables.PLAYER_VARIABLES); }
    private Item hope() { return BuiltInRegistries.ITEM.stream().filter(item -> item instanceof YizhuanrendaoxiwangguItem).findFirst().orElseThrow(); }

    /** Seed selection state through the real store, without claiming to test buying/budget. */
    private void selection(FakePlayer p, List<String> ids, boolean confirmed) {
        var before = TianfuV2Store.read(p);
        Map<String,TianfuSelection.Receipt> receipts = new LinkedHashMap<>();
        for (String id : ids) receipts.put(id, new TianfuSelection.Receipt(id,0,0,false));
        var next = new TianfuSelection.Snapshot(TianfuSelection.SCHEMA,before.revision()+1,100,0,confirmed,
                new TianfuSelection.Origin("",""),null,List.copyOf(ids),Map.copyOf(receipts),0,Set.of());
        if (!TianfuV2Store.commit(p,new TianfuSelection.Change(before.revision(),next))) throw new IllegalStateException("fixture commit failed");
        TianfuData.markApplied(p); TianfuData.markDeferred(p); TianfuData.setVfxEnabled(p,false);
    }
    private void schema() throws Exception {
        check("native-shiqiao-is-boolean",PlayerVariables.class.getField("shiqiao").getType()==boolean.class,PlayerVariables.class.getField("shiqiao").getType());
        check("fake-player-has-real-server-cooldowns",player("g98_schema").getCooldowns() instanceof ServerItemCooldowns,player("g98_schema").getCooldowns().getClass());
    }
    private void opening() {
        FakePlayer p=player("g98_open"); PlayerVariables v=vars(p); v.zhuanshu=0;v.jieduan=0;v.kongqiao=0;
        ItemStack stack=new ItemStack(hope(),2); p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        stack.getItem().use(level,p,InteractionHand.MAIN_HAND);
        eq("hope-use-consumes-one",p.getMainHandItem().getCount(),1);
        eq("hope-use-native-zhuanshu",v.zhuanshu,1); eq("hope-use-native-jieduan",v.jieduan,1);
        eq("hope-use-kaiqiao",TianfuOpening.kaiqiaoScoreOf(p),1);
        check("hope-use-own-applied",TianfuData.isApplied(p),TianfuData.isApplied(p));
        stack.getItem().use(level,p,InteractionHand.MAIN_HAND);
        eq("repeat-hope-use-no-second-consumption",p.getMainHandItem().getCount(),1);
    }
    private void cloneEvent() {
        FakePlayer old=player("g98_old"), fresh=player("g98_clone"); PlayerVariables v=vars(old);
        v.zhuanshu=3;v.jieduan=2;v.kongqiao=5;v.tizhi=19;v.shiqiao=true;v.shouyuan=80;
        v.zhenyuan=321;v.zuida_zhenyuan=500;v.gushi_xiulian_dangqian=123;v.gushi_xiulian_jindu=400;
        old.getPersistentData().putInt(TianfuData.GENERATION_KEY,7);
        TianfuData.addPoints(old,17); TianfuData.markApplied(old);
        CompoundTag own=old.getPersistentData().getCompound(TianfuData.ROOT).copy();
        NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(fresh,old,true));
        PlayerVariables c=vars(fresh);
        check("clone-native-new-attachment",c!=v,c!=v); eq("clone-native-rank",c.zhuanshu,3);
        eq("clone-native-progress",c.gushi_xiulian_dangqian,123); check("clone-native-boolean-shiqiao",c.shiqiao,c.shiqiao);
        eq("clone-own-generation",TianfuData.generation(fresh),7);
        check("clone-own-root",fresh.getPersistentData().getCompound(TianfuData.ROOT).equals(own),fresh.getPersistentData().getCompound(TianfuData.ROOT));
        fresh.getPersistentData().getCompound(TianfuData.ROOT).putInt("points",99);
        eq("clone-own-root-is-independent",TianfuData.points(old),17);
    }
    private void deathBranches() {
        FakePlayer ordinary=player("g98_death"); PlayerVariables v=vars(ordinary);
        v.zhuanshu=2;v.jieduan=3;v.shouyuan=80;v.kongqiao=5;v.tizhi=19;v.shiqiao=true;
        TianfuData.addPoints(ordinary,21); ordinary.getPersistentData().putInt(TianfuData.GENERATION_KEY,4);
        JingJieBaoCunProcedure.execute(level,ordinary);
        eq("ordinary-native-death-keeps-rank",vars(ordinary).zhuanshu,2);
        eq("ordinary-native-death-keeps-ledger",TianfuData.points(ordinary),21);
        eq("ordinary-native-death-keeps-generation",TianfuData.generation(ordinary),4);
        v.shouyuan=0; ordinary.addEffect(new MobEffectInstance(GuzhenrenModMobEffects.FUHUO,100,0));
        JingJieBaoCunProcedure.execute(level,ordinary);
        eq("fuhuo-skips-native-reset",vars(ordinary).zhuanshu,2);
        eq("fuhuo-keeps-generation",TianfuData.generation(ordinary),4);
        eq("fuhuo-keeps-ledger",TianfuData.points(ordinary),21);
        ordinary.removeEffect(GuzhenrenModMobEffects.FUHUO);
        JingJieBaoCunProcedure.execute(level,ordinary);
        eq("actual-native-reset-rank-zero",vars(ordinary).zhuanshu,0);
        check("actual-native-reset-shiqiao-false",!vars(ordinary).shiqiao,vars(ordinary).shiqiao);
        eq("actual-native-reset-kaiqiao",TianfuOpening.kaiqiaoScoreOf(ordinary),0);
        eq("actual-native-reset-generation-once",TianfuData.generation(ordinary),5);
        eq("actual-native-reset-points-zero",TianfuData.points(ordinary),0);
        check("actual-native-reset-unconfirmed",!TianfuData.isConfirmed(ordinary),TianfuData.isConfirmed(ordinary));
        JingJieBaoCunProcedure.execute(level,ordinary);
        eq("later-ordinary-call-does-not-increment-generation",TianfuData.generation(ordinary),5);
    }
    private void pulse(FakePlayer p,int duration,boolean blocked) {
        p.removeAllEffects(); PlayerVariables v=vars(p);
        v.zhuanshu=1;v.tizhi=0;v.zhongzu=0;v.mingge=0;v.shouyuan=100;
        v.zishenshijianjiasu=1;v.zhenyuan=100;v.gushi_xiulian_dangqian=0;v.gushi_xiulian_jindu=400;
        if(duration>0)p.addEffect(new MobEffectInstance(GuzhenrenModMobEffects.XIULIANJINGJIE,duration,0,false,false));
        if(blocked)p.addEffect(new MobEffectInstance(GuzhenrenModMobEffects.LENGQUE,100,0,false,false));
        XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure.execute(level,p.getX(),p.getY(),p.getZ(),p);
    }
    private void cultivation() {
        FakePlayer baseline=player("g98_pulse0"), fast=player("g98_pulse1");
        selection(baseline,List.of(),true); selection(fast,List.of("v2_mind_xiulian_1"),true);
        pulse(baseline,2,false); pulse(fast,2,false);
        eq("accepted-native-pulse-baseline-2.5",vars(baseline).gushi_xiulian_dangqian,2.5);
        eq("accepted-native-pulse-20percent-3",vars(fast).gushi_xiulian_dangqian,3);
        eq("native-baseline-cost-ten-yuan",vars(baseline).zhenyuan,90);
        eq("native-fast-cost-still-ten-yuan",vars(fast).zhenyuan,90);
        pulse(fast,15,false);eq("native-duration-gate-no-progress",vars(fast).gushi_xiulian_dangqian,0);eq("native-duration-gate-no-cost",vars(fast).zhenyuan,100);
        pulse(fast,2,true);eq("native-lengque-gate-no-progress",vars(fast).gushi_xiulian_dangqian,0);eq("native-lengque-gate-no-cost",vars(fast).zhenyuan,100);
        pulse(fast,0,false);eq("native-absent-effect-no-progress",vars(fast).gushi_xiulian_dangqian,0);
    }
    private int cooldownDuration(FakePlayer p,Item item) {
        p.getCooldowns().removeCooldown(item);p.getCooldowns().addCooldown(item,100);
        int ticks=0;while(p.getCooldowns().isOnCooldown(item)&&ticks<200){p.getCooldowns().tick();ticks++;}return ticks;
    }
    private void cooldown() {
        FakePlayer base=player("g98_cd0"), fast=player("g98_cd1"), unconfirmed=player("g98_cd2");
        selection(base,List.of(),true);selection(fast,List.of("v2_mind_cooldown_1"),true);selection(unconfirmed,List.of("v2_mind_cooldown_1"),false);
        eq("real-native-item-cooldown-baseline",cooldownDuration(base,hope()),100);
        eq("real-native-item-cooldown-shortened",cooldownDuration(fast,hope()),87);
        eq("vanilla-item-cooldown-unchanged",cooldownDuration(fast,Items.STICK),100);
        eq("unconfirmed-native-item-cooldown-unchanged",cooldownDuration(unconfirmed,hope()),100);
    }
    private void aptitudeConfirmations() {
        String[] ids={"v2_apt_jia","v2_apt_yi","v2_apt_bing","v2_apt_ding"};
        int[] grades={2,1,5,8}; int[][] caps={{800,990},{600,790},{400,590},{200,390}};
        for(int i=0;i<ids.length;i++) {
            TrackingFakePlayer p=trackingPlayer("g98_apt_"+i); PlayerVariables v=vars(p);
            v.zhuanshu=0;v.jieduan=0;v.kongqiao=0;v.zhenyuan=0;v.zuida_zhenyuan=0;v.shouyuan=1000;
            selection(p,List.of(ids[i]),false);
            level.getScoreboard().getOrCreatePlayerScore(p,level.getScoreboard().getObjective("kaiqiao")).set(1);
            TianfuSelection.Rejection result=TianfuApply.confirmFinal(p);
            check("native-aptitude-"+i+"-confirmed",result==TianfuSelection.Rejection.NONE,result);
            eq("native-aptitude-"+i+"-final-grade",v.kongqiao,grades[i]);
            check("native-aptitude-"+i+"-native-cap-range",v.zuida_zhenyuan>=caps[i][0]&&v.zuida_zhenyuan<=caps[i][1],v.zuida_zhenyuan);
            eq("native-aptitude-"+i+"-current-yuan-initialized",v.zhenyuan,v.zuida_zhenyuan);
            check("native-aptitude-"+i+"-settled",TianfuData.isConfirmed(p)&&TianfuData.isSettled(p),TianfuData.isConfirmed(p));
            eq("native-aptitude-"+i+"-transaction-does-not-close-container",p.closeCount,0);
            v.kongqiao=0;
            UikongqiaoquedingProcedure.execute(level,p);
            eq("native-aptitude-"+i+"-unscoped-native-close-preserved",p.closeCount,1);
        }
    }
    private void nativeRecovery() {
        FakePlayer baseline=player("g98_recovery0"), boosted=player("g98_recovery1");
        recoveryFixture(vars(baseline)); recoveryFixture(vars(boosted));
        selection(baseline,List.of(),true);
        selection(boosted,List.of("zhenyuan_huiliu","v2_mind_recovery_1"),true);
        KE1Procedure.execute(level,0.5,80,0.5,baseline);
        KE1Procedure.execute(level,0.5,80,0.5,boosted);
        eq("native-ke1-yuan-baseline",vars(baseline).zhenyuan,101.5);
        eq("native-ke1-thought-baseline",vars(baseline).niantou,0.2);
        eq("native-ke1-yuan-native-delta-boosted",vars(boosted).zhenyuan,102.025);
        eq("native-ke1-thought-native-delta-boosted",vars(boosted).niantou,0.24);
    }
    private void recoveryFixture(PlayerVariables v) {
        v.zhuanshu=1;v.jieduan=1;v.kongqiao=1;v.shiqiao=false;v.tizhi=0;v.zhongzu=0;
        v.shouyuan=10000;v.shouyuan_ke=0;v.shouyuan_miao=0;v.shouyuan_fen=0;
        v.zishenshijianjiasu=1;v.zhenyuan=100;v.zuida_zhenyuan=500;
        v.niantou=0;v.niantou_zhida=100;v.niantou_rongliang=100;v.qiyun=0;v.qiyun_shangxian=10;
    }
    private void breakthroughTargetLoad() {
        FakePlayer p=player("g98_breakthrough_target"); PlayerVariables v=vars(p);
        v.shiqiao=true;v.kongqiao=0;v.gushi_xiulian_dangqian=10;v.gushi_xiulian_jindu=20;
        UIxiuliananjianProcedure.execute(level,0.5,80,0.5,p);
        eq("native-breakthrough-target-gated-no-state-change",v.gushi_xiulian_dangqian,10);
    }
    private void breakthroughOutcome() {
        int trials=128,baseSuccess=0,boostedSuccess=0;
        for(int i=0;i<trials;i++) {
            FakePlayer base=player("gba"+i), boosted=player("gbb"+i);
            breakthroughFixture(vars(base));breakthroughFixture(vars(boosted));
            selection(base,List.of(),true);selection(boosted,List.of("ji_zhongdao_zhizun"),true);
            UIxiuliananjianProcedure.execute(level,0.5,80,0.5,base);
            UIxiuliananjianProcedure.execute(level,0.5,80,0.5,boosted);
            if(vars(base).zhuanshu==3)baseSuccess++;
            if(vars(boosted).zhuanshu==3)boostedSuccess++;
        }
        check("native-breakthrough-baseline-near-50-percent",baseSuccess>=40&&baseSuccess<=88,baseSuccess+"/"+trials);
        check("native-breakthrough-80-percent-risk-reduction-near-90-percent",boostedSuccess>=100&&boostedSuccess<=128,boostedSuccess+"/"+trials);
        check("native-breakthrough-bonus-changes-actual-native-outcome",boostedSuccess-baseSuccess>=25,(boostedSuccess-baseSuccess)+" additional successes");
    }
    private void breakthroughFixture(PlayerVariables v) {
        v.shiqiao=false;v.kongqiao=1;v.mingge=5;v.zhuanshu=2;v.jieduan=6;v.zhongzu=0;
        v.gushi_xiulian_dangqian=400;v.gushi_xiulian_jindu=400;v.renqi=0;
        v.zuida_zhenyuan=500;v.qiyun=0;v.hunpo=1000;
    }
    private void confirmation() {
        FakePlayer unsafe=player("g98_life0"); PlayerVariables v=vars(unsafe);v.shouyuan=100;v.kongqiao=5;v.zuida_zhenyuan=500;
        selection(unsafe,List.of("v2_apt_bing","v2_shouyuan_kuwei"),false);
        level.getScoreboard().getOrCreatePlayerScore(unsafe,level.getScoreboard().getObjective("kaiqiao")).set(1);
        CompoundTag beforeVars=v.serializeNBT(unsafe.registryAccess()).copy();CompoundTag beforeRoot=unsafe.getPersistentData().getCompound(TianfuData.ROOT).copy();
        var rejected=TianfuApply.confirmFinal(unsafe);
        check("lethal-100-minus250-rejected",rejected==TianfuSelection.Rejection.UNSAFE_LIFESPAN,rejected);
        check("lethal-confirm-native-fields-rollback",v.serializeNBT(unsafe.registryAccess()).equals(beforeVars),v.shouyuan);
        check("lethal-confirm-own-ledger-rollback",unsafe.getPersistentData().getCompound(TianfuData.ROOT).equals(beforeRoot),unsafe.getPersistentData().getCompound(TianfuData.ROOT));
        check("lethal-confirm-unlocked",!TianfuData.isConfirmed(unsafe)&&!TianfuData.isSettled(unsafe),TianfuData.isConfirmed(unsafe));
        FakePlayer safe=player("g98_life1");PlayerVariables s=vars(safe);s.shouyuan=100;s.kongqiao=5;s.zuida_zhenyuan=500;
        selection(safe,List.of("v2_apt_bing","shouyuan_ruhe","chuntian_yishou","shouyuan_mianchang","v2_lifespan_scar"),false);
        level.getScoreboard().getOrCreatePlayerScore(safe,level.getScoreboard().getObjective("kaiqiao")).set(1);
        var accepted=TianfuApply.confirmFinal(safe);
        check("nonlethal-combined-lifespan-confirmed",accepted==TianfuSelection.Rejection.NONE,accepted);
        eq("combined100plus200plus80plus50minus400-is30",s.shouyuan,30);
        check("nonlethal-confirm-locks-and-settles",TianfuData.isConfirmed(safe)&&TianfuData.isSettled(safe),TianfuData.isConfirmed(safe));
        TianfuApply.confirmFinal(safe);eq("repeat-confirm-no-double-charge",s.shouyuan,30);
    }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
    private static final class TrackingFakePlayer extends FakePlayer {
        int closeCount;
        TrackingFakePlayer(ServerLevel level,GameProfile profile){super(level,profile);}
        @Override public void closeContainer(){closeCount++;super.closeContainer();}
    }
}
