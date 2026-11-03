$ErrorActionPreference='Stop'
$root=(Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$out=Join-Path $root 'build\constraints-g98'
New-Item -ItemType Directory -Force $out|Out-Null
$stubs=@{
'net/guzhenren/network/GuzhenrenModVariables.java'=@'
package net.guzhenren.network;
public final class GuzhenrenModVariables {
 public static final Object PLAYER_VARIABLES=new Object();
 public static class PlayerVariables {
  public double shouyuan,kongqiao,yuanshishuliang,qiyun_shangxian,qiyun,niantou_rongliang;
  public int syncs; public void markSyncDirty(){syncs++;}
 }
}
'@
'net/minecraft/server/level/ServerPlayer.java'=@'
package net.minecraft.server.level;
import java.util.Map; import java.util.HashMap;
import net.guzhenren.network.GuzhenrenModVariables.PlayerVariables;
public class ServerPlayer {
 public final PlayerVariables vars=new PlayerVariables();
 public final Map<String,Double> data=new HashMap<>(); public int fetches;
 public PlayerVariables getData(Object key){fetches++;return vars;}
}
'@
'net/guzhenren/tianfu/TianfuData.java'=@'
package net.guzhenren.tianfu;
import net.minecraft.server.level.ServerPlayer;
public final class TianfuData {
 public static double getDouble(ServerPlayer p,String key,double fallback){return p.data.getOrDefault(key,fallback);}
 public static void putDouble(ServerPlayer p,String key,double value){p.data.put(key,value);}
}
'@
'net/guzhenren/tianfu/TianfuTianyiCompat.java'=@'
package net.guzhenren.tianfu;
public final class TianfuTianyiCompat { public static void explainLifespanCapOnce(){} }
'@
}
foreach($entry in $stubs.GetEnumerator()) {
    $path=Join-Path $out ('stubs\'+$entry.Key)
    New-Item -ItemType Directory -Force (Split-Path $path)|Out-Null
    [IO.File]::WriteAllText($path,$entry.Value,[Text.UTF8Encoding]::new($false))
}
$sources=@(Get-ChildItem (Join-Path $out 'stubs') -Recurse -Filter *.java|ForEach-Object FullName)
$sources+=@('v2/NeoForge1211Adapter.java','v2/VersionAdapter.java','v2/TianfuSelection.java','v2/TianfuOriginTable.java','compat/Guzhenren98Settlement.java')|ForEach-Object {Join-Path $root ('src/main/java/net/guzhenren/tianfu/'+$_)}
$sources+=Join-Path $root 'src/fixtureTest/java/net/guzhenren/tianfu/compat/G98ConstraintsRegression.java'
$classes=Join-Path $out 'classes'
New-Item -ItemType Directory -Force $classes|Out-Null
$jdk='C:\Users\ADMIN\.jdks\temurin-21\bin'
& (Join-Path $jdk 'javac.exe') -encoding UTF-8 -proc:none -d $classes @sources
if($LASTEXITCODE -ne 0){throw 'Constraint fixture compilation failed.'}
& (Join-Path $jdk 'java.exe') '-Dfile.encoding=UTF-8' -cp $classes net.guzhenren.tianfu.compat.G98ConstraintsRegression
if($LASTEXITCODE -ne 0){throw 'Constraint fixture regression failed.'}
