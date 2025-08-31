package org.nguh.nguhcraft.protect

import net.minecraft.block.Blocks
import net.minecraft.block.LecternBlock
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.SpawnReason
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.HappyGhastEntity
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.VehicleEntity
import net.minecraft.item.BoatItem
import net.minecraft.item.ItemStack
import net.minecraft.item.MinecartItem
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.server.MinecraftServer
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.shape.VoxelShape
import net.minecraft.world.World
import org.nguh.nguhcraft.isa
import org.nguh.nguhcraft.item.IsLocked
import org.nguh.nguhcraft.item.KeyItem
import org.nguh.nguhcraft.item.LockableBlockEntity
import org.nguh.nguhcraft.server.Manager
import org.nguh.nguhcraft.server.ServerProtectionManager
import java.util.function.Consumer

interface ProtectionManagerAccess {
    fun `Nguhcraft$GetProtectionManager`(): ProtectionManager
    fun `Nguhcraft$SetProtectionManager`(Mgr: ProtectionManager)
}

/** Enum denotes if an entity can teleport somewhere, or why it can’t. */
enum class TeleportResult {
    OK,
    EXIT_DISALLOWED,
    ENTRY_DISALLOWED,
}

typealias RegionLists = Map<RegistryKey<World>, Collection<Region>>

/**
 * Handler that contains common code paths related to world protection.
 *
 * This API generally provides three families of functions:
 *
 * - ‘AllowX’, which check whether a *player* is allowed to perform an action.
 * - ‘IsX’, which check whether an action is allowed in the absence of a player.
 * - ‘HandleX’, which does the above but may also modify an action to do something else instead.
 *
 * For example:
 *
 * - [AllowBlockModify] checks if a player is allowed to interact with
 *   a block in a way that would modify it; this typically handles left
 *   clicking it.
 *
 * - [AllowEntityAttack] checks if a player is allowed to attack an entity.
 *
 * - [AllowEntityInteract] checks if a player is allowed to interact with
 *   an entity; notably, this does not include attacking it.
 *
 * - [AllowItemUse] checks if a player is allowed to use an item (not on a
 *   block).
 *
 * - [HandleBlockInteract] handles interacting (right-clicking) with a block;
 *   this may rewrite the interaction to an item use (e.g. when right-clicking
 *   on a protected chest with an apple in hand, start eating the apple instead).
 *
 * - [IsProtectedBlock] checks whether a block can be modified in the absence
 *   of a player.
 *
 * - [IsProtectedEntity] checks whether an entity is protected from world effects;
 *   this does *not* handle block entities. Use [IsProtectedBlock] for that.
 */
