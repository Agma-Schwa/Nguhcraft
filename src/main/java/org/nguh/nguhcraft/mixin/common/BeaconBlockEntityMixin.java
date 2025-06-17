package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    @Inject(method = "applyPlayerEffects", at = @At("TAIL"))
    static private void inject$applyPlayerEffects(
        World W,
        BlockPos Pos,
        int BeaconLevel,
        @Nullable RegistryEntry<StatusEffect> Primary,
        @Nullable RegistryEntry<StatusEffect> Secondary,
        CallbackInfo CI
    ) { ServerUtils.ApplyBeaconEffectsToVillagers(W, Pos, BeaconLevel, Primary, Secondary); }
}
