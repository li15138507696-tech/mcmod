package net.guzhenren.tianfu.compat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.guzhenren.tianfu.v2.NeoForge1211Adapter;
import net.guzhenren.tianfu.v2.TianfuSelection;
import net.minecraft.server.level.ServerPlayer;

/** Dedicated fixture source set: real adapter, stubbed Minecraft fields/storage, no server. */
public final class G98ConstraintsRegression {
    private static int checks;
    public static void main(String[] args) {
        for (int mask=0;mask<8;mask++) {
            var ids=new ArrayList<String>();
            if((mask&1)!=0)ids.add("v2_luck_forfeit");
            if((mask&2)!=0)ids.add("v2_aptitude_shackled");
            if((mask&4)!=0)ids.add("v2_lifespan_scar");
            ids.add("unknown_saved_id");
            for(boolean legacy:new boolean[]{false,true}) {
                var p=fixture();
                if(legacy) new NeoForge1211Adapter().maintain(p,snapshot(ids));
                else NeoForge1211Adapter.maintainConstraints(p,Set.copyOf(ids));
                eq((mask&1)!=0?0:12,p.vars.qiyun,"luck current");
                eq((mask&1)!=0?0:30,p.vars.qiyun_shangxian,"luck cap");
                eq((mask&2)!=0?5:2,p.vars.kongqiao,"aptitude lock");
                eq((mask&4)!=0?80:120,p.vars.shouyuan,"lifespan cap");
                eq(500,p.vars.niantou_rongliang,"maintenance never charges capacity again");
                int dirty=p.vars.syncs;
                NeoForge1211Adapter.maintainConstraints(p,ids);
                eq(dirty,p.vars.syncs,"unchanged constraints do not resync");
            }
        }
        var p=fixture(); p.vars.shouyuan=70;
        NeoForge1211Adapter.maintainConstraints(p,List.of("v2_lifespan_scar"));
        eq(70,p.data.get("v2_lifespan_cap"),"native lifespan decrease lowers stored cap");
        p.vars.shouyuan=75;
        NeoForge1211Adapter.maintainConstraints(p,List.of("v2_lifespan_scar"));
        eq(70,p.vars.shouyuan,"later recovery cannot bypass lowered cap");
        p=fixture();
        NeoForge1211Adapter.maintainConstraints(p,List.of());
        eq(0,p.fetches,"no constraints skip native attachment lookup");
        p=fixture();p.data.clear();
        NeoForge1211Adapter.maintainConstraints(p,List.of("v2_aptitude_shackled","v2_lifespan_scar"));
        eq(2,p.vars.kongqiao,"missing legacy lock is not invented");
        eq(120,p.vars.shouyuan,"missing legacy cap is not invented");
        System.out.println("PASS: "+checks+" constraint behavior checks using local fixtures; no server started");
    }
    private static ServerPlayer fixture() {
        var p=new ServerPlayer();p.vars.qiyun=12;p.vars.qiyun_shangxian=30;
        p.vars.kongqiao=2;p.vars.shouyuan=120;p.vars.niantou_rongliang=500;
        p.data.put("v2_lock_kongqiao",5.0);p.data.put("v2_lifespan_cap",80.0);
        return p;
    }
    private static TianfuSelection.Snapshot snapshot(List<String> ids) {
        return new TianfuSelection.Snapshot(2,1,100,0,true,new TianfuSelection.Origin("",""),
                null,ids,Map.of(),0,Set.of());
    }
    private static void eq(double expected,double actual,String message) {
        checks++;if(Double.compare(expected,actual)!=0)throw new AssertionError(message+": "+actual+" expected "+expected);
    }
}