abstract class ProtectionManager(protected val Regions: RegionLists) : Manager() {
    /**
     * Check if a player is allowed to break, start breaking, or place a
     * block at this block position.
     */
    private fun _AllowBlockModify(PE: PlayerEntity, W: World, Pos: BlockPos): Boolean {
        // Player has bypass. Always allow.
        if (_BypassesRegionProtection(PE)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Block is within the bounds of a protected region. Deny.
        if (_IsProtectedBlock(W, Pos)) return false

        // Otherwise, allow.
        return true
    }

    /** Check if this entity is protected from attacks by a player. */
    private fun _AllowEntityAttack(AttackingPlayer: PlayerEntity, AttackedEntity: Entity): Boolean {
        fun Allow(Predicate: (R: Region) -> Boolean): Boolean {
            val R = _FindRegionContainingBlock(
                AttackedEntity.world,
                AttackedEntity.blockPos
            ) ?: return true
            return Predicate(R)
        }

        // Player has bypass. Always allow.
        if (_BypassesRegionProtection(AttackingPlayer)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(AttackingPlayer)) return false

        // Check region flags.
        return when (AttackedEntity) {
            is Monster -> true
            is PlayerEntity -> Allow(Region::AllowsPvP)
            is VehicleEntity -> Allow(Region::AllowsVehicleUse)
            else -> Allow(Region::AllowsAttackingFriendlyEntities)
        }
    }

    /** Check if a player is allowed to interact (= right-click) with an entity. */
    private fun _AllowEntityInteract(PE: PlayerEntity, E: Entity): Boolean {
        // Player has bypass. Always allow.
        if (_BypassesRegionProtection(PE)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Check region flags.
        val R = _FindRegionContainingBlock(E.world, E.blockPos) ?: return true
        return when (E) {
            is VehicleEntity, is HappyGhastEntity -> R.AllowsVehicleUse()
            is VillagerEntity -> R.AllowsVillagerTrading()
            else -> R.AllowsEntityInteraction()
        }
    }

    /** Check if a player should suffer fall damage when landing on a block. */
    private fun _AllowFallDamage(PE: PlayerEntity): Boolean {
        val R = _FindRegionContainingBlock(PE.world, PE.blockPos) ?: return true
        return R.AllowsPlayerFallDamage()
    }

    /** Check if a player is allowed to use an item (not on a block). */
    private fun _AllowItemUse(PE: PlayerEntity, W: World, St: ItemStack): Boolean {
        // Player has bypass. Always allow.
        if (_BypassesRegionProtection(PE)) return true

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return false

        // Disallow placing boats in protected regions.
        if (St.item is BoatItem) {
            val R = _FindRegionContainingBlock(W, PE.blockPos) ?: return true
            return R.AllowsVehicleUse()
        }

        // Everything else is allowed by default.
        return true
    }

    /**
     * Check if an entity, especially a player, is allowed to teleport to a location.
     *
     * This is used for ender pearls, chorus fruit, /wild, /home, but NOT admin
     * commands like /tp.
     */
    private fun _AllowTeleport(E: Entity, DestWorld: World, Pos: BlockPos): TeleportResult {
        // Always allow non-players to teleport; this is necessary so we can
        // move mobs and other entities with commands if need be.
        if (E !is PlayerEntity || _BypassesRegionProtection(E)) return TeleportResult.OK

        // Check if the player is allowed to leave their current region.
        //
        // Note that even if the source and destination region are the same, we still count
        // this as exiting/entering, because there might just be a particular PART of the
        // region that we don’t want people to be able to leave or enter.
        FindRegionContainingBlock(E.world, E.blockPos)?.let {
            if (!it.AllowsPlayerExit()) return TeleportResult.EXIT_DISALLOWED
        }

        // Check if the player is allowed to enter the destination region.
        return _AllowTeleportToImpl(DestWorld, Pos)
    }

    /** Same as AllowTeleport(), but only cares about the destination region. */
    private fun _AllowTeleportToFromAnywhere(E: Entity, DestWorld: World, Pos: BlockPos): Boolean {
        // Always allow non-players to teleport; this is necessary so we can
        // move mobs and other entities with commands if need be.
        if (E !is PlayerEntity || _BypassesRegionProtection(E)) return true

        // Check if the player is allowed to enter the destination region.
        return _AllowTeleportToImpl(DestWorld, Pos) == TeleportResult.OK
    }

    private fun _AllowTeleportToImpl(DestWorld: World, Pos: BlockPos): TeleportResult {
        val Dest = FindRegionContainingBlock(DestWorld, Pos) ?: return TeleportResult.OK
        return if (Dest.AllowsPlayerEntry() && Dest.AllowsTeleportation()) TeleportResult.OK
        else TeleportResult.ENTRY_DISALLOWED
    }

    /** Check if a player bypasses region protection. */
    abstract fun _BypassesRegionProtection(PE: PlayerEntity): Boolean

    /** Find the region that contains a block. */
    private fun _FindRegionContainingBlock(W: World, Pos: BlockPos) =
        RegionListFor(W).find { Pos in it }

    /** Get entity collisions. */
    private fun _GetCollisionsForEntity(W: World, E: Entity, Consumer: Consumer<List<VoxelShape>>) {
        if (E !is PlayerEntity || _BypassesRegionProtection(E)) return

        // Find all regions that contain the entity.
        val List = RegionListFor(W)
        Consumer.accept(List.filter { !it.AllowsPlayerExit() && it.Contains(E.x, E.z) }.map { it.InsideShape })
        Consumer.accept(List.filter { !it.AllowsPlayerEntry() && !it.Contains(E.x, E.z) }.map { it.OutsideShape })
    }

    /**
     * Handle interaction (= right-click), optionally with a block.
     *
     * This can also rewrite a block interaction to be a regular interaction
     * instead (e.g. instead of opening a chest by right-clicking on it, the
     * item in the player’s hand is used instead).
     *
     * Rewriting SUCCESS to CONSUME on a successful interaction is the caller’s
     * responsibility. Furthermore, a return value of SUCCESS only indicates that
     * the protection manager is fine with it, and not that the interaction should
     * succeed.
     *
     * @return ActionResult.FAIL if the interaction should be prevented.
     * @return ActionResult.PASS if the interaction should be rewritten to an item use.
     * @return ActionResult.SUCCESS if the interaction should be allowed.
     */
    private fun _HandleBlockInteract(PE: PlayerEntity, W: World, Pos: BlockPos, Stack: ItemStack?): ActionResult {
        // Player has bypass. Always allow.
        if (_BypassesRegionProtection(PE)) return ActionResult.SUCCESS

        // Player is not linked. Always deny.
        if (!IsLinked(PE)) return ActionResult.FAIL

        // Interacting with certain blocks is always fine.
        val St = W.getBlockState(Pos)
        when (St.block) {
            // These either have inventories that are per player and are
            // thus safe to access, or don’t have permanent effects when
            // interacted with.
            Blocks.ENDER_CHEST, Blocks.CRAFTING_TABLE, Blocks.BELL,
                -> return ActionResult.SUCCESS

            // Books on lecterns can be viewed, but not taken or placed;
            // the latter is handled later on in the lectern code.
            Blocks.LECTERN -> if (St.get(LecternBlock.HAS_BOOK)) return ActionResult.SUCCESS

            // Players might want to mark banners on a map.
            else -> if (St isa BlockTags.BANNERS) return ActionResult.SUCCESS
        }

        // Doors are a separate flag.
        if (St isa BlockTags.DOORS) {
            val R = _FindRegionContainingBlock(W, Pos) ?: return ActionResult.SUCCESS
            return if (R.AllowsDoors()) ActionResult.SUCCESS else ActionResult.FAIL
        }

        // As are buttons.
        if (St isa BlockTags.BUTTONS) {
            val R = _FindRegionContainingBlock(W, Pos) ?: return ActionResult.SUCCESS
            return if (R.AllowsButtons()) ActionResult.SUCCESS else ActionResult.FAIL
        }

        // Allow placing minecarts.
        if (Stack != null && Stack.item is MinecartItem && St isa BlockTags.RAILS) {
            val R = _FindRegionContainingBlock(W, Pos) ?: return ActionResult.SUCCESS
            return if (R.AllowsVehicleUse()) ActionResult.SUCCESS else ActionResult.FAIL
        }

        // Block is within the bounds of a protected region. Deny.
        //
        // Take care not to treat locked containers as protected here
        // so the locking code can take over from here and do the check
        // properly.
        if (IsProtectedBlockInternal(W, Pos)) return ActionResult.PASS

        // Otherwise, allow.
        return ActionResult.SUCCESS
    }

    /** Check if a player is linked. */
    abstract fun IsLinked(PE: PlayerEntity): Boolean

    /** Check if a block is a locked chest. */
    private fun _IsLockedBlock(W: World, Pos: BlockPos): Boolean {
        val BE = KeyItem.GetLockableEntity(W, Pos)
        return BE is LockableBlockEntity && BE.IsLocked()
    }

    /** Check if a pressure plate is enabled. */
    private fun _IsPressurePlateEnabled(W: World, Pos: BlockPos): Boolean {
        val R = _FindRegionContainingBlock(W, Pos) ?: return true
        return R.AllowsPressurePlates()
    }

    /** Check if a block is within a protected region. */
    private fun _IsProtectedBlock(W: World, Pos: BlockPos): Boolean {
        // If this is a locked block (container or door), treat it as protected.
        if (_IsLockedBlock(W, Pos)) return true

        // Otherwise, delegate to the region check.
        return IsProtectedBlockInternal(W, Pos)
    }

    /** Like IsProtectedBlock(), but does not check for locked chests. */
    private fun IsProtectedBlockInternal(W: World, Pos: BlockPos): Boolean {
        val R = _FindRegionContainingBlock(W, Pos) ?: return false
        return !R.AllowsBlockModification()
    }

    /**
     * Check if this entity is protected from world effects.
     *
     * This is used for explosions, lightning, potion effects, etc.
     */
    private fun _IsProtectedEntity(E: Entity): Boolean {
        val R = _FindRegionContainingBlock(E.world, E.blockPos) ?: return false
        return !R.AllowsEnvironmentalHazards()
    }

    /** Check if this entity cannot be damaged by a damage source. */
    private fun _IsProtectedEntity(E: Entity, DS: DamageSource): Boolean {
        // First, damage that cannot be guarded against (e.g. out
        // of world) is always allowed; this is so entities don’t
        // end up 10000 blocks beneath protected areas...
        //
        // Conveniently, this also means that /kill works as expected
        // for living entities.
        if (DS.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return false

        // Otherwise, use established protection rules, making sure
        // that we forward the attacker if there is one.
        val A = DS.attacker
        return if (A is PlayerEntity) !_AllowEntityAttack(A, E) else _IsProtectedEntity(E)
    }

    /** Check if the passed bounding box intersects a protected region. */
    private fun _IsProtectedRegion(W: World, MinX: Double, MinZ: Double, MaxX: Double, MaxZ: Double): Boolean {
        val Regions = RegionListFor(W)
        return Regions.any { it.Intersects(MinX, MinZ, MaxX, MaxZ) }
    }

    /** Check whether an entity is allowed to spawn here. */
    fun _IsSpawningAllowed(E: Entity): Boolean {
        // We currently only have restrictions on hostile mobs, so always allow
        // friendly entities to spawn. Note the difference between 'Monster' and
        // 'HostileEntity'; the former is what we want since it also includes e.g.
        // hoglins and ghasts whereas the latter does not.
        if (E !is Monster) return true

        // If E is a living entity, then we will have saved the spawn reason for it;
        // check that first so we always allow commands and spawn eggs to work properly.
        //
        // Note that this is a WHITELIST, i.e. the reasons here are always ALLOWED; only
        // list things here that are absolutely required for the game to function properly,
        // such as chunk loading, commands, and dimension travel.
        when ((E as? LivingEntity)?.SpawnReason) {
            // Command.
            SpawnReason.COMMAND,

            // Entity teleporting or travelling through a portal.
            SpawnReason.DIMENSION_TRAVEL,

            // Spawn eggs (and minecarts and boats, but those aren’t living entities) in
            // dispensers.
            SpawnReason.DISPENSER,

            // Loading a saved entity. This is kind of required, else entities will
            // be deleted on restarts.
            SpawnReason.LOAD,

            // Spawn eggs (and boats, but those still aren’t living entities).
            SpawnReason.SPAWN_ITEM_USE -> return true

            // Do nothing and fall through to the checks below.
            else -> {}
        }

        // Otherwise, check region flags.
        val R = _FindRegionContainingBlock(E.world, E.blockPos) ?: return true
        return R.AllowsHostileMobSpawning()
    }

    /**
     * Get the regions for a world.
     *
     * For internal use only as it returns a mutable list
     * instead of an immutable one.
     */
    protected fun RegionListFor(W: World) = RegionListFor(W.registryKey)

    /** Get the regions for a world by key. */
    protected fun RegionListFor(Key: RegistryKey<World>) = TryGetRegionList(Key)
        ?: throw IllegalArgumentException("No such world: ${Key.value}")

    /**
     * Attempt to get all region in a world.
     *
     * Prefer [GetRegions] over this if you already have a reference to the
     * world; this is meant to be used for cases where you parsed a world
     * registry key from somewhere w/o knowing whether it’s valid or not.
     *
     * @return null if the world does not exist.
     */
    fun TryGetRegions(W: RegistryKey<World>): Collection<Region>? = TryGetRegionList(W)

    /**
     * Get the region list for a world by key.
     *
     * For internal use; returns a mutable list instead of an
     * immutable one.
     */
    private fun TryGetRegionList(Key: RegistryKey<World>) = Regions[Key]

    companion object {
        /**
         * The ProtectionManager also exists on the client, so we retrieve it via the world.
         */
        fun Get(W: World) = (W as ProtectionManagerAccess).`Nguhcraft$GetProtectionManager`()

        // Static convenience wrappers for the functions above.
        @JvmStatic
        fun AllowBlockModify(PE: PlayerEntity, W: World, Pos: BlockPos) =
            Get(W)._AllowBlockModify(PE, W, Pos)

        @JvmStatic
        fun AllowEntityAttack(AttackingPlayer: PlayerEntity, AttackedEntity: Entity) =
            Get(AttackedEntity.world)._AllowEntityAttack(AttackingPlayer, AttackedEntity)

        @JvmStatic
        fun AllowEntityInteract(PE: PlayerEntity, E: Entity) =
            Get(E.world)._AllowEntityInteract(PE, E)

        @JvmStatic
        fun AllowFallDamage(PE: PlayerEntity) =
            Get(PE.world)._AllowFallDamage(PE)

        @JvmStatic
        fun AllowItemUse(PE: PlayerEntity, W: World, St: ItemStack) =
            Get(W)._AllowItemUse(PE, W, St)

        @JvmStatic
        fun AllowTeleport(TeleportingEntity: Entity, DestWorld: World, Pos: BlockPos) =
            GetTeleportResult(TeleportingEntity, DestWorld, Pos) == TeleportResult.OK

        @JvmStatic
        fun AllowTeleportToFromAnywhere(TeleportingEntity: Entity, DestWorld: World, Pos: BlockPos) =
            Get(DestWorld)._AllowTeleportToFromAnywhere(TeleportingEntity, DestWorld, Pos)

        @JvmStatic
        fun BypassesRegionProtection(PE: PlayerEntity) =
            Get(PE.world)._BypassesRegionProtection(PE)

        /** Find the region that contains a block. */
        @JvmStatic
        fun FindRegionContainingBlock(W: World, Pos: BlockPos) =
            Get(W)._FindRegionContainingBlock(W, Pos)

        @JvmStatic
        fun GetCollisionsForEntity(W: World, E: Entity, BB: Box, Consumer: Consumer<List<VoxelShape>>) =
            Get(W)._GetCollisionsForEntity(W, E, Consumer)

        @JvmStatic
        fun GetRegions(W: World) =
            Get(W).RegionListFor(W)

        @JvmStatic
        fun GetRegion(W: World, Name: String) =
            Get(W).RegionListFor(W).find { it.Name == Name }

        @JvmStatic
        fun GetTeleportResult(TeleportingEntity: Entity, DestWorld: World, Pos: BlockPos) =
            Get(DestWorld)._AllowTeleport(TeleportingEntity, DestWorld, Pos)

        @JvmStatic
        fun HandleBlockInteract(PE: PlayerEntity, W: World, Pos: BlockPos, Stack: ItemStack?) =
            Get(W)._HandleBlockInteract(PE, W, Pos, Stack)

        @JvmStatic
        fun IsPressurePlateEnabled(W: World, Pos: BlockPos) =
            Get(W)._IsPressurePlateEnabled(W, Pos)

        @JvmStatic
        fun IsProtectedBlock(W: World, Pos: BlockPos) =
            Get(W)._IsProtectedBlock(W, Pos)

        @JvmStatic
        fun IsProtectedEntity(E: Entity) =
            Get(E.world)._IsProtectedEntity(E)

        @JvmStatic
        fun IsProtectedEntity(E: Entity, DS: DamageSource) =
            Get(E.world)._IsProtectedEntity(E, DS)

        @JvmStatic
        fun IsProtectedRegion(W: World, MinX: Double, MinZ: Double, MaxX: Double, MaxZ: Double) =
            Get(W)._IsProtectedRegion(W, MinX, MinZ, MaxX, MaxZ)

        @JvmStatic
        fun IsSpawningAllowed(E: Entity) =
            Get(E.world)._IsSpawningAllowed(E)
    }
}


val MinecraftServer.ProtectionManager get() =
    Manager.Get<ProtectionManager>(this) as ServerProtectionManager