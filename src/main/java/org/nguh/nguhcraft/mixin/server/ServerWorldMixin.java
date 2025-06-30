package org.nguh.nguhcraft.mixin.server;

import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.TreeToChop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin extends World implements TreeToChop.Accessor {
    protected ServerWorldMixin(MutableWorldProperties properties, RegistryKey<World> registryRef, DynamicRegistryManager registryManager, RegistryEntry<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
        super(properties, registryRef, registryManager, dimensionEntry, isClient, debugWorld, seed, maxChainedNeighborUpdates);
    }

    /** Trees we’re currently chopping in this world. */
    @Unique private final List<TreeToChop> Trees = new ArrayList<>();

    /** Register a tree to be chopped. */
    @Override public void Nguhcraft$StartChoppingTree(TreeToChop Tree) { Trees.add(Tree); }

    /** Get the list of trees to be chopped. */
    @Override public @NotNull List<@NotNull TreeToChop> Nguhcraft$GetTrees() {
        return Trees;
    }
}
