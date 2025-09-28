package sk.gothmur.mod.loot;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import sk.gothmur.mod.stone2steel;

/**
 * Ak padá loot z GRAVEL, s pravdepodobnosťou 'chance' pridá 1x granite_boulder.
 * (Zámerne neškáluje s Fortune; ak chceš neskôr, dá sa doplniť.)
 */
public class GravelBoulderDropModifier extends LootModifier {

    public static final MapCodec<GravelBoulderDropModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            codecStart(inst).and(
                    com.mojang.serialization.codecs.PrimitiveCodec.FLOAT.fieldOf("chance").forGetter(m -> m.chance)
            ).apply(inst, GravelBoulderDropModifier::new)
    );

    private final float chance;

    protected GravelBoulderDropModifier(LootItemCondition[] conditions, float chance) {
        super(conditions);
        this.chance = chance;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        BlockState state = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state != null && state.is(Blocks.GRAVEL)) {
            if (context.getRandom().nextFloat() < this.chance) {
                generatedLoot.add(stone2steel.GRANITE_BOULDER.get().getDefaultInstance());
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
