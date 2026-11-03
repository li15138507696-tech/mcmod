package net.guzhenren.tianfu;

import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家的「主修流派」。
 *
 * <p><b>本体没有这个概念</b>：只有 46 个 {@code liupai_*} 熟练度与 36 个 {@code daohen_*} 道痕。
 * 「数值最高那条」只代表练得最多，不代表想专精（兼修玩家会被误判）——
 * 所以道途让玩家手动指定，存在 {@code PersistentData/daotu/mainDao}。
 *
 * <p>本类按优先级取：
 * <ol>
 *   <li><b>道途的选择</b>（装了道途且玩家设过）——最准；</li>
 *   <li>否则退回 {@code liupai_*} 熟练度最高的那一路——不依赖任何其他 mod；</li>
 *   <li>都没有（全 0）返回空串，调用方按「未设定」处理。</li>
 * </ol>
 *
 * <p>只读，不写任何东西，也不碰道途的 NBT。
 */
public final class TianfuMainDao {

    /** 道途的 NBT 位置（{@code DaotuData.ROOT} / {@code DaotuMainDao.KEY}）。 */
    private static final String DAOTU_ROOT = "daotu";
    private static final String DAOTU_KEY = "mainDao";

    /**
     * 可选主修流派。
     *
     * <p>只收本体<b>两侧字段都真实存在</b>的（{@code daohen_*} 与 {@code liupai_*} 都要有），
     * 与道途的 15 项清单一致——缺一侧就意味着「道痕给不了」。
     * 已对 9.8 与 10.20 两版 {@code PlayerVariables} 逐条核对。
     */
    public static final java.util.List<String> KEYS = java.util.List.of(
            "jiandao", "jindao", "shuidao", "xuedao", "lidao", "mudao", "yudao",
            "fengdao", "bingdao", "jindao2", "hundao", "leidao", "yandao", "lvdao", "tudao");

    private TianfuMainDao() {
    }

    /** 当前主修流派键；空串表示未设定。 */
    public static String of(ServerPlayer player, PlayerVariables v) {
        String fromDaotu = fromDaotu(player);
        return fromDaotu.isEmpty() ? highest(v) : fromDaotu;
    }

    /** 读道途的主修选择；没装道途、没设过、或键不在清单里都返回空串。 */
    static String fromDaotu(ServerPlayer player) {
        try {
            CompoundTag pd = player.getPersistentData();
            if (!pd.contains(DAOTU_ROOT, Tag.TAG_COMPOUND)) {
                return "";
            }
            String key = pd.getCompound(DAOTU_ROOT).getString(DAOTU_KEY);
            return KEYS.contains(key) ? key : "";
        } catch (Throwable t) {
            return ""; // 道途数据结构变了也不该影响杀神
        }
    }

    /** 熟练度最高的那一路；全为 0 时返回空串。 */
    static String highest(PlayerVariables v) {
        String best = "";
        double bestValue = 0.0;
        for (String key : KEYS) {
            double value = liupai(v, key);
            if (value > bestValue) {
                bestValue = value;
                best = key;
            }
        }
        return best;
    }

    /** 读本体 {@code liupai_*} 熟练度。 */
    static double liupai(PlayerVariables v, String key) {
        return switch (key) {
            case "jiandao" -> v.liupai_jiandao;
            case "jindao" -> v.liupai_jindao;
            case "shuidao" -> v.liupai_shuidao;
            case "xuedao" -> v.liupai_xuedao;
            case "lidao" -> v.liupai_lidao;
            case "mudao" -> v.liupai_mudao;
            case "yudao" -> v.liupai_yudao;
            case "fengdao" -> v.liupai_fengdao;
            case "bingdao" -> v.liupai_bingdao;
            case "jindao2" -> v.liupai_jindao2;
            case "hundao" -> v.liupai_hundao;
            case "leidao" -> v.liupai_leidao;
            case "yandao" -> v.liupai_yandao;
            case "lvdao" -> v.liupai_lvdao;
            case "tudao" -> v.liupai_tudao;
            default -> 0.0;
        };
    }
}
