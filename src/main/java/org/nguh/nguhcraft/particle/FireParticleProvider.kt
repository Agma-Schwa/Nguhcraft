package org.nguh.nguhcraft.particle

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.FlameParticle
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.util.RandomSource

// By using this instead of FlameParticle.Provider we can have fire particles that mostly behave like flame particles
// but are a bit bigger.
@Environment(EnvType.CLIENT)
class FireParticleProvider(private val sprite: SpriteSet) : ParticleProvider<SimpleParticleType> {
    override fun createParticle(
        particleOptions: SimpleParticleType,
        clientLevel: ClientLevel,
        x: Double,
        y: Double,
        z: Double,
        xAux: Double,
        yAux: Double,
        zAux: Double,
        random: RandomSource
    ): Particle {
        val flameParticle = FlameParticle.Provider(sprite).createParticle(
            particleOptions,
            clientLevel,
            x,
            y,
            z,
            xAux,
            yAux,
            zAux,
            random
        ).scale(1.5f)
        return flameParticle
    }
}