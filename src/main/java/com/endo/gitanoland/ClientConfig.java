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

    public static final ModConfigSpec SPEC = B.build();
}
