package org.nguh.nguhcraft

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
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
    class ManagerImpl : Manager() {
        /** Encode the game rules into a packet. */
        override fun ToPacket(SP: ServerPlayer) = ClientboundSyncGameRulesPacket(entries.fold(0L) { Acc, R ->
            if (R.Value) Acc or R.Flag else Acc
        })

        /** Load the rules from disk. */
        override fun ReadData(RV: ValueInput) = RV.With(KEY) {
            for (R in entries) R.Value = getBooleanOr(R.Name, R.Value)
        }

        /** Save the rules to disk. */
        override fun WriteData(WV: ValueOutput) = WV.With(KEY) {
            for (R in entries) putBoolean(R.Name, R.Value)
        }
    }

    companion object {
        private const val KEY = "SyncedGameRules"

        /** Update the game rules. */
        @JvmStatic
        @Environment(EnvType.CLIENT)
        fun Update(Packet: ClientboundSyncGameRulesPacket) = entries.forEach {
            it.Value = Packet.Flags and it.Flag != 0L
        }
    }
}