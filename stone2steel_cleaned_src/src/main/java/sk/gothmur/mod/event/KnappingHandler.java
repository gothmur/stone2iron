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
                float roll = rng.nextFloat();

                // cooldown aby sa to nespamovalo
                player.getCooldowns().addCooldown(Items.FLINT, 5);

                if (roll < 0.20f) {
                    // 20 % šanca na biface
                    mainHand.shrink(1);
                    offHand.shrink(1);

                    ItemStack biface = new ItemStack(stone2steel.FLINT_BIFACE.get());
                    if (!player.getInventory().add(biface)) {
                        player.drop(biface, false);
                    }

                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0f, 0.9f);
                    player.displayClientMessage(Component.literal("Podarilo sa vytvoriť päsťný klin!"), true);

                } else if (roll < 0.80f) {
                    // 60 % šanca na shard
                    mainHand.shrink(1);
                    offHand.shrink(1);

                    ItemStack shard = new ItemStack(stone2steel.FLINT_SHARD.get());
                    if (!player.getInventory().add(shard)) {
                        player.drop(shard, false);
                    }

                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.STONE_BREAK, SoundSource.PLAYERS, 1.0f, 1.1f);
                    player.displayClientMessage(Component.literal("Podarilo sa vytvoriť ostrý pazúrik!"), true);

                } else {
                    // 20 % fail
                    mainHand.shrink(1);

                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.STONE_HIT, SoundSource.PLAYERS, 0.7f, 1.0f);
                    player.displayClientMessage(Component.literal("Pokus o štiepanie sa nepodaril."), true);
                }
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
