package org.nguh.nguhcraft.data

import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.SlabBlock
import net.minecraft.client.data.BlockStateModelGenerator
import net.minecraft.client.data.ItemModelGenerator
import net.minecraft.component.DataComponentTypes
import net.minecraft.data.recipe.RecipeExporter
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.decoration.painting.PaintingVariant
import net.minecraft.loot.LootPool
import net.minecraft.loot.LootTable
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.CopyComponentsLootFunction
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.registry.RegistryBuilder
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.registry.tag.PaintingVariantTags
import net.minecraft.registry.tag.TagKey
import org.nguh.nguhcraft.NguhDamageTypes
import org.nguh.nguhcraft.NguhPaintings
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.block.Slab
import org.nguh.nguhcraft.block.Stairs
import org.nguh.nguhcraft.block.Wall
import org.nguh.nguhcraft.item.NguhItems
import java.util.concurrent.CompletableFuture

// =========================================================================
//  Static Registries
// =========================================================================
class NguhcraftBlockTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider.BlockTagProvider(O, RF) {
    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        getOrCreateTagBuilder(BlockTags.PICKAXE_MINEABLE).let {
            for (B in NguhBlocks.PICKAXE_MINEABLE) it.add(B)
        }

        getOrCreateTagBuilder(BlockTags.DOORS).add(NguhBlocks.LOCKED_DOOR)

        // Add blocks from families.
        val Walls = getOrCreateTagBuilder(BlockTags.WALLS)
        val Stairs = getOrCreateTagBuilder(BlockTags.STAIRS)
        val Slabs = getOrCreateTagBuilder(BlockTags.SLABS)
        for (B in NguhBlocks.STONE_VARIANT_FAMILIES) {
            B.Slab?.let { Slabs.add(it) }
            B.Stairs?.let { Stairs.add(it) }
            B.Wall?.let { Walls.add(it) }
        }
    }
}

class NguhcraftDamageTypeTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider<DamageType>(O, RegistryKeys.DAMAGE_TYPE, RF) {
    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        AddAll(DamageTypeTags.BYPASSES_ARMOR)
        AddAll(DamageTypeTags.BYPASSES_ENCHANTMENTS)
        AddAll(DamageTypeTags.BYPASSES_RESISTANCE)
        getOrCreateTagBuilder(DamageTypeTags.BYPASSES_INVULNERABILITY)
            .add(NguhDamageTypes.OBLITERATED)
        getOrCreateTagBuilder(DamageTypeTags.NO_KNOCKBACK)
            .add(NguhDamageTypes.OBLITERATED)
    }

    fun AddAll(T: TagKey<DamageType>) {
        getOrCreateTagBuilder(T).let { for (DT in NguhDamageTypes.ALL) it.add(DT) }
    }
}

class NguhcraftLootTableProvider(
    O: FabricDataOutput,
    RL: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricBlockLootTableProvider(O, RL) {
    override fun generate() {
        NguhBlocks.DROPS_SELF.forEach { addDrop(it) }
        addDrop(NguhBlocks.LOCKED_DOOR) { B: Block -> doorDrops(B) }
        for (S in NguhBlocks.STONE_VARIANT_FAMILY_BLOCKS.filter { it is SlabBlock })
            addDrop(S, ::slabDrops)

        // Copied from nameableContainerDrops(), but modified to also
        // copy the chest variant component.
        addDrop(Blocks.CHEST) { B -> LootTable.builder()
            .pool(addSurvivesExplosionCondition(
                B,
                LootPool.builder().rolls(ConstantLootNumberProvider.create(1.0F))
                    .with(ItemEntry.builder(B)
                        .apply(CopyComponentsLootFunction.builder(CopyComponentsLootFunction.Source.BLOCK_ENTITY)
                            .include(DataComponentTypes.CUSTOM_NAME)
                            .include(NguhBlocks.CHEST_VARIANT_COMPONENT)
                        )
                    )
                )
            )
        }
    }
}

class NguhcraftModelGenerator(O: FabricDataOutput) : FabricModelProvider(O) {
    override fun generateBlockStateModels(G: BlockStateModelGenerator) {
        NguhBlocks.BootstrapModels(G)
    }

    override fun generateItemModels(G: ItemModelGenerator) {
        NguhItems.BootstrapModels(G)
    }
}

class NguhcraftPaintingVariantTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider<PaintingVariant>(O, RegistryKeys.PAINTING_VARIANT, RF) {
    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        getOrCreateTagBuilder(PaintingVariantTags.PLACEABLE).let { for (P in NguhPaintings.PLACEABLE) it.add(P) }
    }
}

class NguhcraftRecipeProvider(
    O: FabricDataOutput,
    RL: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricRecipeProvider(O, RL) {
    override fun getRecipeGenerator(
        WL: RegistryWrapper.WrapperLookup,
        E: RecipeExporter
    ) = NguhcraftRecipeGenerator(WL, E)
    override fun getName() = "Nguhcraft Recipe Provider"
}

// =========================================================================
//  Dynamic Registries
// =========================================================================
class NguhcraftDynamicRegistryProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricDynamicRegistryProvider(O, RF) {
    override fun configure(
        WL: RegistryWrapper.WrapperLookup,
        E: Entries
    ) {
        E.addAll(WL.getOrThrow(RegistryKeys.DAMAGE_TYPE))
        E.addAll(WL.getOrThrow(RegistryKeys.PAINTING_VARIANT))
        E.addAll(WL.getOrThrow(RegistryKeys.TRIM_PATTERN))
    }

    override fun getName() = "Nguhcraft Dynamic Registries"
}

class NguhcraftDataGenerator : DataGeneratorEntrypoint {
    override fun buildRegistry(RB: RegistryBuilder) {
        RB.addRegistry(RegistryKeys.DAMAGE_TYPE, NguhDamageTypes::Bootstrap)
        RB.addRegistry(RegistryKeys.PAINTING_VARIANT, NguhPaintings::Bootstrap)
        RB.addRegistry(RegistryKeys.TRIM_PATTERN, NguhItems::BootstrapArmourTrims)
    }

    override fun onInitializeDataGenerator(FDG: FabricDataGenerator) {
        val P = FDG.createPack()
        P.addProvider(::NguhcraftBlockTagProvider)
        P.addProvider(::NguhcraftDamageTypeTagProvider)
        P.addProvider(::NguhcraftDynamicRegistryProvider)
        P.addProvider(::NguhcraftLootTableProvider)
        P.addProvider(::NguhcraftModelGenerator)
        P.addProvider(::NguhcraftPaintingVariantTagProvider)
        P.addProvider(::NguhcraftRecipeProvider)
    }
}