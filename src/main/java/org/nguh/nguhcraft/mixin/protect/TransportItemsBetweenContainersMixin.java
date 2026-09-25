package org.nguh.nguhcraft.mixin.protect;

import net.minecraft.world.entity.ai.behavior.TransportItemsBetweenContainers;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(TransportItemsBetweenContainers.class)
public abstract class TransportItemsBetweenContainersMixin {
    /**
     * Redirect lock check to use our custom locks.
     * @author Sirraide
     * @reason See above.
     */
    @Overwrite
    private boolean isContainerLocked(final TransportItemsBetweenContainers.TransportItemTarget Target) {
        var L = Target.blockEntity().getLevel();
        if (L == null) return false;
        return ProtectionManager.IsProtectedBlock(L, Target.pos());
    }
}
