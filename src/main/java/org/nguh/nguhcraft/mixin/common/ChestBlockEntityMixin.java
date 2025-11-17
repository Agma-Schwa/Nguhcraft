package org.nguh.nguhcraft.mixin.common;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.nguh.nguhcraft.accessors.ChestBlockEntityAccessor;
import org.nguh.nguhcraft.block.ChestVariant;
import org.nguh.nguhcraft.block.NguhBlocks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Mixin(ChestBlockEntity.class)
public abstract class ChestBlockEntityMixin extends RandomizableContainerBlockEntity implements ChestBlockEntityAccessor {
    protected ChestBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Unique private static final String TAG_CHEST_VARIANT = "NguhcraftChestVariant";

    @Nullable @Unique private ChestVariant Variant = null;
    @Override public @Nullable ChestVariant Nguhcraft$GetChestVariant() { return Variant; }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder B) {
        super.collectImplicitComponents(B);
        if (Variant != null) B.set(NguhBlocks.CHEST_VARIANT_COMPONENT, Variant);
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter CA) {
        super.applyImplicitComponents(CA);
        Variant = CA.get(NguhBlocks.CHEST_VARIANT_COMPONENT);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    public void inject$readData(ValueInput RV, CallbackInfo CI) {
        RV.getString(TAG_CHEST_VARIANT).ifPresent(
            S -> Variant = ChestVariant.valueOf(S.toUpperCase(Locale.ROOT))
        );
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var Tag = super.getUpdateTag(registries);
        if (Variant != null) Tag.putString(TAG_CHEST_VARIANT, Variant.getSerializedName());
        return Tag;
    }

    @Inject(method = "saveAdditional", at = @At("TAIL"))
    public void inject$writeData(ValueOutput WV, CallbackInfo CI) {
        if (Variant != null) WV.putString(TAG_CHEST_VARIANT, Variant.getSerializedName());
    }
}
