package sk.gothmur.mod.item;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import sk.gothmur.mod.stone2steel;

public class BarkContainerEmptyItem extends Item {
    public BarkContainerEmptyItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Raycast len na SOURCE vodu
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResultHolder.pass(stack);

        var pos = hit.getBlockPos();
        var state = level.getBlockState(pos);
        if (!state.getFluidState().is(Fluids.WATER)) return InteractionResultHolder.pass(stack);

        // Naplnenie: empty -> full
        if (!level.isClientSide) {
            ItemStack full = new ItemStack(stone2steel.BARK_CONTAINER_FULL.get());
            // zober 1 ks z ruky a daj full (ak bol stacknutý, vráť zvyšok do inventára)
            stack.shrink(1);
            if (!player.addItem(full)) player.drop(full, false);
            level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.6f, 1.2f);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
