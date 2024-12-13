package org.nguh.nguhcraft.network

import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.codec.PacketCodec

/**
 * Helper to create a custom packet codec.
 *
 * Use this instead of calling 'PacketCodec#of' directly since
 * that function is EXTREMELY finicky: it just silently... doesn’t
 * encode anything if you write
 *
 *     -> { Packet.Write(B) }
 *
 * instead of
 *
 *     -> Packet.Write(B)
 */
fun <PacketType> MakeCodec(
    Encoder: PacketType.(RegistryByteBuf) -> Unit,
    Decoder: (RegistryByteBuf) -> PacketType
): PacketCodec<RegistryByteBuf, PacketType> = PacketCodec.of(
    { Packet: PacketType, B: RegistryByteBuf -> Packet.Encoder(B) },
    { B: RegistryByteBuf -> Decoder(B) }
)