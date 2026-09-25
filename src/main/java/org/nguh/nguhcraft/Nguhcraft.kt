package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.core.Registry
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.resources.ResourceKey
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.tags.BlockTags
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
import org.nguh.nguhcraft.server.TreeToChop
import org.nguh.nguhcraft.server.command.Commands
import java.nio.file.Path
import kotlin.io.path.inputStream

class Nguhcraft : ModInitializer {
    override fun onInitialize() {
        Manager.RunStaticInitialisation()

        // Clientbound packets.
        PayloadTypeRegistry.clientboundPlay().register(ClientboundChatPacket.ID, ClientboundChatPacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ClientboundLinkUpdatePacket.ID, ClientboundLinkUpdatePacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncGameRulesPacket.ID, ClientboundSyncGameRulesPacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncFlagPacket.ID, ClientboundSyncFlagPacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncProtectionMgrPacket.ID, ClientboundSyncProtectionMgrPacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncDisplayPacket.ID, ClientboundSyncDisplayPacket.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ClientboundSyncSpawnsPacket.ID, ClientboundSyncSpawnsPacket.CODEC)

        // Serverbound packets.
        PayloadTypeRegistry.serverboundPlay().register(ServerboundChatPacket.ID, ServerboundChatPacket.CODEC)

        // Misc.
        Commands.Register()
        NguhBlocks.Init()
        NguhItems.Init()
        NguhSounds.Init()
        ServerNetworkHandler.Init()

        ServerLifecycleEvents.SERVER_STARTED.register {
            CheckLogs(it)
            LoadServerState(it)
        }

        ServerTickEvents.START_LEVEL_TICK.register { ServerUtils.TickWorld(it) }
        ServerLifecycleEvents.BEFORE_SAVE.register { it, _, _ -> SaveServerState(it) }
        ServerLifecycleEvents.SERVER_STOPPED.register { if (LoadedServer === it) LoadedServer = null }
    }

    companion object {
        private val LOGGER = LogUtils.getLogger()
        const val MOD_ID = "nguhcraft"
        @JvmStatic fun Id(S: String): Identifier = Identifier.fromNamespaceAndPath(MOD_ID, S)
        @Volatile private var LoadedServer: MinecraftServer? = null

        @JvmStatic fun<T : Any> RKey(Registry: ResourceKey<Registry<T>>, S: String): ResourceKey<T> = ResourceKey.create(Registry, Id(S))

        fun CheckLogs(S: MinecraftServer) {
            // Check that for every naturally generating tree, we must have an
            // entry in the tree chopping code.
            val Logs = S.registryAccess().getOrThrow(BlockTags.OVERWORLD_NATURAL_LOGS).size()
            val Chop = TreeToChop.LOG_TO_LEAVES.size
            if (Logs != Chop) throw IllegalStateException(
                "Mismatch between number of natural logs ($Logs) and logs registered in the " +
                "tree chopper ($Chop). Please update 'LOG_TO_LEAVES' and 'WOOD_TYPES' in " +
                "'org.nguh.nguhcraft.server.TreeToChop' and add any new trees there."
            )
        }

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

            LoadedServer = S
            LOGGER.info("[SETUP] Done")
        }

        private fun SavePath(S: MinecraftServer): Path {
            return S.getWorldPath(LevelResource.ROOT).resolve("nguhcraft.dat")
        }

        private fun SaveServerState(S: MinecraftServer) {
            // Apparently Minecraft now saves the server during startup, before we have loaded our data, so we need to
            // skip saving if we haven't loaded our stuff yet, as not to overwrite with empty data.
            if (LoadedServer !== S) {
                LOGGER.info("Not saving server state: state has not been loaded yet")
                return
            }

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
