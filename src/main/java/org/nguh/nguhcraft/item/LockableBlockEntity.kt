package org.nguh.nguhcraft.item

import com.mojang.logging.LogUtils
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.sounds.SoundSource
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.ChatFormatting
import org.nguh.nguhcraft.protect.ProtectionManager
import kotlin.jvm.optionals.getOrNull

interface LockableBlockEntity {
    /** Get the name of this entity (the ‘X’ in ‘X is locked’). */
    fun `Nguhcraft$GetName`(): Component

    /** Get the current lock.  */
    fun `Nguhcraft$GetLock`(): String?

    /**
     * Set the lock.
     *
     *
     * This only updates the field. Everything else must
     * be done externally. The only place this should be
     * called is in UpdateLock()!
     */
    fun `Nguhcraft$SetLockInternal`(NewLock: String?)
}

/**
 * Check if this entity can be opened by the specified item.
 *
 * Default implementations seem to interact badly w/ mixins, so we implement
 * this as an extension function instead.
 *
 * If a player is passed, this also displays an error message to the player
 * if they can’t open the lock and also checks whether they bypass region
 * protection entirely.
 *
 * @param PE The player attempting to open the lock.
 * @param Key The stack used for opening.
 * @return Whether the lock can be opened.
 */
fun LockableBlockEntity.CheckCanOpen(PE: Player?, Key: ItemStack): Boolean {
    fun CanOpenImpl(St: ItemStack, Lock: String): Boolean {
        fun CheckKey(St: ItemStack, Lock: String): Boolean {
            if (!St.`is`(NguhItems.KEY)) return false
            return St.get(KeyItem.COMPONENT) == Lock
        }

        fun CheckKeyChain(St: ItemStack, Lock: String) = St.get(DataComponents.BUNDLE_CONTENTS)
            ?.items()
            ?.any { CheckKey(it, Lock) } == true

        if (St.`is`(NguhItems.MASTER_KEY)) return true
        if (St.`is`(NguhItems.KEY_CHAIN)) return CheckKeyChain(St, Lock)
        return CheckKey(St, Lock)
    }

    if (!IsLocked()) return true
    if (PE != null && ProtectionManager.BypassesRegionProtection(PE)) return true
    if (CanOpenImpl(Key, `Nguhcraft$GetLock`()!!)) return true
    PE?.displayClientMessage(FormatLockedMessage(`Nguhcraft$GetLock`()!!, `Nguhcraft$GetName`()), true)
    if (PE == null || !PE.level().isClientSide) PE?.playNotifySound(
        SoundEvents.CHEST_LOCKED,
        SoundSource.BLOCKS,
        1.0f,
        1.0f
    )
    return false
}

object LockDeserialisation {
    val LOGGER = LogUtils.getLogger()
}

/** Extract a container lock from saved data. */
fun DeserialiseLock(RV: ValueInput, PreferredKey: String = "Lock"): String? {
    // I’m done dealing with stupid data fixer nonsense to try and
    // rename this field properly, so we’re doing this the dumb way.
    //
    // This field was first called 'Lock' and was a string; it was
    // then called 'lock' and was item predicate until Mojang in
    // their infinite wisdom dispensed with item sub-predicates
    // entirely. Since then it has been renamed back to 'Lock' and
    // is once again a string; deal with this accordingly.
    //
    // The NBT structure of the old-style lock is:
    //
    //    lock: {
    //      predicates: {
    //        "nguhcraft:lock_predicate": <KEY>
    //      }
    //    }
    //
    // Do not use SetLock() here as that will crash during loading.
    return RV.getString(PreferredKey).or {
        val LegacyLock = RV.childOrEmpty("lock")
            .childOrEmpty("predicates")
            .getString("nguhcraft:lock_predicate")

        // Report whether we managed to extract the lock if there was one.
        if (RV.contains("lock")) {
            LegacyLock.ifPresentOrElse({
                LockDeserialisation.LOGGER.info("Successfully parsed legacy lock '{}'. Disregard warning below.", it)
            }) {
                LockDeserialisation.LOGGER.error("Failed to parse legacy lock")
            }
        }

        LegacyLock
    }.getOrNull()
}

/** Format the message that indicates why a container is locked. */
private fun FormatLockedMessage(Lock: String, BlockName: Component): MutableComponent = Component.translatable(
    "nguhcraft.block.locked",
    BlockName,
    Component.literal(Lock).withStyle(ChatFormatting.LIGHT_PURPLE)
)


/** Check whether this is locked. */
fun LockableBlockEntity.IsLocked() = `Nguhcraft$GetLock`() != null

