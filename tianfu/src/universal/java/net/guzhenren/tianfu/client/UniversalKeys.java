package net.guzhenren.tianfu.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@EventBusSubscriber(modid="tianfu", bus=EventBusSubscriber.Bus.MOD, value=Dist.CLIENT)
public final class UniversalKeys {
    private static final KeyMapping OPEN = new KeyMapping("key.tianfu.universal", InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(), "key.categories.misc");
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent e) {
        e.register(OPEN); NeoForge.EVENT_BUS.addListener(UniversalKeys::tick);
    }
    private static void tick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        while (OPEN.consumeClick()) if (mc.screen == null && mc.player != null) mc.player.connection.sendCommand("tianfu open");
    }
}
