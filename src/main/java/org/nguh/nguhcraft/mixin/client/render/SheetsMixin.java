package org.nguh.nguhcraft.mixin.client.render;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.resources.model.Material;
import org.nguh.nguhcraft.accessors.ChestBlockEntityAccessor;
import org.nguh.nguhcraft.block.ChestTextureOverride;
import org.nguh.nguhcraft.item.KeyItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Sheets.class)
public abstract class SheetsMixin {
    /** Render a lock on locked chests and handle chest variants. */
    @Inject(
        method = "chooseMaterial(Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/level/block/state/properties/ChestType;Z)Lnet/minecraft/client/resources/model/Material;",
        at = @At("HEAD"),
        cancellable = true
    )
    private static void inject$getChestTextureId(
        BlockEntity BE,
        ChestType Ty,
        boolean Christmas,
        CallbackInfoReturnable<Material> CIR
    ) {
        if (BE instanceof ChestBlockEntity CBE) {
            var CV = ((ChestBlockEntityAccessor)CBE).Nguhcraft$GetChestVariant();
            var Locked = KeyItem.IsChestLocked(BE);
            CIR.setReturnValue(ChestTextureOverride.GetTexture(CV, Ty, Locked));
        }
    }
}
