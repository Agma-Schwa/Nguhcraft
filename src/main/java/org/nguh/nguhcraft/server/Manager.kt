package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.network.packet.CustomPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity

interface ServerManagerInterface {
    /** Do not call this directly. */
    fun <T : Manager> `Nguhcraft$UnsafeGetManager`(Class: Class<T>): T

    /** Do not call this directly. */
    fun `Nguhcraft$UnsafeGetAllManagers`(): List<Manager>
}

/***
 * Base class for 'managers'.
 *
 * In the terminology of this mod, a 'manager' is a (usually) server-side
 * object that stores data tied to a single session, i.e. a new manager is
 * created every time a server is started. This ensures that we don’t leak
 * state across integrated servers in single player (and that we can properly
 * persist any such state on the dedicated server).
 *
 * Managers hold state global to the entire server; some have per-world state
 * but it’s easier to just store a reference to the world an object belongs
 * to in the object or the manager rather than creating a separate manager
 * for each world.
 *
 * Some managers are also available on the client; for some, the client has
 * access to all the data available to the manager (e.g. WarpManager), for
 * others, some data is server-side only (e.g. ProtectionManager), and the
 * client-side manager is a separate object.
 *
 * Managers are stored in the 'MinecraftServer' class.
 */
abstract class Manager(val TagName: String) {
    /** Load this manager to disk. */
    abstract fun ReadData(Tag: NbtElement)

    /** Sync this manager to a player. */
    fun Sync(SP: ServerPlayerEntity) {
        val Packet = ToPacket(SP)
        if (Packet != null) ServerPlayNetworking.send(SP, Packet)
    }

    /** Sync this manager to all players. */
    fun Sync(S: MinecraftServer) {
        for (SP in S.playerManager.playerList) Sync(SP)
    }

    /** Convert this to a packet that can be synchronised to clients. */
    open fun ToPacket(SP: ServerPlayerEntity): CustomPayload? { return null }

    /** Save this manager to disk. */
    abstract fun WriteData(): NbtElement?

    companion object {
        private val LOGGER = LogUtils.getLogger()
        private fun MinecraftServer.AllManagers() =
            (this as ServerManagerInterface).`Nguhcraft$UnsafeGetAllManagers`()

        /** Get the current manager instance. */
        @JvmStatic
        inline fun <reified T : Manager> Get(
            S: MinecraftServer
        ): T = (S as ServerManagerInterface).`Nguhcraft$UnsafeGetManager`(T::class.java)

        /** Initialise all managers. */
        @JvmStatic
        fun InitFromSaveData(S: MinecraftServer, SaveData: NbtCompound) {
            for (M in S.AllManagers()) {
                val Tag = SaveData.get(M.TagName)
                try {
                    if (Tag != null) M.ReadData(Tag)
                } catch (E: Exception) {
                    LOGGER.error("Failed to load manager data: ${E.message}", E)
                }
            }
        }

        /** Save all managers. */
        @JvmStatic
        fun SaveAll(S: MinecraftServer, SaveData: NbtCompound) {
            for (M in S.AllManagers()) {
                val Tag = M.WriteData()
                if (Tag != null) SaveData.put(M.TagName, Tag)
            }
        }

        /** Send the state of all managers to a player. */
        @JvmStatic
        fun SendAll(SP: ServerPlayerEntity) {
            for (M in SP.server.AllManagers()) M.Sync(SP)
        }

        /** Initialise static data. */
        fun RunStaticInitialisation() {}
    }
}