package sk.gothmur.mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTabs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import sk.gothmur.mod.item.FlintBifaceItem;

@Mod(stone2steel.MODID)
public class stone2steel {
    public static final String MODID = "stone2steel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    // existujúce itemy
    public static final DeferredItem<Item> FLINT_SHARD = ITEMS.registerSimpleItem("flint_shard");
    public static final DeferredItem<Item> FLINT_BIFACE = ITEMS.register("flint_biface",
            () -> new FlintBifaceItem(new Item.Properties(), 4.0, -2.8));

    // NOVÝ item – šľacha (tendon)
    public static final DeferredItem<Item> TENDON = ITEMS.registerSimpleItem("tendon");

    public stone2steel(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        ITEMS.register(modEventBus);

        // Creative tabs
        modEventBus.addListener(this::addToCreativeTabs);

        // ak používaš configs – nechávam pôvodné
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[{}] init complete", MODID);
    }

    private void addToCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(FLINT_SHARD.get());
            event.accept(TENDON.get());
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(FLINT_BIFACE.get());
        }
    }
}
