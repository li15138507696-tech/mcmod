package net.guzhenren.tianfu.compat;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.guzhenren.tianfu.TianfuMod;
import net.guzhenren.tianfu.mixin.TianfuMixinPlugin;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 蛊真人本体「深层语义类」校验（混合探针版：ASM 契约门禁 + 已知档位识别）。
 *
 * <p>优先用 ASM 校验附属真正依赖的 8 个本体类的**方法签名、注入点数量和 PlayerVariables 字段类型**；
 * 只要语义契约全部命中，未来小版本（如 11.23+）无需补录 SHA-256 即可平滑运行，只有真正破坏契约时才硬报错。
 */
public final class Guzhenren98Compatibility {
    private Guzhenren98Compatibility() { }

    private static volatile boolean verified;

    private static final String CLS_QUEDING = "net/guzhenren/procedures/UikongqiaoquedingProcedure";
    private static final String CLS_JINGJIE = "net/guzhenren/procedures/JingJieBaoCunProcedure";
    private static final String CLS_KE1 = "net/guzhenren/procedures/KE1Procedure";
    private static final String CLS_XIULIAN_BTN = "net/guzhenren/procedures/UIxiuliananjianProcedure";
    private static final String CLS_XIULIAN_TICK = "net/guzhenren/procedures/XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure";
    private static final String CLS_TIANZI = "net/guzhenren/procedures/TianziliuchengProcedure";
    private static final String CLS_XIWANG = "net/guzhenren/item/YizhuanrendaoxiwangguItem";
    private static final String CLS_PLAYER_VARS = "net/guzhenren/network/GuzhenrenModVariables$PlayerVariables";

    private static final List<String> REQUIRED_CLASSES = List.of(
            CLS_QUEDING, CLS_JINGJIE, CLS_KE1, CLS_XIULIAN_BTN,
            CLS_XIULIAN_TICK, CLS_TIANZI, CLS_XIWANG, CLS_PLAYER_VARS);

    private static final List<String> REQUIRED_DOUBLE_FIELDS = List.of(
            "zhuanshu", "jieduan", "kongqiao", "tizhi", "shouyuan",
            "zhenyuan", "zuida_zhenyuan", "niantou", "niantou_rongliang",
            "gushi_xiulian_jindu", "gushi_xiulian_dangqian");

    /** 9.8 基线（8/8 实测匹配 `guzhenren-9.8.jar`）。 */
    private static final Map<String, String> HASHES_9_8 = Map.ofEntries(
            Map.entry(CLS_QUEDING, "E1707AA64910048CA7ED76B37F3DA7467DE0CD589C3632E901A4A8B3E1E7912F"),
            Map.entry(CLS_JINGJIE, "F991FBC556CD1B2D6162C133FE98C3E6A99799196603E3A29B36E715988866D2"),
            Map.entry(CLS_KE1, "772225C51A12CBC2C408C6438FE4EE06D0B41977B10D38D088E7661D0F4F64DA"),
            Map.entry(CLS_XIULIAN_BTN, "B03E83AB4E937B85E918B9CC93F6CAF98110F14C861512D4D8C3ECFDD0D10162"),
            Map.entry(CLS_XIULIAN_TICK, "BACAB72763BC9D1BA6DE1ABE30332A206640FB6BCF1221E3F24EC6CA5D9B1D04"),
            Map.entry(CLS_TIANZI, "B585C12B5E217B5F70E2D217C0D8FC3AC17D1D6015E6C74E4FEABEB48953E590"),
            Map.entry(CLS_XIWANG, "8A6D7035C1D510459FC63B8FB98E5C8DA69E1A15BFDDC633DB1B6A2B7D578274"),
            Map.entry(CLS_PLAYER_VARS, "9EFE0F2C17F1EE5B6120EECE3D1C953705D7A965DA2E559FAB83E2344638ECFF"));

    /** 10.4 / 10.20 基线（内层 jar 字节完全相同）。 */
    private static final Map<String, String> HASHES_10_X = buildTenX();

    /** 11.22 基线（转世过程改为 6 参）。 */
    private static final Map<String, String> HASHES_11_22 = buildElevenTwentyTwo();

