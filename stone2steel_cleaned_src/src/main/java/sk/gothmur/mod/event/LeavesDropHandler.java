package sk.gothmur.mod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID)
public class LeavesDropHandler {

    // trošku viac než vanilla sapling (~5 %) -> 8 %
    private static final double CURVED_BRANCH_CHANCE = 0.08D;
    private static final int BIFACE_DMG_ON_LEAVES = 1;

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level)) return;
        if (level.isClientSide) return;

        BlockState state = event.getState();
        if (!state.is(BlockTags.LEAVES)) return; // len listy

        Player player = event.getPlayer();
        if (player == null) return;
        if (player.isCreative()) return; // v creative nespawnujeme drop

        ItemStack held = player.getMainHandItem();
        boolean isBiface = held.is(stone2steel.FLINT_BIFACE.get());
        boolean isAxe = held.getItem() instanceof AxeItem;
        if (!(isBiface || isAxe)) return; // biface alebo ľubovoľná sekera

        // náhodný drop Curved Branch
        if (level.random.nextDouble() < CURVED_BRANCH_CHANCE) {
            BlockPos pos = event.getPos();
            ItemStack drop = new ItemStack(stone2steel.CURVED_BRANCH.get(), 1);
            level.addFreshEntity(new ItemEntity(
                    level,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    drop
            ));
        }

        // malé opotrebenie biface, bez lambda (tretí argument = null)
        if (isBiface) {
            held.hurtAndBreak(BIFACE_DMG_ON_LEAVES, player, null);
        }
    }
}
