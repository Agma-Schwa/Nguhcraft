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
import net.minecraft.client.render.entity.equipment.EquipmentModel
import net.minecraft.component.DataComponentTypes
import net.minecraft.data.DataOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.DataWriter
import net.minecraft.data.recipe.RecipeExporter
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.decoration.painting.PaintingVariant
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.item.equipment.EquipmentAsset
import net.minecraft.loot.LootPool
import net.minecraft.loot.LootTable
import net.minecraft.loot.condition.BlockStatePropertyLootCondition
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.CopyComponentsLootFunction
import net.minecraft.loot.function.SetCountLootFunction
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.predicate.StatePredicate
import net.minecraft.registry.RegistryBuilder
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.BlockTags
import net.minecraft.registry.tag.DamageTypeTags
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.PaintingVariantTags
import net.minecraft.registry.tag.TagKey
import org.nguh.nguhcraft.NguhDamageTypes
import org.nguh.nguhcraft.NguhPaintings
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
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

        valueLookupBuilder(BlockTags.WOOL).let {
            for (B in NguhBlocks.ALL_BROCADE_BLOCKS) it.add(B)
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
        valueLookupBuilder(BlockTags.LOGS_THAT_BURN)
            .add(NguhBlocks.TINTED_OAK_LOG)
            .add(NguhBlocks.TINTED_OAK_WOOD)
            .add(NguhBlocks.STRIPPED_TINTED_OAK_LOG)
            .add(NguhBlocks.STRIPPED_TINTED_OAK_WOOD)
        valueLookupBuilder(BlockTags.FENCE_GATES).add(NguhBlocks.TINTED_OAK_FENCE_GATE)
        valueLookupBuilder(BlockTags.WOODEN_DOORS).add(NguhBlocks.TINTED_OAK_DOOR)
        valueLookupBuilder(BlockTags.WOODEN_TRAPDOORS).add(NguhBlocks.TINTED_OAK_TRAPDOOR)
        valueLookupBuilder(BlockTags.WOODEN_PRESSURE_PLATES).add(NguhBlocks.TINTED_OAK_PRESSURE_PLATE)
        valueLookupBuilder(BlockTags.WOODEN_BUTTONS).add(NguhBlocks.TINTED_OAK_BUTTON)

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
        // Damage types that bypass most resistances.
        AddBypassDamageTypesTo(DamageTypeTags.BYPASSES_ARMOR)
        AddBypassDamageTypesTo(DamageTypeTags.BYPASSES_ENCHANTMENTS)
        AddBypassDamageTypesTo(DamageTypeTags.BYPASSES_RESISTANCE)

        // The 'obliterated' damage bypasses additional resistances.
        builder(DamageTypeTags.BYPASSES_INVULNERABILITY).add(NguhDamageTypes.OBLITERATED)
        builder(DamageTypeTags.NO_KNOCKBACK).add(NguhDamageTypes.OBLITERATED)

        // The 'stonecutter' damage type behaves a bit like 'hot_floor'
        builder(DamageTypeTags.BYPASSES_SHIELD).add(NguhDamageTypes.STONECUTTER)
        builder(DamageTypeTags.NO_KNOCKBACK).add(NguhDamageTypes.STONECUTTER)

        // The 'arcane' damage type acts like magic damage.
        builder(DamageTypeTags.IS_PLAYER_ATTACK).add(NguhDamageTypes.ARCANE)
        builder(DamageTypeTags.BYPASSES_ARMOR).add(NguhDamageTypes.ARCANE)
        builder(DamageTypeTags.AVOIDS_GUARDIAN_THORNS).add(NguhDamageTypes.ARCANE)
        builder(DamageTypeTags.ALWAYS_TRIGGERS_SILVERFISH).add(NguhDamageTypes.ARCANE)
        builder(DamageTypeTags.NO_KNOCKBACK).add(NguhDamageTypes.ARCANE)
        builder(DamageTypeTags.BYPASSES_WOLF_ARMOR).add(NguhDamageTypes.ARCANE)
    }

    fun AddBypassDamageTypesTo(T: TagKey<DamageType>) {
        builder(T).let { for (DT in NguhDamageTypes.BYPASSES_RESISTANCES) it.add(DT) }
    }
}

class NguhcraftEquipmentAssetProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : DataProvider {
    val Resolver = O.getResolver(DataOutput.OutputType.RESOURCE_PACK, "equipment")

