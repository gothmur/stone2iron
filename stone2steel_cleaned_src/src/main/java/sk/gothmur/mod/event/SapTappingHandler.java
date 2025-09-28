package sk.gothmur.mod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID)
public class SapTappingHandler {

    // ---------- LADIACE KONŠTANTY ----------
    // cooldown medzi ťuknutiami (20 tickov = 1 sekunda)
    private static final int TAP_COOLDOWN_TICKS = 20; // ~2.0 s

    // SAP – šanca a množstvá
    private static final float SAP_CHANCE_TAGGED = 0.25f; // 15 %
    private static final float SAP_CHANCE_OTHER  = 0.05f; // 5 %
    private static final int   SAP_MIN           = 1;
    private static final int   SAP_MAX           = 1;

    // BARK – šanca a množstvá
    private static final float BARK_CHANCE = 0.45f; // 20 %
    private static final int   BARK_MIN    = 1;
    private static final int   BARK_MAX    = 2;

    // STICK – šanca a množstvá
    private static final float STICK_CHANCE = 0.20f; // 30 %
    private static final int   STICK_MIN    = 1;
    private static final int   STICK_MAX    = 1;

    // Tag s logmi, ktoré majú vyššiu šancu na sap
    public static final TagKey<net.minecraft.world.level.block.Block> SAP_LOGS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "sap_logs"));

    @SubscribeEvent
    public static void onLeftClickLog(PlayerInteractEvent.LeftClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        Player player = event.getEntity();
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (!held.is(stone2steel.FLINT_BIFACE.get())) return;

        // >>> DÔLEŽITÉ: ak už beží cooldown, nič nerob – a zruš ničenie bloku
        if (player.getCooldowns().isOnCooldown(held.getItem())) {
            event.setCanceled(true);
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // len pre logy
        if (!state.is(BlockTags.LOGS)) return;

        // blok sa NESMIE zničiť – zrušíme akciu ničenia
        event.setCanceled(true);

        // nastav cooldown, nech sa to nespamuje
        player.getCooldowns().addCooldown(held.getItem(), TAP_COOLDOWN_TICKS);

        // opotrebenie nástroja
        held.hurtAndBreak(1, (LivingEntity) player, null);

        RandomSource rng = level.getRandom();

        // šanca na sap podľa toho, či je blok v tagu
        float sapChance = state.is(SAP_LOGS) ? SAP_CHANCE_TAGGED : SAP_CHANCE_OTHER;

        // helper na spawny
        java.util.function.Consumer<ItemStack> drop = stack -> {
            ItemEntity e = new ItemEntity(level,
                    pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, stack);
            level.addFreshEntity(e);
        };

        // ---------- DROP LOGIKA ----------
        // SAP
        if (rng.nextFloat() < sapChance) {
            drop.accept(new ItemStack(stone2steel.TREE_SAP.get(), nextBetween(rng, SAP_MIN, SAP_MAX)));
        }
        // BARK
        if (rng.nextFloat() < BARK_CHANCE) {
            drop.accept(new ItemStack(stone2steel.TREE_BARK.get(), nextBetween(rng, BARK_MIN, BARK_MAX)));
        }
        // STICK
        if (rng.nextFloat() < STICK_CHANCE) {
            drop.accept(new ItemStack(net.minecraft.world.item.Items.STICK, nextBetween(rng, STICK_MIN, STICK_MAX)));
        }
    }

    private static int nextBetween(RandomSource rng, int min, int maxInclusive) {
        if (maxInclusive <= min) return Math.max(1, min);
        return rng.nextInt(maxInclusive - min + 1) + min;
    }
}