    private static Map<String, String> buildTenX() {
        Map<String, String> m = new LinkedHashMap<>(HASHES_9_8);
        m.put(CLS_QUEDING, "307F66451F87ED56E0E344964C88871716DA942372BF01B79C0B30F59AD74E37");
        m.put(CLS_KE1, "F0908D9903FC60A1E356B95D9D1CD29CBF6C8A9B4A85FB140544A9168D30A0DC");
        m.put(CLS_XIULIAN_BTN, "1060052B3C68C0B00401FBDBF013079A47D2F0EF6DF88767582DF55B89B7230B");
        return Map.copyOf(m);
    }

    private static Map<String, String> buildElevenTwentyTwo() {
        Map<String, String> m = new LinkedHashMap<>(HASHES_10_X);
        m.put(CLS_JINGJIE, "647DFA553241EA9BB1732149ADC364A1325CAA602C05D5B59598EE38C76A0205");
        return Map.copyOf(m);
    }

    private record Profile(String label, Map<String, String> hashes) { }

    private static final List<Profile> PROFILES = List.of(
            new Profile("9.8", HASHES_9_8),
            new Profile("10.4/10.20", HASHES_10_X),
            new Profile("11.22", HASHES_11_22));

    public static synchronized void verify() {
        if (verified) return;
        ClassLoader loader = Guzhenren98Compatibility.class.getClassLoader();
        String profile = verifyBytecode(cls -> readResource(loader, cls + ".class"));
        matchedProfile = profile;

        // 二次核验运行期实际加载的 PlayerVariables 字段类型
        try {
            if (PlayerVariables.class.getField("shiqiao").getType() != boolean.class) {
                throw incompatible("shiqiao 必须为 boolean");
            }
            for (String field : REQUIRED_DOUBLE_FIELDS) {
                if (PlayerVariables.class.getField(field).getType() != double.class) {
                    throw incompatible(field + " 必须为 double");
                }
            }
        } catch (NoSuchFieldException failure) {
            throw new IllegalStateException("[天命] 蛊真人本体玩家字段不匹配", failure);
        }
        verified = true;
    }

    /**
     * 对给定的 8 个核心本体类字节流执行契约校验与档位识别（纯 ASM，可用于离线单元测试）。
     *
     * @return 命中的档位名（{@code "9.8"} / {@code "10.4/10.20"} / {@code "11.22"}）或 {@code "契约兼容(...)"}
     */
    public static String verifyBytecode(Function<String, byte[]> bytecodeProvider) {
        Map<String, byte[]> rawBytes = new LinkedHashMap<>();
        Map<String, String> actualHashes = new LinkedHashMap<>();
        for (String cls : REQUIRED_CLASSES) {
            byte[] bytes = bytecodeProvider.apply(cls);
            if (bytes == null || bytes.length == 0) {
                throw incompatible(cls + ".class 缺失（本体未安装或版本完全不符）");
            }
            rawBytes.put(cls, bytes);
            actualHashes.put(cls, sha256Hex(bytes));
        }

        // 1) ASM 语义契约断言（真正的硬门禁）
        String variantTag = verifyContracts(rawBytes);

        // 2) 已知档位比对（仅用于标签识别与日志提示；未登记的小版本只要契约全过仍放行）
        for (Profile profile : PROFILES) {
            if (profile.hashes().equals(actualHashes)) {
                TianfuMod.LOGGER.info("[天命] 蛊真人本体校验通过：{}（{} 个机制类，契约全匹配）",
                        profile.label(), rawBytes.size());
                return profile.label();
            }
        }
        String fallbackLabel = "契约兼容(" + variantTag + ")";
        List<String> diffs = new ArrayList<>();
        for (String cls : REQUIRED_CLASSES) {
            if (!HASHES_11_22.get(cls).equals(actualHashes.get(cls))) {
                diffs.add(cls.substring(cls.lastIndexOf('/') + 1));
            }
        }
        TianfuMod.LOGGER.info("[天命] 蛊真人本体未命中固定档位，但 ASM 语义契约全部通过 → {}（差异类: {}）",
                fallbackLabel, diffs);
        return fallbackLabel;
    }

