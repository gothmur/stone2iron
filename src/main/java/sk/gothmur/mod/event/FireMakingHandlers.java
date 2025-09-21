package sk.gothmur.mod.event;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID)
public class FireMakingHandlers {

    // Pravý klik s BOW_DRILL na FIREBOARD_BLOCK -> dá hráčovi EMBER a opotrebuje bow_drill
    @SubscribeEvent
    public static void onUseBowDrillOnFireboard(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack inHand = player.getItemInHand(event.getHand());

        if (inHand.is(stone2steel.BOW_DRILL.get()) && state.is(stone2steel.FIREBOARD_BLOCK.get())) {
            // mapovanie InteractionHand -> EquipmentSlot
            EquipmentSlot slot = (event.getHand() == InteractionHand.MAIN_HAND)
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

            // ✅ použijeme overload s EquipmentSlot
            inHand.hurtAndBreak(1, player, slot);

            // pridaj EMBER (alebo dropni, ak niet miesta)
            ItemStack ember = new ItemStack(stone2steel.EMBER.get());
            if (!player.addItem(ember)) {
                player.drop(ember, false);
            }

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }

    // Pravý klik s EMBER na KINDLING_BLOCK -> zmení na zapálený campfire
    @SubscribeEvent
    public static void onUseEmberOnKindling(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        var pos = event.getPos();
        var state = level.getBlockState(pos);
        Player player = event.getEntity();
        ItemStack inHand = player.getItemInHand(event.getHand());

        if (inHand.is(stone2steel.EMBER.get()) && state.is(stone2steel.KINDLING_BLOCK.get())) {
            inHand.shrink(1);

            var campfire = net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState()
                    .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, true)
                    .setValue(net.minecraft.world.level.block.CampfireBlock.SIGNAL_FIRE, false);

            level.setBlock(pos, campfire, 3);

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }
    }
}
