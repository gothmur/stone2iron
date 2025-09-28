package sk.gothmur.mod.util;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import sk.gothmur.mod.stone2steel;

public class ModTags {
    public static class Blocks {
        public static final TagKey<Block> ABRASIVE_SURFACES =
                TagKey.create(Registries.BLOCK,
                        ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "abrasive_surfaces"));
    }
}
