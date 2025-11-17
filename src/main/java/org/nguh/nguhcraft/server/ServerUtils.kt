package org.nguh.nguhcraft.server

import com.mojang.logging.LogUtils
import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.effect.MobEffect
import net.minecraft.world.effect.MobEffectInstance
import net.minecraft.world.entity.monster.piglin.AbstractPiglin
import net.minecraft.world.entity.monster.Enemy
import net.minecraft.world.entity.animal.IronGolem
import net.minecraft.world.entity.npc.Villager
import net.minecraft.world.entity.projectile.ProjectileUtil
import net.minecraft.world.entity.projectile.ThrownTrident
import net.minecraft.world.item.ItemStack
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket
import net.minecraft.world.item.crafting.RecipeType
import net.minecraft.world.item.crafting.SmeltingRecipe
import net.minecraft.world.item.crafting.SingleRecipeInput
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.core.Holder
import net.minecraft.server.MinecraftServer
import net.minecraft.commands.CommandSourceStack
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.level.ServerLevel
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.HitResult
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.ClipContext
import net.minecraft.world.level.portal.TeleportTransition
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.Constants.MAX_HOMING_DISTANCE
import org.nguh.nguhcraft.Effects
import org.nguh.nguhcraft.NguhDamageTypes
import org.nguh.nguhcraft.Utils.EnchantLvl
import org.nguh.nguhcraft.accessors.TridentEntityAccessor
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import org.nguh.nguhcraft.entity.EntitySpawnManager
import org.nguh.nguhcraft.item.LockableBlockEntity
import org.nguh.nguhcraft.network.ClientFlags
import org.nguh.nguhcraft.protect.ProtectionManager
import org.nguh.nguhcraft.server.accessors.LivingEntityAccessor
import org.nguh.nguhcraft.server.dedicated.Discord
import org.slf4j.Logger

/** A TeleportTarget that doesn’t store the world directly and can actually be saved. */
class SerialisedTeleportTarget(
    val World: ResourceKey<Level>,
    val X: Double,
    val Y: Double,
    val Z: Double,
    val Yaw: Float,
    val Pitch: Float,
) {
    fun Instantiate(S: MinecraftServer) = TeleportTransition(
        S.getLevel(World)!!,
        Vec3(X, Y, Z),
        Vec3.ZERO,
        Yaw,
        Pitch,
        TeleportTransition.DO_NOTHING
    )

    companion object {
        val CODEC: Codec<SerialisedTeleportTarget> = RecordCodecBuilder.create {
            it.group(
                ResourceKey.codec(Registries.DIMENSION).fieldOf("World").forGetter(SerialisedTeleportTarget::World),
                Codec.DOUBLE.fieldOf("X").forGetter(SerialisedTeleportTarget::X),
                Codec.DOUBLE.fieldOf("Y").forGetter(SerialisedTeleportTarget::Y),
                Codec.DOUBLE.fieldOf("Z").forGetter(SerialisedTeleportTarget::Z),
                Codec.FLOAT.fieldOf("Yaw").forGetter(SerialisedTeleportTarget::Yaw),
                Codec.FLOAT.fieldOf("Pitch").forGetter(SerialisedTeleportTarget::Pitch),
            ).apply(it, ::SerialisedTeleportTarget)
        }
    }
}

/**
 * Server-side utilities.
 *
 * Do NOT put any globals in here that require static initialisation that
 * depends on registries etc. because some of the functions in here are
 * run from the pre-launch entrypoint.
 */
object ServerUtils {
    private val BORDER_TITLE: Component = Component.literal("TURN BACK").withStyle(ChatFormatting.RED)
    private val BORDER_SUBTITLE: Component = Component.literal("You may not cross the border")
    private val LOGGER: Logger = LogUtils.getLogger()

    /** Living entity tick. */
    @JvmStatic
    fun ActOnLivingEntityBaseTick(LE: LivingEntity) {
        // Handle entities with NaN health.
        if (LE.health.isNaN()) {
            // Disconnect players.
            if (LE is ServerPlayer) {
                LOGGER.warn("Player {} had NaN health, disconnecting.", LE.Name.string)
                LE.health = 0F
                LE.connection.disconnect(Component.nullToEmpty("Health was NaN!"))
                return
            }

            // Discard entities.
            LOGGER.warn("Living entity has NaN health, discarding: {}", LE)
            LE.discard()
        }
    }

    /** Sync data on join. */
    @JvmStatic
    fun ActOnPlayerJoin(SP: ServerPlayer) {
        // Sync data with the client.
        val LEA = SP as LivingEntityAccessor
        Manager.SendAll(SP)
        SP.SetClientFlag(ClientFlags.BYPASSES_REGION_PROTECTION, SP.Data.BypassesRegionProtection)
        SP.SetClientFlag(ClientFlags.IN_HYPERSHOT_CONTEXT, LEA.hypershotContext != null)
        SP.SetClientFlag(ClientFlags.VANISHED, SP.Data.Vanished)
    }

