package sk.gothmur.mod.perishables;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class Perishables {
    private Perishables() {}

    // CUSTOM_DATA → náš vnorený root + kľúče
    private static final String TAG_ROOT      = "s2s_perish";
    private static final String TAG_AGE_TICKS = "ageTicks";
    private static final String TAG_LAST_TICK = "lastTick";

    // Tagy pre hodnotenie „pivnice“
    private static final TagKey<Block> COOL_CELLAR_BLOCKS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("stone2steel", "cool_cellar_blocks"));
    private static final TagKey<Block> POOR_CELLAR_BLOCKS =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("stone2steel", "poor_cellar_blocks"));

    // 1 MC deň = 24000 tickov
    private static final long SHELF_RAW_PORK_TICKS    = 2L * 24000L;
    private static final long SHELF_COOKED_PORK_TICKS = 3L * 24000L;

    // Ako často persistne zapisovať dáta pre DRŽANÝ item (aby necukal) – 60 s
    private static final long HELD_PERSIST_INTERVAL = 20L * 60L;

    // Povolená odchýlka čerstvosti pre stacking (v percentách)
    private static final int STACK_TOLERANCE_PERCENT = 10;

    // throttlingy
    private static int gateInv = 0;
    private static int gateContainers = 0;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(Perishables::onPlayerTickPost);
        NeoForge.EVENT_BUS.addListener(Perishables::onTooltip);
        NeoForge.EVENT_BUS.addListener(Perishables::onStackedOnOther);
    }

    // --- Server tick: prepočet v inventári/offhand + (voliteľne) kontajnery pri hráčovi ---
    public static void onPlayerTickPost(final PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;

        // INVENTÁR / OFFHAND
        if (PerishablesConfig.ENABLED.get() && PerishablesConfig.DECAY_RATE.get() > 0.0) {
            gateInv++;
            if (gateInv % PerishablesConfig.TICK_INTERVAL.get() == 0) {
                long now = level.getGameTime();

                // Main inventory sloty
                for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
                    ItemStack st = sp.getInventory().getItem(i);
                    processStackInSlot(level, sp, i, st, now);
                }

                // Offhand
                ItemStack off = sp.getOffhandItem();
                processOffhand(level, sp, off, now);
            }
        }

        // KONTAJNERY V OKOLÍ HRÁČA (budgetované)
        if (PerishablesConfig.CONTAINERS_DECAY.get()
                && PerishablesConfig.CONTAINERS_NEAR_PLAYER.get()
                && PerishablesConfig.DECAY_RATE.get() > 0.0) {

            gateContainers++;
            if (gateContainers % PerishablesConfig.CONTAINERS_TICK_INTERVAL.get() == 0) {
                long now = level.getGameTime();
                tickNearbyContainers(level, sp, now);
            }
        }
    }

    // --- Tooltip (klient): zobraz "Zkazí sa o: Xr Yd Zh" (odhad podľa aktuálneho prostredia) ---
    public static void onTooltip(final ItemTooltipEvent e) {
        ItemStack st = e.getItemStack();
        if (!isPerishablePilot(st)) return;

        // odstráň staré percentuálne riadky z nášho bývalého handlera (ak ešte niekde existuje)
        var tips = e.getToolTip();
        tips.removeIf(c -> {
            var cont = c.getContents();
            if (cont instanceof TranslatableContents tc) {
                String key = tc.getKey();
                return key.startsWith("tooltip.stone2steel.") && key.contains("percent");
            }
            return false;
        });

        Level lvl = e.getEntity() != null ? e.getEntity().level() : Minecraft.getInstance().level;
        long now = (lvl != null) ? lvl.getGameTime() : 0L;

        CompoundTag root = readRoot(st);
        if (root == null) {
            long shelf = shelfTicks(st);
            tips.add(Component.literal("Zkazí sa o: " + formatMcTime(shelf)));
            return;
        }

        long shelf = shelfTicks(st);
        long age   = root.getLong(TAG_AGE_TICKS);
        long last  = root.getLong(TAG_LAST_TICK);

        long dt = (now > 0L && last > 0L) ? Math.max(0L, now - last) : 0L;

        // aktuálny multiplikátor kazenia (prostredie × config)
        double mul;
        if (lvl != null && e.getEntity() != null) {
            mul = envMultiplier(lvl, e.getEntity().blockPosition()) * PerishablesConfig.DECAY_RATE.get();
        } else {
            mul = PerishablesConfig.DECAY_RATE.get();
        }

        // dopočítané "teraz"
        long ageNow = age + (long)Math.floor(dt * Math.max(0.0, mul));

        if (ageNow >= shelf) {
            tips.add(Component.literal("Zhnité"));
            return;
        }

        // Koľko "age" ešte chýba do prahu skazenia
        long remainingAge = shelf - ageNow;

        // Odhad reálnych MC tickov, kým sa to pokazí pri súčasných podmienkach:
        // age += dt * mul  =>  dt ~= remainingAge / mul
        if (mul <= 0.0) {
            tips.add(Component.literal("Nekazí sa (decayRate = 0)"));
            return;
        }
        long ticksLeft = (long)Math.ceil(remainingAge / mul);

        tips.add(Component.literal("Zkazí sa o: " + formatMcTime(ticksLeft)));
    }

    // --- INVENTORY STACKING: 10 % tolerancia + vážený priemer ---
    private static void onStackedOnOther(final ItemStackedOnOtherEvent e) {
        var player = e.getPlayer();
        if (!(player instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel level)) return;
        if (!PerishablesConfig.ENABLED.get() || PerishablesConfig.DECAY_RATE.get() <= 0.0) return;

        ItemStack carried = e.getCarriedItem();   // stack na kurzore
        ItemStack target  = e.getStackedOnItem(); // stack v slote, na ktorý kladieš

        // pilot len pre porkchop (rovnaký item na oboch)
        if (!isPerishablePilot(carried) || !isPerishablePilot(target)) return;
        if (carried.getItem() != target.getItem()) return;

        long now   = level.getGameTime();
        long shelf = shelfTicks(target);
        long age1  = currentAgeNow(level, sp.blockPosition(), carried, now);
        long age2  = currentAgeNow(level, sp.blockPosition(), target,  now);

        int p1 = percentFresh(age1, shelf);
        int p2 = percentFresh(age2, shelf);
        int diff = Math.abs(p1 - p2);

        if (diff > STACK_TOLERANCE_PERCENT) {
            e.setCanceled(true);
            return;
        }

        // vážený priemer podľa počtu kusov
        int c1 = carried.getCount();
        int c2 = target.getCount();
        int total = c1 + c2;
        if (total <= 0) return;

        long combinedAge = Math.round((age1 * (double)c1 + age2 * (double)c2) / (double) total);
        if (combinedAge < 0) combinedAge = 0;
        if (combinedAge > shelf) combinedAge = shelf;

        // zapíš rovnaký CUSTOM_DATA root do oboch → vanilla ich zloží
        CompoundTag rootTag = new CompoundTag();
        rootTag.putLong(TAG_AGE_TICKS, combinedAge);
        rootTag.putLong(TAG_LAST_TICK, now);

        CompoundTag all1 = readAllCustom(carried);
        all1.put(TAG_ROOT, rootTag.copy());
        writeAllCustom(carried, all1);

        CompoundTag all2 = readAllCustom(target);
        all2.put(TAG_ROOT, rootTag.copy());
        writeAllCustom(target, all2);
    }

    // --- Jadro prepočtu (inventár hráča) ---
    private static void processStackInSlot(ServerLevel level, ServerPlayer sp, int slotIdx, ItemStack st, long now) {
        if (!isPerishablePilot(st)) return;

        CompoundTag tag  = readAllCustom(st);
        CompoundTag root = tag.getCompound(TAG_ROOT);

        long last = root.getLong(TAG_LAST_TICK);
        if (last == 0L) {
            root.putLong(TAG_LAST_TICK, now);
            root.putLong(TAG_AGE_TICKS, 0L);
            tag.put(TAG_ROOT, root);
            writeAllCustom(st, tag);
            return;
        }

        long dt = Math.max(0L, now - last);
        if (dt == 0L) return;

        double mul = envMultiplier(level, sp.blockPosition()) * PerishablesConfig.DECAY_RATE.get();
        long add   = (long)Math.floor(dt * mul);

        long oldAge = root.getLong(TAG_AGE_TICKS);
        long newAge = oldAge + add;
        long shelf  = shelfTicks(st);

        // Ak úplne vyhnil → hneď persist a nahraď
        if (newAge >= shelf) {
            int count = st.getCount();
            ItemStack rotten = new ItemStack(sk.gothmur.mod.stone2steel.ROTTEN_SCRAPS.get(), count);
            sp.getInventory().setItem(slotIdx, rotten);
            return;
        }

        // Je to DRŽANÝ hotbarový slot?
        boolean isSelectedHotbar = (slotIdx >= 0 && slotIdx < 9 && sp.getInventory().selected == slotIdx);
        boolean crossedStage = crossedStage(oldAge, newAge, shelf);
        boolean timeForHeld  = (now - last) >= HELD_PERSIST_INTERVAL;

        boolean shouldPersist = !isSelectedHotbar || crossedStage || timeForHeld;

        if (shouldPersist) {
            root.putLong(TAG_AGE_TICKS, newAge);
            root.putLong(TAG_LAST_TICK, now);
            tag.put(TAG_ROOT, root);
            writeAllCustom(st, tag);
        }
    }

    private static void processOffhand(ServerLevel level, ServerPlayer sp, ItemStack st, long now) {
        if (!isPerishablePilot(st)) return;

        CompoundTag tag  = readAllCustom(st);
        CompoundTag root = tag.getCompound(TAG_ROOT);

        long last = root.getLong(TAG_LAST_TICK);
        if (last == 0L) {
            root.putLong(TAG_LAST_TICK, now);
            root.putLong(TAG_AGE_TICKS, 0L);
            tag.put(TAG_ROOT, root);
            writeAllCustom(st, tag);
            return;
        }

        long dt = Math.max(0L, now - last);
        if (dt == 0L) return;

        double mul = envMultiplier(level, sp.blockPosition()) * PerishablesConfig.DECAY_RATE.get();
        long add   = (long)Math.floor(dt * mul);

        long oldAge = root.getLong(TAG_AGE_TICKS);
        long newAge = oldAge + add;
        long shelf  = shelfTicks(st);

        if (newAge >= shelf) {
            int count = st.getCount();
            ItemStack rotten = new ItemStack(sk.gothmur.mod.stone2steel.ROTTEN_SCRAPS.get(), count);
            sp.setItemInHand(InteractionHand.OFF_HAND, rotten);
            return;
        }

        boolean timeForHeld = (now - last) >= HELD_PERSIST_INTERVAL;
        boolean crossedStage = crossedStage(oldAge, newAge, shelf);

        if (timeForHeld || crossedStage) {
            root.putLong(TAG_AGE_TICKS, newAge);
            root.putLong(TAG_LAST_TICK, now);
            tag.put(TAG_ROOT, root);
            writeAllCustom(st, tag);
        }
    }

    // --- Kontajnery v okolí hráča (budgetované) ---
    private static void tickNearbyContainers(ServerLevel level, ServerPlayer sp, long now) {
        int budget = PerishablesConfig.CONTAINERS_PER_PLAYER_BUDGET.get();
        if (budget <= 0) return;

        int radiusBlocks = PerishablesConfig.CONTAINERS_SEARCH_RADIUS_BLOCKS.get();
        int rChunks = Math.max(1, (radiusBlocks + 15) >> 4);
        ChunkPos cpos = sp.chunkPosition();
        double maxDist2 = (double)radiusBlocks * (double)radiusBlocks;

        for (int cz = cpos.z - rChunks; cz <= cpos.z + rChunks && budget > 0; cz++) {
            for (int cx = cpos.x - rChunks; cx <= cpos.x + rChunks && budget > 0; cx++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (budget <= 0) break;
                    if (!(be instanceof Container container)) continue;

                    BlockPos pos = be.getBlockPos();
                    double dx = (pos.getX() + 0.5) - sp.getX();
                    double dy = (pos.getY() + 0.5) - sp.getY();
                    double dz = (pos.getZ() + 0.5) - sp.getZ();
                    double dist2 = dx*dx + dy*dy + dz*dz;
                    if (dist2 > maxDist2) continue;

                    // spracuj obsah kontajnera
                    processContainer(level, pos, container, now);

                    // sync pre BE kontajnery
                    if (be instanceof BaseContainerBlockEntity bce) {
                        bce.setChanged();
                    }

                    budget--;
                }
            }
        }
    }

    private static void processContainer(ServerLevel level, BlockPos pos, Container container, long now) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack st = container.getItem(i);
            if (!isPerishablePilot(st)) continue;

            CompoundTag tag  = readAllCustom(st);
            CompoundTag root = tag.getCompound(TAG_ROOT);

            long last = root.getLong(TAG_LAST_TICK);
            if (last == 0L) {
                root.putLong(TAG_LAST_TICK, now);
                root.putLong(TAG_AGE_TICKS, 0L);
                tag.put(TAG_ROOT, root);
                writeAllCustom(st, tag);
                container.setItem(i, st);
                continue;
            }

            long dt = Math.max(0L, now - last);
            if (dt == 0L) continue;

            double mul = envMultiplier(level, pos) * PerishablesConfig.DECAY_RATE.get();
            long add   = (long)Math.floor(dt * mul);

            long age   = root.getLong(TAG_AGE_TICKS) + add;
            long shelf = shelfTicks(st);

            if (age >= shelf) {
                int count = st.getCount();
                ItemStack rotten = new ItemStack(sk.gothmur.mod.stone2steel.ROTTEN_SCRAPS.get(), count);
                container.setItem(i, rotten);
            } else {
                root.putLong(TAG_AGE_TICKS, age);
                root.putLong(TAG_LAST_TICK, now);
                tag.put(TAG_ROOT, root);
                writeAllCustom(st, tag);
                container.setItem(i, st); // zabezpečí sync do klienta
            }
        }
    }

    // --- Helpers: pilot perishable detection + math ---
    private static boolean isPerishablePilot(ItemStack st) {
        return st.is(Items.PORKCHOP) || st.is(Items.COOKED_PORKCHOP);
    }

    private static long shelfTicks(ItemStack st) {
        return st.is(Items.PORKCHOP) ? SHELF_RAW_PORK_TICKS : SHELF_COOKED_PORK_TICKS;
    }

    private static int percentFresh(long age, long shelf) {
        if (shelf <= 0L) return 0;
        double p = 100.0 - (100.0 * ((double)age / (double)shelf));
        if (p < 0) p = 0;
        if (p > 100) p = 100;
        return (int)Math.round(p);
    }

    private static String stageKey(int pct) {
        if (pct >= 70) return "tooltip.stone2steel.fresh_percent";
        if (pct >= 30) return "tooltip.stone2steel.stale_percent";
        if (pct >= 1)  return "tooltip.stone2steel.spoiled_percent";
        return "tooltip.stone2steel.rotten";
    }

    /** Zistí, či sa percento presunulo cez 70/30/1 hranicu (zmena fázy). */
    private static boolean crossedStage(long oldAge, long newAge, long shelf) {
        int oldPct = percentFresh(oldAge, shelf);
        int newPct = percentFresh(newAge, shelf);
        return stageIndex(oldPct) != stageIndex(newPct);
    }

    private static int stageIndex(int pct) {
        if (pct >= 70) return 3; // Fresh
        if (pct >= 30) return 2; // Stale
        if (pct >= 1)  return 1; // Spoiled
        return 0;                // Rotten
    }

    // aktuálny age "teraz" bez zápisu (na serveri)
    private static long currentAgeNow(Level level, BlockPos pos, ItemStack st, long now) {
        CompoundTag root = readRoot(st);
        if (root == null) return 0L;
        long last = root.getLong(TAG_LAST_TICK);
        long age  = root.getLong(TAG_AGE_TICKS);
        long dt   = Math.max(0L, now - last);
        double mul = envMultiplier(level, pos) * PerishablesConfig.DECAY_RATE.get();
        long add  = (long)Math.floor(dt * mul);
        return age + add;
    }

    // --- Prostredie (server aj klient – vrátane „pivnice“) ---
    private static double envMultiplier(Level level, BlockPos p) {
        double m = 1.0;

        // 1) Teplota biomu
        float temp = level.getBiome(p).value().getBaseTemperature();
        if (temp <= 0.15f) m *= 0.60;
        else if (temp >= 1.00f) m *= 1.10;

        // 2) „Root cellar“ – pod hladinou mora a nízky skylight
        int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, p);
        if (p.getY() < level.getSeaLevel() && sky <= 4) {
            m *= 0.50; // základné chladenie pod zemou
        }

        // 3) Hrúbka „strechy“ nad kontajnerom (počet súvislých plných blokov)
        int depth = overheadDepth(level, p, 8); // pozeráme max 8 blokov hore
        if (depth >= 5)      m *= 0.70; // veľmi dobre prekryté
        else if (depth >= 3) m *= 0.85; // slušne prekryté

        // 4) Materiál okolia – kameňová pivnica lepšia než hlina/drevo (kocka 5×5×5 okolo)
        int r = 2;
        int solid = 0, cool = 0, poor = 0;
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    c.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz);
                    BlockState s = level.getBlockState(c);
                    if (s.isAir()) continue;
                    if (!s.canOcclude()) continue; // rátame len plné bloky
                    solid++;
                    if (s.is(COOL_CELLAR_BLOCKS))      cool++;
                    else if (s.is(POOR_CELLAR_BLOCKS)) poor++;
                }
            }
        }
        if (solid > 0) {
            double coolRatio = cool / (double) solid;
            double poorRatio = poor / (double) solid;

            if (coolRatio >= 0.80)      m *= 0.70; // poriadna kamenná pivnica
            else if (coolRatio >= 0.60) m *= 0.85; // slušne kamenné okolie
            else if (poorRatio >= 0.60) m *= 1.05; // hlina/drevo prevláda → trochu horšie
        }

        // 5) Blízkosť horiacich ohnísk (±3 bloky horizontálne, ±1 vertikálne)
        if (nearLitCampfire(level, p, 3)) m *= 1.50;

        // clamp
        if (m < 0.20) m = 0.20;
        if (m > 3.00) m = 3.00;
        return m;
    }

    /** Počet po sebe idúcich plných blokov nad pozíciou, kým nenarazíme na vzduch/otvor. */
    private static int overheadDepth(Level level, BlockPos base, int maxUp) {
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        int depth = 0;
        for (int y = 1; y <= maxUp; y++) {
            c.set(base.getX(), base.getY() + y, base.getZ());
            BlockState s = level.getBlockState(c);
            if (!s.isAir() && s.canOcclude()) depth++;
            else break;
        }
        return depth;
    }

    private static boolean nearLitCampfire(Level level, BlockPos center, int r) {
        BlockPos.MutableBlockPos c = new BlockPos.MutableBlockPos();
        for (int y = -1; y <= 1; y++) {
            for (int x = -r; x <= r; x++) {
                for (int z = -r; z <= r; z++) {
                    c.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    var s = level.getBlockState(c);
                    if (s.is(Blocks.CAMPFIRE) && s.hasProperty(net.minecraft.world.level.block.CampfireBlock.LIT)
                            && s.getValue(net.minecraft.world.level.block.CampfireBlock.LIT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // --- Data Components (CUSTOM_DATA) helpers ---
    /** Vráti celý CUSTOM_DATA tag (kópiu). Ak nie je, vráti prázdny CompoundTag. */
    private static CompoundTag readAllCustom(ItemStack st) {
        CustomData cd = st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        return cd.copyTag();
    }

    /** Zapíše celý CUSTOM_DATA tag späť do stacku (prepíše hodnotu). */
    private static void writeAllCustom(ItemStack st, CompoundTag fullTag) {
        st.set(DataComponents.CUSTOM_DATA, CustomData.of(fullTag));
    }

    /** Vráti náš vnorený root; ak neexistuje, vráti null (pre tooltip easy branch). */
    private static CompoundTag readRoot(ItemStack st) {
        CustomData cd = st.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag all = cd.copyTag();
        CompoundTag root = all.getCompound(TAG_ROOT);
        return root.isEmpty() ? null : root;
    }

    /** Prevedie MC ticky na reťazec "Xr Yd Zh Ym".
     *  1 deň = 24000 t, 1 hod = 1000 t (MC), 1 rok = 365 dní.
     *  Minúty sú odvodené z hodinového zvyšku: minutes = floor((remainder * 60) / 1000).
     */
    private static String formatMcTime(long ticks) {
        if (ticks <= 0) return "<1m";

        final long TICKS_PER_DAY  = 24000L;
        final long TICKS_PER_HOUR = 1000L;
        final long TICKS_PER_YEAR = 365L * TICKS_PER_DAY;

        long years = ticks / TICKS_PER_YEAR;
        ticks %= TICKS_PER_YEAR;

        long days  = ticks / TICKS_PER_DAY;
        ticks %= TICKS_PER_DAY;

        long hours = ticks / TICKS_PER_HOUR;
        long rem   = ticks % TICKS_PER_HOUR;

        // MC minúty: 60 minút je 1000 tickov → pomerná časť
        long minutes = (rem * 60L) / TICKS_PER_HOUR; // 0..59

        StringBuilder sb = new StringBuilder();
        if (years   > 0) sb.append(years).append("r ");
        if (days    > 0) sb.append(days).append("d ");
        if (hours   > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m");

        String s = sb.toString().trim();
        if (s.isEmpty()) s = "<1m";
        return s;
    }

}
