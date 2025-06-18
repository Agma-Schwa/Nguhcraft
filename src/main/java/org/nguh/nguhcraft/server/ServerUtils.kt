package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.BlockState
import net.minecraft.block.entity.BlockEntity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.effect.StatusEffect
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.mob.AbstractPiglinEntity
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.IronGolemEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.projectile.ProjectileUtil
import net.minecraft.entity.projectile.TridentEntity
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.network.packet.CustomPayload
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket
import net.minecraft.network.packet.s2c.play.TitleS2CPacket
import net.minecraft.recipe.RecipeType
import net.minecraft.recipe.SmeltingRecipe
import net.minecraft.recipe.input.SingleStackRecipeInput
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.server.MinecraftServer
import net.minecraft.server.command.ServerCommandSource
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import net.minecraft.util.Hand
import net.minecraft.util.Identifier
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import net.minecraft.world.TeleportTarget
import net.minecraft.world.World
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.Effects
import org.nguh.nguhcraft.Nbt
import org.nguh.nguhcraft.NguhDamageTypes
import org.nguh.nguhcraft.SyncedGameRule
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.accessors.TridentEntityAccessor
import org.nguh.nguhcraft.block.LockableBlockEntity
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor
import org.nguh.nguhcraft.server.accessors.ServerPlayerAccessor
import org.nguh.nguhcraft.server.dedicated.Discord
import org.nguh.nguhcraft.set
import org.slf4j.Logger

/**
 * Server-side utilities.
 *
 * Do NOT put any globals in here that require static initialisation that
 * depends on registries etc. because some of the functions in here are
 * run from the pre-launch entrypoint.
 */
object ServerUtils {
    private val BORDER_TITLE: Text = Text.literal("TURN BACK").formatted(Formatting.RED)
    private val BORDER_SUBTITLE: Text = Text.literal("You may not cross the border")
    private val LOGGER: Logger = LogUtils.getLogger()

    /** Living entity tick. */
    @JvmStatic
    fun ActOnLivingEntityBaseTick(LE: LivingEntity) {
        // Handle entities with NaN health.
        if (LE.health.isNaN()) {
            // Disconnect players.
            if (LE is ServerPlayerEntity) {
                LOGGER.warn("Player {} had NaN health, disconnecting.", LE.displayName!!.string)
                LE.health = 0F
                LE.networkHandler.disconnect(Text.of("Health was NaN!"))
                return
            }

            // Discard entities.
            LOGGER.warn("Living entity has NaN health, discarding: {}", LE)
            LE.discard()
        }
    }

    /** Sync data on join. */
    @JvmStatic
    fun ActOnPlayerJoin(SP: ServerPlayerEntity) {
        // Sync data with the client.
        val LEA = SP as LivingEntityAccessor
        val SPA = SP as ServerPlayerAccessor
        Manager.SendAll(SP)
        SP.SetClientFlag(ClientFlags.BYPASSES_REGION_PROTECTION, SPA.bypassesRegionProtection)
        SP.SetClientFlag(ClientFlags.IN_HYPERSHOT_CONTEXT, LEA.hypershotContext != null)
        SP.SetClientFlag(ClientFlags.VANISHED, SP.IsVanished)
    }

    /**
    * Early player tick.
    *
    * This currently handles the world border check.
    */
    @JvmStatic
    fun ActOnPlayerTick(SP: ServerPlayerEntity) {
        val SW = SP.serverWorld

        // Check if the player is outside the world border.
        // TODO: Can we make a 'global' region for the entire world
        //       to simplify e.g. this world border check? It would
        //       be a region that cannot be deleted and which is always
        //       at the end of the list (and which expands first if the
        //       world border is increased).
        if (!SW.worldBorder.contains(SP.boundingBox)) {
            SP.Teleport(SW, SW.spawnPos)
            SendTitle(SP, BORDER_TITLE, BORDER_SUBTITLE)
            LOGGER.warn("Player {} tried to leave the border.", SP.displayName!!.string)
        }

        SP.server.ProtectionManager.TickRegionsForPlayer(SP)
    }

    /** Broadcast a join message for a player. */
    @JvmStatic
    fun ActOnPlayerQuit(SP: ServerPlayerEntity, Msg: Text) {
        SP.server.ProtectionManager.TickPlayerQuit(SP)
        SendPlayerJoinQuitMessage(SP, Msg)
    }