    private fun Bootstrap(Add: (RegistryKey<EquipmentAsset>, EquipmentModel) -> Unit) {
        Add(
            NguhItems.AMETHYST_EQUIPMENT_ASSET_KEY,
            EquipmentModel.builder().addHumanoidLayers(Id("amethyst")).build()
        )
    }

    override fun run(W: DataWriter): CompletableFuture<*> {
        val Map = mutableMapOf<RegistryKey<EquipmentAsset>, EquipmentModel>()
        Bootstrap { K, M -> if (Map.putIfAbsent(K, M) != null) throw IllegalStateException("Duplicate key: $K") }
        return DataProvider.writeAllToPath(W, EquipmentModel.CODEC, Resolver::resolveJson, Map)
    }

    override fun getName() = "Nguhcraft Equipment Asset Definitions"
}

class NguhcraftItemTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<RegistryWrapper.WrapperLookup>
) : FabricTagProvider.ItemTagProvider(O, RF) {
    companion object {
        val TINTED_LOGS: TagKey<Item?>? = TagKey.of(RegistryKeys.ITEM, Id("tinted_logs"))
    }

    override fun configure(WL: RegistryWrapper.WrapperLookup) {
        valueLookupBuilder(ItemTags.HEAD_ARMOR).add(NguhItems.AMETHYST_HELMET)
        valueLookupBuilder(ItemTags.CHEST_ARMOR).add(NguhItems.AMETHYST_CHESTPLATE)
        valueLookupBuilder(ItemTags.LEG_ARMOR).add(NguhItems.AMETHYST_LEGGINGS)
        valueLookupBuilder(ItemTags.FOOT_ARMOR).add(NguhItems.AMETHYST_BOOTS)
        valueLookupBuilder(ItemTags.TRIMMABLE_ARMOR)
            .add(NguhItems.AMETHYST_HELMET)
            .add(NguhItems.AMETHYST_CHESTPLATE)
            .add(NguhItems.AMETHYST_LEGGINGS)
            .add(NguhItems.AMETHYST_BOOTS)

        valueLookupBuilder(NguhItems.REPAIRS_AMETHYST_ARMOUR)
            .add(Items.AMETHYST_CLUSTER)

        valueLookupBuilder(ItemTags.SWORDS).add(NguhItems.AMETHYST_SWORD)
        valueLookupBuilder(ItemTags.SHOVELS).add(NguhItems.AMETHYST_SHOVEL)
        valueLookupBuilder(ItemTags.PICKAXES).add(NguhItems.AMETHYST_PICKAXE)
        valueLookupBuilder(ItemTags.AXES).add(NguhItems.AMETHYST_AXE)
        valueLookupBuilder(ItemTags.HOES).add(NguhItems.AMETHYST_HOE)

        valueLookupBuilder(ItemTags.LOGS_THAT_BURN)
            .add(NguhBlocks.TINTED_OAK_LOG.asItem())
            .add(NguhBlocks.TINTED_OAK_WOOD.asItem())
            .add(NguhBlocks.STRIPPED_TINTED_OAK_LOG.asItem())
            .add(NguhBlocks.STRIPPED_TINTED_OAK_WOOD.asItem())

        valueLookupBuilder(ItemTags.PLANKS)
            .add(NguhBlocks.TINTED_OAK_PLANKS.asItem())

        valueLookupBuilder(TINTED_LOGS)
            .add(NguhBlocks.TINTED_OAK_LOG.asItem())
            .add(NguhBlocks.TINTED_OAK_WOOD.asItem())
            .add(NguhBlocks.STRIPPED_TINTED_OAK_LOG.asItem())
            .add(NguhBlocks.STRIPPED_TINTED_OAK_WOOD.asItem())
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
        addDrop(NguhBlocks.TINTED_OAK_DOOR) { B: Block -> doorDrops(B) }
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
        P.addProvider(::NguhcraftEquipmentAssetProvider)
        P.addProvider(::NguhcraftItemTagProvider)
        P.addProvider(::NguhcraftLootTableProvider)
        P.addProvider(::NguhcraftModelGenerator)
        P.addProvider(::NguhcraftPaintingVariantTagProvider)
        P.addProvider(::NguhcraftRecipeProvider)
    }
}