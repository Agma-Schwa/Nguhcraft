package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(AbstractMinecart.class)
public interface AbstractMinecartAccessor {
    @Invoker("comeOffTrack")
    void Nguhcraft$MoveOffRail(ServerLevel SW);
}
