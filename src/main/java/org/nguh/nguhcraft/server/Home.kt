package org.nguh.nguhcraft.server

import net.minecraft.nbt.NbtCompound
import net.minecraft.registry.RegistryKey
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
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
    fun Save() = Nbt {
        set(TAG_NAME, Name)
        set(TAG_WORLD, Utils.SerialiseWorld(World))
        set(TAG_X, Pos.x)
        set(TAG_Y, Pos.y)
        set(TAG_Z, Pos.z)
    }

    companion object {
        private const val TAG_NAME = "Name"
        private const val TAG_WORLD = "World"
        private const val TAG_X = "X"
        private const val TAG_Y = "Y"
        private const val TAG_Z = "Z"
        const val BED_HOME = "bed"
        const val DEFAULT_HOME = "home"

        fun Bed(SP: ServerPlayerEntity) = Home(
            BED_HOME,
            World.OVERWORLD,
            SP.spawnPointPosition ?: SP.server.overworld.spawnPos
        )

        @JvmStatic
        fun Load(Tag: NbtCompound): Home {
            val Name = Tag.getString(TAG_NAME)
            val World = Utils.DeserialiseWorld(Tag.get(TAG_WORLD)!!)
            val Pos = BlockPos(
                Tag.getInt(TAG_X),
                Tag.getInt(TAG_Y),
                Tag.getInt(TAG_Z)
            )
            return Home(Name, World, Pos)
        }
    }
}