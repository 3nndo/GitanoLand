package com.endo.gitanoland;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Ajustes del cliente (config/gitanoland-client.toml). */
public class ClientConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue EFFECT_STRENGTH = B
            .comment("Multiplica la intensidad del efecto visual de Smokeleaf (1.0 = el original del mod).")
            .defineInRange("stonedEffectStrength", 1.5, 0.1, 3.0);

    public static final ModConfigSpec.BooleanValue FIX_KEY_CONTEXTS = B
            .comment("Evita que las teclas de distintos mods se pisen: las de Ice and Fire solo valen montado en un dragon, las de camara de SecurityCraft viendo una camara, y las de otros mods que choquen con TaCZ solo valen cuando no llevas un arma en la mano.")
            .define("fixKeyContexts", true);

    // ---- cuadro de vida del equipo de atentado (se coloca con /atentadohud mover) ----
    public static final ModConfigSpec.IntValue HUD_X = B
            .comment("Cuadro de vida del equipo: posicion X en pixeles de la interfaz (desde la izquierda). Mejor cambiarla con /atentadohud mover.")
            .defineInRange("teamHudX", 6, 0, 4000);
    public static final ModConfigSpec.IntValue HUD_Y = B
            .comment("Cuadro de vida del equipo: posicion Y en pixeles de la interfaz (desde arriba).")
            .defineInRange("teamHudY", 90, 0, 4000);
    public static final ModConfigSpec.IntValue HUD_BG_OPACITY = B
            .comment("Opacidad del fondo del cuadro de vida (0 = totalmente transparente, 100 = opaco).")
            .defineInRange("teamHudBackgroundOpacity", 0, 0, 100);
    public static final ModConfigSpec.IntValue HUD_OPACITY = B
            .comment("Opacidad de las barras y los textos del cuadro de vida (10 = casi invisible, 100 = normal).")
            .defineInRange("teamHudOpacity", 75, 10, 100);

    public static final ModConfigSpec SPEC = B.build();
}
