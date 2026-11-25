package net.guzhenren.tianfu.compat;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import net.guzhenren.tianfu.mixin.TianfuMixinPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 跨本体版本契约测试（9.8 / 10.4 / 10.20 / 11.22 + 未来小版本容错 + 破坏性变更拦截）。
 */
public final class CrossVersionContractTest {
    private static int pass = 0;
    private static int fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) {
            pass++;
            System.out.println("  PASS  " + name);
        } else {
            fail++;
            System.out.println("  FAIL  " + name + " -> " + detail);
        }
    }

    private static Path resolveExisting(String... candidates) {
        for (String c : candidates) {
            Path p = Path.of(c);
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private static Map<String, byte[]> readClassesFromModJar(Path jarPath, java.util.Set<String> classInternalNames)
            throws IOException {
        Map<String, byte[]> direct = new HashMap<>();
        byte[] innerJarBytes = null;
        try (ZipFile zip = new ZipFile(jarPath.toFile())) {
            for (String cls : classInternalNames) {
                ZipEntry e = zip.getEntry(cls + ".class");
                if (e != null) {
                    try (InputStream in = zip.getInputStream(e)) {
                        direct.put(cls, in.readAllBytes());
                    }
                }
            }
            if (direct.size() == classInternalNames.size()) {
                return direct;
            }
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry e = entries.nextElement();
                String name = e.getName();
                if (name.startsWith("META-INF/jarjar/guzhenren-") && name.endsWith(".jar")) {
                    try (InputStream in = zip.getInputStream(e)) {
                        innerJarBytes = in.readAllBytes();
                    }
                    break;
                }
            }
        }
        if (innerJarBytes == null) {
            return direct;
        }
        Map<String, byte[]> inner = new HashMap<>();
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(innerJarBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String n = entry.getName();
                if (n.endsWith(".class")) {
                    String cls = n.substring(0, n.length() - 6);
                    if (classInternalNames.contains(cls)) {
                        inner.put(cls, zis.readAllBytes());
                    }
                }
            }
        }
        return inner;
    }

    private static byte[] addHarmlessDummyMethod(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        MethodNode dummy = new MethodNode(Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "tianfuSyntheticFutureCheck", "()V", null, null);
        dummy.instructions.add(new InsnNode(Opcodes.RETURN));
        dummy.maxStack = 0;
        dummy.maxLocals = 0;
        node.methods.add(dummy);
        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        return cw.toByteArray();
    }

    private static byte[] removeExecuteMethods(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, 0);
        node.methods.removeIf(m -> "execute".equals(m.name));
        ClassWriter cw = new ClassWriter(0);
        node.accept(cw);
        return cw.toByteArray();
    }

    public static void main(String[] args) throws Exception {
        java.util.Set<String> needed = java.util.Set.of(
                "net/guzhenren/procedures/UikongqiaoquedingProcedure",
                "net/guzhenren/procedures/JingJieBaoCunProcedure",
                "net/guzhenren/procedures/KE1Procedure",
                "net/guzhenren/procedures/UIxiuliananjianProcedure",
                "net/guzhenren/procedures/XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure",
                "net/guzhenren/procedures/TianziliuchengProcedure",
                "net/guzhenren/item/YizhuanrendaoxiwangguItem",
                "net/guzhenren/network/GuzhenrenModVariables$PlayerVariables");

        record Target(String expectedLabel, TianfuMixinPlugin.ReincarnationVariant expectedVariant, Path path) { }
        Map<String, Target> targets = new LinkedHashMap<>();

        Path p98 = resolveExisting("_crash3/basever/guzhenren-9.8.jar", "../_crash3/basever/guzhenren-9.8.jar");
        if (p98 != null) targets.put("9.8", new Target("9.8", TianfuMixinPlugin.ReincarnationVariant.V1_3PARAM, p98));

        Path p104 = resolveExisting("_crash3/basever/guzhenren-10.4.jar", "../_crash3/basever/guzhenren-10.4.jar");
        if (p104 != null) targets.put("10.4", new Target("10.4/10.20", TianfuMixinPlugin.ReincarnationVariant.V1_3PARAM, p104));

        Path p1020 = resolveExisting("_crash3/basever/guzhenren-10.20.jar", "../_crash3/basever/guzhenren-10.20.jar");
        if (p1020 != null) targets.put("10.20", new Target("10.4/10.20", TianfuMixinPlugin.ReincarnationVariant.V1_3PARAM, p1020));

        Path p1122 = resolveExisting(
                "D:\\daima\\mcmod\\蛊真人本体\\guzhenren蛊真人11.22-宗门家族优化.jar");
        if (p1122 != null) targets.put("11.22", new Target("11.22", TianfuMixinPlugin.ReincarnationVariant.V2_6PARAM, p1122));

        check("至少找到 1 个本地本体 JAR 供跨版本契约校验", !targets.isEmpty(), "未找到任何本地本体 JAR");

        Map<String, byte[]> sample1122 = null;
        for (var entry : targets.entrySet()) {
            String ver = entry.getKey();
            Target t = entry.getValue();
            Map<String, byte[]> classes = readClassesFromModJar(t.path(), needed);
            if ("11.22".equals(ver)) {
                sample1122 = classes;
            }
            var detected = TianfuMixinPlugin.detectReincarnationVariant(
                    classes.get("net/guzhenren/procedures/JingJieBaoCunProcedure"));
            check(ver + " 转世变体路由 == " + t.expectedVariant(),
                    detected == t.expectedVariant(), String.valueOf(detected));

            String profile = Guzhenren98Compatibility.verifyBytecode(classes::get);
            check(ver + " 契约+档位校验 == " + t.expectedLabel(),
                    t.expectedLabel().equals(profile), profile);
        }

        if (sample1122 != null) {
            System.out.println("=== 未来小版本容错与破坏性变更拦截 ===");
            Map<String, byte[]> syntheticFuture = new HashMap<>(sample1122);
            syntheticFuture.put("net/guzhenren/procedures/TianziliuchengProcedure",
                    addHarmlessDummyMethod(sample1122.get("net/guzhenren/procedures/TianziliuchengProcedure")));
            String futureLabel = Guzhenren98Compatibility.verifyBytecode(syntheticFuture::get);
            check("SHA 变化但语义契约不变 => 放行并标记契约兼容",
                    futureLabel.startsWith("契约兼容("), futureLabel);

            Map<String, byte[]> broken = new HashMap<>(sample1122);
            broken.put("net/guzhenren/procedures/JingJieBaoCunProcedure",
                    removeExecuteMethods(sample1122.get("net/guzhenren/procedures/JingJieBaoCunProcedure")));
            boolean threw = false;
            try {
                Guzhenren98Compatibility.verifyBytecode(broken::get);
            } catch (IllegalStateException expected) {
                threw = expected.getMessage().contains("契约不兼容");
            }
            check("转世入口被破坏 => 硬拦截抛 IllegalStateException", threw, String.valueOf(threw));
        }

        System.out.println();
        System.out.println("================================");
        System.out.println("PASS " + pass + " / FAIL " + fail);
        System.out.println("================================");
        if (fail > 0) {
            System.exit(1);
        }
    }
}
