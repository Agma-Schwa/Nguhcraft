package org.nguh.nguhcraft.event

import com.mojang.serialization.Codec
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.core.UUIDUtil
import org.nguh.nguhcraft.ClassSerialiser
import org.nguh.nguhcraft.MakeEnumCodec
import org.nguh.nguhcraft.server.Manager
import java.util.*
import kotlin.random.Random

enum class EventDifficulty {
    THE_GOOD_DAYS,
    THE_BAD_DAYS,
    THE_WORSE_DAYS,
    THE_WORST_DAYS,
    THE_LAST_DAYS;
    companion object {
        val CODEC = MakeEnumCodec<EventDifficulty>()
    }
}

class EventManager : Manager() {
    private var InvolvedPlayerUUIDs: MutableSet<UUID> = mutableSetOf<UUID>()

    /** Whether the event is currently running. */
    var Running = false
        private set

    /** Difficulty modifier of the event. */
    var Difficulty = EventDifficulty.THE_GOOD_DAYS

    /** RNG for the event. */
    val RNG = Random(System.currentTimeMillis() xor Random.nextLong())

    /** Get the players currently participating in the event. */
    val Players get(): Set<UUID> = InvolvedPlayerUUIDs

    /** Add a player. */
    fun Add(SP: ServerPlayer) = InvolvedPlayerUUIDs.add(SP.uuid)

    /** Remove a player from the event. */
    fun Remove(SP: ServerPlayer) = InvolvedPlayerUUIDs.remove(SP.uuid)

    /** Serialisation. */
    override fun ReadData(RV: ValueInput) = SER.Read(this, RV.childOrEmpty(EVENT_KEY))
    override fun WriteData(WV: ValueOutput) = SER.Write(this, WV.child(EVENT_KEY))
    companion object {
        const val EVENT_KEY = "Event"
        val SER = ClassSerialiser.Builder<EventManager>()
            .add(Codec.BOOL, "Running", EventManager::Running)
            .add(EventDifficulty.CODEC, "Difficulty", EventManager::Difficulty)
            .add(UUIDUtil.CODEC_SET, "InvolvedPlayers", EventManager::InvolvedPlayerUUIDs)
            .build()
    }
}


val MinecraftServer.EventManager get() = Manager.Get<EventManager>(this)