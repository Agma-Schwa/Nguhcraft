package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.server.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BeaconBlockEntity.class)
public abstract class BeaconBlockEntityMixin {
    @Inject(method = "applyEffects", at = @At("TAIL"))
    static private void inject$applyPlayerEffects(
        Level W,
        BlockPos Pos,
        int BeaconLevel,
        @Nullable Holder<MobEffect> Primary,
        @Nullable Holder<MobEffect> Secondary,
        CallbackInfo CI
    ) { ServerUtils.ApplyBeaconEffectsToVillagers(W, Pos, BeaconLevel, Primary, Secondary); }
}
