package sk.gothmur.mod.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Flint Sword – parametre ako wood sword (4 dmg, -2.4 AS, 59 durability). */
public class FlintSwordItem extends SwordItem {
    public FlintSwordItem(Tier tier, Item.Properties props) {
        // +3 modifier pri WOOD -> celkový útok 4, speed -2.4 (vanilla sword)
        super(tier, props.attributes(SwordItem.createAttributes(tier, 3, -2.4F)));
    }
}
