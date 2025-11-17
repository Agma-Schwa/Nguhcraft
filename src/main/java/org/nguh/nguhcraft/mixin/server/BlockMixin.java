package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments;
import org.nguh.nguhcraft.server.ServerUtils;
import org.nguh.nguhcraft.server.TreeToChop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.nguh.nguhcraft.Utils.EnchantLvl;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(
        method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)V",
        cancellable = true,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            ordinal = 0
        )
    )
    private static void inject$dropResources(
        BlockState State,
        Level World,
        BlockPos Pos,
        BlockEntity BE,
        Entity E,
        ItemStack T,
        CallbackInfo CI
    ) {
        if (!(World instanceof ServerLevel SW)) return;

        // Try to smelt the block if the tool has smelting.
        var Smelting = EnchantLvl(World, T, NguhcraftEnchantments.SMELTING);
        ServerUtils.SmeltingResult SR = null;
        if (Smelting != 0) SR = ServerUtils.TrySmeltBlock(SW, State);
        if (SR != null) {
            Block.popResource(SW, Pos, SR.getStack());
            ExperienceOrb.award(SW, Vec3.atCenterOf(Pos), SR.getExperience());
            CI.cancel();
        }
    }

    /** Hook into tree chopping code. */
    @Inject(
        method = "playerWillDestroy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/level/block/state/BlockState;",
        at = @At("TAIL")
    )
    private void inject$onBreak(
        Level W,
        BlockPos Pos,
        BlockState State,
        Player PE,
        CallbackInfoReturnable<BlockState> CIR
    ) {
        TreeToChop.ActOnBlockDestroyed(W, Pos, W.getBlockState(Pos), PE);
    }
}
