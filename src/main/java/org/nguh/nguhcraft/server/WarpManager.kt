package org.nguh.nguhcraft.server

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.MinecraftServer
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.nguh.nguhcraft.Encode
import org.nguh.nguhcraft.Named
import org.nguh.nguhcraft.Read
import org.nguh.nguhcraft.Write

class WarpManager : Manager() {
    /** A single warp. */
    data class Warp(
        val Name: String,
        val World: RegistryKey<World>,
        val X: Double,
        val Y: Double,
        val Z: Double,
        val Yaw: Float,
        val Pitch: Float
    ) {
        val Pos get() = Vec3d(X, Y, Z)
        companion object {
            val CODEC: Codec<Warp> = RecordCodecBuilder.create {
                it.group(
                    Codec.STRING.fieldOf("Name").forGetter(Warp::Name),
                    RegistryKey.createCodec(RegistryKeys.WORLD).fieldOf("World").forGetter(Warp::World),
                    Codec.DOUBLE.fieldOf("X").forGetter(Warp::X),
                    Codec.DOUBLE.fieldOf("Y").forGetter(Warp::Y),
                    Codec.DOUBLE.fieldOf("Z").forGetter(Warp::Z),
                    Codec.FLOAT.fieldOf("Yaw").forGetter(Warp::Yaw),
                    Codec.FLOAT.fieldOf("Pitch").forGetter(Warp::Pitch),
                ).apply(it, ::Warp)
            }
        }
    }

    /** All warps on the server. */
    val Warps = mutableMapOf<String, Warp>()

    /** Load warps from save file. */
    override fun ReadData(RV: ReadView) {
        RV.Read(CODEC).ifPresent {
            Warps.putAll(it.associateBy { it.Name })
        }
    }

    /** Save warps to save file. */
    override fun WriteData(WV: WriteView) = WV.Write(CODEC, Warps.values.toList())

    companion object {
        val CODEC = Warp.CODEC.listOf().Named("Warps")
    }
}

val MinecraftServer.WarpManager get() = Manager.Get<WarpManager>(this)