package sk.gothmur.mod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import sk.gothmur.mod.stone2steel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = stone2steel.MODID)
public class WoodworkHandlers {

    // ~3 sekundy pri 20 TPS
    private static final int SHAPE_TICKS = 60;
    // koľko tickov po poslednom RightClickBlock ešte tolerujeme "držanie" RMB
    private static final int HOLD_GRACE_TICKS = 8;
    // rytmus pre jemný swing / pulzy
    private static final int SWING_PERIOD = 10; // každých ~0.5 s
    private static final int DUST_PERIOD  = 5;  // medzi-sypnutie prachu

    // Stav prebiehajúcej práce na hráča
    private static final Map<UUID, Shaping> ACTIVE = new HashMap<>();

    private enum Mode { SPINDLE, FIREBOARD, BILLET }

    private record Shaping(BlockPos pos,
                           InteractionHand hand,     // ruka s predmetom (stick/billet/biface)
                           ItemStack snapshot1,      // 1 ks snímka z ruky (kontrola zmeny + zdroj item-častíc pre SPINDLE/FIREBOARD)
                           Mode mode,
                           int ticks,                // koľko už odpracované
                           long lastUseGameTime      // posledný tick, kedy prišiel RightClickBlock (držané pravé)
    ) {}

    /* ====================== INTERAKCIE (drž a pracuj) ====================== */

