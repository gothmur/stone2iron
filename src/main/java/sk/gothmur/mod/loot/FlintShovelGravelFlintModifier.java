package sk.gothmur.mod.loot;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import sk.gothmur.mod.stone2steel;

public class FlintShovelGravelFlintModifier extends LootModifier {
    private final float extraChance;

    // JSON codec: "conditions" + "extra_chance"
    public static final MapCodec<FlintShovelGravelFlintModifier> CODEC = RecordCodecBuilder.mapCodec(inst ->
            LootModifier.codecStart(inst).and(
                    Codec.FLOAT.fieldOf("extra_chance").forGetter(m -> m.extraChance)
            ).apply(inst, FlintShovelGravelFlintModifier::new)
    );

    public FlintShovelGravelFlintModifier(LootItemCondition[] conditions, float extraChance) {
        super(conditions);
        this.extraChance = extraChance;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext ctx) {
        // 1) Musí sa ťažiť GRAVEL
        var state = ctx.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null || state.getBlock() != Blocks.GRAVEL) return generatedLoot;

        // 2) Nástroj musí byť NAŠA flint lopata
        ItemStack tool = ctx.getParamOrNull(LootContextParams.TOOL);
        if (tool == null || tool.getItem() != stone2steel.FLINT_SHOVEL.get()) return generatedLoot;

        // 3) Silk Touch má prednosť -> nič nepridávame
        var enchLookup = ctx.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> silk = enchLookup.getOrThrow(Enchantments.SILK_TOUCH);
        int silkLevel = tool.getEnchantmentLevel(silk);
        if (silkLevel > 0) return generatedLoot;

        // 4) Ak už vanilla loot flint pridal, nepridávame druhý
        boolean alreadyHasFlint = generatedLoot.stream().anyMatch(s -> s.is(Items.FLINT));
        if (alreadyHasFlint) return generatedLoot;

        // 5) Náhodná extra šanca
        RandomSource rand = ctx.getRandom();
        if (rand.nextFloat() < this.extraChance) {
            generatedLoot.add(new ItemStack(Items.FLINT));
        }
        return generatedLoot;
    }
}
