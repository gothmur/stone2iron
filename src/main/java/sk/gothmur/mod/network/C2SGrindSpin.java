package sk.gothmur.mod.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import sk.gothmur.mod.stone2steel;

public record C2SGrindSpin(BlockPos pos, boolean start) implements CustomPacketPayload {
    public static final Type<C2SGrindSpin> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(stone2steel.MODID, "c2s_grind_spin"));

    public static final StreamCodec<FriendlyByteBuf, C2SGrindSpin> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, C2SGrindSpin::pos,
                    ByteBufCodecs.BOOL, C2SGrindSpin::start,
                    C2SGrindSpin::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
