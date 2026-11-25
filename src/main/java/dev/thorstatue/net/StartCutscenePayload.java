package dev.thorstatue.net;

import dev.thorstatue.ThorStatueMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.core.Direction;

/** Server -> client: begin the contract cutscene at the given statue. */
public record StartCutscenePayload(BlockPos pos, int facing) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<StartCutscenePayload> TYPE =
        new CustomPacketPayload.Type<>(ThorStatueMod.id("start_cutscene"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StartCutscenePayload> CODEC = StreamCodec.composite(
        BlockPos.STREAM_CODEC, StartCutscenePayload::pos,
        ByteBufCodecs.VAR_INT, StartCutscenePayload::facing,
        StartCutscenePayload::new);

    public Direction direction() { return Direction.from2DDataValue(facing); }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
}
