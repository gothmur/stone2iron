package sk.gothmur.mod.item;

import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

public class FlintShovelItem extends ShovelItem {
    public FlintShovelItem(Tier tier, Item.Properties props) {
        super(tier, props);
    }

    // Žiadne opravovanie (ani cez kovadlinu)
    @Override
    public boolean isValidRepairItem(ItemStack toRepair, ItemStack repair) {
        return false;
    }
}
