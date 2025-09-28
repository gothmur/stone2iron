package sk.gothmur.mod.system;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.event.tick.LevelTickEvent;

import sk.gothmur.mod.block.HeatedRockBlock;
import sk.gothmur.mod.block.RockBase;
import sk.gothmur.mod.stone2steel;

import java.util.HashMap;
import java.util.Map;

public final class CampfireHeatingHandler {

    private CampfireHeatingHandler() {}

    // Ako často beží handler (tick = 1/20 s)
    private static final int SCAN_INTERVAL_TICKS = 20; // 1 s

    // --- OHRIEVANIE (aké máme už teraz) ---
    private static final int STONE_STAGE1 = 10 * 20; // 10 s
    private static final int STONE_STAGE2 = 20 * 20; // 20 s
    private static final int STONE_STAGE3 = 30 * 20; // 30 s

    private static final int ORE_STAGE1   = 20 * 20; // 20 s
    private static final int ORE_STAGE2   = 40 * 20; // 40 s
    private static final int ORE_STAGE3   = 60 * 20; // 60 s

    // --- CHLADNUTIE (nové) — po VYHASENÍ campfiru ---
    // každé toľko sekúnd bez tepla zníž stage o 1
    private static final int STONE_COOL_STEP = 10 * 20; // každých 10 s o stupeň
    private static final int ORE_COOL_STEP   = 20 * 20; // ruda chladne pomalšie

    // per-level počítadlá
    private static final Map<ServerLevel, Map<Long, Integer>> HEAT_COUNTERS = new HashMap<>();
    private static final Map<ServerLevel, Map<Long, Integer>> COOL_COUNTERS = new HashMap<>();
    private static int tickGate = 0;

