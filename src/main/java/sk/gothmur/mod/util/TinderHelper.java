package sk.gothmur.mod.util;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import sk.gothmur.mod.stone2steel;

/** Pomocné funkcie pre prácu s TINDER + tvorbu EMBER. */
public final class TinderHelper {
    private TinderHelper() {}

    /** Najprv skúsi zobrať 1× TINDER z off-hand, potom z inventára. */
    public static boolean consumeOneTinder(Player player) {
        var off = player.getOffhandItem();
        if (off.is(stone2steel.TINDER.get())) {
            off.shrink(1);
            return true;
        }
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(stone2steel.TINDER.get())) {
                stack.shrink(1);
                return true;
            }
        }
        return false;
    }

    /**
     * Spotrebuje 1× TINDER a dá hráčovi 1× EMBER + zahrá zvuky.
     * @return true ak sa podarilo (tinder bol k dispozícii), inak false
     */
    public static boolean makeEmber(ServerLevel level, Player player, BlockPos pos) {
        if (!consumeOneTinder(player)) return false;

        ItemStack ember = stone2steel.EMBER.get().getDefaultInstance();
        if (!player.addItem(ember)) player.drop(ember, false);

        level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 0.7f, 1.0f);
        level.playSound(null, pos, SoundEvents.ITEM_PICKUP,       SoundSource.PLAYERS, 0.40f, 1.00f);
        return true;
    }
}
