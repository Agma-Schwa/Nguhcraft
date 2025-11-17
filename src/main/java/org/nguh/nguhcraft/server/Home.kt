package org.nguh.nguhcraft.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.portal.TeleportTransition
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.Decode
import org.nguh.nguhcraft.Encode
import org.nguh.nguhcraft.Nbt
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.set

/**
* Represents a home.
*
* A home is a per-player warp. There is a special home called "bed" which
* cannot be set by the player and is not saved in the player’s home list;
* rather, it always evaluates to the player’s spawn point in the overworld.
*/
data class Home(
    val Name: String,
    val World: ResourceKey<Level>,
    val Pos: BlockPos,
) {
    fun Save() = CODEC.Encode(this)
    companion object {
        const val BED_HOME = "bed"
        const val DEFAULT_HOME = "home"

        @JvmField
        val CODEC = RecordCodecBuilder.create {
            it.group(
                Codec.STRING.fieldOf("Name").forGetter(Home::Name),
                ResourceKey.codec(Registries.DIMENSION).fieldOf("World").forGetter(Home::World),
                Codec.INT.fieldOf("X").forGetter { it.Pos.x },
                Codec.INT.fieldOf("Y").forGetter { it.Pos.y },
                Codec.INT.fieldOf("Z").forGetter { it.Pos.z },
            ).apply(it) { Name, World, X, Y, Z -> Home(Name, World, BlockPos(X, Y, Z)) }
        }

        fun Bed(SP: ServerPlayer) = Home(
            BED_HOME,
            SP.respawnConfig?.dimension ?: Level.OVERWORLD,
            SP.respawnConfig?.pos ?: SP.Server.overworld().sharedSpawnPos
        )

        @JvmStatic
        fun Load(Tag: CompoundTag) = CODEC.Decode(Tag)
    }
}