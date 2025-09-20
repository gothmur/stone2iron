package sk.gothmur.mod;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import sk.gothmur.mod.item.FlintBifaceItem;
import sk.gothmur.mod.item.FlintKnifeItem;

@Mod(stone2steel.MODID)
public class stone2steel {
    public static final String MODID = "stone2steel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // existujúce
    public static final DeferredItem<Item> FLINT_SHARD = ITEMS.registerSimpleItem("flint_shard");
    public static final DeferredItem<Item> FLINT_BIFACE = ITEMS.register("flint_biface",
            () -> new FlintBifaceItem(new Item.Properties(), 4.0, -2.8));
    public static final DeferredItem<Item> TENDON = ITEMS.registerSimpleItem("tendon");
    public static final DeferredItem<Item> TREE_SAP = ITEMS.registerSimpleItem("tree_sap");
    public static final DeferredItem<Item> TREE_BARK = ITEMS.registerSimpleItem("tree_bark");

    // NOVÉ – výrobná línia
    public static final DeferredItem<Item> ASH = ITEMS.registerSimpleItem("ash");                       // popol
    public static final DeferredItem<Item> SAP_GLUE = ITEMS.registerSimpleItem("sap_glue");             // lepidlo (sap + ash)
    public static final DeferredItem<Item> PLANT_FIBER = ITEMS.registerSimpleItem("plant_fiber");       // vlákno (z trávy – nožík)
    public static final DeferredItem<Item> CORDAGE = ITEMS.registerSimpleItem("cordage");               // zväzok vlákien
    public static final DeferredItem<Item> ROPE = ITEMS.registerSimpleItem("rope");                     // lano

    // Nožík – odomyká zber plant_fiber z trávy
    public static final DeferredItem<Item> FLINT_KNIFE = ITEMS.register("flint_knife",
            () -> new FlintKnifeItem(new Item.Properties(), 2.0, -1.8, 60)); // dmg 2, rýchlejší, 60 použití

    // náš Creative Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> STONE2STEEL_TAB =
            CREATIVE_MODE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.stone2steel"))
                    .icon(() -> FLINT_BIFACE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(FLINT_SHARD.get());
                        output.accept(TENDON.get());
                        output.accept(TREE_SAP.get());
                        output.accept(TREE_BARK.get());
                        output.accept(ASH.get());
                        output.accept(SAP_GLUE.get());
                        output.accept(PLANT_FIBER.get());
                        output.accept(CORDAGE.get());
                        output.accept(ROPE.get());
                        output.accept(FLINT_KNIFE.get());
                        output.accept(FLINT_BIFACE.get());
                    })
                    .build()
            );

    public stone2steel(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("[{}] init complete", MODID);
    }
}
