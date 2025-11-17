package org.nguh.nguhcraft.entity

import com.mojang.logging.LogUtils
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.Mob
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.core.UUIDUtil
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.Named
import org.nguh.nguhcraft.Read
import org.nguh.nguhcraft.Write
import org.nguh.nguhcraft.network.ClientboundSyncSpawnsPacket
import org.nguh.nguhcraft.server.Data
import org.nguh.nguhcraft.server.Manager
import java.util.*

class EntitySpawnManager(val S: MinecraftServer) : Manager() {
    /** Spawn data shared between client and server. */
    open class Spawn(
        val World: ResourceKey<Level>,
        val SpawnPos: Vec3,
        val Id: String,
    ) {
        override fun toString() = "$Id in ${World.location()} at $SpawnPos"
        companion object {
            val PACKET_CODEC = StreamCodec.composite(
                ResourceKey.streamCodec(Registries.DIMENSION),
                Spawn::World,
                Vec3.STREAM_CODEC,
                Spawn::SpawnPos,
                ByteBufCodecs.STRING_UTF8,
                Spawn::Id,
                ::Spawn
            )
        }
    }

    /** Server-only spawn data. */
    class ServerSpawn(
        World: ResourceKey<Level>,
        SpawnPos: Vec3,
        Id: String,
        val Nbt: CompoundTag,
        var Entity: Optional<UUID> = Optional.empty(),
    ) : Spawn(World, SpawnPos, Id) {
        companion object {
            val CODEC: Codec<ServerSpawn> = RecordCodecBuilder.create {
                it.group(
                    ResourceKey.codec(Registries.DIMENSION).fieldOf("World").forGetter(ServerSpawn::World),
                    Vec3.CODEC.fieldOf("SpawnPos").forGetter(ServerSpawn::SpawnPos),
                    Codec.STRING.fieldOf("Id").forGetter(ServerSpawn::Id),
                    CompoundTag.CODEC.fieldOf("Nbt").forGetter(ServerSpawn::Nbt),
                    UUIDUtil.AUTHLIB_CODEC.optionalFieldOf("Entity").forGetter(ServerSpawn::Entity),
                ).apply(it, ::ServerSpawn)
            }
        }
    }

    /** All spawns that are currently on the server. */
    val Spawns = mutableListOf<ServerSpawn>()

    /** Add a spawn. */
    fun Add(Sp: ServerSpawn, ShouldSync: Boolean = true) {
        if (Spawns.find { it.Id == Sp.Id } != null)
            throw IllegalArgumentException("A spawn with the name '${Sp.Id} already exists")

        Spawns.add(Sp)
        if (ShouldSync) Sync(S)
    }

    /** Delete a spawn. */
    fun Delete(Sp: ServerSpawn) {
        Spawns.remove(Sp)
        Sync(S)
    }

    /** Tick all spawns in a world. */
    fun Tick(SW: ServerLevel) {
        if (SW.gameTime > 100 && SW.gameTime % 100L != 0L) return // Tick all 5 seconds
        for (Sp in Spawns.filter { it.World == SW.dimension() }) {
            val Block = BlockPos.containing(Sp.SpawnPos)

            // Skip spawns that are not loaded.
            if (!SW.isLoaded(Block)) continue

            // Skip spawns whose entity is alive.
            val E = Sp.Entity.orElse(null)?.let { SW.getEntity(it) }
            if (E?.isAlive == true) continue

            // The entity belonging to this spawn is dead; respawn it. The code for
            // this was borrowed from SummonCommand::summon().
            //
            // FIXME: Mojang tend to break their Entity NBT format all the time, so
            //        come up w/ our custom format (use the one from the event branch)
            //        for defining entity spawns.
            Sp.Entity = Optional.empty()
            val NewEntity = EntityType.loadEntityRecursive(Sp.Nbt.copy(), SW, EntitySpawnReason.SPAWNER) {
                it.Data.ManagedBySpawnPos = true
                it.snapTo(Sp.SpawnPos, 0.0f, 0.0f)
                it
            }

            // Ignore this if the spawn failed.
            if (NewEntity == null || !SW.tryAddFreshEntityWithPassengers(NewEntity)) {
                LOGGER.error("Failed to spawn entity for spawn $Sp")
                continue
            }

            // Make the entity persistent.
            if (NewEntity is Mob) {
                NewEntity.setPersistenceRequired()
                NewEntity.setCanPickUpLoot(false)
                for (E in EquipmentSlot.entries) NewEntity.setDropChance(E, 0.0f)
            }

            // Register this as our entity.
            Sp.Entity = Optional.of(NewEntity.uuid)
        }
    }

    override fun ReadData(RV: ValueInput) = RV.Read(CODEC).ifPresent(Spawns::addAll)
    override fun WriteData(WV: ValueOutput) = WV.Write(CODEC, Spawns)

    override fun ToPacket(SP: ServerPlayer): CustomPacketPayload? {
        if (!SP.hasPermissions(4)) return null
        return ClientboundSyncSpawnsPacket(Spawns)
    }

    companion object {
        private val CODEC = ServerSpawn.CODEC.listOf().Named("EntitySpawns")
        private val LOGGER = LogUtils.getLogger()
    }
}

val MinecraftServer.EntitySpawnManager get() = Manager.Get<EntitySpawnManager>(this)