package org.nguh.nguhcraft.mixin.common;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.component.ComponentMap;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;
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
public abstract class ChestBlockEntityMixin extends LootableContainerBlockEntity implements ChestBlockEntityAccessor {
    protected ChestBlockEntityMixin(BlockEntityType<?> blockEntityType, BlockPos blockPos, BlockState blockState) {
        super(blockEntityType, blockPos, blockState);
    }

    @Unique private static final String TAG_CHEST_VARIANT = "NguhcraftChestVariant";

    @Nullable @Unique private ChestVariant Variant = null;
    @Override public @Nullable ChestVariant Nguhcraft$GetChestVariant() { return Variant; }

    @Override
    protected void addComponents(ComponentMap.Builder B) {
        super.addComponents(B);
        if (Variant != null) B.add(NguhBlocks.CHEST_VARIANT_COMPONENT, Variant);
    }

    @Override
    protected void readComponents(ComponentsAccess CA) {
        super.readComponents(CA);
        Variant = CA.get(NguhBlocks.CHEST_VARIANT_COMPONENT);
    }

    @Inject(method = "readNbt", at = @At("TAIL"))
    public void inject$readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        if (nbt.contains(TAG_CHEST_VARIANT, NbtElement.STRING_TYPE))
            Variant = ChestVariant.valueOf(nbt.getString(TAG_CHEST_VARIANT).toUpperCase(Locale.ROOT));
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
        var Tag = super.toInitialChunkDataNbt(registries);
        if (Variant != null) Tag.putString(TAG_CHEST_VARIANT, Variant.asString());
        return Tag;
    }

    @Inject(method = "writeNbt", at = @At("TAIL"))
    public void inject$writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries, CallbackInfo ci) {
        if (Variant != null) nbt.putString(TAG_CHEST_VARIANT, Variant.asString());
    }
}
