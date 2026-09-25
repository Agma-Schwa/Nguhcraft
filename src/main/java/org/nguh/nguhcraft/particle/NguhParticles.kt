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
        fun<T: ParticleOptions> Register(particle: ParticleType<T>, name: String) {
            Registry.register(BuiltInRegistries.PARTICLE_TYPE, Id(name), particle)
        }

        Register(FIRE, "fire")
    }

    fun Init() {
        fun<T: ParticleOptions> Register(
            particle: ParticleType<T>,
            provider: ParticleProviderRegistry.PendingParticleProvider<T>
        ) {
            ParticleProviderRegistry.getInstance().register(particle, provider);
        }

        Register(FIRE, { spriteSet -> FireParticleProvider(spriteSet) })
    }
}