package sk.gothmur.mod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID) // ✅ odstránené deprecated 'bus = ...'
public class WoodworkHandlers {

    // PRAVÝ KLIK s FLINT_BIFACE na LOG -> WOOD_BILLET (drop do invu)
    @SubscribeEvent
    public static void onBifaceOnLog(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        var held = player.getItemInHand(event.getHand());

        boolean isLog = state.is(net.minecraft.tags.BlockTags.LOGS);
        if (isLog && held.is(stone2steel.FLINT_BIFACE.get())) {
            // odmena: 1x wood_billet
            giveOrDrop(player, new ItemStack(stone2steel.WOOD_BILLET.get(), 1));
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    // PRAVÝ KLIK s WOOD_BILLET na ABRASIVE_SURFACE -> FIREBOARD (item)
    @SubscribeEvent
    public static void onBilletOnAbrasive(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        var held = player.getItemInHand(event.getHand());

        boolean isAbrasive = state.is(stone2steel.ABRASIVE_SURFACES);
        if (held.is(stone2steel.WOOD_BILLET.get()) && isAbrasive) {
            // minút jeden billet a dať 1x fireboard (item – placeable na náš blok)
            held.shrink(1);
            giveOrDrop(player, new ItemStack(stone2steel.FIREBOARD.get(), 1));
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
