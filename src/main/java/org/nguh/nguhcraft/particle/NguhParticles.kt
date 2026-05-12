package org.nguh.nguhcraft.particle

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes
import net.minecraft.core.Registry
import net.minecraft.core.particles.ParticleOptions
import net.minecraft.core.particles.ParticleType
import net.minecraft.core.registries.BuiltInRegistries
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhParticles {
    val FIRE = FabricParticleTypes.simple()

    init {
        fun<T: ParticleOptions> Register(particle: ParticleType<T>, name: String) {
            Registry.register(BuiltInRegistries.PARTICLE_TYPE, Id(name), particle)
        }

        Register(FIRE, "fire")
    }

    @Environment(EnvType.CLIENT)
    fun ClientSideInitialisation() {
        fun<T: ParticleOptions> Register(
            particle: ParticleType<T>,
            provider: ParticleFactoryRegistry.PendingParticleFactory<T>
        ) {
            ParticleFactoryRegistry.getInstance().register(particle, provider);
        }

        Register(FIRE, { spriteSet -> FireParticleProvider(spriteSet) })
    }
}