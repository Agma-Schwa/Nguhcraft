package org.nguh.nguhcraft.data

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
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
import net.minecraft.loot.condition.BlockStatePropertyLootCondition
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.CopyComponentsLootFunction
import net.minecraft.loot.function.SetCountLootFunction
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.predicate.StatePredicate
import net.minecraft.registry.RegistryBuilder
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.registry.tag.PaintingVariantTags
import net.minecraft.registry.tag.TagKey
import org.nguh.nguhcraft.NguhDamageTypes
import org.nguh.nguhcraft.NguhPaintings
import org.nguh.nguhcraft.block.Fence
import org.nguh.nguhcraft.block.NguhBlockModels
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.block.Slab
import org.nguh.nguhcraft.block.Stairs
import org.nguh.nguhcraft.block.VerticalSlabBlock
import org.nguh.nguhcraft.block.Wall
import org.nguh.nguhcraft.item.NguhItems
import java.util.concurrent.CompletableFuture

// =========================================================================
//  Static Registries
// =========================================================================
@Environment(EnvType.CLIENT)
class NguhcraftBlockTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider.BlockTagProvider(O, RF) {
    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        valueLookupBuilder(BlockTags.PICKAXE_MINEABLE).let { T ->
            for (B in NguhBlocks.PICKAXE_MINEABLE) T.add(B)
            for (B in NguhBlockModels.VERTICAL_SLABS.filter { !it.Wood }) T.add(B.VerticalSlab)
        }

        // Block tags for miscellaneous custom blocks.
        valueLookupBuilder(BlockTags.PLANKS).add(NguhBlocks.TINTED_OAK_PLANKS)
        valueLookupBuilder(BlockTags.DOORS).add(NguhBlocks.LOCKED_DOOR)
        valueLookupBuilder(BlockTags.WOODEN_SLABS)
            .add(NguhBlocks.TINTED_OAK_SLAB)
            .let {
                for (B in NguhBlockModels.VERTICAL_SLABS.filter { it.Wood })
                    it.add(B.VerticalSlab)
            }

        valueLookupBuilder(BlockTags.WOODEN_STAIRS).add(NguhBlocks.TINTED_OAK_STAIRS)
        valueLookupBuilder(BlockTags.WOODEN_FENCES).add(NguhBlocks.TINTED_OAK_FENCE)

        // Block tag for bonemealing flowers.
        valueLookupBuilder(NguhBlocks.CAN_DUPLICATE_WITH_BONEMEAL)
            .add(Blocks.DANDELION)
            .add(Blocks.POPPY)
            .add(Blocks.BLUE_ORCHID)
            .add(Blocks.ALLIUM)
            .add(Blocks.AZURE_BLUET)
            .add(Blocks.RED_TULIP)
            .add(Blocks.ORANGE_TULIP)
            .add(Blocks.WHITE_TULIP)
            .add(Blocks.PINK_TULIP)
            .add(Blocks.OXEYE_DAISY)
            .add(Blocks.CORNFLOWER)
            .add(Blocks.LILY_OF_THE_VALLEY)

        // Add blocks from families.
        val Fences = valueLookupBuilder(BlockTags.FENCES)
        val Walls = valueLookupBuilder(BlockTags.WALLS)
        val Stairs = valueLookupBuilder(BlockTags.STAIRS)
        val Slabs = valueLookupBuilder(BlockTags.SLABS)
        for (B in NguhBlocks.ALL_VARIANT_FAMILIES) {
            B.Fence?.let { Fences.add(it) }
            B.Slab?.let { Slabs.add(it) }
            B.Stairs?.let { Stairs.add(it) }
            B.Wall?.let { Walls.add(it) }
        }

        for (V in NguhBlockModels.VERTICAL_SLABS)
            Slabs.add(V.VerticalSlab)
    }
}

@Environment(EnvType.CLIENT)
class NguhcraftDamageTypeTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider<DamageType>(O, RegistryKeys.DAMAGE_TYPE, RF) {
    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        AddAll(DamageTypeTags.BYPASSES_ARMOR)
        AddAll(DamageTypeTags.BYPASSES_ENCHANTMENTS)
        AddAll(DamageTypeTags.BYPASSES_RESISTANCE)
        builder(DamageTypeTags.BYPASSES_INVULNERABILITY)
            .add(NguhDamageTypes.OBLITERATED)
        builder(DamageTypeTags.NO_KNOCKBACK)
            .add(NguhDamageTypes.OBLITERATED)
    }

    fun AddAll(T: TagKey<DamageType>) {
        builder(T).let { for (DT in NguhDamageTypes.ALL) it.add(DT) }
    }
}

@Environment(EnvType.CLIENT)
class NguhcraftLootTableProvider(
    O: FabricDataOutput,
    RL: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricBlockLootTableProvider(O, RL) {
    override fun generate() {
        NguhBlocks.DROPS_SELF.forEach { addDrop(it) }
        addDrop(NguhBlocks.LOCKED_DOOR) { B: Block -> doorDrops(B) }
        for (S in NguhBlocks.ALL_VARIANT_FAMILY_BLOCKS.filter { it is SlabBlock })
            addDrop(S, ::slabDrops)
        for (V in NguhBlockModels.VERTICAL_SLABS)
            addDrop(V.VerticalSlab, ::VerticalSlabDrops)

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

    fun VerticalSlabDrops(Drop: Block) = LootTable.builder().pool(
        LootPool.builder()
            .rolls(ConstantLootNumberProvider.create(1.0F))
            .with(
                applyExplosionDecay(
                    Drop,
                    ItemEntry.builder(Drop)
                        .apply(
                            SetCountLootFunction.builder(ConstantLootNumberProvider.create(2.0F))
                                .conditionally(BlockStatePropertyLootCondition.builder(Drop)
                                    .properties(StatePredicate.Builder.create().exactMatch(
                                        VerticalSlabBlock.TYPE,
                                        VerticalSlabBlock.Type.DOUBLE))
                                )
                        )
                )
            )
    )
}

@Environment(EnvType.CLIENT)
class NguhcraftModelGenerator(O: FabricDataOutput) : FabricModelProvider(O) {
    override fun generateBlockStateModels(G: BlockStateModelGenerator) {
        NguhBlockModels.BootstrapModels(G)
    }

    override fun generateItemModels(G: ItemModelGenerator) {
        NguhItems.BootstrapModels(G)
    }
}

@Environment(EnvType.CLIENT)
class NguhcraftPaintingVariantTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider<PaintingVariant>(O, RegistryKeys.PAINTING_VARIANT, RF) {
    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        builder(PaintingVariantTags.PLACEABLE).let { for (P in NguhPaintings.PLACEABLE) it.add(P) }
    }
}

@Environment(EnvType.CLIENT)
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