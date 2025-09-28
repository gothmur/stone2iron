package sk.gothmur.mod.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import sk.gothmur.mod.stone2steel;

public class FullDurabilityShapelessRecipe extends CustomRecipe {

    /** Recept: rope + stick + flint_biface(damage==0) -> flint_axe */
    public FullDurabilityShapelessRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput inv, Level level) {
        boolean hasRope = false, hasStick = false, hasFreshBiface = false;
        int nonEmpty = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getItem(i);
            if (st.isEmpty()) continue;
            nonEmpty++;
            if (!hasRope && st.is(stone2steel.ROPE.get())) { hasRope = true; continue; }
            if (!hasStick && st.is(Items.STICK)) { hasStick = true; continue; }
            if (!hasFreshBiface && st.is(stone2steel.FLINT_BIFACE.get()) && st.getDamageValue() == 0) {
                hasFreshBiface = true; continue;
            }
            return false; // niečo naviac / zlé
        }
        return nonEmpty == 3 && hasRope && hasStick && hasFreshBiface;
    }

    @Override
    public ItemStack assemble(CraftingInput inv, net.minecraft.core.HolderLookup.Provider provider) {
        return new ItemStack(stone2steel.FLINT_AXE.get());
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) { return w * h >= 3; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return sk.gothmur.mod.registry.ModRecipes.FULL_DURABILITY_SHAPELESS.get();
    }

    /* ======================= Serializer (1.21) ======================= */
    public static class Serializer implements RecipeSerializer<FullDurabilityShapelessRecipe> {
        // JSON nepoužívame – vždy rovnaká logika:
        private static final MapCodec<FullDurabilityShapelessRecipe> CODEC =
                MapCodec.unit(new FullDurabilityShapelessRecipe(CraftingBookCategory.MISC));

        // Namiesto StreamCodec.unit(...) použijeme vlastný codec,
        // ktorý nič nezapisuje a pri decode vracia NOVÚ inštanciu.
        private static final StreamCodec<RegistryFriendlyByteBuf, FullDurabilityShapelessRecipe> STREAM_CODEC =
                StreamCodec.of(
                        (buf, recipe) -> { /* no-op, nič nezapisujeme */ },
                        buf -> new FullDurabilityShapelessRecipe(CraftingBookCategory.MISC)
                );

        @Override public MapCodec<FullDurabilityShapelessRecipe> codec() { return CODEC; }
        @Override public StreamCodec<RegistryFriendlyByteBuf, FullDurabilityShapelessRecipe> streamCodec() { return STREAM_CODEC; }
    }
}
