package sk.gothmur.mod.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class HeatedRockBlock extends Block {
    public static final EnumProperty<RockBase> BASE = EnumProperty.create("base", RockBase.class);
    public static final IntegerProperty HEAT_STAGE = IntegerProperty.create("heat_stage", 0, 3);

    public HeatedRockBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BASE, RockBase.STONE)
                .setValue(HEAT_STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(BASE, HEAT_STAGE);
    }
}
