package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtIo
import net.minecraft.nbt.NbtSizeTracker
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.server.MinecraftServer
import net.minecraft.util.Identifier
import net.minecraft.util.WorldSavePath
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.item.NguhItems
import org.nguh.nguhcraft.network.*
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.server.*
import org.nguh.nguhcraft.server.command.Commands
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream

// TODO: Port all patches.
// - [ ] 1. Big Chungus
//   - [x] Enchantments
//     - [x] Health
//     - [x] Homing
//     - [x] Hypershot
//     - [x] Saturation
//     - [x] Smelting
//     - [x] Channeling II
//       - [x] On entity hit
//       - [x] On block hit
//     - [x] Make Multishot apply to bows (should already work in latest snapshot?)
//   - [x] Render enchantment levels properly
//   - [x] Patch enchant command to ignore restrictions
//   - [x] Prevent players from leaving the border
//   - [ ] Machine gun Ghasts
//   - [ ] Import Saved Lore as Lore
// - [x] 2. Vanish (There’s a separate mod for this that looks promising)
// - [x] 3. Tree Chopping
//   - [x] Basic implementation
//   - [x] Acacia and other diagonal trees.
// - [ ] Extras
//   - [ ] Increase chat message history and scrollback size (to like 10000)
//   - [ ] Moderator permission (store bit in player nbt)
//   - [x] Creative mode tab for treasures etc
//   - [x] Render potion levels properly.
//   - [ ] Make homing arrows target ‘Target Blocks’ if there are no valid entities (or if you target it directly)
// - [ ] World protection
//       This is used both for protected areas and to prevent unlinked
//       players from doing anything.
//   - [T] Have a way of locking chests and make them unbreakable.
//     - [x] Maybe barrels too if they can be locked.
//     - [x] Left click with key to unlock (drops the lock).
//     - [x] Show key in the ‘Chest is locked’ message.
//     - [x] Recipe to duplicate keys.
//     - [x] Allow opening in /bypass mode.
//     - [x] Enforce region protection.
//     - [x] What happens when a locked chest becomes a double chest?
//     - [x] Add little lock icon to chest (custom texture like furnace).
//     - [x] Command to generate a key.
//     - [T] Show full key in advanced tooltip.
//   - [x] Disallow hoppers.
//   - [T] Disallow Hopper minecarts.
//   - [x] Block placement/breaking
//     - [x] Block state changes (opening door, activating button/lever)
//     - [x] Special case: editing signs
//   - [T] Entity interactions
//     - [x] Attacking entities
//     - [x] TNT should not damage entities in protected regions.
//     - [x] Using beds and respawn anchors
//     - [x] Ender pearls
//     - [x] Chorus fruit
//     - [x] Armour stands
//     - [x] Using shulker boxes
//     - [T] Interacting w/ villagers
//     - [x] Using boats/minecarts -> ACTUALLY TEST THIS
//     - [x] Breaking boats/minecarts.
//     - [x] Placing boats/minecarts.
//     - [x] End crystals
//       - [x] Placing end crystals.
//       - [x] Destroying end crystals.
//       - [x] End crystal explosion effects.
//     - [x] AOEs (potions, dragon fireball)
//     - [x] Lightning.
//       - [x] Should not damage entities.
//       - [x] Should not ignite blocks.
//         - [x] In protected areas.
//         - [x] If caused by Channeling II (so I can smite people in the PoŊ).
//   - [x] Melting.
//   - [x] Freezing.
//   - [x] Snowing
//   - [x] Creating snow/iron golems.
//   - [x] Creating withers
//   - [x] Frost walker.
//   - [x] Picking up fluids using buckets.
//   - [x] Make channeling trident fire texture blue.
//   - [ ] Projectiles
//     - [x] Investigate modifying LivingEntity#isInvulnerableTo() (check ALL projectiles)
//     - [x] #onCollision()
//       - [x] Eggs should not spawn chickens.
//       - [x] Potions should not apply (negative) effects.
//     - [x] #onEntityHit()
//       - [x] Fishing hooks should not be allowed to hook anything.
//     - [ ] #onBlockHit()
//       - [x] Water potions should not be able to extinguish fire.
//       - [ ] Small fireballs should not spawn fire.
//     - [x] Wither skulls hitting entities.
//     - [x] Arrows, fireballs, snowballs, other projectiles, tridents.
//     - [ ] Allow picking up projectiles.
//   - [x] Clear fire ticks.
//   - [T] Fire spread.
//   - [x] Lava flow.
//   - [x] Water flow.
//   - [T] Fire destroying blocks.
//   - [x] Also enable protection for ops by default and add a `/bypass` command
//         to toggle it on a per-player basis (so that Agma doesn’t accidentally
//         destroy anything). Also only check that flag instead of the op permission
//         when checking for protection.
//   - [x] Block interactions
//     - [x] Using ender chests (should always be allowed)
//     - [x] Using crafting tables (should always be allowed)
//     - [x] Reading books on lecterns (but not taking them)
//       - [x] Prevent taking on the server side.
//       - [x] Grey out button on the client side.
//   - [x] Disable enderman griefing entirely.
//   - [x] TNT
//   - [x] Pistons that extend into protected areas or pull blocks out of them.
//   - [x] Flint and steel.
//   - [T] Vine spread
//   - [x] Creepers
//   - [x] Ranged weapons (bows, crossbows, tridents, fire charges, fireworks)
//   - [x] Pressure plates
// - [ ] Impaling should always work in water.
// - [x] MINECARTS
//   - [x] Main patch.
//   - [ ] Interpolate movement in a way that doesn’t suck.
// - [x] A /discard command that just outright removes an entity from the world without dropping loot etc.
// - [ ] Drowned have a 50% chance to spawn with Channeling II tridents (but don’t drop the Channeling II)
// - [ ] Finally, look at some of the other mods Annwan linked.
// - [x] /warp, /delwarp, /setwarp, /warps
// - [x] /home, /sethome, /home player:<name>, /home bed
// - [x] Disable the ‘open to LAN’ button because it is NOT going to work.
// - [x] /top
// - [x] Completion for /region.
// - [ ] DELETE UNUSED CHUNKS FOR THE 1.21 update.
// - [x] Different item icon for decorative hoppers (a white dot or sth, or maybe the structure void texture overlaid).
// - [x] Interacting w/ chiselled bookshelves.

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
        @JvmStatic fun Id(S: String): Identifier = Identifier.of(MOD_ID, S)
        @JvmStatic fun<T> RKey(Registry: RegistryKey<Registry<T>>, S: String): RegistryKey<T> = RegistryKey.of(Registry, Id(S))

        private fun LoadServerState(S: MinecraftServer) {
            LOGGER.info("[SETUP] Setting up server state")

            // Load saved state.
            try {
                // Read from disk.
                val Tag = NbtIo.readCompressed(
                    SavePath(S).inputStream(),
                    NbtSizeTracker.ofUnlimitedBytes()
                )

                // Some of the world data was previously stored in a separate file.
                val ProtMgr = Manager.Get<ProtectionManager>(S)
                if (!Tag.contains(ProtMgr.TagName)) Tag.put(ProtMgr.TagName, Nbt {
                    for (SW in S.worlds) {
                        try {
                            val Path = SW.server.getSavePath(WorldSavePath.ROOT).resolve(
                                "nguhcraft.extraworlddata.${SW.registryKey.value.path}.dat"
                            )

                            if (Path.exists()) {
                                val Regions = NbtIo.readCompressed(Path, NbtSizeTracker.ofUnlimitedBytes())
                                    .getList("Regions", NbtElement.COMPOUND_TYPE.toInt())
                                if (!Regions.isNullOrEmpty()) set(Utils.SerialiseWorldToString(SW.registryKey), Regions)
                            }
                        } catch (E: Exception) {
                            LOGGER.error("Nguhcraft: Failed to load external region data: ${E.message}")
                        }
                    }
                })

                // Load global data.
                Manager.InitFromSaveData(S, Tag)
            } catch (E: Exception) {
                LOGGER.warn("Nguhcraft: Failed to load persistent state; using defaults: ${E.message}")
            }

            LOGGER.info("[SETUP] Done")
        }

        private fun SavePath(S: MinecraftServer): Path {
            return S.getSavePath(WorldSavePath.ROOT).resolve("nguhcraft.dat")
        }

        private fun SaveServerState(S: MinecraftServer) {
            LOGGER.info("Saving server state")
            try {
                val Tag = NbtCompound()
                Manager.SaveAll(S, Tag)
                NbtIo.writeCompressed(Tag, SavePath(S))
            } catch (E: Exception) {
                LOGGER.error("Nguhcraft: Failed to save persistent state")
                E.printStackTrace()
            }
        }
    }
}
