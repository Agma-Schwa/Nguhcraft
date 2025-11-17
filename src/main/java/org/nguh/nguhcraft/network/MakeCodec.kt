package org.nguh.nguhcraft.network

import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.StreamCodec

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
    Encoder: PacketType.(RegistryFriendlyByteBuf) -> Unit,
    Decoder: (RegistryFriendlyByteBuf) -> PacketType
): StreamCodec<RegistryFriendlyByteBuf, PacketType> = StreamCodec.ofMember(
    { Packet: PacketType, B: RegistryFriendlyByteBuf -> Packet.Encoder(B) },
    { B: RegistryFriendlyByteBuf -> Decoder(B) }
)