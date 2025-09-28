package sk.gothmur.mod.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import sk.gothmur.mod.recipe.FullDurabilityShapelessRecipe;
import sk.gothmur.mod.stone2steel;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, stone2steel.MODID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FullDurabilityShapelessRecipe>>
            FULL_DURABILITY_SHAPELESS = RECIPE_SERIALIZERS.register(
            "full_durability_shapeless",
            FullDurabilityShapelessRecipe.Serializer::new
    );

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}
