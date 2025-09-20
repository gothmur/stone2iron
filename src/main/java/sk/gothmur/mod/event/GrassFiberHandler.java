package sk.gothmur.mod.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID)
public class GrassFiberHandler {

    // Rozšíriteľný tag – iné trávy z modov
    public static final TagKey<net.minecraft.world.level.block.Block> FIBER_PLANTS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "fiber_plants"));

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        LevelAccessor levelAcc = event.getLevel();
        if (!(levelAcc instanceof ServerLevel level)) return; // iba server
        Player player = event.getPlayer();
        if (player == null) return;

        // hráč musí držať flint_knife v main alebo offhand
        boolean hasKnife =
                player.getMainHandItem().is(stone2steel.FLINT_KNIFE.get()) ||
                        player.getOffhandItem().is(stone2steel.FLINT_KNIFE.get());
        if (!hasKnife) return;

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);

        // (A) vanilla krátka/dlhá tráva alebo (B) čokoľvek v našom tagu
        boolean isGrassVanilla = state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS);
        boolean isFiberPlant = isGrassVanilla || state.is(FIBER_PLANTS);
        if (!isFiberPlant) return;

        // dropni 1–2x plant_fiber
        RandomSource rng = level.getRandom();
        int count = 1 + (rng.nextFloat() < 0.5f ? 1 : 0);

        ItemStack drop = new ItemStack(stone2steel.PLANT_FIBER.get(), count);
        ItemEntity ent = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.4, pos.getZ() + 0.5, drop);
        level.addFreshEntity(ent);

        // opotrebenie noža (vezmeme ten v mainhande ak je, inak offhand)
        ItemStack knife = player.getMainHandItem().is(stone2steel.FLINT_KNIFE.get())
                ? player.getMainHandItem()
                : player.getOffhandItem();
        knife.hurtAndBreak(1, (LivingEntity) player, null);
    }
}
