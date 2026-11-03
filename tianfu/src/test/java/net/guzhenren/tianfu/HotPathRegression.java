package net.guzhenren.tianfu;

import java.lang.management.ManagementFactory;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.ToDoubleFunction;
import net.guzhenren.tianfu.TianfuTalents.Op;

/** Plain JVM checks against the shipped talent data; does not launch Minecraft. */
public final class HotPathRegression {
    private record Probe(Op op, ToDoubleFunction<Set<String>> read) { }
    private static final List<Probe> PROBES = List.of(
            new Probe(Op.REGEN_ZY, TianfuTalents::regenZy),
            new Probe(Op.REGEN_NT, TianfuTalents::regenNt),
            new Probe(Op.XIULIAN, TianfuTalents::xiulian),
            new Probe(Op.BREAKTHROUGH, TianfuTalents::breakthrough),
            new Probe(Op.LIANGU, TianfuTalents::liangu),
            new Probe(Op.GU_CD, TianfuTalents::guCd),
            new Probe(Op.SHAZHAO_CD, TianfuTalents::shazhaoCd),
            new Probe(Op.REDUCTION, TianfuTalents::reduction));
    private static volatile double sink;

    public static void main(String[] args) {
        var talents = TianfuTalents.all();
        Set<String> allIds = new LinkedHashSet<>();
        int checks = 0;
        for (var talent : talents) {
            require(allIds.add(talent.id()), "duplicate catalog id: " + talent.id()); checks++;
            var spec = TianfuTalents.byIdIncludingUnavailable(talent.id());
            require(spec != null && spec.cost() == talent.cost() && spec.desc().equals(talent.desc()),
                    "legacy lookup differs: " + talent.id()); checks++;
        }
        require(TianfuTalents.byIdIncludingUnavailable("unknown_saved_id") == null, "unknown ID accepted"); checks++;
        var random = new Random(98);
        for (int sample = 0; sample < 250; sample++) {
            Set<String> ids = new LinkedHashSet<>();
            for (int i = 0; i < 40; i++) ids.add(talents.get(random.nextInt(talents.size())).id());
            ids.add("unknown_saved_id"); ids.add("jiyuan_jia");
            for (var probe : PROBES) {
                double expected = 0;
                for (String id : ids) {
                    var definition = talents.stream().filter(t -> t.id().equals(id)).findFirst().orElse(null);
                    if (definition != null) for (var fx : definition.fx()) if (fx.op() == probe.op()) expected += fx.a();
                }
                require(Double.compare(expected, probe.read().applyAsDouble(ids)) == 0,
                        "catalog aggregation differs: " + probe.op()); checks++;
            }
        }
        Set<String> workload = new LinkedHashSet<>();
        for (var talent : talents) {
            if (talent.fx().stream().anyMatch(fx -> PROBES.stream().anyMatch(p -> p.op() == fx.op()))) workload.add(talent.id());
            if (workload.size() == 24) break;
        }
        run(workload, 20000);
        var bean = (com.sun.management.ThreadMXBean)ManagementFactory.getThreadMXBean();
        require(bean.isThreadAllocatedMemorySupported(), "allocation measurement unavailable");
        bean.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        long allocated = bean.getThreadAllocatedBytes(thread), start = System.nanoTime();
        run(workload, 100000);
        long elapsed = System.nanoTime() - start;
        allocated = bean.getThreadAllocatedBytes(thread) - allocated;
        System.out.println("PASS: " + checks + " catalog/aggregation checks; no server started");
        System.out.println("BENCH calls=800000 ids=" + workload.size() + " allocatedBytes=" + allocated
                + " elapsedNanos=" + elapsed + " checksum=" + sink);
    }
    private static void run(Set<String> ids, int iterations) {
        double value = 0;
        for (int i = 0; i < iterations; i++) for (var probe : PROBES) value += probe.read().applyAsDouble(ids);
        sink = value;
    }
    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
