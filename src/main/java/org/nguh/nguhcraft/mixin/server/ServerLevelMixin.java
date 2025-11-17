package org.nguh.nguhcraft.mixin.server;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.WritableLevelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.nguh.nguhcraft.server.TreeToChop;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin extends Level implements TreeToChop.Accessor {
    protected ServerLevelMixin(WritableLevelData properties, ResourceKey<Level> registryRef, RegistryAccess registryManager, Holder<DimensionType> dimensionEntry, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates) {
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
