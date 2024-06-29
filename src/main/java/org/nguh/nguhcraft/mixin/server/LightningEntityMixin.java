package org.nguh.nguhcraft.mixin.server;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.nguh.nguhcraft.protect.ProtectionManager;
import org.nguh.nguhcraft.server.accessors.LightningEntityAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightningEntity.class)
public abstract class LightningEntityMixin extends Entity implements LightningEntityAccessor {
    public LightningEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    /**
    * Whether this was created by Channeling, in which case
    * we do not want to spawn fire, ever.
    */
    @Unique public boolean CreatedByChanneling = false;
    @Override public void Nguhcraft$SetCreatedByChanneling() { CreatedByChanneling = true; }

    @Unique private static boolean IntersectsProtectedRegion(World W, BlockPos Pos) {
        // We check within 5 blocks around the lightning strike
        // to make sure we don’t accidentally spread over into
        // a protected region even if the lightning strike itself
        // isn’t directly in one.
        var X = Pos.getX();
        var Z = Pos.getZ();
        return ProtectionManager.GetIntersectingRegion(
            W,
            X - 5,
            Z - 5,
            X + 5,
            Z + 5
        ) != null;
    }

    @Unique private boolean IntersectsProtectedRegion() {
        return IntersectsProtectedRegion(getWorld(), getBlockPos());
    }

    /** Do not deoxidise blocks in protected regions. */
    @Inject(method = "cleanOxidation", at = @At("HEAD"), cancellable = true)
    private static void inject$cleanOxidation(World W, BlockPos Pos, CallbackInfo CI) {
        if (IntersectsProtectedRegion(W, Pos)) CI.cancel();
    }

    /** Do not spawn fire in protected regions. */
    @Inject(method = "spawnFire", at = @At("HEAD"), cancellable = true)
    private void inject$spawnFire(int Attempts, CallbackInfo CI) {
        if (CreatedByChanneling || IntersectsProtectedRegion()) CI.cancel();
    }

    /** Do not strike entities in protected regions. */
    @ModifyExpressionValue(
        method = "tick",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/entity/LightningEntity;cosmetic:Z",
            ordinal = 0
        )
    )
    private boolean inject$tick(boolean Value) {
        // Mark this as cosmetic to prevent entities from being struck.
        if (IntersectsProtectedRegion()) return true;
        return Value;
    }
}
