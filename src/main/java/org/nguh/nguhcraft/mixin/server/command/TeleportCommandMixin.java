package org.nguh.nguhcraft.mixin.server.command;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.server.commands.LookAt;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.TeleportCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

import static org.nguh.nguhcraft.server.ExtensionsKt.SavePositionBeforeTeleport;

@Mixin(TeleportCommand.class)
public abstract class TeleportCommandMixin {
    /** Save last position before teleporting. */
    @Inject(
        method = "performTeleport",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z"
        )
    )
    private static void inject$teleport(CommandSourceStack source, Entity target, ServerLevel world, double x, double y, double z, Set<Relative> movementFlags, float yaw, float pitch, @Nullable LookAt facingLocation, CallbackInfo ci) {
        if (target instanceof ServerPlayer SP)
            SavePositionBeforeTeleport(SP);
    }
}
