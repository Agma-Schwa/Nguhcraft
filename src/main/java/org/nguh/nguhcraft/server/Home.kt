package org.nguh.nguhcraft.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.TeleportTarget
import net.minecraft.world.World
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
    val World: RegistryKey<World>,
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
                RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("World").forGetter(Home::World),
                Codec.INT.fieldOf("X").forGetter { it.Pos.x },
                Codec.INT.fieldOf("Y").forGetter { it.Pos.y },
                Codec.INT.fieldOf("Z").forGetter { it.Pos.z },
            ).apply(it) { Name, World, X, Y, Z -> Home(Name, World, BlockPos(X, Y, Z)) }
        }

        fun Bed(SP: ServerPlayerEntity) = Home(
            BED_HOME,
            SP.respawn?.dimension ?: World.OVERWORLD,
            SP.respawn?.pos ?: SP.Server.overworld.spawnPos
        )

        @JvmStatic
        fun Load(Tag: NbtCompound) = CODEC.Decode(Tag)
    }
}