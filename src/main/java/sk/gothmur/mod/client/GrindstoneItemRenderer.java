package sk.gothmur.mod.client;

import sk.gothmur.mod.client.model.GrindstoneItemModel;
import sk.gothmur.mod.item.GrindstoneBlockItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class GrindstoneItemRenderer extends GeoItemRenderer<GrindstoneBlockItem> {
    public GrindstoneItemRenderer() {
        super(new GrindstoneItemModel());
    }
}
