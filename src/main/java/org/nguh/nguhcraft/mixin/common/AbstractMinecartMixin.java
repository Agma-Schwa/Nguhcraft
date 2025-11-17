package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractBoat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(AbstractMinecart.class)
public abstract class AbstractMinecartMixin extends VehicleEntity {
    public AbstractMinecartMixin(EntityType<?> entityType, Level world) {
        super(entityType, world);
    }

    /**
     * If this minecart is being ridden by a player, prevent collisions
     * with anything that is not a player or minecart.
     *
     * @reason Complete replacement.
     * @author Sirraide
     */
    @Overwrite
    public boolean canCollideWith(Entity E) {
        if (!(getFirstPassenger() instanceof Player))
            return AbstractBoat.canVehicleCollide(this, E);
        return false;
    }
}
