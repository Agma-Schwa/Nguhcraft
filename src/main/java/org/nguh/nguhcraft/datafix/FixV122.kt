package org.nguh.nguhcraft.datafix

import com.mojang.datafixers.schemas.Schema
import com.mojang.serialization.Dynamic
import net.minecraft.util.datafix.fixes.DataComponentRemainderFix

/**
 * This data fixer moves container lock components of key
 * and lock items to be our new custom lock components.
 *
 * Runs before 'LockComponentPredicateFix()', at the start
 * of schema 4068.
 */
class KeyLockItemComponentisationFix(S: Schema) : DataComponentRemainderFix(
    S,
    "NguhcraftKeyLockItemComponentisationFix",
    "minecraft:lock",
    "nguhcraft:key"
) {
    /**
     * The renaming is handled by the ComponentFix
     * implementation, so just return the value
     * unchanged here.
     */
    override fun <T> fixComponent(D: Dynamic<T>) = D
}