    /** Registruj cez: NeoForge.EVENT_BUS.addListener(CampfireHeatingHandler::onLevelTickPost); */
    public static void onLevelTickPost(final LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        tickGate++;
        if (tickGate % SCAN_INTERVAL_TICKS != 0) return;

        Map<Long, Integer> heat = HEAT_COUNTERS.computeIfAbsent(level, k -> new HashMap<>());
        Map<Long, Integer> cool = COOL_COUNTERS.computeIfAbsent(level, k -> new HashMap<>());

        // 1) OHRIEVANIE – ako doteraz (campfire → susedia)
        level.players().forEach(p -> {
            BlockPos center = p.blockPosition();
            int r = 8;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int y = -2; y <= 2; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        BlockState s = level.getBlockState(cursor);
                        if (s.is(Blocks.CAMPFIRE) && s.getValue(CampfireBlock.LIT)) {
                            heatNeighbours(level, cursor, heat, cool);
                        }
                    }
                }
            }
        });

        // 2) CHLADNUTIE – pre všetky heated_rock v okolí hráčov, ktoré už NIE sú pri lit campfiri
        level.players().forEach(p -> {
            BlockPos center = p.blockPosition();
            int r = 8;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

            for (int y = -2; y <= 2; y++) {
                for (int x = -r; x <= r; x++) {
                    for (int z = -r; z <= r; z++) {
                        cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                        BlockState s = level.getBlockState(cursor);
                        if (!s.is(stone2steel.HEATED_ROCK.get())) continue;

                        // ak je vedľa lit campfire, nechladneme
                        if (adjacentLitCampfire(level, cursor)) {
                            // resetni cool counter a pokračuj
                            cool.remove(cursor.asLong());
                            continue;
                        }

                        // zvyšuj cool čas a krokovo zniž stage
                        RockBase base = s.getValue(HeatedRockBlock.BASE);
                        int step = isOre(base) ? ORE_COOL_STEP : STONE_COOL_STEP;

                        long key = cursor.asLong();
                        int t = cool.getOrDefault(key, 0) + SCAN_INTERVAL_TICKS;
                        if (t >= step) {
                            int stage = s.getValue(HeatedRockBlock.HEAT_STAGE);
                            if (stage > 0) {
                                // stage-- :
                                level.setBlock(cursor, s.setValue(HeatedRockBlock.HEAT_STAGE, stage - 1), 3);
                            } else {
                                // stage == 0 → návrat na vanilla blok
                                level.setBlock(cursor, vanillaStateFrom(base), 3);
                                // vyčisti počítadlá
                                heat.remove(key);
                            }
                            t = 0; // po kroku reset
                        }
                        cool.put(key, t);
                    }
                }
            }
        });
    }

    // --- Pomocné metódy ---

    private static void heatNeighbours(ServerLevel level, BlockPos camp, Map<Long, Integer> heat, Map<Long, Integer> cool) {
        BlockPos[] targets = new BlockPos[] {
                camp.above(), camp.below(),
                camp.north(), camp.east(), camp.south(), camp.west()
        };

        for (BlockPos t : targets) {
            BlockState s = level.getBlockState(t);

            RockBase base;
            boolean alreadyHeated = false;
            if (s.is(stone2steel.HEATED_ROCK.get())) {
                base = s.getValue(HeatedRockBlock.BASE);
                alreadyHeated = true;
            } else {
                base = rockBaseFromVanilla(s);
            }
            if (base == null) continue;

            long key = t.asLong();

            // pri ohrievaní resetni chladnutie
            cool.remove(key);

            int time = heat.getOrDefault(key, 0) + SCAN_INTERVAL_TICKS;
            heat.put(key, time);

            int stage = computeHeatStage(base, time);
            if (stage <= 0) continue;

            if (alreadyHeated) {
                int current = s.getValue(HeatedRockBlock.HEAT_STAGE);
                if (stage != current) {
                    level.setBlock(t, s.setValue(HeatedRockBlock.HEAT_STAGE, stage), 3);
                }
            } else {
                level.setBlock(t,
                        stone2steel.HEATED_ROCK.get().defaultBlockState()
                                .setValue(HeatedRockBlock.BASE, base)
                                .setValue(HeatedRockBlock.HEAT_STAGE, stage),
                        3);
            }
        }
    }

    private static boolean adjacentLitCampfire(ServerLevel level, BlockPos pos) {
        return isLitCampfire(level.getBlockState(pos.above())) ||
                isLitCampfire(level.getBlockState(pos.below())) ||
                isLitCampfire(level.getBlockState(pos.north())) ||
                isLitCampfire(level.getBlockState(pos.south())) ||
                isLitCampfire(level.getBlockState(pos.east()))  ||
                isLitCampfire(level.getBlockState(pos.west()));
    }

    private static boolean isLitCampfire(BlockState s) {
        return s.is(Blocks.CAMPFIRE) && s.getValue(CampfireBlock.LIT);
    }

    private static int computeHeatStage(RockBase base, int time) {
        if (!isOre(base)) {
            if (time >= STONE_STAGE3) return 3;
            if (time >= STONE_STAGE2) return 2;
            if (time >= STONE_STAGE1) return 1;
            return 0;
        } else {
            if (time >= ORE_STAGE3) return 3;
            if (time >= ORE_STAGE2) return 2;
            if (time >= ORE_STAGE1) return 1;
            return 0;
        }
    }

    private static boolean isOre(RockBase base) {
        return base == RockBase.COPPER_ORE || base == RockBase.DEEPSLATE_COPPER_ORE;
    }

    private static RockBase rockBaseFromVanilla(BlockState s) {
        if (s.is(Blocks.STONE)) return RockBase.STONE;
        if (s.is(Blocks.ANDESITE)) return RockBase.ANDESITE;
        if (s.is(Blocks.DIORITE)) return RockBase.DIORITE;
        if (s.is(Blocks.GRANITE)) return RockBase.GRANITE;
        if (s.is(Blocks.DEEPSLATE)) return RockBase.DEEPSLATE;
        if (s.is(Blocks.COPPER_ORE)) return RockBase.COPPER_ORE;
        if (s.is(Blocks.DEEPSLATE_COPPER_ORE)) return RockBase.DEEPSLATE_COPPER_ORE;
        return null;
    }

    private static BlockState vanillaStateFrom(RockBase base) {
        return switch (base) {
            case STONE -> Blocks.STONE.defaultBlockState();
            case ANDESITE -> Blocks.ANDESITE.defaultBlockState();
            case DIORITE -> Blocks.DIORITE.defaultBlockState();
            case GRANITE -> Blocks.GRANITE.defaultBlockState();
            case DEEPSLATE -> Blocks.DEEPSLATE.defaultBlockState();
            case COPPER_ORE -> Blocks.COPPER_ORE.defaultBlockState();
            case DEEPSLATE_COPPER_ORE -> Blocks.DEEPSLATE_COPPER_ORE.defaultBlockState();
        };
    }
}
