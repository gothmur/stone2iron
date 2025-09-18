package sk.gothmur.mod.event;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID)
public class KnappingHandler {

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand  = player.getOffhandItem();

        // obe ruky musia držať FLINT
        if (mainHand.is(Items.FLINT) && offHand.is(Items.FLINT)) {
            if (!player.level().isClientSide) {
                RandomSource rng = player.level().getRandom();
                boolean success = rng.nextFloat() < 0.70f; // 70% šanca

                // voliteľný krátky cooldown na flint ( ~0.25s pri 20 tps )
                player.getCooldowns().addCooldown(Items.FLINT, 5);

                if (success) {
                    // úspech: spotrebuj oba flinty, daj shard
                    mainHand.shrink(1);
                    offHand.shrink(1);

                    ItemStack shard = new ItemStack(stone2steel.FLINT_SHARD.get());
                    if (!player.getInventory().add(shard)) {
                        player.drop(shard, false);
                    }

                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
                    player.displayClientMessage(Component.literal("Ostrý pazúrik: podarilo sa!"), true);
                } else {
                    // neúspech: spotrebuj iba 1 flint z main hand
                    mainHand.shrink(1);

                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.7f, 0.9f + rng.nextFloat() * 0.2f);
                    player.displayClientMessage(Component.literal("Nepodarilo sa vytvoriť ostrie."), true);
                }
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
