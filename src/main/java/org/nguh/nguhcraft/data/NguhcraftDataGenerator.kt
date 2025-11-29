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
import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.ItemModelGenerators
import net.minecraft.client.resources.model.EquipmentClientInfo
import net.minecraft.core.HolderLookup
import net.minecraft.core.RegistrySetBuilder
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.data.CachedOutput
import net.minecraft.data.DataProvider
import net.minecraft.data.PackOutput
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.*
import net.minecraft.world.damagesource.DamageType
import net.minecraft.world.entity.decoration.PaintingVariant
import net.minecraft.world.item.Items
import net.minecraft.world.item.equipment.EquipmentAsset
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.SlabBlock
import net.minecraft.world.level.storage.loot.LootPool
import net.minecraft.world.level.storage.loot.LootTable
import net.minecraft.world.level.storage.loot.entries.LootItem
import net.minecraft.world.level.storage.loot.functions.CopyComponentsFunction
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator
import org.nguh.nguhcraft.NguhDamageTypes
import org.nguh.nguhcraft.NguhPaintings
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.*
import org.nguh.nguhcraft.item.NguhItems
import java.util.concurrent.CompletableFuture

// =========================================================================
//  Static Registries
// =========================================================================
@Environment(EnvType.CLIENT)
class NguhcraftBlockTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<HolderLookup.Provider>
) : FabricTagProvider.BlockTagProvider(O, RF) {
    override fun addTags(WL: HolderLookup.Provider) {
        valueLookupBuilder(BlockTags.MINEABLE_WITH_PICKAXE).let { T ->
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

        // Block tags for crops.
        valueLookupBuilder(BlockTags.CROPS).add(NguhBlocks.GRAPE_CROP).add(NguhBlocks.PEANUT_CROP)
        valueLookupBuilder(BlockTags.MAINTAINS_FARMLAND).add(NguhBlocks.GRAPE_CROP).add(NguhBlocks.PEANUT_CROP)

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
    RF: CompletableFuture<HolderLookup.Provider>
) : FabricTagProvider<DamageType>(O, Registries.DAMAGE_TYPE, RF) {
    override fun addTags(WL: HolderLookup.Provider) {
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
    RF: CompletableFuture<HolderLookup.Provider>
) : DataProvider {
    val Resolver = O.createPathProvider(PackOutput.Target.RESOURCE_PACK, "equipment")

    private fun Bootstrap(Add: (ResourceKey<EquipmentAsset>, EquipmentClientInfo) -> Unit) {
        Add(
            NguhItems.AMETHYST_EQUIPMENT_ASSET_KEY,
            EquipmentClientInfo.builder().addHumanoidLayers(Id("amethyst")).build()
        )
    }

    override fun run(W: CachedOutput): CompletableFuture<*> {
        val Map = mutableMapOf<ResourceKey<EquipmentAsset>, EquipmentClientInfo>()
        Bootstrap { K, M -> if (Map.putIfAbsent(K, M) != null) throw IllegalStateException("Duplicate key: $K") }
        return DataProvider.saveAll(W, EquipmentClientInfo.CODEC, Resolver::json, Map)
    }

    override fun getName() = "Nguhcraft Equipment Asset Definitions"
}

class NguhcraftItemTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<HolderLookup.Provider>
) : FabricTagProvider.ItemTagProvider(O, RF) {
    override fun addTags(WL: HolderLookup.Provider) {
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

        valueLookupBuilder(NguhItems.TINTED_LOGS)
            .add(NguhBlocks.TINTED_OAK_LOG.asItem())
            .add(NguhBlocks.TINTED_OAK_WOOD.asItem())
            .add(NguhBlocks.STRIPPED_TINTED_OAK_LOG.asItem())
            .add(NguhBlocks.STRIPPED_TINTED_OAK_WOOD.asItem())

        valueLookupBuilder(ItemTags.CHICKEN_FOOD).add(NguhItems.GRAPE_SEEDS)
        valueLookupBuilder(ItemTags.FOX_FOOD).add(NguhItems.GRAPES)
        valueLookupBuilder(ItemTags.PARROT_FOOD).add(NguhItems.GRAPE_SEEDS).add(NguhItems.PEANUTS)
    }
}

@Environment(EnvType.CLIENT)
class NguhcraftLootTableProvider(
    O: FabricDataOutput,
    RL: CompletableFuture<HolderLookup.Provider>
) : FabricBlockLootTableProvider(O, RL) {
    override fun generate() {
        NguhBlocks.DROPS_SELF.forEach { dropSelf(it) }
        add(NguhBlocks.LOCKED_DOOR) { B: Block -> createDoorTable(B) }
        add(NguhBlocks.TINTED_OAK_DOOR) { B: Block -> createDoorTable(B) }
        for (S in NguhBlocks.ALL_VARIANT_FAMILY_BLOCKS.filter { it is SlabBlock })
            add(S, ::createSlabItemTable)
        for (V in NguhBlockModels.VERTICAL_SLABS)
            add(V.VerticalSlab, ::VerticalSlabDrops)

        val GrapeCropHasMaxAge = LootItemBlockStatePropertyCondition
            .hasBlockStateProperties(NguhBlocks.GRAPE_CROP)
            .setProperties(StatePropertiesPredicate.Builder.properties()
                .hasProperty(GrapeCropBlock.AGE, GrapeCropBlock.MAX_AGE)
            )

        // Crop drops.
        add(NguhBlocks.GRAPE_CROP, createCropDrops(
            NguhBlocks.GRAPE_CROP,
            NguhItems.GRAPES,
            NguhItems.GRAPE_SEEDS,
            GrapeCropHasMaxAge
        ).withPool(LootPool.lootPool().setRolls(
            UniformGenerator.between(0.0F, 1.0F)).add(
            LootItem.lootTableItem(NguhItems.GRAPE_LEAF))
            .`when`(GrapeCropHasMaxAge)
        ).withPool(LootPool.lootPool().setRolls(
            ConstantValue.exactly(1.0F)).add(
            LootItem.lootTableItem(Items.STICK))
            .`when`(LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(NguhBlocks.GRAPE_CROP)
                .setProperties(StatePropertiesPredicate.Builder.properties()
                    .hasProperty(GrapeCropBlock.STICK_LOGGED, true)
                )
            )
        ))

        add(NguhBlocks.PEANUT_CROP, createCropDrops(
            NguhBlocks.PEANUT_CROP,
            NguhItems.PEANUTS,
            NguhItems.PEANUTS,
            LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(NguhBlocks.PEANUT_CROP)
                .setProperties(StatePropertiesPredicate.Builder.properties()
                    .hasProperty(CropBlock.AGE, CropBlock.MAX_AGE)
                )
        ))

        // Copied from nameableContainerDrops(), but modified to also
        // copy the chest variant component.
        add(Blocks.CHEST) { B -> LootTable.lootTable()
            .withPool(applyExplosionCondition(
                B,
                LootPool.lootPool().setRolls(ConstantValue.exactly(1.0F))
                    .add(LootItem.lootTableItem(B)
                        .apply(CopyComponentsFunction.copyComponents(CopyComponentsFunction.Source.BLOCK_ENTITY)
                            .include(DataComponents.CUSTOM_NAME)
                            .include(NguhBlocks.CHEST_VARIANT_COMPONENT)
                        )
                    )
                )
            )
        }
    }

    fun VerticalSlabDrops(Drop: Block) = LootTable.lootTable().withPool(
        LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .add(
                applyExplosionDecay(
                    Drop,
                    LootItem.lootTableItem(Drop)
                        .apply(
                            SetItemCountFunction.setCount(ConstantValue.exactly(2.0F))
                                .`when`(LootItemBlockStatePropertyCondition.hasBlockStateProperties(Drop)
                                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(
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
    override fun generateBlockStateModels(G: BlockModelGenerators) {
        NguhBlockModels.BootstrapModels(G)
    }

    override fun generateItemModels(G: ItemModelGenerators) {
        NguhItems.BootstrapModels(G)
    }
}

@Environment(EnvType.CLIENT)
class NguhcraftPaintingVariantTagProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<HolderLookup.Provider>
) : FabricTagProvider<PaintingVariant>(O, Registries.PAINTING_VARIANT, RF) {
    override fun addTags(WL: HolderLookup.Provider) {
        builder(PaintingVariantTags.PLACEABLE).let { for (P in NguhPaintings.PLACEABLE) it.add(P) }
    }
}

@Environment(EnvType.CLIENT)
class NguhcraftRecipeProvider(
    O: FabricDataOutput,
    RL: CompletableFuture<HolderLookup.Provider>
) : FabricRecipeProvider(O, RL) {
    override fun createRecipeProvider(
        WL: HolderLookup.Provider,
        E: RecipeOutput
    ) = NguhcraftRecipeGenerator(WL, E)
    override fun getName() = "Nguhcraft Recipe Provider"
}

// =========================================================================
//  Dynamic Registries
// =========================================================================
class NguhcraftDynamicRegistryProvider(
    O: FabricDataOutput,
    RF: CompletableFuture<HolderLookup.Provider>
) : FabricDynamicRegistryProvider(O, RF) {
    override fun configure(
        WL: HolderLookup.Provider,
        E: Entries
    ) {
        E.addAll(WL.lookupOrThrow(Registries.DAMAGE_TYPE))
        E.addAll(WL.lookupOrThrow(Registries.PAINTING_VARIANT))
        E.addAll(WL.lookupOrThrow(Registries.TRIM_PATTERN))
    }

    override fun getName() = "Nguhcraft Dynamic Registries"
}

class NguhcraftDataGenerator : DataGeneratorEntrypoint {
    override fun buildRegistry(RB: RegistrySetBuilder) {
        RB.add(Registries.DAMAGE_TYPE, NguhDamageTypes::Bootstrap)
        RB.add(Registries.PAINTING_VARIANT, NguhPaintings::Bootstrap)
        RB.add(Registries.TRIM_PATTERN, NguhItems::BootstrapArmourTrims)
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
