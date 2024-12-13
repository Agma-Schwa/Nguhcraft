package org.nguh.nguhcraft

import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.sound.SoundEvent
import org.nguh.nguhcraft.Nguhcraft.Companion.Id

object NguhSounds {
    val NGUH = Register("nguh")
    val NGUHROVISION_2024 = Register("music_disc.nguhrovision_2024")

    fun Init() { }

    private fun Register(S: String) = SoundEvent.of(Id(S)).also {
        Registry.register(Registries.SOUND_EVENT, Id(S), it)
    }
}