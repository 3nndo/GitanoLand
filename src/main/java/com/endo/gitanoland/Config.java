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
            .defineInRange("bagCapacityLarge", 3000, 1, 1_000_000);

    public static final ModConfigSpec.BooleanValue CASINO_KIRKY_ONLY = B
            .comment("MineVegas: true = todas las maquinas (tragaperras, plinko, poker, blackjack...) solo aceptan Kirkys como apuesta,",
                    "aunque alguien intente anadir otros objetos con la Mano del Crupier. Los premios en otros objetos siguen siendo configurables.")
            .define("casinoKirkyOnly", true);

    public static final ModConfigSpec.BooleanValue CASINO_REQUIRE_SAFE = B
            .comment("MineVegas: true = una tragaperras, Plinko, ruleta o rueda de la fortuna solo arranca si esta enlazada a una Caja fuerte",
                    "(las apuestas entran en la caja y los premios en Kirkys salen de ella). false = como antes: crean los Kirkys de la nada.")
            .define("casinoRequireSafe", true);
    public static final ModConfigSpec.IntValue CASINO_SAFE_RESERVE_FACTOR = B
            .comment("MineVegas: la caja enlazada debe tener al menos (apuesta en Kirkys x este numero) para dejar jugar; es una estimacion del premio maximo.")
            .defineInRange("casinoSafeReserveFactor", 50, 1, 100000);

    public static final ModConfigSpec.BooleanValue NPC_SHOP_REQUIRE_SAFE = B
            .comment("true = los aldeanos que pagan en Kirkys (compradores de weed, el Stoner...) solo comercian si un jugador enlazo su Caja fuerte",
                    "a ese aldeano con la Llave (los Kirkys salen de esa caja). false = como antes: se crean de la nada.")
            .define("npcShopRequireSafe", true);

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

    public static final ModConfigSpec.IntValue PLINKO_COST = B
            .comment("Plinko (Jackpot): Kirkys que cuesta una partida.")
            .defineInRange("plinkoCost", 3, 0, 64);
    public static final ModConfigSpec.IntValue PLINKO_REWARD = B
            .comment("Plinko (Jackpot): Kirkys del premio.")
            .defineInRange("plinkoReward", 8, 0, 64);
    public static final ModConfigSpec.IntValue CARDS_COST = B
            .comment("Mesa de cartas, blackjack y poker (Jackpot): Kirkys de la apuesta.")
            .defineInRange("cardsCost", 5, 0, 64);
    public static final ModConfigSpec.IntValue CARDS_REWARD = B
            .comment("Mesa de cartas, blackjack y poker (Jackpot): Kirkys del premio.")
            .defineInRange("cardsReward", 10, 0, 64);
    public static final ModConfigSpec.IntValue POOL_COST = B
            .comment("Mesa de billar (Jackpot): Kirkys que cuesta una partida.")
            .defineInRange("poolCost", 5, 0, 64);
    public static final ModConfigSpec.IntValue POOL_REWARD = B
            .comment("Mesa de billar (Jackpot): Kirkys del premio por ganar.")
            .defineInRange("poolReward", 12, 0, 64);

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

    public static final ModConfigSpec.BooleanValue CASINO_ATTACKS = B
            .comment("Atentados en equipo: /atentado equipo [nombre] [jugador]... (en cualquier sitio) saca para todos el titulo ATENTADO",
                    "con el equipo y los nombres, y cada miembro ve en pantalla la vida de su equipo mientras dura. false = desactivado.")
            .define("casinoAttacksEnabled", true);
    public static final ModConfigSpec.IntValue CASINO_ATTACK_DURATION = B
            .comment("Minutos que dura un atentado (el cuadro con la vida del equipo se quita solo; tambien con /atentado fin).")
            .defineInRange("casinoAttackDurationMinutes", 15, 1, 240);
    public static final ModConfigSpec.IntValue CASINO_ATTACK_COOLDOWN = B
            .comment("Minutos que tiene que esperar un jugador entre dos atentados.")
            .defineInRange("casinoAttackCooldownMinutes", 10, 0, 1440);

    public static final ModConfigSpec.DoubleValue GUNSMITH_PRICE_MULTIPLIER = B
            .comment("Multiplicador de TODOS los precios en Kirkys del Gunsmith (1.0 = precio base, 0.50 = -50 %).",
                    "Se redondea al Kirky más cercano, así que los precios muy bajos pueden no cambiar.")
            .defineInRange("gunsmithPriceMultiplier", 0.50, 0.1, 20.0);
    public static final ModConfigSpec.DoubleValue AMMO_PRICE_MULTIPLIER = B
            .comment("Multiplicador de TODOS los precios en Kirkys del Ammo Maker (1.0 = sin cambios).")
            .defineInRange("ammoMakerPriceMultiplier", 1.0, 0.1, 20.0);

    public static final ModConfigSpec.IntValue WEEDSGUY_SPAWN_CHANCE = B
            .comment("Probabilidad (en %) de que cada día aparezca el WeedsGuy cerca de un jugador del Overworld, como el Wandering Trader (0 = nunca).",
                    "Solo hay un WeedsGuy a la vez y se va solo a los 2 días del juego.")
            .defineInRange("weedsGuySpawnChancePerDay", 25, 0, 100);

    public static final ModConfigSpec.IntValue SPECIALTY_CHANGE_BASE = B
            .comment("Kirkys base para pagarle a un aldeano-tienda que cambie de especialidad (boton de la tienda).",
                    "El precio de cada especialidad = base x (peso total / peso de esa especialidad): cuanto mas rara, mas cara (0 = gratis).")
            .defineInRange("specialtyChangeBaseKirkys", 60, 0, 100000);

    public static final ModConfigSpec.BooleanValue MISSIONS_ENABLED = B
            .comment("true = los aldeanos normales (herrero de armaduras, clérigo, bibliotecario...) tienen un trato especial por nivel que paga Kirkys (y algún trueque normal con Kirkys).")
            .define("missionsEnabled", true);

    public static final ModConfigSpec.IntValue ACHIEVEMENT_KIRKYS = B
            .comment("Kirkys por cada logro normal (se reclaman en el menú de recompensas).")
            .defineInRange("achievementKirkys", 5, 0, 1000);
    public static final ModConfigSpec.IntValue ACHIEVEMENT_CHALLENGE_KIRKYS = B
            .comment("Kirkys por cada logro de desafío (los morados).")
            .defineInRange("achievementChallengeKirkys", 12, 0, 1000);
    public static final ModConfigSpec.IntValue DAILY_KIRKYS = B
            .comment("Kirkys de la recompensa diaria (una vez por día real).")
            .defineInRange("dailyKirkys", 10, 0, 1000);

    // ---- EXPERIMENTAL: funciones en pruebas, desactivadas por defecto ----
    public static final ModConfigSpec.BooleanValue BANK_ENABLED;
    public static final ModConfigSpec.IntValue BANK_TAX_PERCENT;

    static {
        B.push("experimental");
        BANK_ENABLED = B
                .comment("EXPERIMENTAL: banco digital (/banco saldo | depositar | retirar). Esta en pruebas; false = el comando no existe.")
                .define("bankEnabled", false);
        BANK_TAX_PERCENT = B
                .comment("Impuesto (en %) que se cobra al depositar Kirkys en el banco. Retirar es gratis (0 = sin impuesto).")
                .defineInRange("bankDepositTaxPercent", 5, 0, 100);
        B.pop();
    }

    public static final ModConfigSpec SPEC = B.build();
}
