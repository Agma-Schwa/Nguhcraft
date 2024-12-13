package org.nguh.nguhcraft.server.dedicated

import net.fabricmc.api.DedicatedServerModInitializer
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.minecraft.server.dedicated.MinecraftDedicatedServer

@Environment(EnvType.SERVER)
class NguhcraftDedicatedServer : DedicatedServerModInitializer {
    override fun onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTING.register {
            Discord.Start(it as MinecraftDedicatedServer)
        }

        ServerLifecycleEvents.SERVER_STOPPING.register {
            Discord.Stop()
        }
    }
}
