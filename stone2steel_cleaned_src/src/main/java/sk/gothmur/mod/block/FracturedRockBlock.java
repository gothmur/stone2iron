package sk.gothmur.mod.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public class FracturedRockBlock extends Block {
    public static final EnumProperty<RockBase> BASE = EnumProperty.create("base", RockBase.class);

    public FracturedRockBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BASE, RockBase.STONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, net.minecraft.world.level.block.state.BlockState> builder) {
        builder.add(BASE);
    }
}