    private static String verifyContracts(Map<String, byte[]> rawBytes) {
        // 1. JingJieBaoCunProcedure: 3参或6参 execute + 恰好 2 处 shiqiao:Z PUTFIELD
        ClassNode jingjie = parseClass(rawBytes.get(CLS_JINGJIE));
        TianfuMixinPlugin.ReincarnationVariant variant = TianfuMixinPlugin.detectReincarnationVariant(jingjie);
        if (variant == TianfuMixinPlugin.ReincarnationVariant.UNKNOWN) {
            throw contractBroken("JingJieBaoCunProcedure 缺少 3 参或 6 参 execute 方法");
        }
        String expectedJjDesc = variant == TianfuMixinPlugin.ReincarnationVariant.V2_6PARAM
                ? TianfuMixinPlugin.JINGJIE_DESC_6
                : TianfuMixinPlugin.JINGJIE_DESC_3;
        MethodNode jjExecute = findMethod(jingjie, "execute", expectedJjDesc);
        int shiqiaoPuts = countFieldPut(jjExecute, CLS_PLAYER_VARS, "shiqiao", "Z");
        if (shiqiaoPuts != 2) {
            throw contractBroken("JingJieBaoCunProcedure.shiqiao:Z 写入点数=" + shiqiaoPuts + "（预期 2）");
        }

        // 2. UikongqiaoquedingProcedure: execute(LevelAccessor, Entity)V + 至少 1 处 closeContainer()V
        ClassNode queding = parseClass(rawBytes.get(CLS_QUEDING));
        MethodNode qdExecute = findMethod(queding, "execute",
                "(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V");
        if (qdExecute == null) {
            throw contractBroken("UikongqiaoquedingProcedure 缺少 execute(LevelAccessor, Entity)V");
        }
        int closeCount = countMethodInsn(qdExecute, Opcodes.INVOKEVIRTUAL, null, "closeContainer", "()V");
        if (closeCount < 1) {
            throw contractBroken("UikongqiaoquedingProcedure 未找到 closeContainer()V 调用");
        }

        // 3. KE1Procedure: 7 处 zhenyuan:D PUTFIELD + 4 处 niantou:D PUTFIELD
        ClassNode ke1 = parseClass(rawBytes.get(CLS_KE1));
        MethodNode ke1Execute = findMethod(ke1, "execute",
                "(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V");
        if (ke1Execute == null) {
            throw contractBroken("KE1Procedure 缺少 execute(LevelAccessor, D, D, D, Entity)V");
        }
        int zyPuts = countFieldPut(ke1Execute, CLS_PLAYER_VARS, "zhenyuan", "D");
        int ntPuts = countFieldPut(ke1Execute, CLS_PLAYER_VARS, "niantou", "D");
        if (zyPuts != 7 || ntPuts != 4) {
            throw contractBroken("KE1Procedure 写回点数异常: zhenyuan=" + zyPuts + "(预期7), niantou=" + ntPuts + "(预期4)");
        }

        // 4. UIxiuliananjianProcedure: 4 处 java/lang/Math.random()D
        ClassNode uiBtn = parseClass(rawBytes.get(CLS_XIULIAN_BTN));
        MethodNode uiBtnExecute = findMethod(uiBtn, "execute",
                "(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V");
        if (uiBtnExecute == null) {
            throw contractBroken("UIxiuliananjianProcedure 缺少 execute(LevelAccessor, D, D, D, Entity)V");
        }
        int randCount = countMethodInsn(uiBtnExecute, Opcodes.INVOKESTATIC, "java/lang/Math", "random", "()D");
        if (randCount != 4) {
            throw contractBroken("UIxiuliananjianProcedure Math.random() 调用数=" + randCount + "（预期 4）");
        }

        // 5. XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure: 3 处 gushi_xiulian_dangqian:D PUTFIELD
        ClassNode xlTick = parseClass(rawBytes.get(CLS_XIULIAN_TICK));
        MethodNode xlExecute = findMethod(xlTick, "execute",
                "(Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V");
        if (xlExecute == null) {
            throw contractBroken("Xiulianjingjie...Procedure 缺少 execute(LevelAccessor, D, D, D, Entity)V");
        }
        int xlPuts = countFieldPut(xlExecute, CLS_PLAYER_VARS, "gushi_xiulian_dangqian", "D");
        if (xlPuts != 3) {
            throw contractBroken("Xiulianjingjie...gushi_xiulian_dangqian 写入点数=" + xlPuts + "（预期 3）");
        }

        // 6. YizhuanrendaoxiwangguItem: use(Level, Player, InteractionHand)InteractionResultHolder
        ClassNode xiwang = parseClass(rawBytes.get(CLS_XIWANG));
        MethodNode useMethod = findMethod(xiwang, "use",
                "(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResultHolder;");
        if (useMethod == null) {
            throw contractBroken("YizhuanrendaoxiwangguItem 缺少 use(Level, Player, InteractionHand)InteractionResultHolder");
        }

        // 7. TianziliuchengProcedure: execute(Entity)V
        ClassNode tianzi = parseClass(rawBytes.get(CLS_TIANZI));
        if (findMethod(tianzi, "execute", "(Lnet/minecraft/world/entity/Entity;)V") == null) {
            throw contractBroken("TianziliuchengProcedure 缺少 execute(Entity)V");
        }

        // 8. PlayerVariables: shiqiao:Z 及 11 项核心 double 字段
        ClassNode pv = parseClass(rawBytes.get(CLS_PLAYER_VARS));
        Map<String, String> fieldDescs = new HashMap<>();
        if (pv.fields != null) {
            for (FieldNode f : pv.fields) {
                fieldDescs.put(f.name, f.desc);
            }
        }
        if (!"Z".equals(fieldDescs.get("shiqiao"))) {
            throw contractBroken("PlayerVariables.shiqiao 缺失或类型非 boolean");
        }
        for (String name : REQUIRED_DOUBLE_FIELDS) {
            if (!"D".equals(fieldDescs.get(name))) {
                throw contractBroken("PlayerVariables." + name + " 缺失或类型非 double");
            }
        }

        return variant == TianfuMixinPlugin.ReincarnationVariant.V2_6PARAM ? "6参转世" : "3参转世";
    }

