package org.nguh.nguhcraft.mixin.server;

import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceKey;
import org.nguh.nguhcraft.block.GrowingLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;
import java.util.function.Function;

@Mixin(Blocks.class)
public abstract class BlocksMixin {

    @Redirect(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Blocks;register(Ljava/lang/String;Ljava/util/function/Function;Lnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)Lnet/minecraft/world/level/block/Block;"))
    private static Block redirect$register(String id, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties settings) {
        if (Objects.equals(id, "cherry_leaves")) {
            return register(keyOf(id), GrowingLeavesBlock.Companion::CHERRY_LEAVES, settings);
        }
        if (Objects.equals(id, "oak_leaves")) {
            return register(keyOf(id), GrowingLeavesBlock.Companion::OAK_LEAVES, settings);
        }
        if (Objects.equals(id, "dark_oak_leaves")) {
            return register(keyOf(id), GrowingLeavesBlock.Companion::DARK_OAK_LEAVES, settings);
        }
        return register(keyOf(id), factory, settings);
    }

    @Invoker("register")
    private static Block register(ResourceKey<Block> key, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties settings) {
        throw new AssertionError();
    }

    @Invoker("vanillaBlockId")
    private static ResourceKey<Block> keyOf(String id) {
        throw new AssertionError();
    }
}
