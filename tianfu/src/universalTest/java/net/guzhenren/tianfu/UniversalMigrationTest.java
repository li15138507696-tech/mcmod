package net.guzhenren.tianfu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

public final class UniversalMigrationTest {
    private static void check(boolean b) { if (!b) throw new AssertionError(); }
    private static CompoundTag legacy() {
        CompoundTag p = new CompoundTag(); CompoundTag old = new CompoundTag();
        old.putBoolean("applied", true); old.put("v2_receipts", new CompoundTag()); p.put("tianfu", old); return p;
    }
    private static CompoundTag snapshot(CompoundTag p) {
        CompoundTag s = new CompoundTag(); s.putString("source", "legacy_snapshot"); s.putBoolean("initialized", true);
        s.putInt("points", 0); s.put("owned", new ListTag()); p.put("tianfu_universal", s); return s;
    }
    private static CompoundTag receipt() { CompoundTag r = new CompoundTag(); r.putString("id", "origin_test"); r.putInt("paidCost", 2); return r; }
    public static void main(String[] args) {
        CompoundTag p = legacy(); CompoundTag before = p.getCompound("tianfu").copy();
        CompoundTag s = TianfuUniversal.migrate(p); check(s.getInt("points") == 8); check(p.getCompound("tianfu").equals(before));
        p = legacy(); s = snapshot(p); TianfuUniversal.migrate(p); check(s.getInt("points") == 8);
        s.putInt("points", 0); TianfuUniversal.migrate(p); check(s.getInt("points") == 0);
        p = legacy(); s = snapshot(p); CompoundTag wrapped = new CompoundTag(); wrapped.put("origin", receipt());
        p.getCompound("tianfu").put("v2_origin_receipt", wrapped); TianfuUniversal.migrate(p); check(s.getInt("points") == 0);
        p = legacy(); s = snapshot(p); CompoundTag receipts = new CompoundTag(); receipts.put("x", receipt());
        p.getCompound("tianfu").put("v2_receipts", receipts); TianfuUniversal.migrate(p); check(s.getInt("points") == 0);
        p = legacy(); s = snapshot(p); ListTag purchased = new ListTag(); purchased.add(StringTag.valueOf("some_old_id"));
        p.getCompound("tianfu").put("purchased", purchased); TianfuUniversal.migrate(p); check(s.getInt("points") == 0);
        p = legacy(); s = snapshot(p); ListTag owned = new ListTag(); owned.add(StringTag.valueOf("jingu_qiangjian"));
        s.put("owned", owned); TianfuUniversal.migrate(p); check(s.getInt("points") == 0);
        System.out.println("UniversalMigrationTest passed (8 checks)");
    }
}
