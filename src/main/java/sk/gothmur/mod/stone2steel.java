package sk.gothmur.mod;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import org.slf4j.Logger;

import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import sk.gothmur.mod.block.FireboardBlock;
import sk.gothmur.mod.block.KindlingBlock;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;

import sk.gothmur.mod.registry.ModSounds;
import sk.gothmur.mod.registry.ModRecipes;

import sk.gothmur.mod.item.FlintBifaceItem;
import sk.gothmur.mod.item.FlintKnifeItem;
import sk.gothmur.mod.item.FlintAxeItem;
import sk.gothmur.mod.item.FlintShovelItem;
import sk.gothmur.mod.loot.FlintShovelGravelFlintModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;

@Mod(stone2steel.MODID)
public class stone2steel {
    public static final String MODID = "stone2steel";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // --- ITEMS ---
    public static final DeferredItem<Item> FLINT_SHARD = ITEMS.registerSimpleItem("flint_shard");

    public static final DeferredItem<Item> FLINT_BIFACE =
            ITEMS.register("flint_biface", () -> new FlintBifaceItem(new Item.Properties(), 2.0D, -2.4D));

    public static final DeferredItem<Item> FLINT_KNIFE =
            ITEMS.register("flint_knife", () -> new FlintKnifeItem(new Item.Properties(), 1.0D, -2.0D, 60)); // ⚙ durability=60

    public static final DeferredItem<Item> TENDON = ITEMS.registerSimpleItem("tendon");
    public static final DeferredItem<Item> TREE_SAP = ITEMS.registerSimpleItem("tree_sap");
    public static final DeferredItem<Item> TREE_BARK = ITEMS.registerSimpleItem("tree_bark");
    public static final DeferredItem<Item> PLANT_FIBER = ITEMS.registerSimpleItem("plant_fiber");
    public static final DeferredItem<Item> CORDAGE = ITEMS.registerSimpleItem("cordage");
    public static final DeferredItem<Item> ROPE = ITEMS.registerSimpleItem("rope");
    public static final DeferredItem<Item> ASH = ITEMS.registerSimpleItem("ash");
    public static final DeferredItem<Item> SAP_GLUE = ITEMS.registerSimpleItem("sap_glue");
    public static final DeferredItem<Item> CURVED_BRANCH = ITEMS.registerSimpleItem("curved_branch");
    public static final DeferredItem<Item> SPINDLE = ITEMS.registerSimpleItem("spindle");
    public static final DeferredItem<Item> EMBER = ITEMS.registerSimpleItem("ember");
    public static final DeferredItem<Item> BOW_DRILL = ITEMS.registerSimpleItem("bow_drill"); // názov z receptu
    public static final DeferredItem<Item> TINDER = ITEMS.registerSimpleItem("tinder");

    // Flint Shovel
    public static final DeferredHolder<Item, FlintShovelItem> FLINT_SHOVEL =
            ITEMS.register("flint_shovel", () ->
                    new FlintShovelItem(
                            Tiers.WOOD,
                            new Item.Properties()
                                    .durability(50) // menšia výdrž než drevo (59)
                                    .attributes(DiggerItem.createAttributes(Tiers.WOOD, 1.5F, -3.0F)) // damage/speed pre lopaty
                    )
            );

    // --- Global Loot Modifiers (GLM) ---
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM_SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final Supplier<MapCodec<FlintShovelGravelFlintModifier>> FLINT_SHOVEL_FLINT_CODEC =
            GLM_SERIALIZERS.register("flint_shovel_flint", () -> FlintShovelGravelFlintModifier.CODEC);

    public static final DeferredItem<AxeItem> FLINT_AXE =
            ITEMS.register("flint_axe",
                    () -> new FlintAxeItem(
                            Tiers.STONE,
                            new Item.Properties().attributes(
                                    // Axe = DiggerItem → nastav DMG & AS ako vanilla stone axe
                                    DiggerItem.createAttributes(Tiers.STONE, 7.0F, -3.2F)
                            )
                    ));

    // nový polotovar na drevo-prácu
    public static final DeferredItem<Item> WOOD_BILLET = ITEMS.registerSimpleItem("wood_billet");

    // Fireboard
    public static final DeferredBlock<FireboardBlock> FIREBOARD_BLOCK =
            BLOCKS.register("fireboard",
                    () -> new FireboardBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(1.0F)
                                    .noOcclusion()
                                    .isRedstoneConductor((s,l,p) -> false)
                                    .isSuffocating((s,l,p) -> false)
                                    .isViewBlocking((s,l,p) -> false)
                    )
            );

    public static final DeferredItem<BlockItem> FIREBOARD =
            ITEMS.registerSimpleBlockItem("fireboard", FIREBOARD_BLOCK);

    // Kindling
    public static final DeferredBlock<KindlingBlock> KINDLING_BLOCK =
            BLOCKS.register("kindling",
                    () -> new KindlingBlock(
                            BlockBehaviour.Properties.of()
                                    .mapColor(MapColor.WOOD)
                                    .strength(0.5F)
                                    .noOcclusion()
                                    .isRedstoneConductor((s,l,p) -> false)
                                    .isSuffocating((s,l,p) -> false)
                                    .isViewBlocking((s,l,p) -> false)
                    )
            );

    public static final DeferredItem<BlockItem> KINDLING =
            ITEMS.registerSimpleBlockItem("kindling", KINDLING_BLOCK);

    // Tag pre brúsne povrchy (definovaný v data tagoch); tu len odkaz (public nech ho vedia používať handlery)
    public static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> ABRASIVE_SURFACES =
            net.minecraft.tags.TagKey.create(Registries.BLOCK,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID, "abrasive_surfaces"));

    // --- Creative Tab (nepovinné) ---
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .icon(() -> FLINT_BIFACE.get().getDefaultInstance())
                    .title(Component.translatable("itemGroup.stone2steel"))
                    .withTabsBefore(CreativeModeTabs.TOOLS_AND_UTILITIES)
                    .displayItems((params, out) -> {
                        out.accept(FLINT_SHARD.get());
                        out.accept(FLINT_BIFACE.get());
                        out.accept(FLINT_KNIFE.get());
                        out.accept(TENDON.get());
                        out.accept(TREE_SAP.get());
                        out.accept(TREE_BARK.get());
                        out.accept(PLANT_FIBER.get());
                        out.accept(CORDAGE.get());
                        out.accept(ROPE.get());
                        out.accept(ASH.get());
                        out.accept(SAP_GLUE.get());
                        out.accept(CURVED_BRANCH.get());
                        out.accept(SPINDLE.get());
                        out.accept(WOOD_BILLET.get());
                        out.accept(EMBER.get());
                        out.accept(BOW_DRILL.get());
                        out.accept(FIREBOARD.get());
                        out.accept(KINDLING.get());
                        out.accept(TINDER.get());
                        out.accept(FLINT_AXE.get());
                        out.accept(FLINT_SHOVEL.get()); // <- pridané do tabu
                    })
                    .build());

    // Konštruktor s injekciou mod-busu a kontajnera (NeoForge štýl)
    public stone2steel(IEventBus modBus, ModContainer modContainer) {
        ModRecipes.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        TABS.register(modBus);
        ModSounds.register(modBus);

        // FIX: registrácia GLM na ten istý event bus:
        GLM_SERIALIZERS.register(modBus);

        modBus.addListener(this::commonSetup);

        // správny enum: COMMON (nie Common)
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent evt) {
        LOGGER.info("[{}] init complete", MODID);
    }
}