    /**
    * Early player tick.
    *
    * This currently handles the world border check.
    */
    @JvmStatic
    fun ActOnPlayerTick(SP: ServerPlayer) {
        val SW = SP.level()

        // Check if the player is outside the world border.
        // TODO: Can we make a 'global' region for the entire world
        //       to simplify e.g. this world border check? It would
        //       be a region that cannot be deleted and which is always
        //       at the end of the list (and which expands first if the
        //       world border is increased).
        if (!SW.worldBorder.isWithinBounds(SP.boundingBox)) {
            SP.Teleport(SW, SW.sharedSpawnPos)
            SendTitle(SP, BORDER_TITLE, BORDER_SUBTITLE)
            LOGGER.warn("Player {} tried to leave the border.", SP.Name.string)
        }

        SP.Server.ProtectionManager.TickRegionsForPlayer(SP)
    }

    /** Broadcast a join message for a player. */
    @JvmStatic
    fun ActOnPlayerQuit(SP: ServerPlayer, Msg: Component) {
        SP.Server.ProtectionManager.TickPlayerQuit(SP)
        SendPlayerJoinQuitMessage(SP, Msg)
    }

    /** Apply beacon effects to mobs. */
    @JvmStatic
    fun ApplyBeaconEffectsToVillagers(
        W: Level,
        Pos: BlockPos,
        BeaconLevel: Int,
        Primary: Holder<MobEffect>?,
        Secondary: Holder<MobEffect>?
    ) {
        // Check if these effects are applicable to villagers.
        var Primary = Primary
        var Secondary = Secondary
        if (!Effects.BEACON_EFFECTS_AFFECTING_VILLAGERS.contains(Secondary)) Secondary = null
        if (!Effects.BEACON_EFFECTS_AFFECTING_VILLAGERS.contains(Primary)) Primary = Secondary
        if (W !is ServerLevel || Primary == null) return

        // This calculation is taken from BeaconBlockEntity::applyPlayerEffects().
        val Distance = (BeaconLevel * 10 + 10).toDouble()
        val Amplifier = if (BeaconLevel >= 4 && Primary == Secondary) 1 else 0
        val Duration = (9 + BeaconLevel * 2) * 20
        val SeparateSecondary = BeaconLevel >= 4 && Primary != Secondary && Secondary != null
        val B = AABB(Pos).inflate(Distance).expandTowards(0.0, W.height.toDouble(), 0.0)

        // Apply the status effect(s) to all villager entities.
        for (E in W.getEntities(EntityType.VILLAGER, B) { true }) {
            E.addEffect(MobEffectInstance(Primary, Duration, Amplifier, true, true))
            if (SeparateSecondary)
                E.addEffect(MobEffectInstance(Secondary, Duration, 0, true, true))
        }
    }

    /** Check if we’re running on a dedicated server. */
    fun IsDedicatedServer() = FabricLoader.getInstance().environmentType == EnvType.SERVER
    fun IsIntegratedServer() = !IsDedicatedServer()

    /** Check if a player is linked or an operator. */
    @JvmStatic
    fun IsLinkedOrOperator(SP: ServerPlayer) =
        IsIntegratedServer() || Discord.__IsLinkedOrOperatorImpl(SP)

    /** Check if this server command source has moderator permissions. */
    @JvmStatic
    fun IsModerator(S: CommandSourceStack) =
        S.hasPermission(4) || S.player?.Data?.IsModerator == true

    /** @return `true` if the entity entered or was already in a hypershot context. */
    @JvmStatic
    fun MaybeEnterHypershotContext(
        Shooter: LivingEntity,
        Hand: InteractionHand,
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
        val HSLvl = EnchantLvl(Shooter.level(), Weapon, NguhcraftEnchantments.HYPERSHOT)
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
        if (Shooter is ServerPlayer) Shooter.SetClientFlag(
            ClientFlags.IN_HYPERSHOT_CONTEXT,
            true
        )

        return true
    }

    @JvmStatic
    fun MaybeMakeHomingArrow(W: Level, Shooter: LivingEntity): LivingEntity? {
        // Perform a ray cast up to the max distance, starting at the shooter’s
        // position. Passing a 1 for the tick delta yields the actual camera pos
        // etc.
        val VCam = Shooter.getEyePosition(1.0f)
        val VRot = Shooter.getViewVector(1.0f)
        var VEnd = VCam.add(VRot.x * MAX_HOMING_DISTANCE, VRot.y * MAX_HOMING_DISTANCE, VRot.z * MAX_HOMING_DISTANCE)
        val Ray = W.clip(ClipContext(
            VCam,
            VEnd,
            ClipContext.Block.OUTLINE,
            ClipContext.Fluid.NONE, Shooter
        ))

        // If we hit something, don’t go further.
        if (Ray.type !== HitResult.Type.MISS) VEnd = Ray.location

        // Search for an entity to target. Extend the arrow’s bounding box to
        // the block that we’ve hit, or to the max distance if we missed and
        // check for entity collisions.
        val BB = AABB.unitCubeFromLowerCorner(VCam).expandTowards(VEnd.subtract(VCam)).inflate(1.0)
        val EHR = ProjectileUtil.getEntityHitResult(
            Shooter,
            VCam,
            VEnd,
            BB,
            { !it.isSpectator && it.isPickable() },
            Mth.square(MAX_HOMING_DISTANCE).toDouble()
        )

        // If we’re aiming at an entity, use it as the target.
        if (EHR != null) {
            if (EHR.entity is LivingEntity) return EHR.entity as LivingEntity
        }

        // If we can’t find an entity, look around to see if there is anything else nearby.
        val Es = W.getEntities(Shooter, BB.inflate(5.0)) {
            it is LivingEntity &&
            it !is Villager &&
            it !is IronGolem &&
            (it !is AbstractPiglin || it.target != null) &&
            it.isPickable() &&
            !it.isSpectator &&
            Shooter.hasLineOfSight(it)
        }

        // Prefer hostile entities over friendly ones and sort by distance.
        Es.sortWith { A, B ->
            if (A is Enemy == B is Enemy) A.distanceTo(Shooter).compareTo(B.distanceTo(Shooter))
            else if (A is Enemy) -1
            else 1
        }

        return Es.firstOrNull() as LivingEntity?
    }

