package sk.gothmur.mod.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import sk.gothmur.mod.blockentity.GrindstoneBlockEntity;
import sk.gothmur.mod.client.model.GrindstoneGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class GrindstoneRenderer extends GeoBlockRenderer<GrindstoneBlockEntity> {

    // Dôležité: renderer musí mať Context konštruktor, aby sedel na
    // BlockEntityRenderers.register(..., GrindstoneRenderer::new)
    public GrindstoneRenderer(BlockEntityRendererProvider.Context ctx) {
        super(new GrindstoneGeoModel());
    }
}
