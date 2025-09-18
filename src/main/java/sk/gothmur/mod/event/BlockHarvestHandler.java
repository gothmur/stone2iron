package sk.gothmur.mod.event;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID)
public class BlockHarvestHandler {

    // zakáže drop bez sekery
    @SubscribeEvent
    public static void onHarvestCheck(PlayerEvent.HarvestCheck event) {
        BlockState state = event.getTargetBlock();
        if (state.is(BlockTags.LOGS)) {
            ItemStack tool = event.getEntity().getMainHandItem();

            // ak tool NIE JE v ItemTags.AXES → nedostane drop
            if (!tool.is(ItemTags.AXES)) {
                event.setCanHarvest(false);
            }
        }
    }

    // zabráni zničeniu bloku bez sekery
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        Player player = event.getPlayer();

        if (state.is(BlockTags.LOGS)) {
            ItemStack tool = player.getMainHandItem();

            if (!tool.is(ItemTags.AXES)) {
                // zruší rozbitie, blok zostane
                event.setCanceled(true);

                // správa hráčovi
                if (!player.level().isClientSide) {
                    player.displayClientMessage(
                            Component.literal("Potrebujete sekeru!"),
                            true
                    );
                }
            }
        }
    }
}
