package sk.gothmur.mod;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import sk.gothmur.mod.item.FlintBifaceItem;
import sk.gothmur.mod.item.FlintKnifeItem;

@Mod(stone2steel.MODID)
public class stone2steel {
    public static final String MODID = "stone2steel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // --- základné suroviny a komponenty ---
    public static final DeferredItem<Item> FLINT_SHARD  = ITEMS.registerSimpleItem("flint_shard");
    public static final DeferredItem<Item> TENDON       = ITEMS.registerSimpleItem("tendon");
    public static final DeferredItem<Item> PLANT_FIBER  = ITEMS.registerSimpleItem("plant_fiber");
    public static final DeferredItem<Item> CORDAGE      = ITEMS.registerSimpleItem("cordage");
    public static final DeferredItem<Item> ROPE         = ITEMS.registerSimpleItem("rope");
    public static final DeferredItem<Item> TREE_SAP     = ITEMS.registerSimpleItem("tree_sap");
    public static final DeferredItem<Item> TREE_BARK    = ITEMS.registerSimpleItem("tree_bark");
    public static final DeferredItem<Item> SAP_GLUE     = ITEMS.registerSimpleItem("sap_glue");
    public static final DeferredItem<Item> ASH          = ITEMS.registerSimpleItem("ash");

    // --- polotovary pre bowdrill slučku ---
    public static final DeferredItem<Item> CURVED_BRANCH = ITEMS.registerSimpleItem("curved_branch");
    public static final DeferredItem<Item> WOOD_BILLET   = ITEMS.registerSimpleItem("wood_billet");
    public static final DeferredItem<Item> SPINDLE       = ITEMS.registerSimpleItem("spindle");
    public static final DeferredItem<Item> FIREBOARD     = ITEMS.registerSimpleItem("fireboard");
    public static final DeferredItem<Item> BOW_DRILL     = ITEMS.registerSimpleItem("bow_drill");

    // --- nástroje ---
    public static final DeferredItem<Item> FLINT_BIFACE = ITEMS.register("flint_biface",
            () -> new FlintBifaceItem(new Item.Properties(), 1.0D, -2.2D)); // dmg, speed

    // DOPLNENÉ: trvácnosť (durability) = 45
    public static final DeferredItem<Item> FLINT_KNIFE = ITEMS.register("flint_knife",
            () -> new FlintKnifeItem(new Item.Properties(), 1.0D, -2.0D, 45)); // dmg, speed, durability

    // --- creative tab ---
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> S2S_TAB =
            TABS.register("s2s_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.stone2steel"))
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .icon(() -> FLINT_BIFACE.get().getDefaultInstance())
                    .displayItems((params, out) -> {
                        out.accept(FLINT_SHARD.get());
                        out.accept(FLINT_BIFACE.get());
                        out.accept(FLINT_KNIFE.get());

                        out.accept(TENDON.get());
                        out.accept(PLANT_FIBER.get());
                        out.accept(CORDAGE.get());
                        out.accept(ROPE.get());
                        out.accept(TREE_SAP.get());
                        out.accept(TREE_BARK.get());
                        out.accept(SAP_GLUE.get());
                        out.accept(ASH.get());

                        out.accept(CURVED_BRANCH.get());
                        out.accept(WOOD_BILLET.get());
                        out.accept(SPINDLE.get());
                        out.accept(FIREBOARD.get());
                        out.accept(BOW_DRILL.get());
                    })
                    .build());

    public stone2steel(IEventBus modBus, ModContainer modContainer) {
        ITEMS.register(modBus);
        TABS.register(modBus);
        modBus.addListener(this::commonSetup);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent e) {
        LOGGER.info("[{}] init complete", MODID);
    }
}
