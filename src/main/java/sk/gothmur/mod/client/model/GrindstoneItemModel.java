package sk.gothmur.mod.client.model;

import net.minecraft.resources.ResourceLocation;
import sk.gothmur.mod.item.GrindstoneBlockItem;
import sk.gothmur.mod.stone2steel;
import software.bernie.geckolib.model.GeoModel;

public class GrindstoneItemModel extends GeoModel<GrindstoneBlockItem> {

    @Override
    public ResourceLocation getModelResource(GrindstoneBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "geo/grindstone.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GrindstoneBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "textures/block/grindstone.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GrindstoneBlockItem animatable) {
        return ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "animations/grindstone.animation.json");
    }
}
