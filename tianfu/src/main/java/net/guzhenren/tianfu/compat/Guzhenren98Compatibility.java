package net.guzhenren.tianfu.compat;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.guzhenren.tianfu.TianfuMod;

/** Exact semantic-class baseline for the user supplied Guzhenren 9.8 (metadata version 0.1). */
public final class Guzhenren98Compatibility {
    private Guzhenren98Compatibility() { }
    private static volatile boolean verified;
    private static final Map<String, String> SEMANTICS = Map.ofEntries(
            Map.entry("net/guzhenren/procedures/UikongqiaoquedingProcedure", "E1707AA64910048CA7ED76B37F3DA7467DE0CD589C3632E901A4A8B3E1E7912F"),
            Map.entry("net/guzhenren/procedures/JingJieBaoCunProcedure", "F991FBC556CD1B2D6162C133FE98C3E6A99799196603E3A29B36E715988866D2"),
            Map.entry("net/guzhenren/procedures/KE1Procedure", "772225C51A12CBC2C408C6438FE4EE06D0B41977B10D38D088E7661D0F4F64DA"),
            Map.entry("net/guzhenren/procedures/UIxiuliananjianProcedure", "B03E83AB4E937B85E918B9CC93F6CAF98110F14C861512D4D8C3ECFDD0D10162"),
            Map.entry("net/guzhenren/procedures/XiulianjingjieXiaoGuoChiXuShiMeiKeFaShengProcedure", "BACAB72763BC9D1BA6DE1ABE30332A206640FB6BCF1221E3F24EC6CA5D9B1D04"),
            Map.entry("net/guzhenren/procedures/TianziliuchengProcedure", "B585C12B5E217B5F70E2D217C0D8FC3AC17D1D6015E6C74E4FEABEB48953E590"),
            Map.entry("net/guzhenren/procedures/SxProcedure", "07447FC82B0C8AFEA25E9BC7007E3AB5012A1A1724C0C41E6E4420CC00AC8E81"),
            Map.entry("net/guzhenren/item/YizhuanrendaoxiwangguItem", "8A6D7035C1D510459FC63B8FB98E5C8DA69E1A15BFDDC633DB1B6A2B7D578274"),
            Map.entry("net/guzhenren/network/GuzhenrenModVariables$PlayerVariables", "9EFE0F2C17F1EE5B6120EECE3D1C953705D7A965DA2E559FAB83E2344638ECFF"));

    public static synchronized void verify() {
        if (verified) return;
        ClassLoader loader = Guzhenren98Compatibility.class.getClassLoader();
        for (var entry : SEMANTICS.entrySet()) {
            String resource = entry.getKey() + ".class";
            try (InputStream input = loader.getResourceAsStream(resource)) {
                if (input == null) throw incompatible(resource + " 缺失");
                String actual = HexFormat.of().withUpperCase().formatHex(
                        MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
                if (!actual.equals(entry.getValue())) throw incompatible(resource + " 与已验证的 9.8 不一致");
            } catch (IOException | NoSuchAlgorithmException failure) {
                throw new IllegalStateException("[天命] 无法核验蛊真人 9.8 机制: " + resource, failure);
            }
        }
        try {
            if (PlayerVariables.class.getField("shiqiao").getType() != boolean.class)
                throw incompatible("shiqiao 必须为 boolean");
            for (String field : new String[]{"zhuanshu", "jieduan", "kongqiao", "tizhi", "shouyuan",
                    "zhenyuan", "zuida_zhenyuan", "niantou", "niantou_rongliang",
                    "gushi_xiulian_jindu", "gushi_xiulian_dangqian"}) {
                if (PlayerVariables.class.getField(field).getType() != double.class)
                    throw incompatible(field + " 必须为 double");
            }
        } catch (NoSuchFieldException failure) {
            throw new IllegalStateException("[天命] 蛊真人 9.8 玩家字段不匹配", failure);
        }
        verified = true;
        TianfuMod.LOGGER.info("[天命] 蛊真人 9.8 深层适配核验通过：{} 个机制类及玩家字段类型", SEMANTICS.size());
    }

    private static IllegalStateException incompatible(String detail) {
        return new IllegalStateException("[天命] 此构建严格适配蛊真人本体 9.8；" + detail);
    }
}
