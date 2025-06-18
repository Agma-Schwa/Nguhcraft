package org.nguh.nguhcraft.server

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.nguh.nguhcraft.NbtListOf
import org.nguh.nguhcraft.Utils
import org.nguh.nguhcraft.set

class WarpManager : Manager("Warps") {
    companion object {
        private const val TAG_NAME = "Name"
        private const val TAG_WORLD = "World"
        private const val TAG_X = "X"
        private const val TAG_Y = "Y"
        private const val TAG_Z = "Z"
        private const val TAG_YAW = "Yaw"
        private const val TAG_PITCH = "Pitch"
    }

    /** A single warp. */
    data class Warp(
        val Name: String,
        val World: RegistryKey<World>,
        val Pos: Vec3d,
        val Yaw: Float,
        val Pitch: Float
    )

    /** All warps on the server. */
    val Warps = mutableMapOf<String, Warp>()

    /** Load warps from save file. */
    override fun ReadData(Tag: NbtElement) {
        for (Elem in (Tag as NbtList)) {
            val W = Elem as NbtCompound
            Warps[W.getString(TAG_NAME)] = Warp(
                W.getString(TAG_NAME),
                Utils.DeserialiseWorld(W.get(TAG_WORLD)!!),
                Vec3d(W.getDouble(TAG_X), W.getDouble(TAG_Y), W.getDouble(TAG_Z)),
                W.getFloat(TAG_YAW),
                W.getFloat(TAG_PITCH)
            )
        }
    }

    /** Save warps to save file. */
    override fun WriteData() = NbtListOf(Warps.values) {
        set(TAG_NAME, it.Name)
        set(TAG_WORLD, Utils.SerialiseWorld(it.World))
        set(TAG_X, it.Pos.x)
        set(TAG_Y, it.Pos.y)
        set(TAG_Z, it.Pos.z)
        set(TAG_YAW, it.Yaw)
        set(TAG_PITCH, it.Pitch)
    }
}

val MinecraftServer.WarpManager get() = Manager.Get<WarpManager>(this)