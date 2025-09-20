package sk.gothmur.mod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import sk.gothmur.mod.stone2steel;
import sk.gothmur.mod.util.ModTags;

@EventBusSubscriber(modid = stone2steel.MODID, bus = EventBusSubscriber.Bus.GAME)
public class WoodworkHandlers {

    private static final int COOLDOWN_TICKS_FAST = 20;   // ~1s
    private static final int COOLDOWN_TICKS_SLOW = 100;  // ~5s
    private static final int BIFACE_DMG_PER_BILLET = 2;

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) return;

        Player player = event.getEntity();
        InteractionHand hand = event.getHand();
        ItemStack held = player.getItemInHand(hand);
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // 1) BIFACE + LOG -> WOOD_BILLET (funguje)
        if (held.is(stone2steel.FLINT_BIFACE.get()) && state.is(BlockTags.LOGS)) {
            if (player.getCooldowns().isOnCooldown(held.getItem())) return;

            player.getCooldowns().addCooldown(held.getItem(), COOLDOWN_TICKS_SLOW);
            held.hurtAndBreak(BIFACE_DMG_PER_BILLET, player, null);
            giveOrDrop(player, new ItemStack(stone2steel.WOOD_BILLET.get(), 1));
            player.causeFoodExhaustion(0.002f);
            player.displayClientMessage(Component.translatable("msg.stone2steel.billet_ok"), true);

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // helper: je blok abrazívny? (tag ALEBO fallback na vanilla blocks)
        boolean isAbrasive = state.is(ModTags.Blocks.ABRASIVE_SURFACES)
                || state.is(Blocks.STONE)
                || state.is(Blocks.COBBLESTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.ANDESITE)
                || state.is(Blocks.GRANITE)
                || state.is(Blocks.DIORITE);

        // 2) STICK + ABRASIVE_SURFACE -> SPINDLE
        if (held.is(Items.STICK) && isAbrasive) {
            if (player.getCooldowns().isOnCooldown(held.getItem())) return;

            player.getCooldowns().addCooldown(held.getItem(), COOLDOWN_TICKS_FAST);
            held.shrink(1);
            giveOrDrop(player, new ItemStack(stone2steel.SPINDLE.get(), 1));
            player.causeFoodExhaustion(0.002f);
            player.displayClientMessage(Component.translatable("msg.stone2steel.spindle_ok"), true);

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            return;
        }

        // 3) WOOD_BILLET + ABRASIVE_SURFACE -> FIREBOARD
        if (held.is(stone2steel.WOOD_BILLET.get()) && isAbrasive) {
            if (player.getCooldowns().isOnCooldown(held.getItem())) return;

            player.getCooldowns().addCooldown(held.getItem(), COOLDOWN_TICKS_SLOW);
            held.shrink(1);
            giveOrDrop(player, new ItemStack(stone2steel.FIREBOARD.get(), 1));
            player.causeFoodExhaustion(0.003f);
            player.displayClientMessage(Component.translatable("msg.stone2steel.fireboard_ok"), true);

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.addItem(stack)) {
            player.drop(stack, false);
        }
    }
}
