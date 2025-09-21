package sk.gothmur.mod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import sk.gothmur.mod.stone2steel;
import sk.gothmur.mod.registry.ModSounds;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = stone2steel.MODID)
public class FireMakingHandlers {

    private static final int SHAPE_TICKS = 60;   // ~3 s pri 20 TPS
    private static final int HOLD_GRACE_TICKS = 8;
    private static final int SWING_PERIOD = 10;
    private static final int DUST_PERIOD  = 5;

    private record Drilling(BlockPos pos,
                            InteractionHand hand,     // ruka s bow drillom
                            ItemStack snapshot,       // 1 ks snapshot (kontrola zmeny)
                            int ticks,
                            long lastUseGameTime) { }

    private static final Map<UUID, Drilling> ACTIVE = new HashMap<>();

    /** BOW_DRILL na FIREBOARD_BLOCK -> EMBER (drž ~3 s) */
    @SubscribeEvent
    public static void onBowDrillOnFireboard(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);

        if (!state.is(stone2steel.FIREBOARD_BLOCK.get()) || !held.is(stone2steel.BOW_DRILL.get())) return;

        Drilling d = ACTIVE.get(player.getUUID());
        if (d != null && d.hand == hand && d.pos.equals(pos)) {
            ACTIVE.put(player.getUUID(), new Drilling(d.pos, d.hand, d.snapshot, d.ticks, level.getGameTime()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (d == null) {
            begin(event, player, hand, pos, level);
        } else {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    private static void begin(PlayerInteractEvent.RightClickBlock event, Player player, InteractionHand hand, BlockPos pos, Level level) {
        ItemStack snap = player.getItemInHand(hand).copy();
        snap.setCount(1);

        ACTIVE.put(player.getUUID(), new Drilling(pos.immutable(), hand, snap, 0, level.getGameTime()));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        player.swing(hand, true);

        // ► Loop zvuku: spustíme hneď na začiatku (prvý 1s klip)
        playLoopGrind(level, pos, 0);

        // malé triesky zo samotného fireboard bloku
        spawnBlockChips(level, pos, 1, 0.08, 0.02, 0.08, 0.01);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;

        Drilling d = ACTIVE.get(player.getUUID());
        if (d == null) return;

        if (!stillValid(player, d)) { ACTIVE.remove(player.getUUID()); playCancel(level, d.pos); return; }

        long now = level.getGameTime();
        if (now - d.lastUseGameTime > HOLD_GRACE_TICKS) { ACTIVE.remove(player.getUUID()); playCancel(level, d.pos); return; }

        int t = d.ticks + 1;

        // ► Loop zvuku každých ~20 tickov (≈ 1s): t=20 a t=40 (t=60 je už finish)
        if (t == 20 || t == 40) {
            playLoopGrind(level, d.pos, t);
        }

        // rytmus: swing + triesky
        if (t % SWING_PERIOD == 0) {
            player.swing(d.hand, true);
            spawnBlockChips(level, d.pos, chipsFor(t), 0.12, 0.02, 0.12, 0.02);
        }

        if (t % DUST_PERIOD == 0) spawnDust(level, d.pos, 1);

        if (t >= SHAPE_TICKS) {
            spawnSuccessPoof(level, d.pos);

            // poškodiť Bow Drill o 1 (signatúra s EquipmentSlot)
            ItemStack drill = player.getItemInHand(d.hand);
            EquipmentSlot slot = (d.hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            if (!drill.isEmpty()) {
                drill.hurtAndBreak(1, player, slot);
            }

            // odmena: ember
            giveOrDrop(player, new ItemStack(stone2steel.EMBER.get(), 1));
            player.displayClientMessage(Component.translatable("msg.stone2steel.ember_ok"), true);

            // krátky úspechový cue
            level.playSound(null, d.pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.7f, 1.0f);
            level.playSound(null, d.pos, SoundEvents.ITEM_PICKUP,        SoundSource.PLAYERS, 0.40f, 1.00f);

            ACTIVE.remove(player.getUUID());
            return;
        }

        ACTIVE.put(player.getUUID(), new Drilling(d.pos, d.hand, d.snapshot, t, d.lastUseGameTime));
    }

    private static boolean stillValid(Player p, Drilling d) {
        ItemStack cur = p.getItemInHand(d.hand).copy();
        cur.setCount(1);
        if (!ItemStack.isSameItemSameComponents(cur, d.snapshot)) return false;

        var state = p.level().getBlockState(d.pos);
        boolean close = p.distanceToSqr(d.pos.getX() + 0.5, d.pos.getY() + 0.5, d.pos.getZ() + 0.5) < 36.0;
        return state.is(stone2steel.FIREBOARD_BLOCK.get()) && close;
    }

    /* ================= helpers: zvuk/efekty/loot ================= */

    private static void playLoopGrind(Level level, BlockPos pos, int t) {
        // jemný ramp-up hlasitosti + pitch variance
        // t=0  → tichšie, t=20 → o trochu hlasnejšie, t=40 → najvýraznejší
        float baseVol = (t == 0) ? 0.65f : (t == 20 ? 0.75f : 0.85f);
        float volJitter = (float)(Math.random() * 0.06f) - 0.03f; // ±0.03
        float volume = clamp01(baseVol + volJitter);

        float basePitch = (t == 0) ? 0.96f : (t == 20 ? 1.00f : 1.05f);
        float pitchJitter = (float)(Math.random() * 0.10f) - 0.05f; // ±0.05
        float pitch = clamp(basePitch + pitchJitter, 0.85f, 1.15f);

        level.playSound(null, pos, ModSounds.BOW_DRILL_GRIND.get(), SoundSource.PLAYERS, volume, pitch);
    }

    private static float clamp01(float v) { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

    private static int chipsFor(int t) { return (t <= 20) ? 1 : (t <= 40 ? 2 : 3); }

    private static void spawnBlockChips(Level level, BlockPos pos, int count, double ox, double oy, double oz, double speed) {
        if (!(level instanceof ServerLevel sl)) return;
        var state = sl.getBlockState(pos);
        sl.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                count, ox, oy, oz, speed);
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

    private static void playCancel(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.20f, 0.85f);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}
