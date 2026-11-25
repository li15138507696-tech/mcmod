package net.guzhenren.tianfu;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.guzhenren.tianfu.v2.TianfuEconomy;
import net.guzhenren.tianfu.v2.TianfuInterlock;
import net.guzhenren.tianfu.v2.TianfuLegacyPrices;
import net.guzhenren.tianfu.v2.TianfuPageTable;
import net.guzhenren.tianfu.v2.TianfuSelection;

/**
 * 已删除条目彻底清除测试（#14：6 条原下架条目 + 3 条原未兑现条目）。
 */
public final class RetiredRefundTest {
    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("  PASS  " + name);
        } else {
            fail++;
            System.out.println("  FAIL  " + name + "  -> " + detail);
        }
    }

    private static TianfuSelection.Snapshot snapshot(int points, List<String> purchased,
            Map<String, TianfuSelection.Receipt> receipts) {
        int grant = 0;
        for (TianfuSelection.Receipt r : receipts.values()) {
            grant += r.grant();
        }
        return new TianfuSelection.Snapshot(3, 1L, points, 0, false,
                new TianfuSelection.Origin("", ""), null,
                purchased, receipts, grant, Set.of());
    }

    public static void main(String[] args) {
        System.out.println("=== 1. 9 条已删除天赋彻底从目录与表中清除 ===");
        List<String> deletedIds = List.of(
                "wuxing_daocang",
                "zonghen_chubei",
                "bingxue_daoti",
                "hundao_ronghe",
                "shen_liupai_jianji",
                "malice_killer_aura",
                "shazhao_tiancheng",
                "zhenyuan_ninglian",
                "niantou_ninglian");
        for (String id : deletedIds) {
            check(id + " 已从 TianfuTalents.byId 移除", TianfuTalents.byId(id) == null, "");
            check(id + " 已从 TianfuTalents.byIdIncludingUnavailable 移除",
                    TianfuTalents.byIdIncludingUnavailable(id) == null, "");
            check(id + " 已从 TianfuPool.byId 移除", TianfuPool.byId(id) == null, "");
            check(id + " 已从 TianfuPageTable.BY_ID 移除", !TianfuPageTable.BY_ID.containsKey(id), "");
            check(id + " 已从 TianfuLegacyPrices.BY_ID 移除", !TianfuLegacyPrices.BY_ID.containsKey(id), "");
            check(id + " 已从 TianfuInterlock 移除", !TianfuInterlock.known(id), "");
        }

        System.out.println("=== 2. Economy.buy：已删除条目返回 UNKNOWN_ID ===");
        var empty = snapshot(40, List.of(), Map.of());
        boolean allUnknown = true;
        boolean allZeroWrite = true;
        for (String id : deletedIds) {
            var r = TianfuEconomy.buy(empty, id);
            if (r.ok() || r.reason() != TianfuSelection.Rejection.UNKNOWN_ID) {
                allUnknown = false;
                System.out.println("        " + id + " -> " + r.reason());
            }
            if (r.snapshot() != empty) {
                allZeroWrite = false;
            }
        }
        check("9 条已删除条目全部返回 UNKNOWN_ID", allUnknown, "");
        check("UNKNOWN_ID 拒绝时快照未被改动（零写入）", allZeroWrite, "");
        check("余额未变", empty.points() == 40, "");

        System.out.println("=== 3. 保留条目不受影响 ===");
        check("一掷乾坤仍在池内", TianfuTalents.byId("qiyun_yizhi") != null, "");
        var r3 = TianfuEconomy.buy(empty, "jiandao_qinhe");
        check("买 剑道亲和 仍然成功", r3.ok(), String.valueOf(r3.reason()));
        check("扣点正确（1 点 => 余额 " + (empty.points() - 1) + "）",
                r3.ok() && r3.snapshot().points() == empty.points() - 1,
                r3.ok() ? String.valueOf(r3.snapshot().points()) : "fail");

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