    /** Apply beacon effects to mobs. */
    @JvmStatic
    fun ApplyBeaconEffectsToVillagers(
        W: World,
        Pos: BlockPos,
        BeaconLevel: Int,
        Primary: RegistryEntry<StatusEffect>?,
        Secondary: RegistryEntry<StatusEffect>?
    ) {
        // Check if these effects are applicable to villagers.
        var Primary = Primary
        var Secondary = Secondary
        if (!Effects.BEACON_EFFECTS_AFFECTING_VILLAGERS.contains(Secondary)) Secondary = null
        if (!Effects.BEACON_EFFECTS_AFFECTING_VILLAGERS.contains(Primary)) Primary = Secondary
        if (W !is ServerWorld || Primary == null) return

        // This calculation is taken from BeaconBlockEntity::applyPlayerEffects().
        val Distance = (BeaconLevel * 10 + 10).toDouble()
        val Amplifier = if (BeaconLevel >= 4 && Primary == Secondary) 1 else 0
        val Duration = (9 + BeaconLevel * 2) * 20
        val SeparateSecondary = BeaconLevel >= 4 && Primary != Secondary && Secondary != null
        val B = Box(Pos).expand(Distance).stretch(0.0, W.height.toDouble(), 0.0)

        // Apply the status effect(s) to all villager entities.
        for (E in W.getEntitiesByType(EntityType.VILLAGER, B) { true }) {
            E.addStatusEffect(StatusEffectInstance(Primary, Duration, Amplifier, true, true))
            if (SeparateSecondary)
                E.addStatusEffect(StatusEffectInstance(Secondary, Duration, 0, true, true))
        }
    }

    /** Check if we’re running on a dedicated server. */
    fun IsDedicatedServer() = FabricLoader.getInstance().environmentType == EnvType.SERVER
    fun IsIntegratedServer() = !IsDedicatedServer()

    /** Check if a player is linked or an operator. */
    @JvmStatic
    fun IsLinkedOrOperator(SP: ServerPlayerEntity) =
        IsIntegratedServer() || Discord.__IsLinkedOrOperatorImpl(SP)

    /** Check if this server command source has moderator permissions. */
    @JvmStatic
    fun IsModerator(S: ServerCommandSource) = S.hasPermissionLevel(4) || S.player?.IsModerator == true

    /** @return `true` if the entity entered or was already in a hypershot context. */
    @JvmStatic
    fun MaybeEnterHypershotContext(
        Shooter: LivingEntity,
        Hand: Hand,
        Weapon: ItemStack,
        Projectiles: List<ItemStack>,
        Speed: Float,
        Div: Float,
        Crit: Boolean
    ): Boolean {
        // Entity already in hypershot context.
        val NLE = (Shooter as LivingEntityAccessor)
        if (NLE.hypershotContext != null) return true

        // Stack does not have hypershot.
        val HSLvl = EnchantLvl(Shooter.world, Weapon, NguhcraftEnchantments.HYPERSHOT)
        if (HSLvl == 0) return false

        // Enter hypershot context.
        NLE.setHypershotContext(
            HypershotContext(
                Hand,
                Weapon,
                Projectiles.stream().map { obj: ItemStack -> obj.copy() }.toList(),
                Speed,
                Div,
                Crit,
                HSLvl
            )
        )

        // If this is a player, tell them about this.
        if (Shooter is ServerPlayerEntity) Shooter.SetClientFlag(
            ClientFlags.IN_HYPERSHOT_CONTEXT,
            true
        )

        return true
    }

    @JvmStatic
    fun MaybeMakeHomingArrow(W: World, Shooter: LivingEntity): LivingEntity? {
        // Perform a ray cast up to the max distance, starting at the shooter’s
        // position. Passing a 1 for the tick delta yields the actual camera pos
        // etc.
        val VCam = Shooter.getCameraPosVec(1.0f)
        val VRot = Shooter.getRotationVec(1.0f)
        var VEnd = VCam.add(VRot.x * MAX_HOMING_DISTANCE, VRot.y * MAX_HOMING_DISTANCE, VRot.z * MAX_HOMING_DISTANCE)
        val Ray = W.raycast(RaycastContext(
            VCam,
            VEnd,
            RaycastContext.ShapeType.OUTLINE,
            RaycastContext.FluidHandling.NONE, Shooter
        ))

        // If we hit something, don’t go further.
        if (Ray.type !== HitResult.Type.MISS) VEnd = Ray.pos

        // Search for an entity to target. Extend the arrow’s bounding box to
        // the block that we’ve hit, or to the max distance if we missed and
        // check for entity collisions.
        val BB = Box.from(VCam).stretch(VEnd.subtract(VCam)).expand(1.0)
        val EHR = ProjectileUtil.raycast(
            Shooter,
            VCam,
            VEnd,
            BB,
            { !it.isSpectator && it.canHit() },
            MathHelper.square(MAX_HOMING_DISTANCE).toDouble()
        )

        // If we’re aiming at an entity, use it as the target.
        if (EHR != null) {
            if (EHR.entity is LivingEntity) return EHR.entity as LivingEntity
        }

        // If we can’t find an entity, look around to see if there is anything else nearby.
        val Es = W.getOtherEntities(Shooter, BB.expand(5.0)) {
            it is LivingEntity &&
            it !is VillagerEntity &&
            it !is IronGolemEntity &&
            (it !is AbstractPiglinEntity || it.target != null) &&
            it.canHit() &&
            !it.isSpectator &&
            Shooter.canSee(it)
        }

        // Prefer hostile entities over friendly ones and sort by distance.
        Es.sortWith { A, B ->
            if (A is Monster == B is Monster) A.distanceTo(Shooter).compareTo(B.distanceTo(Shooter))
            else if (A is Monster) -1
            else 1
        }

        return Es.firstOrNull() as LivingEntity?
    }

