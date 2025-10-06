package sk.gothmur.mod.registry;

import sk.gothmur.mod.stone2steel;
import sk.gothmur.mod.block.GrindstoneBlock;
import sk.gothmur.mod.blockentity.GrindstoneBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, stone2steel.MODID);

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, stone2steel.MODID);

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, stone2steel.MODID);

    // ----- GRINDSTONE -----
    public static final DeferredHolder<Block, GrindstoneBlock> GRINDSTONE =
            BLOCKS.register("grindstone", () -> new GrindstoneBlock());

    public static final DeferredHolder<Item, BlockItem> GRINDSTONE_ITEM =
            ITEMS.register("grindstone", () -> new BlockItem(GRINDSTONE.get(), new Item.Properties()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GrindstoneBlockEntity>> GRINDSTONE_BE =
            BLOCK_ENTITIES.register("grindstone",
                    () -> BlockEntityType.Builder.of(GrindstoneBlockEntity::new, GRINDSTONE.get()).build(null));

    public static void init(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
    }
}
