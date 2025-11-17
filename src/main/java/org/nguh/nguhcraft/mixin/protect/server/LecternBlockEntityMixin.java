package org.nguh.nguhcraft.mixin.protect.server;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.core.BlockPos;
import org.nguh.nguhcraft.server.accessors.LecternScreenHandlerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntity {
    public LecternBlockEntityMixin(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Store the position of the lectern block in the screen handler. */
    @ModifyReturnValue(method = "createMenu", at = @At("RETURN"))
    private AbstractContainerMenu inject$createMenu(AbstractContainerMenu Original) {
        ((LecternScreenHandlerAccessor)Original).Nguhcraft$SetLecternPos(getBlockPos());
        return Original;
    }
}
