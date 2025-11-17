package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.NewMinecartBehavior;
import net.minecraft.world.entity.vehicle.MinecartBehavior;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.GameRules;
import org.nguh.nguhcraft.entity.MinecartUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NewMinecartBehavior.class)
public abstract class NewMinecartBehaviorMixin extends MinecartBehavior {
    NewMinecartBehaviorMixin(AbstractMinecart minecart) { super(minecart); }

    @Unique private static final double POWERED_RAIL_BOOST = 0.2;
    @Unique private static final int DEFAULT_SPEED_PER_SEC = 8;

    /** Increase powered rail acceleration. */
    @ModifyConstant(method = "calculateBoostTrackSpeed", constant = @Constant(doubleValue = 0.06))
    private double inject$accelerateFromPoweredRail(double value) {
        return POWERED_RAIL_BOOST;
    }

    /** Increase initial velocity. */
    @Redirect(
        method = "calculatePlayerInputSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/Vec3;scale(D)Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private Vec3 inject$applyInitialVelocity(Vec3 Vec, double value) {
        return Vec.scale(.01);
    }

    /** Disable underwater slowdown on slopes. */
    @Redirect(
        method = "calculateSlopeSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;isInWater()Z"
        )
    )
    private boolean inject$applySlopeVelocity(AbstractMinecart M) { return false; }

    /** Accelerate on every step. */
    @Redirect(
        method = "calculateTrackSpeed",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/world/entity/vehicle/NewMinecartBehavior$TrackIteration;hasBoosted:Z",
            ordinal = 0
        )
    )
    private boolean inject$calcNewHorizontalVelocity(NewMinecartBehavior.TrackIteration instance) { return false; }

    /**
    * Stop immediately when we hit an unpowered rail.
    *
    * @reason Complete replacement.
    * @author Sirraide
    */
    @Overwrite
    private Vec3 calculateHaltTrackSpeed(Vec3 V, BlockState S) {
        return S.is(Blocks.POWERED_RAIL) && !S.getValue(PoweredRailBlock.POWERED)
            ? Vec3.ZERO
            : V;
    }

    /** Disable max speed reduction underwater. */
    @Redirect(
        method = "getMaxSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;isInWater()Z"
        )
    )
    private boolean inject$getMaxSpeed$0(AbstractMinecart M) { return false; }

    /** Reset max speed to 8 for minecarts that aren’t ridden by a player. */
    @Redirect(
        method = "getMaxSpeed",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/GameRules;getInt(Lnet/minecraft/world/level/GameRules$Key;)I"
        )
    )
    private int inject$getMaxSpeed$1(GameRules I, GameRules.Key<GameRules.IntegerValue> R) {
        // Note ‘getControllingPassenger()’ is only valid for e.g. boats where the
        // player is actually in control; this is not the case for minecarts, so use
        // ‘getFirstPassenger()’ instead.
        return minecart.getFirstPassenger() instanceof Player
            ? I.getInt(R)
            : DEFAULT_SPEED_PER_SEC;
    }

    /**
     * Implement Minecart collisions.
     */
    @Inject(
        method = "pushAndPickupEntities",
        at = @At("HEAD"),
        cancellable = true
    )
    private void inject$handleCollision(CallbackInfoReturnable<Boolean> CIR) {
        // Return 'false' in here to prevent the minecart from inverting
        // its movement direction.
        if (MinecartUtils.HandleCollisions(this.minecart))
            CIR.setReturnValue(false);
    }
}
