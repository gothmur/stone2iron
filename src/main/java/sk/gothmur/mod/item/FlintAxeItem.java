package sk.gothmur.mod.item;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;

public class FlintAxeItem extends AxeItem {
    public FlintAxeItem(Tier tier, Item.Properties props) {
        super(tier, props);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true; // po craftení niečo zostane (naša sekera)
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        // vrátime poškodenú sekeru (o 1), alebo prázdny stack ak by sa zlomila
        ItemStack copy = stack.copy();
        copy.setCount(1);
        // bezpečné manuálne poškodenie bez hráča/levelu
        int newDamage = copy.getDamageValue() + 1;
        if (newDamage >= copy.getMaxDamage()) {
            return ItemStack.EMPTY; // ak sa má zlomiť pri craftení
        }
        copy.setDamageValue(newDamage);
        return copy;
    }
}
