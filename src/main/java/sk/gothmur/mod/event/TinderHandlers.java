package sk.gothmur.mod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Výroba TINDERU: drž FLINT_BIFACE na ABRASIVE_SURFACES ~3 s.
 * Vyžaduje aspoň 1× TREE_BARK kdekoľvek v inventári (spotrebuje sa pri dokončení).
 */
@EventBusSubscriber(modid = stone2steel.MODID)
public class TinderHandlers {

    private static final int SHAPE_TICKS = 60;   // ~3 s pri 20 TPS
    private static final int HOLD_GRACE_TICKS = 8;
    private static final int SWING_PERIOD = 10;
    private static final int DUST_PERIOD  = 5;

    private record TinderWork(BlockPos pos,
                              InteractionHand hand,   // ruka s biface
                              ItemStack barkVisual,   // 1× kôra na partikly (nezávislé od inventára)
                              int ticks,
                              long lastUseGameTime) { }

    private static final Map<UUID, TinderWork> ACTIVE = new HashMap<>();

    /** FLINT_BIFACE na ABRASIVE_SURFACES -> TINDER (kôra z inventára) */
    @SubscribeEvent
    public static void onBifaceOnAbrasive(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);

        if (!state.is(stone2steel.ABRASIVE_SURFACES) || !held.is(stone2steel.FLINT_BIFACE.get())) return;

        // musí mať aspoň jednu kôru v inventári
        if (!hasAnyBark(player)) return;

        TinderWork w = ACTIVE.get(player.getUUID());
        if (w != null && w.hand == hand && w.pos.equals(pos)) {
            // stále drží RMB – obnov "lastUse"
            ACTIVE.put(player.getUUID(), new TinderWork(w.pos, w.hand, w.barkVisual, w.ticks, level.getGameTime()));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }

        if (w == null) {
            begin(event, player, hand, pos, level);
        } else {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
        }
    }

    private static void begin(PlayerInteractEvent.RightClickBlock event, Player player, InteractionHand hand, BlockPos pos, Level level) {
        // vizuál – použijeme 1× kôru len pre častice (inventár spotrebujeme až na konci)
        ItemStack barkParticle = new ItemStack(stone2steel.TREE_BARK.get());
        barkParticle.setCount(1);

        ACTIVE.put(player.getUUID(), new TinderWork(pos.immutable(), hand, barkParticle, 0, level.getGameTime()));

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);

        player.swing(hand, true);

        // jemný štart zvuk + drobné item-triesky z kôry
        playGrind(level, pos, 0.32f, 1.00f);
        spawnItemChips(level, pos, barkParticle, 1);
    }

    @SubscribeEvent
    public static void onTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();
        if (level.isClientSide()) return;

        TinderWork w = ACTIVE.get(player.getUUID());
        if (w == null) return;

        // stále platné podmienky?
        if (!stillValid(player, w)) {
            ACTIVE.remove(player.getUUID());
            playCancel(level, w.pos);
            return;
        }

        // drží RMB?
        long now = level.getGameTime();
        if (now - w.lastUseGameTime > HOLD_GRACE_TICKS) {
            ACTIVE.remove(player.getUUID());
            playCancel(level, w.pos);
            return;
        }

        int t = w.ticks + 1;

        // rytmus: swing + kôrové triesky + jemný grind každých ~0.5 s
        if (t % SWING_PERIOD == 0) {
            player.swing(w.hand, true);
            spawnLayeredItemChips(level, w.pos, w.barkVisual, t);
            playGrind(level, w.pos, grindVolumeFor(t), grindPitch());
        }

        if (t % DUST_PERIOD == 0) spawnDust(level, w.pos, 1);

        if (t >= SHAPE_TICKS) {
            spawnSuccessPoof(level, w.pos);

            // spotrebuj 1× bark z inventára (ak z nejakého dôvodu medzitým zmizla, len zruš)
            if (!shrinkOneBark(player)) {
                ACTIVE.remove(player.getUUID());
                playCancel(level, w.pos);
                return;
            }

            // poškodiť biface o 1
            ItemStack biface = player.getItemInHand(w.hand);
            EquipmentSlot slot = (w.hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            if (!biface.isEmpty()) {
                biface.hurtAndBreak(1, player, slot);
            }

            // odmena: tinder
            giveOrDrop(player, new ItemStack(stone2steel.TINDER.get(), 1));
            player.displayClientMessage(Component.translatable("msg.stone2steel.tinder_ok"), true);

            // úspechový cue
            level.playSound(null, w.pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.45f, 1.00f);

            ACTIVE.remove(player.getUUID());
            return;
        }

        ACTIVE.put(player.getUUID(), new TinderWork(w.pos, w.hand, w.barkVisual, t, w.lastUseGameTime));
    }

    private static boolean stillValid(Player p, TinderWork w) {
        // musí stále držať FLINT_BIFACE v tej istej ruke
        if (!p.getItemInHand(w.hand).is(stone2steel.FLINT_BIFACE.get())) return false;

        // musí stále mieriť na brúsny povrch a byť nablízku
        var state = p.level().getBlockState(w.pos);
        boolean close = p.distanceToSqr(w.pos.getX() + 0.5, w.pos.getY() + 0.5, w.pos.getZ() + 0.5) < 36.0;
        if (!state.is(stone2steel.ABRASIVE_SURFACES) || !close) return false;

        // musí mať v inventári aspoň 1× kôru
        return hasAnyBark(p);
    }

    /* ================= helpers ================= */

    private static boolean hasAnyBark(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(stone2steel.TREE_BARK.get()) && st.getCount() > 0) return true;
        }
        return false;
    }

    private static boolean shrinkOneBark(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(stone2steel.TREE_BARK.get()) && st.getCount() > 0) {
                st.shrink(1);
                if (st.isEmpty()) p.getInventory().setItem(i, ItemStack.EMPTY);
                return true;
            }
        }
        return false;
    }

    private static float grindVolumeFor(int t) {
        return 0.25f + (t > 40 ? 0.08f : (t > 20 ? 0.04f : 0.0f));
    }
    private static float grindPitch() { return 0.95f + (float)(Math.random() * 0.15); }

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

    private static void playGrind(Level level, BlockPos pos, float volume, float pitch) {
        level.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, volume, pitch);
    }
    private static void playCancel(Level level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.WOOD_BREAK, SoundSource.PLAYERS, 0.20f, 0.85f);
    }
    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) player.drop(stack, false);
    }
}
