package sk.gothmur.mod.item;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;

import sk.gothmur.mod.block.FracturedRockBlock;
import sk.gothmur.mod.block.HeatedRockBlock;
import sk.gothmur.mod.block.RockBase;
import sk.gothmur.mod.stone2steel;

public class BarkContainerFullItem extends Item {
    // teraz LÁME až pri stage 3
    private static final int MIN_HEAT_STAGE_TO_CRACK = 3; // 0..3

    public BarkContainerFullItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockState state = level.getBlockState(pos);

        // Musí ísť o horiaci campfire
        if (!state.is(Blocks.CAMPFIRE) || !state.getValue(CampfireBlock.LIT)) {
            return InteractionResult.PASS;
        }

        boolean crackedAny = false;

        if (!level.isClientSide) {
            BlockPos[] targets = new BlockPos[]{
                    pos.above(), pos.below(),
                    pos.north(), pos.east(), pos.south(), pos.west()
            };
            for (BlockPos t : targets) {
                crackedAny |= crackIfHeated(level, t);
            }

            if (crackedAny) {
                // HOT: odstráň campfire, veľká para
                level.removeBlock(pos, false);
                bigSteam((ServerLevel) level, pos);
            } else {
                // Nie je dosť horúci: iba uhas (LIT=false), malá para
                level.setBlock(pos, state.setValue(CampfireBlock.LIT, false), 3);
                smallSteam((ServerLevel) level, pos);
            }

            // Minúť náplň: full -> empty
            var player = ctx.getPlayer();
            if (player != null) {
                ctx.getItemInHand().shrink(1);
                var empty = stone2steel.BARK_CONTAINER_EMPTY.get().getDefaultInstance();
                if (!player.addItem(empty)) player.drop(empty, false);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean crackIfHeated(Level level, BlockPos p) {
        BlockState s = level.getBlockState(p);
        if (!s.is(stone2steel.HEATED_ROCK.get())) return false;

        RockBase base = s.getValue(HeatedRockBlock.BASE);
        int stage = s.getValue(HeatedRockBlock.HEAT_STAGE);

        // praskáme až pri stage == 3
        if (stage < MIN_HEAT_STAGE_TO_CRACK) return false;

        BlockState fractured = stone2steel.FRACTURED_ROCK.get()
                .defaultBlockState()
                .setValue(FracturedRockBlock.BASE, base);

        level.setBlock(p, fractured, 3);
        return true;
    }

    private void bigSteam(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.9f, 1.0f);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                40, 0.6, 0.6, 0.6, 0.01);
    }

    private void smallSteam(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5f, 1.2f);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                12, 0.4, 0.4, 0.4, 0.01);
    }
}
