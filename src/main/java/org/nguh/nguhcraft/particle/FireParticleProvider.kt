package org.nguh.nguhcraft.particle

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.particle.FlameParticle
import net.minecraft.client.particle.Particle
import net.minecraft.client.particle.ParticleProvider
import net.minecraft.client.particle.SpriteSet
import net.minecraft.core.particles.SimpleParticleType

// By using this instead of FlameParticle.Provider we can have fire particles that mostly behave like flame particles
// but are a bit bigger.
@Environment(EnvType.CLIENT)
class FireParticleProvider(private val sprite: SpriteSet) : ParticleProvider<SimpleParticleType> {
    override fun createParticle(
        particleOptions: SimpleParticleType,
        clientLevel: ClientLevel,
        d: Double,
        e: Double,
        f: Double,
        g: Double,
        h: Double,
        i: Double
    ): Particle {
        val flameParticle = FlameParticle.Provider(sprite).createParticle(
            particleOptions,
            clientLevel,
            d,
            e,
            f,
            g,
            h,
            i
        )!!.scale(1.5f)
        return flameParticle
    }
}