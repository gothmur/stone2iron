package sk.gothmur.mod.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import sk.gothmur.mod.blockentity.GrindstoneBlockEntity;
import sk.gothmur.mod.stone2steel;

public class GrindstoneBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    // 1.21.x: Block musí mať codec so správnym typom
    public static final MapCodec<GrindstoneBlock> CODEC = simpleCodec(GrindstoneBlock::new);
    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    // menšia kolízia/outline – cca pol bloku
    private static final VoxelShape SHAPE_HALF = Block.box(0, 0, 0, 16, 8, 16);

    // ✔ no-arg konštruktor pre registráciu cez Supplier (ModRegistry.register("grindstone", GrindstoneBlock::new))
    public GrindstoneBlock() {
        this(BlockBehaviour.Properties.of().strength(2.0F, 6.0F).noOcclusion());
    }

    public GrindstoneBlock(BlockBehaviour.Properties props) {
        super(props);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return this.defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // render cez GeoBlockRenderer (EntityBlockRenderer)
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    // outline hitbox pri mierení
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_HALF;
    }

    // kolízia s entitami
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE_HALF;
    }

    // 1.21.x interaction pipeline: namiesto use(...) sa prepisuje useWithoutItem(...)
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Etapa 1: držanie RMB rieši klientský tick handler + sieť, tu nič nerobíme
        return InteractionResult.PASS;
    }

    @Override @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GrindstoneBlockEntity(pos, state);
    }

    // DÔLEŽITÉ: ticker beží na serveri aj na klientovi
    @Override @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != stone2steel.GRINDSTONE_BE.get()) return null;

        if (level.isClientSide) {
            return (lvl, p, st, be) -> GrindstoneBlockEntity.clientTick(lvl, p, st, (GrindstoneBlockEntity) be);
        } else {
            return (lvl, p, st, be) -> GrindstoneBlockEntity.serverTick(lvl, p, st, (GrindstoneBlockEntity) be);
        }
    }
}
