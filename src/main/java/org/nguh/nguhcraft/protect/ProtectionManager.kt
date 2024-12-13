package org.nguh.nguhcraft.protect

import net.minecraft.block.Blocks
import net.minecraft.block.LecternBlock
import net.minecraft.entity.Entity
import net.minecraft.entity.damage.DamageSource
import net.minecraft.entity.mob.Monster
import net.minecraft.entity.passive.VillagerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.vehicle.VehicleEntity
import net.minecraft.inventory.ContainerLock
import net.minecraft.item.BoatItem
import net.minecraft.item.ItemStack
import net.minecraft.item.MinecartItem
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.util.ActionResult
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import org.nguh.nguhcraft.block.LockableBlockEntity
import org.nguh.nguhcraft.isa
import org.nguh.nguhcraft.item.KeyItem

/**
 * Handler that contains common code paths related to world protection.
 *
 * This API generally provides three families of functions:
 *
 * - ‘AllowX’, which check whether a *player* is allowed to perform an action.
 * - ‘IsX’, which check whether an action is allowed in the absence of a player.
 * - ‘HandleX’, which does the above but may also modify an action to do something else instead.
 *
 * The most important of these are:
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
abstract class ProtectionManager(
    /** Regions that are currently in each dimension. */
    val OverworldRegions: Collection<Region>,
    val NetherRegions: Collection<Region>,
    val EndRegions: Collection<Region>
) {
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
            is VehicleEntity -> R.AllowsVehicleUse()
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

    /** Check if a player bypasses region protection. */
    abstract fun _BypassesRegionProtection(PE: PlayerEntity): Boolean

    /** Find the region that contains a block. */
    private fun _FindRegionContainingBlock(W: World, Pos: BlockPos) =
        RegionListFor(W).find { it.Contains(Pos) }

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

    /**
     * Check whether a position can be teleported to.
     *
     * This should only be used for ‘natural’ events, e.g. ender pearls,
     * not commands. If you don’t want people to use commands to teleport
     * somewhere they shouldn’t be, don’t give them access to those commands.
     */
    private fun _IsLegalTeleportTarget(W: World, Pos: BlockPos): Boolean {
        val R = _FindRegionContainingBlock(W, Pos) ?: return true
        return R.AllowsTeleportation()
    }

    /** Check if a player is linked. */
    abstract fun IsLinked(PE: PlayerEntity): Boolean

    /** Check if a block is a locked chest. */
    private fun _IsLockedBlock(W: World, Pos: BlockPos): Boolean {
        val BE = KeyItem.GetLockableEntity(W, Pos)
        return BE is LockableBlockEntity && BE.lock != ContainerLock.EMPTY
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
    private fun _IsProtectedRegion(W: World, MinX: Int, MinZ: Int, MaxX: Int, MaxZ: Int): Boolean {
        val Regions = RegionListFor(W)
        return Regions.any { it.Intersects(MinX, MinZ, MaxX, MaxZ) }
    }

    /** Check whether an entity is allowed to spawn here. */
    fun _IsSpawningAllowed(E: Entity): Boolean {
        if (E !is Monster) return true
        val R = _FindRegionContainingBlock(E.world, E.blockPos) ?: return true
        return R.AllowsHostileMobSpawning()
    }

    /** Check if an item stack is a vehicle. */
    private fun IsVehicle(St: ItemStack?) = St != null && (St.item is MinecartItem || St.item is BoatItem)

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
     * Attempt to get a region in a world.
     *
     * Prefer [GetRegion] over this if you already have a reference to the
     * world; this is meant to be used for cases where you parsed a world
     * registry key from somewhere w/o knowing whether it’s valid or not.
     *
     * @return null if the world or region does not exist.
     */
    fun TryGetRegion(W: RegistryKey<World>, Name: String): Region? = TryGetRegionList(W)?.find { it.Name == Name }

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
    private fun TryGetRegionList(Key: RegistryKey<World>) = when (Key) {
        World.OVERWORLD -> OverworldRegions
        World.NETHER -> NetherRegions
        World.END -> EndRegions
        else -> null
    }

    /** Dump a string representation of the manager state. */
    override fun toString(): String {
        var S = "ProtectionManager {\n"
        for (R in OverworldRegions) S += "  Overworld: $R\n"
        for (R in NetherRegions) S += "  Nether: $R\n"
        for (R in EndRegions) S += "  End: $R\n"
        S += "}"
        return S
    }

    companion object {
        /**
         * Get the manager instance.
         *
         * The manager is a singleton instead of a namespace to prevent
         * protection state from leaking between sessions on the client.
         *
         * Note: The manager is *not* world-specific. Rather, the world
         * is simply a convenient place to put the accessor for it since
         * it is both present on the client and server and also passed in
         * one way or another to every single API call of the manager.
         *
         * The server-side manager is stored in the server instance, and
         * the client-side manger in the client network handler.
         */
        fun Get(W: World): ProtectionManager = (W as ProtectionManagerAccessor).`Nguhcraft$GetProtectionManager`()

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
        fun BypassesRegionProtection(PE: PlayerEntity) =
            Get(PE.world)._BypassesRegionProtection(PE)

        /** Find the region that contains a block. */
        @JvmStatic
        fun FindRegionContainingBlock(W: World, Pos: BlockPos) =
            Get(W)._FindRegionContainingBlock(W, Pos)

        @JvmStatic
        fun GetRegions(W: World) =
            Get(W).RegionListFor(W)

        @JvmStatic
        fun GetRegion(W: World, Name: String) =
            Get(W).RegionListFor(W).find { it.Name == Name }

        @JvmStatic
        fun HandleBlockInteract(PE: PlayerEntity, W: World, Pos: BlockPos, Stack: ItemStack?) =
            Get(W)._HandleBlockInteract(PE, W, Pos, Stack)

        @JvmStatic
        fun IsLegalTeleportTarget(W: World, Pos: BlockPos) =
            Get(W)._IsLegalTeleportTarget(W, Pos)

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
        fun IsProtectedRegion(W: World, MinX: Int, MinZ: Int, MaxX: Int, MaxZ: Int) =
            Get(W)._IsProtectedRegion(W, MinX, MinZ, MaxX, MaxZ)

        @JvmStatic
        fun IsSpawningAllowed(E: Entity) =
            Get(E.world)._IsSpawningAllowed(E)
    }
}
