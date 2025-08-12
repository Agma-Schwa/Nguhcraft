package org.nguh.nguhcraft

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import org.nguh.nguhcraft.network.ClientboundSyncGameRulesPacket
import org.nguh.nguhcraft.server.Manager

/**
* Global setting that is synced to the client.
* <p>
* This is like a game rule, except that 1. it is global and not
* per-world, and 2. any change to one of these is synced to the
* client.
*/
enum class SyncedGameRule(
    /** Name of this rule. */
    val Name: String,

    /** Default value for this rule. */
    val Default: Boolean
) {
    END_ENABLED("EndEnabled", false);

    /** Flag for this rule. */
    private val Flag = 1L shl ordinal

    /** Current value. */
    @Volatile
    private var Value: Boolean = Default

    /** Set the value. */
    fun Set(S: MinecraftServer, NewValue: Boolean = true) {
        if (Value == NewValue) return
        Value = NewValue
        Manager.Get<ManagerImpl>(S).Sync(S)
    }

    /** Check if this rule is set. */
    fun IsSet() = Value

    /** Manager to sync and persist the game rule state. */
    class ManagerImpl : Manager("SyncedGameRules") {
        /** Encode the game rules into a packet. */
        override fun ToPacket(SP: ServerPlayerEntity) = ClientboundSyncGameRulesPacket(entries.fold(0L) { Acc, R ->
            if (R.Value) Acc or R.Flag else Acc
        })

        /** Load the rules from disk. */
        override fun ReadData(Tag: NbtElement) {
            if (Tag !is NbtCompound) return
            for (R in entries)
                if (Tag.contains(R.Name))
                    R.Value = Tag.getBoolean(R.Name)
        }

        /** Save the rules to disk. */
        override fun WriteData() = Nbt {
            for (R in entries) set(R.Name, R.Value)
        }
    }

    companion object {
        /** Update the game rules. */
        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun Update(Packet: ClientboundSyncGameRulesPacket) = entries.forEach {
            it.Value = Packet.Flags and it.Flag != 0L
        }
    }
}