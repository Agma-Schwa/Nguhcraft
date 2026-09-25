package org.nguh.nguhcraft.particle

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.particle.v1.ParticleProviderRegistry
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.core.Registry
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

@Environment(EnvType.CLIENT)
object NguhParticles {
    val FIRE = FabricParticleTypes.simple()

    init {
        fun<T: ParticleOptions> Register(Particle: ParticleType<T>, Name: String) {
            Registry.register(BuiltInRegistries.PARTICLE_TYPE, Id(Name), Particle)
        }

        Register(FIRE, "fire")
    }

    fun Init() {
        fun<T: ParticleOptions> Register(
            Particle: ParticleType<T>,
            Provider: ParticleProviderRegistry.PendingParticleProvider<T>
        ) {
            ParticleProviderRegistry.getInstance().register(Particle, Provider);
        }

        Register(FIRE, { SpriteSet -> FireParticleProvider(SpriteSet) })
    }
}