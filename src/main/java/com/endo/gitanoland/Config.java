package com.endo.gitanoland;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    public static final ModConfigSpec.IntValue STARTER_KIRKYS = B
            .comment("Kirkys que recibe cada jugador la primera vez que entra al mundo (0 = ninguno).",
                    "Para repetirlo con un jugador de prueba: /tag <jugador> remove gitanoland_starter")
            .defineInRange("starterKirkys", 100, 0, 64000);

    public static final ModConfigSpec.IntValue BAG_CAPACITY_SMALL = B
            .comment("Kirkys que caben en la Bolsa pequeña.")
            .defineInRange("bagCapacitySmall", 500, 1, 1_000_000);
    public static final ModConfigSpec.IntValue BAG_CAPACITY = B
            .comment("Kirkys que caben en la Bolsa de Kirkys (normal).")
            .defineInRange("bagCapacity", 1000, 1, 1_000_000);
    public static final ModConfigSpec.IntValue BAG_CAPACITY_LARGE = B
            .comment("Kirkys que caben en la Bolsa grande.")
            .defineInRange("bagCapacityLarge", 2000, 1, 1_000_000);

    public static final ModConfigSpec.BooleanValue UNLOCK_ALL_TIERS = B
            .comment("true = los aldeanos-tienda ofrecen TODOS los tratos desde el principio.",
                    "false = hay que comprar para subir de nivel y desbloquear los siguientes.")
            .define("unlockAllTiers", true);

    public static final ModConfigSpec.BooleanValue AUTO_SLOTS = B
            .comment("Si Jackpot está instalado, las tragaperras nuevas se colocan ya configuradas con Kirkys.")
            .define("autoConfigureSlotMachines", true);

    public static final ModConfigSpec.IntValue SLOT_COST = B
            .comment("Kirkys que cuesta una jugada.")
            .defineInRange("slotCost", 2, 0, 64);
    public static final ModConfigSpec.IntValue SLOT_TWO_MATCH = B
            .comment("Premio (en Kirkys) por dos iguales.")
            .defineInRange("slotTwoMatchReward", 8, 0, 64);
    public static final ModConfigSpec.IntValue SLOT_THREE_MATCH = B
            .comment("Premio (en Kirkys) por tres iguales.")
            .defineInRange("slotThreeMatchReward", 40, 0, 64);
    public static final ModConfigSpec.IntValue SLOT_JACKPOT = B
            .comment("Premio (en Kirkys) por jackpot.")
            .defineInRange("slotJackpotReward", 64, 0, 64);

    public static final ModConfigSpec.BooleanValue INFINITE_REWARDS = B
            .comment("false = la máquina paga desde un cofre enlazado (economía cerrada: los Kirkys perdidos",
                    "van al cofre y los premios salen de él). true = premios infinitos (crea Kirkys de la nada).")
            .define("infiniteRewards", false);

    public static final ModConfigSpec.DoubleValue HIT_MARKUP_CHANCE = B
            .comment("Probabilidad (0-1) de que un golpe a un aldeano-tienda le suba los precios.")
            .defineInRange("hitMarkupChance", 0.4, 0.0, 1.0);
    public static final ModConfigSpec.IntValue HIT_MARKUP_STEP = B
            .comment("Cuánto sube el recargo (en %) cada vez que un golpe le afecta. Nunca baja de 0 (nunca más barato).")
            .defineInRange("hitMarkupStep", 10, 1, 100);
    public static final ModConfigSpec.IntValue HIT_MARKUP_MAX = B
            .comment("Recargo máximo acumulable (en %).")
            .defineInRange("hitMarkupMax", 100, 1, 1000);
    public static final ModConfigSpec.IntValue HIT_MARKUP_DECAY = B
            .comment("Puntos de recargo (%) que se le pasan al aldeano por cada día del juego (0 = nunca se le pasa).")
            .defineInRange("hitMarkupDecayPerDay", 10, 0, 1000);

    public static final ModConfigSpec.DoubleValue GUNSMITH_PRICE_MULTIPLIER = B
            .comment("Multiplicador de TODOS los precios en Kirkys del Gunsmith (1.03 = +3 %).",
                    "Se redondea al Kirky más cercano, así que los precios muy bajos pueden no cambiar.")
            .defineInRange("gunsmithPriceMultiplier", 1.03, 0.1, 20.0);
    public static final ModConfigSpec.DoubleValue AMMO_PRICE_MULTIPLIER = B
            .comment("Multiplicador de TODOS los precios en Kirkys del Ammo Maker (1.0 = sin cambios).")
            .defineInRange("ammoMakerPriceMultiplier", 1.0, 0.1, 20.0);

    public static final ModConfigSpec.BooleanValue MISSIONS_ENABLED = B
            .comment("true = los aldeanos normales (herrero de armaduras, clérigo, bibliotecario...) tienen un trato especial por nivel que paga Kirkys (y algún trueque normal con Kirkys).")
            .define("missionsEnabled", true);

    public static final ModConfigSpec.IntValue ACHIEVEMENT_KIRKYS = B
            .comment("Kirkys por cada logro normal (se reclaman en el menú de recompensas).")
            .defineInRange("achievementKirkys", 5, 0, 1000);
    public static final ModConfigSpec.IntValue ACHIEVEMENT_CHALLENGE_KIRKYS = B
            .comment("Kirkys por cada logro de desafío (los morados).")
            .defineInRange("achievementChallengeKirkys", 10, 0, 1000);
    public static final ModConfigSpec.IntValue DAILY_KIRKYS = B
            .comment("Kirkys de la recompensa diaria (una vez por día real).")
            .defineInRange("dailyKirkys", 5, 0, 1000);

    public static final ModConfigSpec SPEC = B.build();
}
