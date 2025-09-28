package sk.gothmur.mod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import sk.gothmur.mod.block.HeatedRockBlock;
import sk.gothmur.mod.stone2steel;

public class GraniteMaulItem extends Item {
    public GraniteMaulItem(Properties props) {
        super(props);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // 1) Fractured rock → veľmi rýchlo (cieľ ~3 rany pocitovo)
        if (state.is(stone2steel.FRACTURED_ROCK.get())) {
            return 12.0F; // tuned for ~3 hits on fractured
        }

        // 2) Heated rock → trochu rýchlejšie podľa stage, ale nie zadarmo
        if (state.is(stone2steel.HEATED_ROCK.get())) {
            int stage = state.getValue(HeatedRockBlock.HEAT_STAGE);
            return switch (stage) {
                case 3 -> 2.0F;
                case 2 -> 1.5F;
                case 1 -> 1.2F;
                default -> 1.0F;
            };
        }

        // 3) Surový kameň & meďná ruda → pomalé (cieľ ~15 rán pocitovo)
        if (isRawStoneOrCopper(state)) {
            return 0.4F;
        }

        // ostatné bloky – default
        return super.getDestroySpeed(stack, state);
    }

    private static boolean isRawStoneOrCopper(BlockState s) {
        return s.is(Blocks.STONE) ||
                s.is(Blocks.ANDESITE) ||
                s.is(Blocks.DIORITE) ||
                s.is(Blocks.GRANITE) ||
                s.is(Blocks.DEEPSLATE) ||
                s.is(Blocks.COPPER_ORE) ||
                s.is(Blocks.DEEPSLATE_COPPER_ORE);
    }
}
