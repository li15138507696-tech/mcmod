package net.guzhenren.tianfu;
import java.util.List;
import java.util.Set;
public final class UniversalRulesTest {
    private static void check(boolean ok) { if (!ok) throw new AssertionError(); }
    public static void main(String[] args) {
        check(UniversalRules.initialPoints(false, false, false, 0) == 8);
        check(UniversalRules.initialPoints(true, false, false, 0) == 8);
        check(UniversalRules.initialPoints(true, true, false, 0) == 0);
        check(UniversalRules.initialPoints(true, false, false, 7) == 7);
        check(UniversalRules.initialPoints(true, false, true, 0) == 0);
        check(UniversalRules.importOwned(false, List.of("jingu_qiangjian")).isEmpty());
        check(UniversalRules.importOwned(true, List.of("jingu_qiangjian", "unsafe", "jingu_qiangjian")).equals(List.of("jingu_qiangjian")));
        check(!UniversalRules.mayImport(true));
        check(Math.abs(UniversalRules.amount("health", Set.of("jingu_qiangjian", "xueqi_rulin")) - 0.32) < 0.00001);
        check(UniversalRules.repair1419(true, true, true, false, 0, false, false, 0));
        check(!UniversalRules.repair1419(true, true, true, true, 0, false, false, 0));
        check(!UniversalRules.repair1419(true, true, true, false, 0, true, false, 0));
        check(!UniversalRules.repair1419(true, true, true, false, 0, false, true, 0));
        check(!UniversalRules.repair1419(true, true, true, false, 0, false, false, 3));
        check(!UniversalRules.repair1419(true, true, true, false, 1, false, false, 0));
        check(!UniversalRules.repair1419(false, true, true, false, 0, false, false, 0));
        check(!UniversalRules.repair1419(true, false, true, false, 0, false, false, 0));
        for (int h : new int[]{120, 160, 200, 300}) {
            int rows = Math.min(6, net.guzhenren.tianfu.client.UniversalLayout.rowsPerPage(h));
            var l = net.guzhenren.tianfu.client.UniversalLayout.forHeight(h, rows);
            check(l.rowsY() + (rows - 1) * l.rowStep() + l.rowHeight() <= l.recommendationY());
            check(l.recommendationY() + 9 <= l.buttonsY());
            check(l.buttonsY() + l.buttonHeight() <= h);
        }
        System.out.println("UniversalRulesTest passed");
    }
}
