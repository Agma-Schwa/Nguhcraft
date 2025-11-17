package org.nguh.nguhcraft.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.world.entity.Entity

class NguhcraftEntityData(
    var ManagedBySpawnPos: Boolean = false
) {
    interface Access {
        fun `Nguhcraft$GetEntityData`(): NguhcraftEntityData
    }

    companion object {
        const val TAG_ROOT = "NguhcraftEntityData"
        @JvmField val CODEC = RecordCodecBuilder.create {
            it.group(
                Codec.BOOL.optionalFieldOf("ManagedBySpawnPos", false).forGetter(NguhcraftEntityData::ManagedBySpawnPos)
            ).apply(it, ::NguhcraftEntityData)
        }
    }
}

val Entity.Data get() = (this as NguhcraftEntityData.Access).`Nguhcraft$GetEntityData`()