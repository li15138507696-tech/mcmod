package net.guzhenren.tianfu;

import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;

/**
 * 玩家的「主修流派」（1.4.18 重做）。
 *
 * <p><b>本体没有这个概念</b>：只有 46 个 {@code liupai_*} 熟练度与 44 个 {@code daohen_*}
 * 道痕。「数值最高那条」只代表练得最多，不代表想专精（兼修玩家会被误判）。
 *
 * <p>取值优先级：
 * <ol>
 *   <li><b>tianfu 自己持久化的主修</b>（{@link #set}，存在 {@code PersistentData/tianfu/mainDao}）
 *       —— 玩家明确选择过，最权威；</li>
 *   <li><b>道途的 {@code daotu.mainDao}</b>（装了道途且玩家设过）；</li>
 *   <li><b>唯一最高</b>的 {@code liupai_*} 熟练度 —— 仅当最高值<b>唯一</b>时采用。</li>
 * </ol>
 *
 * <p>1.4.18 修正：旧实现用 {@code value > bestValue} 严格大于取最大，并列时静默落到
 * {@code KEYS} 首位（{@code jiandao}），等于把并列玩家判成剑道；全 0 时返回空串。
 * 现在改为**并列返回空串**，由调用方要求玩家选择（任务书 2.4 / 2.5）。
 *
 * <p>主修<b>一经确定不会被自动改写</b>：即使其它流派数值后来超过它，{@link #of} 仍然返回
 * 记录值（任务书 2.6）——因为 1、2 两级都是显式记录，第 3 级的自动识别只在完全没有记录时生效。
 *
 * <p>29 项特殊流派不参与主修识别：{@link #KEYS} 只含 15 项主修候选，第 3 级自动识别只会在这
 * 15 项里挑（任务书 2.7）。
 */
public final class TianfuMainDao {

    /** 道途的 NBT 位置（{@code DaotuData.ROOT} / {@code DaotuMainDao.KEY}）。 */
    private static final String DAOTU_ROOT = "daotu";
    private static final String DAOTU_KEY = "mainDao";
    /** tianfu 自己持久化的主修键。 */
    public static final String OWN_KEY = "mainDao";

    /**
     * 可选主修流派（15 项）。
     *
     * <p>只收本体<b>两侧字段都真实存在</b>的（{@code daohen_*} 与 {@code liupai_*} 都要有）。
     * 注意：44 项其实两侧都存在，这 15 项是**人为筛选的主修候选**，其余 29 项属特殊流派。
     * 已对 9.8 与 10.20 两版 {@code PlayerVariables} 逐条核对。
     */
    public static final java.util.List<String> KEYS = java.util.List.of(
            "jiandao", "jindao", "shuidao", "xuedao", "lidao", "mudao", "yudao",
            "fengdao", "bingdao", "jindao2", "hundao", "leidao", "yandao", "lvdao", "tudao");

    private TianfuMainDao() {
    }

    /** 当前主修流派键；空串表示**未设定/并列/全 0**，调用方必须让玩家选择。 */
    public static String of(ServerPlayer player, PlayerVariables v) {
        String own = recorded(player);
        if (!own.isEmpty()) {
            return own;
        }
        String fromDaotu = fromDaotu(player);
        if (!fromDaotu.isEmpty()) {
            return fromDaotu;
        }
        return uniqueHighest(v);
    }

    /** tianfu 自己记录的主修；没有或非法返回空串。 */
    public static String recorded(ServerPlayer player) {
        return valid(TianfuData.getString(player, OWN_KEY, ""));
    }

    /**
     * 写入主修（玩家明确选择）。
     *
     * @param key 必须是 {@link #KEYS} 之一
     * @return 是否写入成功
     */
    public static boolean set(ServerPlayer player, String key) {
        if (player == null || !KEYS.contains(key)) {
            return false;
        }
        TianfuData.putString(player, OWN_KEY, key);
        return true;
    }

    /** 清空 tianfu 记录的主修（回退到道途/唯一最高）。 */
    public static void clear(ServerPlayer player) {
        TianfuData.removeKey(player, OWN_KEY);
    }

    /** 读道途的主修选择；没装道途、没设过、或键不在清单里都返回空串。 */
    static String fromDaotu(ServerPlayer player) {
        try {
            CompoundTag pd = player.getPersistentData();
            if (!pd.contains(DAOTU_ROOT, Tag.TAG_COMPOUND)) {
                return "";
            }
            return valid(pd.getCompound(DAOTU_ROOT).getString(DAOTU_KEY));
        } catch (Throwable t) {
            return ""; // 道途数据结构变了也不该影响主修
        }
    }

    /**
     * 熟练度最高且**唯一**的那一路；并列或全 0 都返回空串。
     *
     * <p>并列返回空串是 1.4.18 的关键修正：旧实现用严格大于取最大，并列时会静默保留
     * 先遍历到的 {@code jiandao}，等于把并列玩家判成剑道。
     *
     * <p>只遍历 {@link #KEYS}（15 项主修候选），因此 29 项特殊流派即使数值再高也不参与识别。
     */
    static String uniqueHighest(PlayerVariables v) {
        return uniqueHighest(key -> liupai(v, key));
    }

    /** 可注入访问器的重载：让并列判定能脱离 {@code PlayerVariables} 单测。 */
    static String uniqueHighest(java.util.function.ToDoubleFunction<String> liupaiByKey) {
        String best = "";
        double bestValue = 0.0;
        int ties = 0;
        for (String key : KEYS) {
            double value = liupaiByKey.applyAsDouble(key);
            // 非有限值与 ≤0 都不参与：负熟练度不该被当成「最高」。
            if (!(value > 0.0) || !Double.isFinite(value)) {
                continue;
            }
            if (value > bestValue) {
                bestValue = value;
                best = key;
                ties = 1;
            } else if (value == bestValue) {
                ties++;
            }
        }
        return ties == 1 ? best : "";
    }

    /** 只接受 15 项主修候选；其余（含 29 项特殊流派）一律视为非法。 */
    private static String valid(String key) {
        return key != null && KEYS.contains(key) ? key : "";
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
