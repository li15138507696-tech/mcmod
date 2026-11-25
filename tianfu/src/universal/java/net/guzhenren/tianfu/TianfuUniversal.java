package net.guzhenren.tianfu;

import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.commands.Commands;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@Mod("tianfu")
public final class TianfuUniversal {
    private static final String KEY = "tianfu_universal";
    public TianfuUniversal(IEventBus bus) {
        bus.addListener(TianfuUniversal::registerPayload);
        NeoForge.EVENT_BUS.addListener(TianfuUniversal::commands);
        NeoForge.EVENT_BUS.addListener(TianfuUniversal::login);
        NeoForge.EVENT_BUS.addListener(TianfuUniversal::clonePlayer);
        NeoForge.EVENT_BUS.addListener(TianfuUniversal::respawn);
        NeoForge.EVENT_BUS.addListener(TianfuUniversal::tick);
    }
    private static void registerPayload(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent e) {
        e.registrar("1.4.20-universal").playToClient(UniversalState.TYPE, UniversalState.CODEC, UniversalState::handle);
    }
    private static boolean actualReceipt(CompoundTag r) {
        return r.contains("id", Tag.TAG_STRING) && !r.getString("id").isEmpty()
                && r.contains("paidCost", Tag.TAG_ANY_NUMERIC);
    }
    private static boolean hasActualReceipts(CompoundTag old) {
        CompoundTag receipts = old.getCompound("v2_receipts");
        for (String key : receipts.getAllKeys())
            if (receipts.contains(key, Tag.TAG_COMPOUND) && actualReceipt(receipts.getCompound(key))) return true;
        if (!old.contains("v2_origin_receipt", Tag.TAG_COMPOUND)) return false;
        CompoundTag origin = old.getCompound("v2_origin_receipt");
        if (origin.contains("origin", Tag.TAG_COMPOUND)) origin = origin.getCompound("origin");
        return actualReceipt(origin);
    }
    private static CompoundTag data(ServerPlayer p) {
        return migrate(p.getPersistentData());
    }
    static CompoundTag migrate(CompoundTag persistent) {
        if (!persistent.contains(KEY, Tag.TAG_COMPOUND) || !persistent.getCompound(KEY).getBoolean("migration_1420")) {
            boolean existing = persistent.contains(KEY, Tag.TAG_COMPOUND);
            CompoundTag fresh = existing ? persistent.getCompound(KEY) : new CompoundTag();
            boolean legacy = persistent.contains("tianfu", Tag.TAG_COMPOUND);
            CompoundTag old = legacy ? persistent.getCompound("tianfu") : new CompoundTag();
            boolean validDifficulty = old.contains("difficulty", Tag.TAG_INT) && old.getInt("difficulty") >= 0 && old.getInt("difficulty") < 4;
            ListTag oldPurchased = old.getList("purchased", Tag.TAG_STRING);
            boolean hasSelections = !oldPurchased.isEmpty() || old.getBoolean("confirmed") || hasActualReceipts(old);
            java.util.ArrayList<String> ids = new java.util.ArrayList<>();
            for (int i = 0; i < oldPurchased.size(); i++) ids.add(oldPurchased.getString(i));
            if (!existing) {
                fresh.putInt("points", UniversalRules.initialPoints(legacy, validDifficulty, hasSelections, old.getInt("points")));
                ListTag imported = new ListTag();
                for (String id : UniversalRules.importOwned(old.getBoolean("confirmed"), ids)) imported.add(StringTag.valueOf(id));
                fresh.put("owned", imported);
                fresh.putString("source", legacy ? "legacy_snapshot" : "new");
                fresh.putBoolean("initialized", true);
            } else if (fresh.contains("points", Tag.TAG_INT) && fresh.contains("owned", Tag.TAG_LIST)
                    && UniversalRules.repair1419(legacy, "legacy_snapshot".equals(fresh.getString("source")),
                        fresh.getBoolean("initialized"), !fresh.getList("owned", Tag.TAG_STRING).isEmpty(), fresh.getInt("points"),
                        validDifficulty, hasSelections, old.getInt("points"))) {
                fresh.putInt("points", 8);
            }
            fresh.putBoolean("migration_1420", true);
            persistent.put(KEY, fresh);
        }
        return persistent.getCompound(KEY);
    }
    private static Set<String> owned(CompoundTag d) {
        Set<String> result = new HashSet<>(); ListTag list = d.getList("owned", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) result.add(list.getString(i));
        return result;
    }
    private static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("tianfu").requires(s -> s.isPlayer())
            .executes(c -> show(c.getSource().getPlayerOrException(), true))
            .then(Commands.literal("open").executes(c -> show(c.getSource().getPlayerOrException(), true)))
            .then(Commands.literal("buy").then(Commands.argument("talent", StringArgumentType.word())
                .suggests((c, b) -> { for (var t : UniversalRules.CATALOG) b.suggest(t.id()); return b.buildFuture(); })
                .executes(c -> buy(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "talent"))))));
    }
    private static int show(ServerPlayer p, boolean open) {
        CompoundTag d = data(p); Set<String> ids = owned(d);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(p, new UniversalState(d.getInt("points"), List.copyOf(ids), open));
        return 1;
    }
    private static int buy(ServerPlayer p, String id) {
        var talent = UniversalRules.find(id); CompoundTag d = data(p);
        if (talent == null || owned(d).contains(id) || d.getInt("points") < talent.cost()) {
            p.sendSystemMessage(Component.literal("§c未知天赋、已拥有或点数不足。")); return 0;
        }
        ListTag list = d.getList("owned", Tag.TAG_STRING); list.add(StringTag.valueOf(id));
        d.put("owned", list); d.putInt("points", d.getInt("points") - talent.cost());
        sync(p); show(p, false); return 1;
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    private static void respawn(PlayerEvent.PlayerRespawnEvent e) { if (e.getEntity() instanceof ServerPlayer p) sync(p); }
    private static void clonePlayer(PlayerEvent.Clone e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            for (String key : List.of(KEY, "tianfu")) {
                if (!p.getPersistentData().contains(key, Tag.TAG_COMPOUND)
                        && e.getOriginal().getPersistentData().contains(key, Tag.TAG_COMPOUND))
                    p.getPersistentData().put(key, e.getOriginal().getPersistentData().getCompound(key).copy());
            }
            if (!p.getPersistentData().contains("tianfu_generation") && e.getOriginal().getPersistentData().contains("tianfu_generation"))
                p.getPersistentData().put("tianfu_generation", e.getOriginal().getPersistentData().get("tianfu_generation").copy());
        }
    }
    private static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 100 == 0) sync(p);
    }
    private static void sync(ServerPlayer p) {
        Set<String> ids = owned(data(p));
        apply(p, Attributes.MAX_HEALTH, "health", UniversalRules.amount("health", ids));
        apply(p, Attributes.MOVEMENT_SPEED, "speed", UniversalRules.amount("speed", ids));
        apply(p, Attributes.ATTACK_DAMAGE, "attack", UniversalRules.amount("attack", ids));
        apply(p, Attributes.ATTACK_SPEED, "attack_speed", UniversalRules.amount("attack_speed", ids));
        apply(p, Attributes.KNOCKBACK_RESISTANCE, "knockback", UniversalRules.amount("knockback", ids));
    }
    private static void apply(ServerPlayer p, Holder<Attribute> attr, String key, double amount) {
        AttributeInstance a = p.getAttribute(attr); if (a == null) return;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("tianfu", "universal_" + key);
        AttributeModifier old = a.getModifier(id);
        if (amount == 0) { if (old != null) a.removeModifier(id); return; }
        if (old == null || old.amount() != amount)
            a.addOrUpdateTransientModifier(new AttributeModifier(id, amount, key.equals("knockback")
                    ? AttributeModifier.Operation.ADD_VALUE : AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }
}
