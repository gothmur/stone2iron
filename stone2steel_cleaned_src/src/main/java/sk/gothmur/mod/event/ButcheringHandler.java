package sk.gothmur.mod.event;

import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID)
public class ButcheringHandler {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        LivingEntity victim = event.getEntity();
        Level level = victim.level();

        if (level.isClientSide()) return;
        if (!(event.getSource().getEntity() instanceof Player player)) return;

        // hráč musí držať päsťný klin v jednej z rúk
        boolean hasBiface =
                player.getMainHandItem().is(stone2steel.FLINT_BIFACE.get()) ||
                        player.getOffhandItem().is(stone2steel.FLINT_BIFACE.get());
        if (!hasBiface) return;

        // zvieratá, z ktorých padajú šľachy
        EntityType<?> type = victim.getType();
        if (!(type == EntityType.COW ||
                type == EntityType.PIG ||
                type == EntityType.SHEEP ||
                type == EntityType.GOAT ||
                type == EntityType.CHICKEN)) {
            return;
        }

        RandomSource rng = level.getRandom();

        // šanca a množstvo: 70% → 1 ks, s 30% bonusom na +1 (teda občas 2 ks)
        int count = 0;
        if (rng.nextFloat() < 0.70f) {
            count = 1;
            if (rng.nextFloat() < 0.30f) count++;
        }

        if (count > 0) {
            ItemStack stack = new ItemStack(stone2steel.TENDON.get(), count);
            ItemEntity drop = new ItemEntity(level, victim.getX(), victim.getY() + 0.5, victim.getZ(), stack);
            event.getDrops().add(drop); // pridáme k vanilla dropom
        }
    }
}
