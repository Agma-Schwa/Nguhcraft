package org.nguh.nguhcraft

import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.Registry
import net.minecraft.sounds.SoundEvent
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhSounds {
    val NGUH = Register("nguh")
    val NGUHROVISION_2024 = Register("music_disc.nguhrovision_2024")

    fun Init() { }

    private fun Register(S: String) = SoundEvent.createVariableRangeEvent(Id(S)).also {
        Registry.register(BuiltInRegistries.SOUND_EVENT, Id(S), it)
    }
}