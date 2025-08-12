package org.nguh.nguhcraft.entity

import com.mojang.logging.LogUtils
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.entity.EntityType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.mob.MobEntity
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Uuids
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.nguh.nguhcraft.Decode
import org.nguh.nguhcraft.Encode
import org.nguh.nguhcraft.NbtListOf
import org.nguh.nguhcraft.network.ClientboundSyncSpawnsPacket
import org.nguh.nguhcraft.server.Manager
import java.util.*

class EntitySpawnManager(val S: MinecraftServer) : Manager("EntitySpawns") {
    interface EntityAccess {
        fun `Nguhcraft$SetManagedBySpawnPos`()
    }

    /** Spawn data shared between client and server. */
    open class Spawn(
        val World: RegistryKey<World>,
        val SpawnPos: Vec3d,
        val Id: String,
    ) {
        override fun toString() = "$Id in ${World.value} at $SpawnPos"
        companion object {
            val PACKET_CODEC = PacketCodec.tuple(
                RegistryKey.createPacketCodec(RegistryKeys.WORLD),
                Spawn::World,
                Vec3d.PACKET_CODEC,
                Spawn::SpawnPos,
                PacketCodecs.STRING,
                Spawn::Id,
                ::Spawn
            )
        }
    }

    /** Server-only spawn data. */
    class ServerSpawn(
        World: RegistryKey<World>,
        SpawnPos: Vec3d,
        Id: String,
        val Nbt: NbtCompound,
        var Entity: Optional<UUID> = Optional.empty(),
    ) : Spawn(World, SpawnPos, Id) {
        companion object {
            val CODEC: Codec<ServerSpawn> = RecordCodecBuilder.create {
                it.group(
                    RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("World").forGetter(ServerSpawn::World),
                    Vec3d.CODEC.fieldOf("SpawnPos").forGetter(ServerSpawn::SpawnPos),
                    Codec.STRING.fieldOf("Id").forGetter(ServerSpawn::Id),
                    NbtCompound.CODEC.fieldOf("Nbt").forGetter(ServerSpawn::Nbt),
                    Uuids.CODEC.optionalFieldOf("Entity").forGetter(ServerSpawn::Entity),
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
    fun Tick(SW: ServerWorld) {
        if (SW.time > 100 && SW.time % 100L != 0L) return // Tick all 5 seconds
        for (Sp in Spawns.filter { it.World == SW.registryKey }) {
            val Block = BlockPos.ofFloored(Sp.SpawnPos)

            // Skip spawns that are not loaded.
            if (!SW.isPosLoaded(Block)) continue

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
            val NewEntity = EntityType.loadEntityWithPassengers(Sp.Nbt.copy(), SW, SpawnReason.SPAWNER) {
                (it as EntityAccess).`Nguhcraft$SetManagedBySpawnPos`()
                it.refreshPositionAndAngles(Sp.SpawnPos, 0.0f, 0.0f)
                it
            }

            // Ignore this if the spawn failed.
            if (NewEntity == null || !SW.spawnNewEntityAndPassengers(NewEntity)) {
                LOGGER.error("Failed to spawn entity for spawn $Sp")
                continue
            }

            // Make the entity persistent.
            if (NewEntity is MobEntity) {
                NewEntity.setPersistent()
                NewEntity.setCanPickUpLoot(false)
                for (E in EquipmentSlot.entries) NewEntity.setEquipmentDropChance(E, 0.0f)
            }

            // Register this as our entity.
            Sp.Entity = Optional.of(NewEntity.uuid)
        }
    }

    override fun ReadData(Tag: NbtElement) {
        if (Tag !is NbtList) return
        for (Sp in Tag) Add(ServerSpawn.CODEC.Decode(Sp), false) // Don’t try to sync at load time.
    }

    override fun ToPacket(SP: ServerPlayerEntity): CustomPayload? {
        if (!SP.hasPermissionLevel(4)) return null
        return ClientboundSyncSpawnsPacket(Spawns)
    }

    override fun WriteData() = NbtListOf {
        for (Sp in Spawns) add(ServerSpawn.CODEC.Encode(Sp))
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
    }
}

val MinecraftServer.EntitySpawnManager get() = Manager.Get<EntitySpawnManager>(this)