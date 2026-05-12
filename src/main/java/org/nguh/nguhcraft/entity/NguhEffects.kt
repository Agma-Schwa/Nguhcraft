package org.nguh.nguhcraft.entity

import net.minecraft.core.Registry
import net.minecraft.core.registries.BuiltInRegistries
import org.nguh.nguhcraft.Nguhcraft.Companion.Id


object NguhEffects {
    val FIRE_BREATHING = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Id("fire_breathing"), FireBreathingEffect())
}