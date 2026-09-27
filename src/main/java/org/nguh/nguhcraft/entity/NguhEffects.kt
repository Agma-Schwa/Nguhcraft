package org.nguh.nguhcraft.entity

import net.minecraft.core.Holder
import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.effect.MobEffect
import org.nguh.nguhcraft.Nguhcraft.Companion.Id


object NguhEffects {
    val FIRE_BREATHING: Holder.Reference<MobEffect> = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Id("fire_breathing"), FireBreathingEffect())
}