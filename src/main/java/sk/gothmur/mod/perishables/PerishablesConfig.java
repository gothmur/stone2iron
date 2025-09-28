package sk.gothmur.mod.perishables;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PerishablesConfig {
    private PerishablesConfig() {}

    public static final ModConfigSpec SPEC;

    // core (už si používal)
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue  DECAY_RATE;
    public static final ModConfigSpec.IntValue     TICK_INTERVAL;

    // containers – nové
    public static final ModConfigSpec.BooleanValue CONTAINERS_DECAY;
    public static final ModConfigSpec.BooleanValue CONTAINERS_NEAR_PLAYER;
    public static final ModConfigSpec.IntValue     CONTAINERS_TICK_INTERVAL;
    public static final ModConfigSpec.IntValue     CONTAINERS_PER_PLAYER_BUDGET;
    public static final ModConfigSpec.IntValue     CONTAINERS_SEARCH_RADIUS_BLOCKS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.push("perishables");
        ENABLED       = b.comment("Zapne/vypne celý systém kazenia jedla.")
                .define("enabled", true);
        DECAY_RATE    = b.comment("Globálny multiplikátor kazenia (1.0 = default).")
                .defineInRange("decayRate", 1.0, 0.0, 10.0);
        TICK_INTERVAL = b.comment("Ako často tickovať inventár hráča (v ticky).")
                .defineInRange("tickInterval", 100, 1, 1200);
        b.pop();

        b.push("containers");
        CONTAINERS_DECAY = b.comment("Kazenie jedla aj v kontajneroch (chest/barrel/shulker).")
                .define("containersDecay", true);
        CONTAINERS_NEAR_PLAYER = b.comment("Aktualizovať kontajnery v blízkosti hráča aj bez otvorenia.")
                .define("nearPlayerEnabled", true);
        CONTAINERS_TICK_INTERVAL = b.comment("Interval pre spracovanie kontajnerov pri hráčovi (v ticky).")
                .defineInRange("nearPlayerTickInterval", 40, 1, 1200);
        CONTAINERS_PER_PLAYER_BUDGET = b.comment("Max. počet kontajnerov na hráča a interval.")
                .defineInRange("nearPlayerBudget", 2, 0, 64);
        CONTAINERS_SEARCH_RADIUS_BLOCKS = b.comment("Hľadaný rádius okolo hráča (v blokoch).")
                .defineInRange("nearPlayerRadiusBlocks", 16, 4, 128);
        b.pop();

        SPEC = b.build();
    }
}
