package com.endo.gitanoland.client;

import com.endo.gitanoland.ClientConfig;
import com.endo.gitanoland.GitanoLand;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.tacz.guns.api.item.IGun;
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
 * Evita que teclas de distintos mods se pisen segun lo que estas haciendo.
 *
 * <p>Cada tecla ajena que comparte combinacion con una tecla de TaCZ, de dragon (Ice and Fire) o de camara
 * (SecurityCraft) recibe un contexto que envuelve el suyo original y solo le anade una condicion:
 * <ul>
 *   <li>choca con una tecla de TaCZ: no vale mientras llevas un arma de TaCZ en la mano;</li>
 *   <li>choca con una de dragon: no vale montado en un dragon;</li>
 *   <li>choca con una de camara: no vale mirando una camara de SecurityCraft.</li>
 * </ul>
 * Las teclas de TaCZ, dragon y camara conservan su contexto y su comportamiento; solo se marcan para que
 * Controles no las de por conflicto con las anteriores. Vanilla no se toca. Funciona sea cual sea el contexto
 * que use cada mod (en juego, universal o propio), salvo los de pantalla.
 *
 * <p>Los choques que sigan existiendo (entre mods que no tienen una condicion distinta) se escriben en el log.
 */
@EventBusSubscriber(modid = GitanoLand.MODID, value = Dist.CLIENT)
public final class KeyContexts {
    private static final Logger LOG = LogUtils.getLogger();
    private static final Set<String> VANILLA_KEYS = Set.of("key.attack", "key.use", "key.forward", "key.left", "key.back",
            "key.right", "key.jump", "key.sneak", "key.sprint", "key.drop", "key.inventory", "key.swapOffhand", "key.chat",
            "key.playerlist", "key.pickItem", "key.command", "key.socialInteractions", "key.screenshot",
            "key.togglePerspective", "key.smoothCamera", "key.fullscreen", "key.spectatorOutlines", "key.advancements",
            "key.saveToolbarActivator", "key.loadToolbarActivator");

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

    // ------------------------------------------------------------------ contexto envolvente

    private enum Kind { TACZ, DRAGON, CAMERA, FREE }

    /** Teclas "que mandan" en cada situacion (se rellenan al arrancar). */
    private static final java.util.List<KeyMapping> TACZ_KEYS = new java.util.ArrayList<>();
    private static final java.util.List<KeyMapping> DRAGON_KEYS = new java.util.ArrayList<>();
    private static final java.util.List<KeyMapping> CAMERA_KEYS = new java.util.ArrayList<>();

    /** true si la tecla comparte AHORA combinacion con alguna de la lista (se mira en cada pulsacion, no al arrancar). */
    private static boolean clashes(KeyMapping owner, java.util.List<KeyMapping> list) {
        if (owner.getKey() == InputConstants.UNKNOWN) return false;
        for (KeyMapping s : list) {
            if (s != owner && s.getKey().equals(owner.getKey()) && s.getKeyModifier() == owner.getKeyModifier()) return true;
        }
        return false;
    }

    /** Envuelve el contexto original de una tecla. Las "FREE" se excluyen solo mientras choquen con una tecla especial. */
    private static final class Ctx implements IKeyConflictContext {
        final Kind kind;
        final IKeyConflictContext delegate;
        final KeyMapping owner;

        Ctx(Kind kind, IKeyConflictContext delegate, KeyMapping owner) {
            this.kind = kind;
            this.delegate = delegate;
            this.owner = owner;
        }

        @Override
        public boolean isActive() {
            if (!delegate.isActive()) return false;
            if (kind != Kind.FREE) return true;
            return !(clashes(owner, TACZ_KEYS) && holdingGun())
                    && !(clashes(owner, DRAGON_KEYS) && ridingDragon())
                    && !(clashes(owner, CAMERA_KEYS) && viewingSecurityCamera());
        }

        @Override
        public boolean conflicts(IKeyConflictContext other) {
            if (other instanceof Ctx o) {
                // Una tecla "libre" y una especial con la misma combinacion no se pisan: manda cada una en su situacion.
                if ((kind == Kind.FREE) != (o.kind == Kind.FREE)) return false;
                return delegate.conflicts(o.delegate);
            }
            return delegate.conflicts(other);
        }
    }

    private static Kind specialKind(String name) {
        if (name.contains("tacz")) return Kind.TACZ;
        if (name.startsWith("key.dragon_")) return Kind.DRAGON;
        if (name.startsWith("key.securitycraft.camera") || name.equals("key.securitycraft.setDefaultViewingDirection")) return Kind.CAMERA;
        return null;
    }

    private static boolean isVanilla(String name) {
        return VANILLA_KEYS.contains(name) || name.startsWith("key.hotbar.");
    }

    private static boolean untouchable(KeyMapping k) {
        String name = k.getName();
        return isVanilla(name) || name.startsWith("key.gitanoland") || k.getKeyConflictContext() == KeyConflictContext.GUI;
    }

    private static String combo(KeyMapping k) {
        return k.getKeyModifier() + "+" + k.getKey().getName();
    }

    private static String ctxName(IKeyConflictContext c) {
        return c instanceof Enum<?> e ? e.name() : c.getClass().getName();
    }

    // ------------------------------------------------------------------ aplicacion

    /** Idempotente: se llama al arrancar y otra vez en el primer tick, por si algun mod registra teclas tarde. */
    public static void apply() {
        if (!ClientConfig.FIX_KEY_CONTEXTS.get()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;
        KeyMapping[] all = mc.options.keyMappings;

        // 1) Marcar las teclas de TaCZ, dragon y camara (conservan su contexto y comportamiento).
        int marked = 0;
        for (KeyMapping k : all) {
            if (untouchable(k) || k.getKeyConflictContext() instanceof Ctx) continue;
            Kind kind = specialKind(k.getName());
            if (kind == null) continue;
            k.setKeyConflictContext(new Ctx(kind, k.getKeyConflictContext(), k));
            (switch (kind) { case TACZ -> TACZ_KEYS; case DRAGON -> DRAGON_KEYS; default -> CAMERA_KEYS; }).add(k);
            marked++;
        }

        // 2) Resto de teclas de mods: se excluyen solo mientras comparten combinacion con una de las anteriores.
        int free = 0;
        for (KeyMapping k : all) {
            if (untouchable(k) || k.getKeyConflictContext() instanceof Ctx) continue;
            k.setKeyConflictContext(new Ctx(Kind.FREE, k.getKeyConflictContext(), k));
            free++;
        }
        if (marked + free == 0) return;
        KeyMapping.resetMapping();
        if (!applied) {
            applied = true;
            LOG.info("[GitanoLand] Contextos de teclas: {} de TaCZ/dragon/camara marcadas, {} de otros mods vigiladas (solo se excluyen mientras chocan).", marked, free);
            logLeftovers(all);
        }
    }

    /** Teclas que siguen compartiendo combinacion y contexto: hay que cambiarlas a mano. */
    private static void logLeftovers(KeyMapping[] keys) {
        for (int i = 0; i < keys.length; i++) {
            for (int j = i + 1; j < keys.length; j++) {
                KeyMapping a = keys[i], b = keys[j];
                if (a.getKey() != InputConstants.UNKNOWN && a.getKey().equals(b.getKey()) && a.getKeyModifier() == b.getKeyModifier()
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
