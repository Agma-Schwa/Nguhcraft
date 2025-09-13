package org.nguh.nguhcraft.mixin.entity;

import net.minecraft.entity.mob.GhastEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.NamedCodec;
import org.nguh.nguhcraft.entity.GhastModeAccessor;
import org.nguh.nguhcraft.entity.MachineGunGhastMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.nguh.nguhcraft.SerialisationKt.Read;
import static org.nguh.nguhcraft.SerialisationKt.Write;

@Mixin(GhastEntity.class)
public abstract class GhastEntityMixin implements GhastModeAccessor {
    @Unique private static final NamedCodec<MachineGunGhastMode> MODE_CODEC
        = new NamedCodec<>("NguhcraftEventsGhastMode", MachineGunGhastMode.CODEC);

    @Unique MachineGunGhastMode Mode = MachineGunGhastMode.NORMAL;

    @Override public @NotNull MachineGunGhastMode Nguhcraft$GetGhastMode() { return Mode; }
    @Override public void Nguhcraft$SetGhastMode(@NotNull MachineGunGhastMode mode) { Mode = mode; }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void inject$readCustomData(ReadView RV, CallbackInfo CI) {
        Mode = Read(RV, MODE_CODEC).orElse(MachineGunGhastMode.NORMAL);
    }

    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void inject$writeCustomData(WriteView WV, CallbackInfo CI) {
        Write(WV, MODE_CODEC, Mode);
    }
}