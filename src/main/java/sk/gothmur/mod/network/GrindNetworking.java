package sk.gothmur.mod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import sk.gothmur.mod.blockentity.GrindstoneBlockEntity;
import sk.gothmur.mod.stone2steel;

@EventBusSubscriber(modid = stone2steel.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class GrindNetworking {
    private GrindNetworking() {}

    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(C2SGrindSpin.TYPE, C2SGrindSpin.STREAM_CODEC, GrindNetworking::handleGrindSpin);
    }

    private static void handleGrindSpin(C2SGrindSpin msg, IPayloadContext ctx) {
        if (ctx.player() instanceof ServerPlayer sender) {
            var level = sender.serverLevel();
            BlockEntity be = level.getBlockEntity(msg.pos());
            if (be instanceof GrindstoneBlockEntity g) {
                g.setSpinning(sender.getUUID(), msg.start());
            }
        }
    }

    /** Klient -> server odoslanie (bez ClientPacketDistributor) */
    public static void sendSpinToServer(BlockPos pos, boolean start) {
        var con = Minecraft.getInstance().getConnection();
        if (con != null) con.send(new C2SGrindSpin(pos, start));
    }
}
