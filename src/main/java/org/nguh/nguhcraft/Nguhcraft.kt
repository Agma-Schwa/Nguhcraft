package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.core.Registry
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ProblemReporter
import net.minecraft.world.level.storage.LevelResource
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.item.NguhItems
import org.nguh.nguhcraft.network.*
import org.nguh.nguhcraft.server.Manager
import org.nguh.nguhcraft.server.ServerNetworkHandler
import org.nguh.nguhcraft.server.ServerUtils
import org.nguh.nguhcraft.server.command.Commands
import java.nio.file.Path
import kotlin.io.path.inputStream

class Nguhcraft : ModInitializer {
    override fun onInitialize() {
        Manager.RunStaticInitialisation()

        // Clientbound packets.
        PayloadTypeRegistry.playS2C().register(ClientboundChatPacket.ID, ClientboundChatPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncGameRulesPacket.ID, ClientboundSyncGameRulesPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncFlagPacket.ID, ClientboundSyncFlagPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncProtectionMgrPacket.ID, ClientboundSyncProtectionMgrPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncDisplayPacket.ID, ClientboundSyncDisplayPacket.CODEC)
        PayloadTypeRegistry.playS2C().register(ClientboundSyncSpawnsPacket.ID, ClientboundSyncSpawnsPacket.CODEC)

        // Serverbound packets.
        PayloadTypeRegistry.playC2S().register(ServerboundChatPacket.ID, ServerboundChatPacket.CODEC)

        // Misc.
        Commands.Register()
        NguhItems.Init()
        NguhBlocks.Init()
        NguhSounds.Init()
        ServerNetworkHandler.Init()

        ServerLifecycleEvents.SERVER_STARTED.register { LoadServerState(it) }
        ServerTickEvents.START_WORLD_TICK.register { ServerUtils.TickWorld(it) }
        ServerLifecycleEvents.BEFORE_SAVE.register { it, _, _ -> SaveServerState(it) }
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
        const val MOD_ID = "nguhcraft"
        @JvmStatic fun Id(S: String): ResourceLocation = ResourceLocation.fromNamespaceAndPath(MOD_ID, S)
        @JvmStatic fun<T> RKey(Registry: ResourceKey<Registry<T>>, S: String): ResourceKey<T> = ResourceKey.create(Registry, Id(S))

        private fun LoadServerState(S: MinecraftServer) {
            LOGGER.info("[SETUP] Setting up server state")

            // Load saved state.
            try {
                // Read from disk.
                val Tag = NbtIo.readCompressed(
                    SavePath(S).inputStream(),
                    NbtAccounter.unlimitedHeap()
                )

                // Load global data.
                ProblemReporter.ScopedCollector(NguhErrorReporter(), LOGGER).use {
                    Manager.InitFromSaveData(S, TagValueInput.create(it, S.registryAccess(), Tag))
                }
            } catch (E: Exception) {
                LOGGER.warn("Nguhcraft: Failed to load persistent state; using defaults: ${E.message}")
            }

            LOGGER.info("[SETUP] Done")
        }

        private fun SavePath(S: MinecraftServer): Path {
            return S.getWorldPath(LevelResource.ROOT).resolve("nguhcraft.dat")
        }

        private fun SaveServerState(S: MinecraftServer) {
            LOGGER.info("Saving server state")
            try {
                ProblemReporter.ScopedCollector(NguhErrorReporter(), LOGGER).use {
                    val WV = TagValueOutput.createWithoutContext(it)
                    Manager.SaveAll(S, WV)
                    NbtIo.writeCompressed(WV.buildResult(), SavePath(S))
                }
            } catch (E: Exception) {
                LOGGER.error("Nguhcraft: Failed to save persistent state")
                E.printStackTrace()
            }
        }
    }
}
