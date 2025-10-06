package sk.gothmur.mod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import sk.gothmur.mod.blockentity.GrindstoneBlockEntity;
import sk.gothmur.mod.stone2steel;

/**
 * Minimalistický klientský handler pre držanie RMB nad grindstonom.
 * - Na CLIENT tick sleduje, či hráč drží "Use Item/Place Block" (RMB).
 * - Ak sa pozerá na GrindstoneBlockEntity, bumpne animáciu.
 * - Pri pustení RMB nechá animáciu "dobehnúť" do štvrťotáčky.
 */
@EventBusSubscriber(modid = stone2steel.MODID, value = Dist.CLIENT)
public final class KeySpinHandler {
    private KeySpinHandler() {}

    private static boolean wasHolding = false;
    private static BlockPos trackedPos = null;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.level == null) return;
        LocalPlayer player = mc.player;
        if (player == null) return;

        // Je stlačené "Use Item/Place Block" (RMB)?
        boolean keyDown = mc.options.keyUse.isDown();

        // Na čo sa pozeráme?
        BlockPos lookPos = null;
        HitResult hr = mc.hitResult;
        if (hr instanceof BlockHitResult bhr && hr.getType() == HitResult.Type.BLOCK) {
            lookPos = bhr.getBlockPos();
        }

        // Ak práve držíme RMB a pozeráme na grindstone, bumpni animáciu
        if (keyDown && lookPos != null) {
            BlockEntity be = mc.level.getBlockEntity(lookPos);
            if (be instanceof GrindstoneBlockEntity g) {
                g.clientBumpSpin();
                trackedPos = lookPos; // pamätaj si posledný platný BE
                wasHolding = true;
                return; // hotovo pre tento tick
            }
        }

        // Ak sme práve pustili RMB a predtým sme držali, nechaj dobehnúť na poslednom BE
        if (!keyDown && wasHolding && trackedPos != null) {
            BlockEntity be = mc.level.getBlockEntity(trackedPos);
            if (be instanceof GrindstoneBlockEntity g) {
                g.clientOnRelease();
            }
        }

        // Reset stavu, keď nedržíme
        if (!keyDown) {
            wasHolding = false;
            trackedPos = null;
        }
    }
}
