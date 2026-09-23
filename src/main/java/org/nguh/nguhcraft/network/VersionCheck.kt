package org.nguh.nguhcraft.network

import net.fabricmc.fabric.api.networking.v1.FriendlyByteBufs
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.resources.Identifier
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object VersionCheck {
    /**
     * Mod version.
     *
     * Needs to be compatible between server and client. Increment this
     * whenever there are breaking changes to client/server communication,
     * static registries, etc.
     */
    const val NGUHCRAFT_VERSION: Int = 46

    /** Packet sent by the client to reply to the version handshake. */
    val Packet: FriendlyByteBuf = FriendlyByteBufs.create().writeInt(NGUHCRAFT_VERSION)

    /** Identifier used for the version check. */
    val ID: Identifier = Id("login/version_check")
}