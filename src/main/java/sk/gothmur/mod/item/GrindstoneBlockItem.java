package sk.gothmur.mod.item;

import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;
import sk.gothmur.mod.client.GrindstoneItemRenderer;

import java.util.function.Consumer;

/**
 * Geo item pre grindstone blok – custom 3D render v ruke/inventári bez RenderProvider API.
 */
public class GrindstoneBlockItem extends BlockItem implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GrindstoneBlockItem(Block block, Item.Properties props) {
        super(block, props);
    }

    // NeoForge cesta: priraď BEWLR cez IClientItemExtensions
    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private GrindstoneItemRenderer renderer;
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new GrindstoneItemRenderer();
                return renderer;
            }
        });
    }

    // GeoAnimatable: item nijako neanimujeme, takže bez kontrolérov
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // no-op
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object itemStack) {
        return 0;
    }
}
