package net.guzhenren.tianfu.mixin;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

/**
 * 跨本体版本 Mixin 路由插件（9.8 / 10.4 / 10.20 / 11.22+ 单包自适应）。
 *
 * <p>纯 ASM 字节码探测，不提前加载目标类：
 * <ul>
 *   <li>{@code JingJieBaoCunProcedure.execute(Event, LevelAccessor, Entity)V}（3 参）→ 启用 {@code JingJieBaoCunProcedureMixin}</li>
 *   <li>{@code JingJieBaoCunProcedure.execute(Event, LevelAccessor, double, double, double, Entity)V}（6 参）→ 启用 {@code JingJieBaoCunV2ProcedureMixin}</li>
 * </ul>
 */
public final class TianfuMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("tianfu-mixin");

    public static final String JINGJIE_CLASS = "net.guzhenren.procedures.JingJieBaoCunProcedure";
    public static final String JINGJIE_DESC_3 =
            "(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/world/entity/Entity;)V";
    public static final String JINGJIE_DESC_6 =
            "(Lnet/neoforged/bus/api/Event;Lnet/minecraft/world/level/LevelAccessor;DDDLnet/minecraft/world/entity/Entity;)V";

    private static final String MIXIN_JINGJIE_V1 = "net.guzhenren.tianfu.mixin.JingJieBaoCunProcedureMixin";
    private static final String MIXIN_JINGJIE_V2 = "net.guzhenren.tianfu.mixin.JingJieBaoCunV2ProcedureMixin";

    public enum ReincarnationVariant { V1_3PARAM, V2_6PARAM, UNKNOWN }

    private ReincarnationVariant cachedVariant;

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (MIXIN_JINGJIE_V1.equals(mixinClassName)) {
            ReincarnationVariant v = detectRuntimeVariant();
            boolean apply = v == ReincarnationVariant.V1_3PARAM;
            LOGGER.info("[天命] 转世钩子路由：检测到 {} → JingJieBaoCunProcedureMixin(3参)={}", v, apply);
            return apply;
        }
        if (MIXIN_JINGJIE_V2.equals(mixinClassName)) {
            ReincarnationVariant v = detectRuntimeVariant();
            boolean apply = v == ReincarnationVariant.V2_6PARAM;
            LOGGER.info("[天命] 转世钩子路由：检测到 {} → JingJieBaoCunV2ProcedureMixin(6参)={}", v, apply);
            return apply;
        }
        return true;
    }

    private ReincarnationVariant detectRuntimeVariant() {
        if (cachedVariant != null) {
            return cachedVariant;
        }
        ClassNode node = loadClassNode(JINGJIE_CLASS);
        cachedVariant = detectReincarnationVariant(node);
        if (cachedVariant == ReincarnationVariant.UNKNOWN) {
            LOGGER.warn("[天命] 未识别到 JingJieBaoCunProcedure 的 3 参或 6 参 execute 方法，转世钩子已自动跳过以防崩溃");
        }
        return cachedVariant;
    }

    private ClassNode loadClassNode(String className) {
        try {
            ClassNode node = MixinService.getService().getBytecodeProvider().getClassNode(className, false);
            if (node != null) {
                return node;
            }
        } catch (Throwable ignored) {
            // 回退到 ClassLoader 字节流读取（不触发类初始化）
        }
        String resource = className.replace('.', '/') + ".class";
        try (InputStream in = TianfuMixinPlugin.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return readClassNode(in.readAllBytes());
        } catch (IOException ignored) {
            return null;
        }
    }

    /** 纯 ASM 字节解析，便于纯 JVM 单测离线验证。 */
    public static ClassNode readClassNode(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        ClassNode node = new ClassNode();
        new ClassReader(bytes).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return node;
    }

    /** 从 ASM ClassNode 判定转世过程签名版本。 */
    public static ReincarnationVariant detectReincarnationVariant(ClassNode node) {
        if (node == null || node.methods == null) {
            return ReincarnationVariant.UNKNOWN;
        }
        boolean has3 = false;
        boolean has6 = false;
        for (MethodNode m : node.methods) {
            if (!"execute".equals(m.name)) {
                continue;
            }
            if (JINGJIE_DESC_3.equals(m.desc)) {
                has3 = true;
            } else if (JINGJIE_DESC_6.equals(m.desc)) {
                has6 = true;
            }
        }
        if (has6) {
            return ReincarnationVariant.V2_6PARAM;
        }
        if (has3) {
            return ReincarnationVariant.V1_3PARAM;
        }
        return ReincarnationVariant.UNKNOWN;
    }

    /** 便捷重载：直接对原始 class 字节判定变体。 */
    public static ReincarnationVariant detectReincarnationVariant(byte[] classBytes) {
        return detectReincarnationVariant(readClassNode(classBytes));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
