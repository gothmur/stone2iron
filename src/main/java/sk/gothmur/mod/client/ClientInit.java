package sk.gothmur.mod.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

import sk.gothmur.mod.stone2steel;
import sk.gothmur.mod.client.GrindstoneRenderer;

@EventBusSubscriber(modid = stone2steel.MODID, value = Dist.CLIENT)
public class ClientInit {

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // BlockEntity renderer pre grindstone (GeckoLib GeoBlockRenderer)
        event.registerBlockEntityRenderer(stone2steel.GRINDSTONE_BE.get(), GrindstoneRenderer::new);
    }
}