    @JvmStatic
    fun Multicast(P: Collection<ServerPlayer>, Packet: CustomPacketPayload) {
        for (Player in P) ServerPlayNetworking.send(Player, Packet)
    }

    /**
     * Obliterate the player.
     *
     * This summons a lightning bolt at their location (which is only there
     * for atmosphere, though), then kills them.
     */
    fun Obliterate(SP: ServerPlayer) {
        if (SP.isDeadOrDying || SP.isSpectator || SP.isCreative) return
        val SW = SP.level()
        StrikeLightning(SW, SP.position(), null, true)
        SP.hurtServer(SW, NguhDamageTypes.Obliterated(SW), Float.MAX_VALUE)
    }

    fun RoundExp(Exp: Float): Int {
        var Int = Mth.floor(Exp)
        val Frac = Mth.frac(Exp)
        if (Frac != 0.0f && Math.random() < Frac.toDouble()) Int++
        return Int
    }

    /** Broadcast a join message for a player. */
    @JvmStatic
    fun SendPlayerJoinQuitMessage(SP: ServerPlayer, Msg: Component) {
        if (!SP.Data.Vanished) SP.Server.Broadcast(Msg)
    }

    /**
    * Send a title (and subtitle) to a player.
    *
    * @param Title The title to send. Ignored if `null`.
    * @param Subtitle The subtitle to send. Ignored if `null`.
    */
    fun SendTitle(SP: ServerPlayer, Title: Component?, Subtitle: Component?) {
        if (Title != null) SP.connection.send(ClientboundSetTitleTextPacket(Title))
        if (Subtitle != null) SP.connection.send(ClientboundSetSubtitleTextPacket(Subtitle))
    }

    /** Unconditionally strike lightning. */
    @JvmStatic
    @JvmOverloads
    fun StrikeLightning(W: ServerLevel, Where: Vec3, TE: ThrownTrident? = null, Cosmetic: Boolean = false) {
        val Lightning = EntityType.LIGHTNING_BOLT.spawn(
            W,
            BlockPos.containing(Where),
            EntitySpawnReason.SPAWN_ITEM_USE
        )

        if (Lightning != null) {
            Lightning.setVisualOnly(Cosmetic)
            Lightning.cause = TE?.owner as? ServerPlayer
            if (TE != null) (TE as TridentEntityAccessor).`Nguhcraft$SetStruckLightning`()
        }
    }

    /** Called during the world tick on the server. */
    @JvmStatic
    fun TickWorld(SW: ServerLevel) {
        TreeToChop.Tick(SW)
        SW.server.EntitySpawnManager.Tick(SW)
    }

    /** Result of smelting a stack. */
    data class SmeltingResult(val Stack: ItemStack, val Experience: Int)

    /** Try to smelt this block as an item. */
    @JvmStatic
    fun TrySmeltBlock(W: ServerLevel, Block: BlockState): SmeltingResult? {
        val I = ItemStack(Block.block.asItem())
        if (I.isEmpty) return null

        val Input = SingleRecipeInput(I)
        val optional = W.recipeAccess().getRecipeFor(RecipeType.SMELTING, Input, W)
        if (optional.isEmpty) return null

        val Recipe: SmeltingRecipe = optional.get().value()
        val Smelted: ItemStack = Recipe.assemble(Input, W.registryAccess())
        if (Smelted.isEmpty) return null
        return SmeltingResult(Smelted.copyWithCount(I.count), RoundExp(Recipe.experience()))
    }

    /** Update the lock on a container. Pass null to unlock it. */
    fun UpdateLock(LE: LockableBlockEntity, NewLock: String?) {
        LE as BlockEntity // Every LockableBlockEntity is a BlockEntity.
        LE.`Nguhcraft$SetLockInternal`(NewLock)
        (LE.level as ServerLevel).chunkSource.blockChanged(LE.blockPos)
        LE.setChanged()
    }
}
