package com.endo.gitanoland.client;

import com.endo.gitanoland.ClientConfig;
import com.endo.gitanoland.GitanoLand;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.IGun;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.settings.IKeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.slf4j.Logger;

/**
 * Evita que teclas de distintos mods se pisen reasignando su contexto según lo que estás haciendo:
 * <ul>
 *   <li>Teclas de Ice and Fire ({@code key.dragon_*}): solo activas montado en un dragón.</li>
 *   <li>Teclas de cámara de SecurityCraft: solo activas viendo una cámara.</li>
 *   <li>Teclas de otros mods (no vanilla, no TaCZ): se desactivan mientras llevas un arma de TaCZ en la mano,
 *       montas un dragón o miras una cámara. Así R, Z, G, H... son del arma cuando la llevas y del mod cuando no.</li>
 *   <li>Teclas de TaCZ: se quedan como estaban (su código ya comprueba el arma); solo se marcan para que
 *       Controles no las dé por conflicto con los demás mods.</li>
 * </ul>
 * Solo toca teclas cuyo contexto es el de juego normal; lo demás (pantallas, universales) queda como está.
 * Los conflictos que sigan existiendo entre mods "libres" se escriben en el log.
 */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public final class KeyContexts {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> VANILLA_KEYS = Set.of("key.attack", "key.use", "key.forward", "key.left", "key.back",
            "key.right", "key.jump", "key.sneak", "key.sprint", "key.drop", "key.inventory", "key.swapOffhand", "key.chat",
            "key.playerlist", "key.pickItem", "key.command", "key.socialInteractions", "key.screenshot",
            "key.togglePerspective", "key.smoothCamera", "key.fullscreen", "key.spectatorOutlines", "key.advancements",
            "key.saveToolbarActivator", "key.loadToolbarActivator");

    private static boolean isVanilla(String name) {
        return VANILLA_KEYS.contains(name) || name.startsWith("key.hotbar.");
    }

    private static String combo(KeyMapping k) {
        return k.getKeyModifier() + "+" + k.getKey().getName();
    }

    private static boolean applied = false;

    // ------------------------------------------------------------------ estado del jugador

    private static boolean holdingGun() {
        Player p = Minecraft.getInstance().player;
        return p != null && IGun.mainhandHoldGun(p);
    }

    private static boolean ridingDragon() {
        Player p = Minecraft.getInstance().player;
        Entity v = p == null ? null : p.getVehicle();
        return v != null && "iceandfire".equals(BuiltInRegistries.ENTITY_TYPE.getKey(v.getType()).getNamespace());
    }

    private static boolean viewingSecurityCamera() {
        Minecraft mc = Minecraft.getInstance();
        Entity cam = mc.getCameraEntity();
        return cam != null && cam != mc.player
                && "securitycraft".equals(BuiltInRegistries.ENTITY_TYPE.getKey(cam.getType()).getNamespace());
    }

    // ------------------------------------------------------------------ contextos

    private enum Ctx implements IKeyConflictContext {
        /** Teclas de TaCZ: se comportan como "en juego", pero no chocan con las de otros mods. */
        TACZ {
            @Override public boolean isActive() { return KeyConflictContext.IN_GAME.isActive(); }
        },
        /** Teclas de mods "libres": en juego y sin arma, dragón ni cámara. */
        FREE {
            @Override public boolean isActive() {
                return KeyConflictContext.IN_GAME.isActive() && !holdingGun() && !ridingDragon() && !viewingSecurityCamera();
            }
        },
        DRAGON {
            @Override public boolean isActive() { return KeyConflictContext.IN_GAME.isActive() && ridingDragon(); }
        },
        CAMERA {
            @Override public boolean isActive() { return viewingSecurityCamera(); }
        };

        /** Chocan con las de su mismo contexto y con las de vanilla (IN_GAME) o universales. */
        @Override
        public boolean conflicts(IKeyConflictContext other) {
            return other == this || other == KeyConflictContext.IN_GAME || other == KeyConflictContext.UNIVERSAL;
        }
    }

    // ------------------------------------------------------------------ aplicación

    /** Idempotente: se llama al arrancar y otra vez en el primer tick, por si algún mod registra teclas tarde. */
    public static void apply() {
        if (!ClientConfig.FIX_KEY_CONTEXTS.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        // 1) Teclas con contexto propio: TaCZ, dragon y camara.
        int tacz = 0, dragon = 0, camera = 0;
        Set<String> special = new HashSet<>();
        for (KeyMapping k : mc.options.keyMappings) {
            if (k.getKeyConflictContext() != KeyConflictContext.IN_GAME) continue;
            String name = k.getName();
            if (isVanilla(name) || name.startsWith("key.gitanoland")) continue;
            if (name.contains("tacz")) {
                k.setKeyConflictContext(Ctx.TACZ);
                special.add(combo(k));
                tacz++;
            } else if (name.startsWith("key.dragon_")) {
                k.setKeyConflictContext(Ctx.DRAGON);
                special.add(combo(k));
                dragon++;
            } else if (name.startsWith("key.securitycraft.camera") || name.equals("key.securitycraft.setDefaultViewingDirection")) {
                k.setKeyConflictContext(Ctx.CAMERA);
                special.add(combo(k));
                camera++;
            }
        }
        // 2) Teclas de otros mods que chocan con alguna de las anteriores: solo valen cuando no hay arma/dragon/camara.
        int free = 0;
        List<KeyMapping> others = new ArrayList<>();
        for (KeyMapping k : mc.options.keyMappings) {
            if (k.getKeyConflictContext() != KeyConflictContext.IN_GAME) continue;
            String name = k.getName();
            if (isVanilla(name) || name.startsWith("key.gitanoland")) continue;
            if (special.contains(combo(k))) {
                k.setKeyConflictContext(Ctx.FREE);
                free++;
            }
            others.add(k);
        }
        if (tacz + dragon + camera + free == 0) return;
        KeyMapping.resetMapping();
        if (!applied) {
            applied = true;
            LOG.info("[GitanoLand] Contextos de teclas: {} de TaCZ, {} de dragon, {} de camara, {} de otros mods movidas a 'sin arma'.",
                    tacz, dragon, camera, free);
            logLeftovers(mc.options.keyMappings);
        }
    }

    /** Teclas de mods distintos (o de vanilla) que siguen compartiendo tecla y contexto: hay que cambiarlas a mano. */
    private static void logLeftovers(KeyMapping[] keys) {
        for (int i = 0; i < keys.length; i++) {
            for (int j = i + 1; j < keys.length; j++) {
                KeyMapping a = keys[i], b = keys[j];
                if (a.getKey().equals(b.getKey()) && a.getKeyModifier() == b.getKeyModifier()
                        && a.getKey() != com.mojang.blaze3d.platform.InputConstants.UNKNOWN
                        && (a.getKeyConflictContext().conflicts(b.getKeyConflictContext())
                        || b.getKeyConflictContext().conflicts(a.getKeyConflictContext()))) {
                    LOG.info("[GitanoLand] Conflicto restante en {}: {} <-> {}", a.getKey().getDisplayName().getString(),
                            a.getName(), b.getName());
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().player != null && !applied) apply();
    }
}