    // STICK na ABRASIVE_SURFACE -> SPINDLE
    @SubscribeEvent
    public static void onStickOnAbrasive(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        var hand = event.getHand();
        var held = player.getItemInHand(hand);

        if (!state.is(stone2steel.ABRASIVE_SURFACES) || !held.is(Items.STICK)) return;

        Shaping s = ACTIVE.get(player.getUUID());
        if (s != null && s.mode() == Mode.SPINDLE && s.hand() == hand && s.pos().equals(pos)) {
            ACTIVE.put(player.getUUID(), new Shaping(s.pos(), s.hand(), s.snapshot1(), s.mode(), s.ticks(), level.getGameTime()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (s == null) {
            beginShaping(event, player, hand, pos, Mode.SPINDLE, level);
        } else {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    // WOOD_BILLET na ABRASIVE_SURFACE -> FIREBOARD
    @SubscribeEvent
    public static void onBilletOnAbrasive(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        var hand = event.getHand();
        var held = player.getItemInHand(hand);

        if (!state.is(stone2steel.ABRASIVE_SURFACES) || !held.is(stone2steel.WOOD_BILLET.get())) return;

        Shaping s = ACTIVE.get(player.getUUID());
        if (s != null && s.mode() == Mode.FIREBOARD && s.hand() == hand && s.pos().equals(pos)) {
            ACTIVE.put(player.getUUID(), new Shaping(s.pos(), s.hand(), s.snapshot1(), s.mode(), s.ticks(), level.getGameTime()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (s == null) {
            beginShaping(event, player, hand, pos, Mode.FIREBOARD, level);
        } else {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    // FLINT_BIFACE na LOG -> WOOD_BILLET („rubanie dreva“)
    @SubscribeEvent
    public static void onBifaceOnLog(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        var hand = event.getHand();
        var held = player.getItemInHand(hand);

        if (!state.is(BlockTags.LOGS) || !held.is(stone2steel.FLINT_BIFACE.get())) return;

        Shaping s = ACTIVE.get(player.getUUID());
        if (s != null && s.mode() == Mode.BILLET && s.hand() == hand && s.pos().equals(pos)) {
            ACTIVE.put(player.getUUID(), new Shaping(s.pos(), s.hand(), s.snapshot1(), s.mode(), s.ticks(), level.getGameTime()));
            event.setCanceled(true); event.setCancellationResult(InteractionResult.CONSUME); return;
        }
        if (s == null) {
            beginShaping(event, player, hand, pos, Mode.BILLET, level);
        } else {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /* ====================== ZAČIATOK PRÁCE ====================== */

    private static void beginShaping(PlayerInteractEvent.RightClickBlock event,
                                     Player player, InteractionHand hand, BlockPos pos, Mode mode, Level level) {
        ItemStack snap = player.getItemInHand(hand).copy();
        snap.setCount(1);

        ACTIVE.put(player.getUUID(), new Shaping(pos.immutable(), hand, snap, mode, 0, level.getGameTime()));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        player.swing(hand, true);

        // štartové efekty
        if (mode == Mode.BILLET) {
            playChopSound(level, pos, 0.35f, 1.00f);
            spawnLayeredBlockChips(level, pos, 0);
        } else {
            playGrindSound(level, pos, 0.32f, 1.00f);
            spawnItemChips(level, pos, snap, 1);
        }
    }

    /* ====================== TICK: progres, efekty, dokončenie ====================== */

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;

        Shaping s = ACTIVE.get(player.getUUID());
        if (s == null) return;

        // stále platné podmienky?
        if (!stillValid(player, s)) {
            ACTIVE.remove(player.getUUID());
            playCancelSound(level, s.pos());
            return;
        }

        // uvoľnené RMB?
        long now = level.getGameTime();
        if (now - s.lastUseGameTime() > HOLD_GRACE_TICKS) {
            ACTIVE.remove(player.getUUID());
            playCancelSound(level, s.pos());
            return;
        }

        int t = s.ticks() + 1;

        // rytmický jemný swing + hlavné pulzy
        if (t % SWING_PERIOD == 0) {
            player.swing(s.hand(), true);
            if (s.mode() == Mode.BILLET) {
                spawnLayeredBlockChips(level, s.pos(), t);
                playChopPulse(level, s.pos(), player.getRandom(), t);
            } else { // SPINDLE/FIREBOARD
                spawnLayeredItemChips(level, s.pos(), s.snapshot1(), t);
                playGrindPulse(level, s.pos(), player.getRandom(), t);
            }
        }

        // medziprach
        if (t % DUST_PERIOD == 0) {
            spawnDust(level, s.pos(), 1);
        }

        // akcent v polovici
        if (t == SHAPE_TICKS / 2) {
            switch (s.mode()) {
                case BILLET -> playChopSound(level, s.pos(), 0.40f, 1.10f);
                case SPINDLE, FIREBOARD -> {
                    spawnMidItemBurst(level, s.pos(), s.snapshot1());
                    playGrindSound(level, s.pos(), 0.40f, 1.10f);
                }
            }
        }

        // dokončenie
        if (t >= SHAPE_TICKS) {
            spawnSuccessPoof(level, s.pos());
            switch (s.mode()) {
                case SPINDLE -> {
                    player.getItemInHand(s.hand()).shrink(1);
                    giveOrDrop(player, new ItemStack(stone2steel.SPINDLE.get(), 1));
                    player.displayClientMessage(Component.translatable("msg.stone2steel.spindle_ok"), true);
                    playGrindSuccess(level, s.pos());
                }
                case FIREBOARD -> {
                    player.getItemInHand(s.hand()).shrink(1);
                    giveOrDrop(player, new ItemStack(stone2steel.FIREBOARD.get(), 1));
                    player.displayClientMessage(Component.translatable("msg.stone2steel.fireboard_ok"), true);
                    playGrindSuccess(level, s.pos());
                }
                case BILLET -> {
                    giveOrDrop(player, new ItemStack(stone2steel.WOOD_BILLET.get(), 1));
                    player.displayClientMessage(Component.translatable("msg.stone2steel.billet_ok"), true);
                    playChopSuccess(level, s.pos());
                }
            }
            ACTIVE.remove(player.getUUID());
            return;
        }

        // inkrement tickov
        ACTIVE.put(player.getUUID(),
                new Shaping(s.pos(), s.hand(), s.snapshot1(), s.mode(), t, s.lastUseGameTime()));
    }

    private static boolean stillValid(Player player, Shaping s) {
        // hráč musí držať rovnakú položku (typ + NBT) v rovnakej ruke
        ItemStack current = player.getItemInHand(s.hand()).copy();
        current.setCount(1);
        if (!ItemStack.isSameItemSameComponents(current, s.snapshot1())) return false;

        var state = player.level().getBlockState(s.pos());
        boolean distOk = player.distanceToSqr(s.pos().getX() + 0.5, s.pos().getY() + 0.5, s.pos().getZ() + 0.5) < 36.0;

        return switch (s.mode()) {
            case SPINDLE, FIREBOARD -> state.is(stone2steel.ABRASIVE_SURFACES) && distOk;
            case BILLET -> state.is(BlockTags.LOGS) && distOk;
        };
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }

    /* ========================== Vizuály + zvuky ========================== */

    // — ITEM triesky (spindle / fireboard) —
    private static void spawnItemChips(Level level, BlockPos pos, ItemStack item, int count) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, 0.08, 0.02, 0.08, 0.01);
    }

    private static void spawnLayeredItemChips(Level level, BlockPos pos, ItemStack item, int t) {
        if (!(level instanceof ServerLevel sl)) return;
        int count = (t <= 20) ? 1 : (t <= 40 ? 2 : 3);
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, 0.12, 0.02, 0.12, 0.02);
        sl.sendParticles(ParticleTypes.ASH,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                1, 0.10, 0.02, 0.10, 0.00);
    }

    private static void spawnMidItemBurst(Level level, BlockPos pos, ItemStack item) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, item),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                3, 0.18, 0.03, 0.18, 0.03);
        sl.sendParticles(ParticleTypes.ASH,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                2, 0.12, 0.02, 0.12, 0.00);
    }

    // — BLOCK triesky (z bloku pod rukou; používame pri BILLET) —
    private static void spawnLayeredBlockChips(Level level, BlockPos pos, int t) {
        if (!(level instanceof ServerLevel sl)) return;
        var state = sl.getBlockState(pos);
        int count = (t <= 20) ? 1 : (t <= 40 ? 2 : 3);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, 0.12, 0.02, 0.12, 0.02);
        sl.sendParticles(ParticleTypes.ASH,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                1, 0.10, 0.02, 0.10, 0.00);
    }

    private static void spawnDust(Level level, BlockPos pos, int count) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.ASH,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, 0.10, 0.02, 0.10, 0.00);
    }

    private static void spawnSuccessPoof(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.CLOUD,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                6, 0.20, 0.06, 0.20, 0.02);
    }

    // zvuky – brúsenie (SPINDLE/FIREBOARD) a „rubanie“ (BILLET)
    private static void playGrindSound(Level level, BlockPos pos, float volume, float pitch) {
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, volume, pitch);
    }
    private static void playGrindPulse(Level level, BlockPos pos, RandomSource rng, int t) {
        float volBase = 0.25f + (t > 40 ? 0.08f : (t > 20 ? 0.04f : 0.0f));
        float vol = volBase + (rng.nextFloat() * 0.05f);
        float pitch = 0.95f + (rng.nextFloat() * 0.15f);
        playGrindSound(level, pos, vol, pitch);
    }
    private static void playGrindSuccess(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.CANDLE_EXTINGUISH, SoundSource.PLAYERS, 0.45f, 1.00f);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP,      SoundSource.PLAYERS, 0.40f, 1.00f);
    }

    private static void playChopSound(Level level, BlockPos pos, float volume, float pitch) {
        level.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.PLAYERS, volume, pitch);
    }
    private static void playChopPulse(Level level, BlockPos pos, RandomSource rng, int t) {
        float volBase = 0.30f + (t > 40 ? 0.08f : (t > 20 ? 0.04f : 0.0f));
        float vol = volBase + (rng.nextFloat() * 0.05f);
        float pitch = 0.90f + (rng.nextFloat() * 0.10f);
        playChopSound(level, pos, vol, pitch);
    }
    private static void playChopSuccess(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WOOD_BREAK,  SoundSource.PLAYERS, 0.55f, 0.95f);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.40f, 1.00f);
    }
    private static void playCancelSound(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.20f, 0.85f);
    }
}
