package org.nguh.nguhcraft.data

import net.fabricmc.api.EnvType
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.client.data.BlockStateModelGenerator
import net.minecraft.client.data.ItemModelGenerator
import net.minecraft.component.DataComponentTypes
import net.minecraft.data.recipe.ComplexRecipeJsonBuilder
import net.minecraft.data.recipe.RecipeExporter
import net.minecraft.data.recipe.RecipeGenerator
import net.minecraft.data.recipe.ShapedRecipeJsonBuilder
import net.minecraft.entity.damage.DamageType
import net.minecraft.entity.decoration.painting.PaintingVariant
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.Items
import net.minecraft.loot.LootPool
import net.minecraft.loot.LootTable
import net.minecraft.loot.entry.ItemEntry
import net.minecraft.loot.function.CopyComponentsLootFunction
import net.minecraft.loot.provider.number.ConstantLootNumberProvider
import net.minecraft.recipe.book.RecipeCategory
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
import org.nguh.nguhcraft.block.ChestVariant
import org.nguh.nguhcraft.block.NguhBlocks
import org.nguh.nguhcraft.item.KeyDuplicationRecipe
import org.nguh.nguhcraft.item.KeyLockPairingRecipe
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

class NguhcraftRecipeGenerator(
    val WL: RegistryWrapper.WrapperLookup,
    val E: RecipeExporter
) : RecipeGenerator(WL, E) {
    val Lookup = WL.getOrThrow(RegistryKeys.ITEM)

    override fun generate() {
        // Armour trims.
        NguhItems.SMITHING_TEMPLATES.forEach { offerSmithingTrimRecipe(
            it,
            RegistryKey.of(RegistryKeys.RECIPE, Id("${getItemPath(it)}_smithing"))
        ) }

        offerSmithingTemplateCopyingRecipe(NguhItems.ATLANTIC_ARMOUR_TRIM, Items.NAUTILUS_SHELL)
        offerSmithingTemplateCopyingRecipe(NguhItems.CENRAIL_ARMOUR_TRIM, ingredientFromTag(ItemTags.IRON_ORES))
        offerSmithingTemplateCopyingRecipe(NguhItems.ICE_COLD_ARMOUR_TRIM, Items.SNOW_BLOCK)
        offerSmithingTemplateCopyingRecipe(NguhItems.VENEFICIUM_ARMOUR_TRIM, Items.SLIME_BALL)

        // Slablet crafting.
        for ((Lesser, Greater) in SLABLETS) {
            offerShapelessRecipe(Lesser, Greater, "slablets", 2)
            offerShapelessRecipe(Greater, 1, Lesser to 2)
        }

        // Modded items.
        offerShaped(NguhBlocks.DECORATIVE_HOPPER) {
            pattern("i i")
            pattern("i i")
            pattern(" i ")
            cinput('i', Items.IRON_INGOT)
        }

        offerShaped(NguhItems.KEY) {
            pattern("g ")
            pattern("gr")
            pattern("gr")
            cinput('g', Items.GOLD_INGOT)
            cinput('r', Items.REDSTONE)
        }

        offerShaped(NguhItems.LOCK, 3) {
            pattern(" i ")
            pattern("i i")
            pattern("iri")
            cinput('i', Items.IRON_INGOT)
            cinput('r', Items.REDSTONE)
        }

        offerShaped(NguhBlocks.LOCKED_DOOR, 3) {
            pattern("##")
            pattern("##")
            pattern("##")
            cinput('#', Items.GOLD_INGOT)
        }

        offerShaped(NguhBlocks.PEARLESCENT_CHAIN) {
            pattern("N")
            pattern("A")
            pattern("N")
            cinput('A', Items.AMETHYST_SHARD)
            cinput('N', Items.IRON_NUGGET)
        }

        offerShaped(NguhBlocks.PEARLESCENT_LANTERN) {
            pattern("NAN")
            pattern("A#A")
            pattern("NNN")
            cinput('A', Items.AMETHYST_SHARD)
            cinput('N', Items.IRON_NUGGET)
            cinput('#', Items.PEARLESCENT_FROGLIGHT)
        }

        // Special recipes.
        ComplexRecipeJsonBuilder.create(::KeyLockPairingRecipe).offerTo(E, "key_lock_pairing")
        ComplexRecipeJsonBuilder.create(::KeyDuplicationRecipe).offerTo(E, "key_duplication")

        // Miscellaneous.
        offerShapelessRecipe(Items.STRING, 4, ItemTags.WOOL to 1)
        offerShapelessRecipe(Items.HOPPER, 1, NguhBlocks.DECORATIVE_HOPPER to 1, Items.CHEST to 1)
        offerShapelessRecipe(NguhBlocks.DECORATIVE_HOPPER, 1, Items.HOPPER to 1)
    }

    // Combines a call to input() and criterion() because having to specify the latter
    // all the time is just really stupid.
    fun ShapedRecipeJsonBuilder.cinput(C: Char, I: ItemConvertible): ShapedRecipeJsonBuilder {
        input(C, I)
        criterion("has_${getItemPath(I)}", conditionsFromItem(I))
        return this
    }

    inline fun offerShaped(
        Output: ItemConvertible,
        Count: Int = 1,
        Name: String = getItemPath(Output),
        Consumer: ShapedRecipeJsonBuilder.() -> Unit,
    ) {
        val B = createShaped(RecipeCategory.MISC, Output, Count)
        B.Consumer()
        B.offerTo(E, Name)
    }

    // offerShapelessRecipe() sucks, so this is a better version.
    inline fun <reified T> offerShapelessRecipe(Output: ItemConvertible, Count: Int, vararg Inputs: Pair<T, Int>) {
        val B = createShapeless(RecipeCategory.MISC, Output, Count)
        for ((I, C) in Inputs) when (I) {
            is ItemConvertible -> B.input(I, C).criterion("has_${getItemPath(I)}", conditionsFromItem(I))
            is TagKey<*> -> B.input(ingredientFromTag(I as TagKey<Item>), C).criterion("has_${I.id.path}", conditionsFromTag(I))
            else -> throw IllegalArgumentException("Invalid input type: ${I::class.simpleName}")
        }

        B.offerTo(E, "${getItemPath(Output)}_from_${Inputs.joinToString("_and_") { 
            (I, _) -> when (I) {
                is ItemConvertible -> getItemPath(I)
                is TagKey<*> -> I.id.path
                else -> throw IllegalArgumentException("Invalid input type: ${I::class.simpleName}")
            }
        }}")
    }

    companion object {
        private val SLABLETS = arrayOf(
            NguhItems.SLABLET_1 to NguhItems.SLABLET_2,
            NguhItems.SLABLET_2 to NguhItems.SLABLET_4,
            NguhItems.SLABLET_4 to NguhItems.SLABLET_8,
            NguhItems.SLABLET_8 to NguhItems.SLABLET_16,
            NguhItems.SLABLET_16 to Items.PETRIFIED_OAK_SLAB,
        )
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