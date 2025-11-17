package org.nguh.nguhcraft.client.accessors

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.client.Minecraft
import net.minecraft.network.chat.Component

@Environment(EnvType.CLIENT)
class ClientDisplayData(
    var Lines: List<Component> = listOf()
)

@Environment(EnvType.CLIENT)
interface ClientDisplayDataAccessor {
    fun `Nguhcraft$GetDisplayData`(): ClientDisplayData
}

val Minecraft.DisplayData: ClientDisplayData? get()
    = (this.connection as? ClientDisplayDataAccessor)?.`Nguhcraft$GetDisplayData`()