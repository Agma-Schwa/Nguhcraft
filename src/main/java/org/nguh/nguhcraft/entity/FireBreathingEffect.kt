package org.nguh.nguhcraft.entity

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectCategory
import net.minecraft.world.entity.LivingEntity
import org.nguh.nguhcraft.particle.NguhParticles

class FireBreathingEffect: MobEffect(MobEffectCategory.BENEFICIAL, 0xe84723) {
    override fun shouldApplyEffectTickThisTick(duration: Int, amplifier: Int): Boolean {
        return true
    }

    override fun applyEffectTick(level: ServerLevel, entity: LivingEntity, amplifier: Int): Boolean {
        val angle = entity.lookAngle.offsetRandom(level.random, 0.25f)
        repeat(level.random.nextInt(2 * (amplifier + 1), 3 * (amplifier + 1))) {
            val particle = if (level.random.nextDouble() < 0.66) {
                NguhParticles.FIRE
            } else {
                ParticleTypes.FLAME
            }
            level.sendParticles(
                particle,
                entity.x,
                entity.y + 1.8,
                entity.z,
                0,
                angle.x,
                angle.y,
                angle.z,
                (amplifier + 1.0) * 0.25
            )
        }
        if (level.gameTime % 5L == 0L) {
            level.playSound(
                null,
                entity.blockPosition(),
                SoundEvents.FIRECHARGE_USE,
                entity.soundSource,
                0.5f,
                1.0f + ((level.random.nextDouble() - 0.5) * 0.4).toFloat()
            )
        }

        return super.applyEffectTick(level, entity, amplifier)
    }
}