    @JvmStatic
    fun Multicast(P: Collection<ServerPlayerEntity>, Packet: CustomPayload) {
        for (Player in P) ServerPlayNetworking.send(Player, Packet)
    }

    /**
     * Obliterate the player.
     *
     * This summons a lightning bolt at their location (which is only there
     * for atmosphere, though), then kills them.
     */
    fun Obliterate(SP: ServerPlayerEntity) {
        if (SP.isDead || SP.isSpectator || SP.isCreative) return
        val SW = SP.serverWorld
        StrikeLightning(SW, SP.pos, null, true)
        SP.damage(SW, NguhDamageTypes.Obliterated(SW), Float.MAX_VALUE)
    }

    fun RoundExp(Exp: Float): Int {
        var Int = MathHelper.floor(Exp)
        val Frac = MathHelper.fractionalPart(Exp)
        if (Frac != 0.0f && Math.random() < Frac.toDouble()) Int++
        return Int
    }

    /** Broadcast a join message for a player. */
    @JvmStatic
    fun SendPlayerJoinQuitMessage(SP: ServerPlayerEntity, Msg: Text) {
        if (!SP.IsVanished) SP.server.Broadcast(Msg)
    }

    /**
    * Send a title (and subtitle) to a player.
    *
    * @param Title The title to send. Ignored if `null`.
    * @param Subtitle The subtitle to send. Ignored if `null`.
    */
    fun SendTitle(SP: ServerPlayerEntity, Title: Text?, Subtitle: Text?) {
        if (Title != null) SP.networkHandler.sendPacket(TitleS2CPacket(Title))
        if (Subtitle != null) SP.networkHandler.sendPacket(SubtitleS2CPacket(Subtitle))
    }

    /** Unconditionally strike lightning. */
    @JvmStatic
    @JvmOverloads
    fun StrikeLightning(W: ServerWorld, Where: Vec3d, TE: TridentEntity? = null, Cosmetic: Boolean = false) {
        val Lightning = EntityType.LIGHTNING_BOLT.spawn(
            W,
            BlockPos.ofFloored(Where),
            SpawnReason.SPAWN_ITEM_USE
        )

        if (Lightning != null) {
            Lightning.setCosmetic(Cosmetic)
            Lightning.channeler = TE?.owner as? ServerPlayerEntity
            if (TE != null) (TE as TridentEntityAccessor).`Nguhcraft$SetStruckLightning`()
        }
    }

    /** Load a teleport target from NBT data. */
    @JvmStatic
    fun TeleportTargetFromNbt(Server: MinecraftServer, Tag : NbtCompound): TeleportTarget? {
        val Pos = Vec3d(Tag.getDouble("X"), Tag.getDouble("Y"), Tag.getDouble("Z"))
        val Yaw = Tag.getFloat("Yaw")
        val Pitch = Tag.getFloat("Pitch")
        val Dim = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(Tag.getString("World")))
        val SW = Server.getWorld(Dim) ?: return null
        return TeleportTarget(SW, Pos, Vec3d.ZERO, Yaw, Pitch, TeleportTarget.NO_OP)
    }

    /** Save a teleport target to NBT data. */
    @JvmStatic
    fun TeleportTargetToNbt(Target: TeleportTarget) = Nbt {
        set("X", Target.position.x)
        set("Y", Target.position.y)
        set("Z", Target.position.z)
        set("Yaw", Target.yaw)
        set("Pitch", Target.pitch)
        set("World", Target.world.registryKey.value.toString())
    }

    /** Result of smelting a stack. */
    data class SmeltingResult(val Stack: ItemStack, val Experience: Int)

    /** Try to smelt this block as an item. */
    @JvmStatic
    fun TrySmeltBlock(W: ServerWorld, Block: BlockState): SmeltingResult? {
        val I = ItemStack(Block.block.asItem())
        if (I.isEmpty) return null

        val Input = SingleStackRecipeInput(I)
        val optional = W.recipeManager.getFirstMatch(RecipeType.SMELTING, Input, W)
        if (optional.isEmpty) return null

        val Recipe: SmeltingRecipe = optional.get().value()
        val Smelted: ItemStack = Recipe.craft(Input, W.registryManager)
        if (Smelted.isEmpty) return null
        return SmeltingResult(Smelted.copyWithCount(I.count), RoundExp(Recipe.experience))
    }

    /** Update the lock on a container. */
    fun UpdateLock(LE: LockableBlockEntity, NewLock: ContainerLock) {
        LE as BlockEntity // Every LockableBlockEntity is a BlockEntity.
        LE.SetLockInternal(NewLock)
        (LE.world as ServerWorld).chunkManager.markForUpdate(LE.pos)
        LE.markDirty()
    }
}
