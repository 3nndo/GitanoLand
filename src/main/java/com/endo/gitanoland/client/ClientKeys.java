package com.endo.gitanoland.client;

import com.endo.gitanoland.GitanoLand;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/** Atajo del menú de recompensas. Lleva Alt de modificador para no pisar teclas sueltas de otros mods. */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public class ClientKeys {
    private static final org.slf4j.Logger LOG = com.mojang.logging.LogUtils.getLogger();
    public static final KeyMapping REWARDS = new KeyMapping("key.gitanoland.rewards", KeyConflictContext.IN_GAME,
            KeyModifier.ALT, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_K, "key.categories.gitanoland");

    @SubscribeEvent
    public static void register(RegisterKeyMappingsEvent event) {
        event.register(REWARDS);
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (REWARDS.consumeClick()) {
            if (mc.player == null) continue;
            LOG.info("[GitanoLand] Alt+K pulsado: pidiendo el menu de recompensas");
            net.neoforged.neoforge.network.PacketDistributor.sendToServer(com.endo.gitanoland.net.OpenRewardsPayload.INSTANCE);
        }
    }
}
