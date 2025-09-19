package sk.gothmur.mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.world.item.Item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hlavná trieda módu – minimalistická verzia
 */
@Mod(stone2steel.MODID)
public class stone2steel {
    public static final String MODID = "stone2steel";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Registrácia ITEMOV pod naším namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    // Náš nový item: ostrý pazúrik (flint shard)
    public static final DeferredItem<Item> FLINT_SHARD = ITEMS.registerSimpleItem("flint_shard");

    public stone2steel(IEventBus modEventBus, ModContainer modContainer) {
        // lifecycle
        modEventBus.addListener(this::commonSetup);

        // registrácia deferred registries
        ITEMS.register(modEventBus);

        // config šablóna (ponechané, ak používaš template Config; môžeš vypnúť, ak nechceš)
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[{}] init complete", MODID);
    }
}
