package sk.gothmur.mod.client.model;

import net.minecraft.resources.ResourceLocation;
import sk.gothmur.mod.blockentity.GrindstoneBlockEntity;
import sk.gothmur.mod.stone2steel;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class GrindstoneGeoModel extends GeoModel<GrindstoneBlockEntity> {

    @Override
    public ResourceLocation getModelResource(GrindstoneBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "geo/grindstone.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GrindstoneBlockEntity animatable) {
        return ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "textures/block/grindstone.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GrindstoneBlockEntity animatable) {
        // Animácie nepoužívame, ale súbor môže ostať – nevadí.
        return ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "animations/grindstone.animation.json");
    }

    @Override
    public void setCustomAnimations(GrindstoneBlockEntity animatable, long instanceId, AnimationState<GrindstoneBlockEntity> state) {
        super.setCustomAnimations(animatable, instanceId, state);
        var proc = getAnimationProcessor();
        var vrch = proc.getBone("Vrch"); // musí sa presne zhodovať s názvom kosti v .geo.json
        if (vrch != null) {
            vrch.setRotY((float) Math.toRadians(-animatable.getAngleDeg()));
        }
    }
}
