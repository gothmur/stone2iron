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
import net.minecraft.world.entity.EquipmentSlot;
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
    private static final int SHAPE_TICKS = 60;   // ~3 s
    private static final int HOLD_GRACE_TICKS = 8;
    private static final int SWING_PERIOD = 10;
    private static final int DUST_PERIOD  = 5;

    private static final Map<UUID, Shaping> ACTIVE = new HashMap<>();

    private enum Mode { SPINDLE, FIREBOARD, BILLET, TINDER }

    private record Shaping(BlockPos pos,
                           InteractionHand hand,
                           ItemStack snapshot1,
                           Mode mode,
                           int ticks,
                           long lastUseGameTime) { }

    /* === STICK na ABRASIVE_SURFACES -> SPINDLE === */
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

    /* === WOOD_BILLET na ABRASIVE_SURFACES -> FIREBOARD === */
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

    /* === FLINT_BIFACE na LOGS -> WOOD_BILLET (drž) === */
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
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (s == null) {
            beginShaping(event, player, hand, pos, Mode.BILLET, level);
        } else {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /* === TREE_BARK + FLINT_BIFACE na ABRASIVE_SURFACES -> TINDER (drž) === */
    @SubscribeEvent
    public static void onBarkWithBiface(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        if (!state.is(stone2steel.ABRASIVE_SURFACES)) return;

        Player player = event.getEntity();
        var hand  = event.getHand();
        var other = (hand == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;

        ItemStack a = player.getItemInHand(hand);
        ItemStack b = player.getItemInHand(other);

        boolean handIsBark   = a.is(stone2steel.TREE_BARK.get())    && b.is(stone2steel.FLINT_BIFACE.get());
        boolean handIsBiface = a.is(stone2steel.FLINT_BIFACE.get()) && b.is(stone2steel.TREE_BARK.get());
        if (!handIsBark && !handIsBiface) return;

        InteractionHand materialHand = handIsBark ? hand : other; // kôra je „materiál“ (spotrebuje sa)

        Shaping s = ACTIVE.get(player.getUUID());
        if (s != null && s.mode() == Mode.TINDER && s.hand() == materialHand && s.pos().equals(pos)) {
            ACTIVE.put(player.getUUID(), new Shaping(s.pos(), s.hand(), s.snapshot1(), s.mode(), s.ticks(), level.getGameTime()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (s == null) {
            beginShaping(event, player, materialHand, pos, Mode.TINDER, level);
        } else {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    /* === Spoločný lifecycle === */
    private static void beginShaping(PlayerInteractEvent.RightClickBlock event,
                                     Player player, InteractionHand hand, BlockPos pos, Mode mode, Level level) {
        ItemStack snap = player.getItemInHand(hand).copy();
        snap.setCount(1);

        ACTIVE.put(player.getUUID(), new Shaping(pos.immutable(), hand, snap, mode, 0, level.getGameTime()));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        player.swing(hand, true);
        if (mode == Mode.BILLET) {
            playChopSound(level, pos, 0.35f, 1.00f);
            spawnWoodChips(level, pos, 1);
        } else {
            playGrindSound(level, pos, 0.32f, 1.00f);
            spawnItemChips(level, pos, snap, 1);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;

        Shaping s = ACTIVE.get(player.getUUID());
        if (s == null) return;

        if (!stillValid(player, s)) { ACTIVE.remove(player.getUUID()); playCancelSound(level, s.pos()); return; }

        long now = level.getGameTime();
        if (now - s.lastUseGameTime() > HOLD_GRACE_TICKS) { ACTIVE.remove(player.getUUID()); playCancelSound(level, s.pos()); return; }

        int t = s.ticks() + 1;

        if (t % SWING_PERIOD == 0) {
            player.swing(s.hand(), true);
            if (s.mode() == Mode.BILLET) {
                spawnLayeredWoodChips(level, s.pos(), t);
                playChopPulse(level, s.pos(), player.getRandom(), t);
            } else {
                spawnLayeredItemChips(level, s.pos(), s.snapshot1(), t);
                playGrindPulse(level, s.pos(), player.getRandom(), t);
            }
        }
        if (t % DUST_PERIOD == 0) spawnDust(level, s.pos(), 1);

        if (t == SHAPE_TICKS / 2) {
            if (s.mode() == Mode.BILLET) { playChopSound(level, s.pos(), 0.40f, 1.10f); }
            else { spawnMidItemBurst(level, s.pos(), s.snapshot1()); playGrindSound(level, s.pos(), 0.40f, 1.10f); }
        }

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
                case TINDER -> {
                    // -1 bark, poškodiť biface v druhej ruke o 1, +1 tinder
                    InteractionHand other = (s.hand() == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
                    player.getItemInHand(s.hand()).shrink(1);
                    ItemStack tool = player.getItemInHand(other);
                    if (tool.is(stone2steel.FLINT_BIFACE.get())) {
                        EquipmentSlot slot = (other == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        tool.hurtAndBreak(1, player, slot);
                    }
                    giveOrDrop(player, new ItemStack(stone2steel.TINDER.get(), 1));
                    player.displayClientMessage(Component.translatable("msg.stone2steel.tinder_ok"), true);
                    playGrindSuccess(level, s.pos());
                }
            }
            ACTIVE.remove(player.getUUID());
            return;
        }

        ACTIVE.put(player.getUUID(), new Shaping(s.pos(), s.hand(), s.snapshot1(), s.mode(), t, s.lastUseGameTime()));
    }

    private static boolean stillValid(Player player, Shaping s) {
        ItemStack current = player.getItemInHand(s.hand()).copy();
        current.setCount(1);
        if (!ItemStack.isSameItemSameComponents(current, s.snapshot1())) return false;

        var state = player.level().getBlockState(s.pos());
        boolean distOk = player.distanceToSqr(s.pos().getX() + 0.5, s.pos().getY() + 0.5, s.pos().getZ() + 0.5) < 36.0;

        return switch (s.mode()) {
            case SPINDLE, FIREBOARD -> state.is(stone2steel.ABRASIVE_SURFACES) && distOk;
            case BILLET -> state.is(BlockTags.LOGS) && distOk;
            case TINDER -> state.is(stone2steel.ABRASIVE_SURFACES) && distOk
                    && player.getItemInHand((s.hand() == InteractionHand.MAIN_HAND) ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND)
                    .is(stone2steel.FLINT_BIFACE.get());
        };
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }

    /* === vizuály + zvuky === */
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
    private static void spawnDust(Level level, BlockPos pos, int count) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.ASH,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, 0.10, 0.02, 0.10, 0.00);
    }
    private static void spawnWoodChips(Level level, BlockPos pos, int count) {
        if (!(level instanceof ServerLevel sl)) return;
        var state = sl.getBlockState(pos);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, 0.08, 0.02, 0.08, 0.01);
    }
    private static void spawnLayeredWoodChips(Level level, BlockPos pos, int t) {
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
    private static void spawnSuccessPoof(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        sl.sendParticles(ParticleTypes.CLOUD,
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                6, 0.20, 0.06, 0.20, 0.02);
    }

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