    private static ClassNode parseClass(byte[] bytes) {
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    private static MethodNode findMethod(ClassNode node, String name, String desc) {
        if (node == null || node.methods == null) {
            return null;
        }
        for (MethodNode m : node.methods) {
            if (name.equals(m.name) && desc.equals(m.desc)) {
                return m;
            }
        }
        return null;
    }

    private static int countFieldPut(MethodNode method, String owner, String name, String desc) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int count = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == Opcodes.PUTFIELD && insn instanceof FieldInsnNode fi) {
                if (owner.equals(fi.owner) && name.equals(fi.name) && desc.equals(fi.desc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static int countMethodInsn(MethodNode method, int opcode, String owner, String name, String desc) {
        if (method == null || method.instructions == null) {
            return 0;
        }
        int count = 0;
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn.getOpcode() == opcode && insn instanceof MethodInsnNode mi) {
                if ((owner == null || owner.equals(mi.owner)) && name.equals(mi.name) && desc.equals(mi.desc)) {
                    count++;
                }
            }
        }
        return count;
    }

    private static byte[] readResource(ClassLoader loader, String resource) {
        try (InputStream input = loader.getResourceAsStream(resource)) {
            return input == null ? null : input.readAllBytes();
        } catch (IOException failure) {
            throw new IllegalStateException("[天命] 无法核验蛊真人机制类: " + resource, failure);
        }
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            return HexFormat.of().withUpperCase().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException failure) {
            throw new IllegalStateException(failure);
        }
    }

    private static IllegalStateException incompatible(String detail) {
        return new IllegalStateException("[天命] 蛊真人本体不兼容：" + detail);
    }

    private static IllegalStateException contractBroken(String detail) {
        return new IllegalStateException("[天命] 蛊真人本体契约不兼容：" + detail);
    }

    // ------------------------------------------------------------------ 数值探测

    private static volatile String matchedProfile = "未知";

    /** 命中的档位名（"9.8" / "10.4/10.20" / "11.22" / "契约兼容(...)"）；未命中为"未知"。 */
    public static String matchedProfile() {
        return matchedProfile;
    }

    /**
     * 本体某个类里是否含指定的 double 常量（读真实 class 字节，**不看版本号**）。
     */
    public static boolean baseClassContainsDouble(String internalName, double value) {
        String resource = internalName + ".class";
        try (InputStream input = Guzhenren98Compatibility.class.getClassLoader()
                .getResourceAsStream(resource)) {
            if (input == null) {
                return false;
            }
            byte[] data = input.readAllBytes();
            return containsDouble(data, value);
        } catch (IOException failure) {
            return false;
        }
    }

    /** 纯字节搜索（包内可见，便于单测）。 */
    static boolean containsDouble(byte[] data, double value) {
        byte[] pattern = java.nio.ByteBuffer.allocate(8).putDouble(value).array();
        byte first = pattern[0];
        for (int i = 0; i + 8 <= data.length; i++) {
            if (data[i] != first) {
                continue;
            }
            boolean hit = true;
            for (int j = 1; j < 8; j++) {
                if (data[i + j] != pattern[j]) {
                    hit = false;
                    break;
                }
            }
            if (hit) {
                return true;
            }
        }
        return false;
    }
}
