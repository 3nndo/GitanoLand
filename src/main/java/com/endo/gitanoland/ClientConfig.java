package com.endo.gitanoland;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Ajustes del cliente (config/gitanoland-client.toml). */
public class ClientConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.DoubleValue EFFECT_STRENGTH = B
            .comment("Multiplica la intensidad del efecto visual de Smokeleaf (1.0 = el original del mod).")
            .defineInRange("stonedEffectStrength", 1.5, 0.1, 3.0);

    public static final ModConfigSpec SPEC = B.build();
}
