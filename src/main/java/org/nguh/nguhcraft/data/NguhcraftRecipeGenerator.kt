package org.nguh.nguhcraft.data

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.data.family.BlockFamily
import net.minecraft.data.recipe.ComplexRecipeJsonBuilder
import net.minecraft.data.recipe.RecipeExporter
import net.minecraft.data.recipe.RecipeGenerator
import net.minecraft.data.recipe.ShapedRecipeJsonBuilder
import net.minecraft.item.Item
import net.minecraft.item.ItemConvertible
import net.minecraft.item.Items
import net.minecraft.recipe.book.RecipeCategory
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.RegistryWrapper
import net.minecraft.registry.tag.ItemTags
import net.minecraft.registry.tag.TagKey
import org.nguh.nguhcraft.Nguhcraft
import org.nguh.nguhcraft.block.*
import org.nguh.nguhcraft.item.KeyDuplicationRecipe
import org.nguh.nguhcraft.item.KeyLockPairingRecipe
import org.nguh.nguhcraft.item.NguhItems

@Environment(EnvType.CLIENT)
class NguhcraftRecipeGenerator(
    val WL: RegistryWrapper.WrapperLookup,
    val E: RecipeExporter
) : RecipeGenerator(WL, E) {
    val Lookup = WL.getOrThrow(RegistryKeys.ITEM)

    override fun generate() {
        // =========================================================================
        //  Armour Trims
        // =========================================================================
        NguhItems.SMITHING_TEMPLATES.forEach { offerSmithingTrimRecipe(
            it,
            RegistryKey.of(RegistryKeys.RECIPE, Nguhcraft.Companion.Id("${getItemPath(it)}_smithing"))
        ) }

        offerSmithingTemplateCopyingRecipe(NguhItems.ATLANTIC_ARMOUR_TRIM, Items.NAUTILUS_SHELL)
        offerSmithingTemplateCopyingRecipe(NguhItems.CENRAIL_ARMOUR_TRIM, ingredientFromTag(ItemTags.IRON_ORES))
        offerSmithingTemplateCopyingRecipe(NguhItems.ICE_COLD_ARMOUR_TRIM, Items.SNOW_BLOCK)
        offerSmithingTemplateCopyingRecipe(NguhItems.VENEFICIUM_ARMOUR_TRIM, Items.SLIME_BALL)

        // =========================================================================
        //  Slabs and Slablets
        // =========================================================================
        for ((Lesser, Greater) in SLABLETS) {
            offerShapelessRecipe(Lesser, Greater, "slablets", 2)
            offerShapelessRecipe(Greater, 1, Lesser to 2)
        }

        // =========================================================================
        //  Items
        // =========================================================================
        offerShaped(NguhItems.KEY) {
            pattern("g ")
            pattern("gr")
            pattern("gr")
            cinput('g', Items.GOLD_INGOT)
            cinput('r', Items.REDSTONE)
        }

        offerShaped(NguhItems.KEY_CHAIN) {
            pattern(" i ")
            pattern("i i")
            pattern(" i ")
            cinput('i', Items.IRON_NUGGET)
        }

        offerShaped(NguhItems.LOCK, 3) {
            pattern(" i ")
            pattern("i i")
            pattern("iri")
            cinput('i', Items.IRON_INGOT)
            cinput('r', Items.REDSTONE)
        }

        offerShapelessRecipe(Items.STRING, 4, ItemTags.WOOL to 1)

        // =========================================================================
        //  Miscellaneous Blocks
        // =========================================================================
        offerShaped(NguhBlocks.DECORATIVE_HOPPER) {
            pattern("i i")
            pattern("i i")
            pattern(" i ")
            cinput('i', Items.IRON_INGOT)
        }

        offerShaped(NguhBlocks.LOCKED_DOOR, 3) {
            pattern("##")
            pattern("##")
            pattern("##")
            cinput('#', Items.GOLD_INGOT)
        }

        offerShaped(NguhBlocks.WROUGHT_IRON_BLOCK, 4) {
            pattern("###")
            pattern("# #")
            pattern("###")
            cinput('#', Items.IRON_INGOT)
        }

        offerShaped(NguhBlocks.WROUGHT_IRON_BARS, 64) {
            pattern("###")
            pattern("###")
            cinput('#', NguhBlocks.WROUGHT_IRON_BLOCK)
        }

        offerShaped(NguhBlocks.GOLD_BARS, 16) {
            pattern("###")
            pattern("###")
            cinput('#', Items.GOLD_INGOT)
        }

        offerShaped(NguhBlocks.COMPRESSED_STONE, 4) {
            pattern("###")
            pattern("# #")
            pattern("###")
            cinput('#', Items.SMOOTH_STONE)
        }

        offerShaped(NguhBlocks.PYRITE, 2) {
            pattern("GI")
            pattern("IG")
            cinput('G', Items.GLOWSTONE_DUST)
            cinput('I', Items.IRON_NUGGET)
        }

        offerShaped(NguhBlocks.PYRITE_BRICKS, 4) {
            pattern("##")
            pattern("##")
            cinput('#', NguhBlocks.PYRITE)
        }

        offerChainAndLantern(NguhBlocks.OCHRE_CHAIN, NguhBlocks.OCHRE_LANTERN, Items.RESIN_CLUMP, Items.OCHRE_FROGLIGHT)
        offerChainAndLantern(NguhBlocks.PEARLESCENT_CHAIN, NguhBlocks.PEARLESCENT_LANTERN, Items.AMETHYST_SHARD, Items.PEARLESCENT_FROGLIGHT)
        offerChainAndLantern(NguhBlocks.VERDANT_CHAIN, NguhBlocks.VERDANT_LANTERN, Items.EMERALD, Items.VERDANT_FROGLIGHT)

        offerShapelessRecipe(Items.HOPPER, 1, NguhBlocks.DECORATIVE_HOPPER to 1, Items.CHEST to 1)
        offerShapelessRecipe(NguhBlocks.DECORATIVE_HOPPER, 1, Items.HOPPER to 1)

        // =========================================================================
        //  Block Families
        // =========================================================================
        for ((Base, Gilded) in listOf(
            Blocks.CALCITE to NguhBlocks.GILDED_CALCITE,
            NguhBlocks.POLISHED_CALCITE to NguhBlocks.GILDED_POLISHED_CALCITE,
            NguhBlocks.CALCITE_BRICKS to NguhBlocks.GILDED_CALCITE_BRICKS,
            NguhBlocks.CHISELED_CALCITE to NguhBlocks.GILDED_CHISELED_CALCITE,
            NguhBlocks.CHISELED_CALCITE_BRICKS to NguhBlocks.GILDED_CHISELED_CALCITE_BRICKS
        )) offerShaped(Gilded, 2, "from_${Registries.BLOCK.getKey(Base).get().value.path.lowercase()}_and_gold_ingot") {
            pattern("GC")
            pattern("CG")
            cinput('C', Base)
            cinput('G', Items.GOLD_INGOT)
        }

        offerShaped(NguhBlocks.POLISHED_CALCITE, 4) {
            pattern("##")
            pattern("##")
            cinput('#', Items.CALCITE)
        }

        offerShaped(NguhBlocks.TINTED_OAK_PLANKS, 4) {
            pattern(" # ")
            pattern("#A#")
            pattern(" # ")
            cinput('#', Items.PALE_OAK_PLANKS)
            cinput('A', Items.AMETHYST_SHARD)
        }

        // Usual crafting recipes for custom stone types.
        for (F in NguhBlocks.ALL_VARIANT_FAMILIES) {
            F.Fence?.let {
                offerShaped(it, 6) {
                    pattern("###")
                    pattern("###")
                    cinput('#', F.baseBlock)
                }
            }

            F.Slab?.let {
                offerShaped(it, 3) {
                    pattern("###")
                    cinput('#', F.baseBlock)

                    F.Chiseled?.let {
                        offerShaped(it, 4) {
                            pattern("#")
                            pattern("#")
                            cinput('#', F.Slab!!)
                        }
                    }
                }
            }

            F.Stairs?.let {
                offerShaped(it, 4) {
                    pattern("#  ")
                    pattern("## ")
                    pattern("###")
                    cinput('#', F.baseBlock)
                }
            }


            F.Polished?.let {
                offerShaped(it, 4) {
                    pattern("##")
                    pattern("##")
                    cinput('#', F.baseBlock)
                }
            }

            F.Wall?.let {
                offerShaped(it, 6) {
                    pattern("###")
                    pattern("###")
                    cinput('#', F.baseBlock)
                }
            }
        }

        offerShaped(Items.TUFF, 2, "from_blackstone_and_quartz") {
            pattern("BQ")
            pattern("QB")
            cinput('B', Items.BLACKSTONE)
            cinput('Q', Items.QUARTZ)
        }

        offerShapelessRecipe(NguhBlocks.CINNABAR, 2, Items.NETHERRACK to 1, Items.COBBLESTONE to 1)

        // =========================================================================
        //  Vertical Slabs
        // =========================================================================
        for (V in NguhBlockModels.VERTICAL_SLABS) offerShaped(V.VerticalSlab, 3) {
            pattern("#")
            pattern("#")
            pattern("#")
            cinput('#', V.Base)
        }

        // =========================================================================
        //  Stone Cutting
        // =========================================================================
        for (F in NguhBlocks.STONE_VARIANT_FAMILIES) offerStonecuttingFamily(F)
        for (F in NguhBlocks.STONE_FAMILY_GROUPS) offerRelatedStonecuttingFamilies(F)
        offerStonecuttingFamily(NguhBlocks.POLISHED_CALCITE_FAMILY, Blocks.CALCITE)
        offerStonecuttingFamily(NguhBlocks.CALCITE_BRICK_FAMILY, Blocks.CALCITE)
        offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, NguhBlocks.PYRITE_BRICKS, NguhBlocks.PYRITE)

        for (V in NguhBlockModels.VERTICAL_SLABS.filter { !it.Wood })
            offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, V.VerticalSlab, V.Base)

        // =========================================================================
        //  Smelting
        // =========================================================================
        offerSmelting(Items.BONE_BLOCK, Items.CALCITE)

        // =========================================================================
        //  Special Recipes
        // =========================================================================
        ComplexRecipeJsonBuilder.create(::KeyLockPairingRecipe).offerTo(E, "key_lock_pairing")
        ComplexRecipeJsonBuilder.create(::KeyDuplicationRecipe).offerTo(E, "key_duplication")
    }

    /** Add stonecutting recipes for a family. */
    fun offerStonecuttingFamily(F: BlockFamily, Base: Block = F.baseBlock) {
        /// Do NOT include 'Polished' here; that is handled in offerRelatedStonecuttingFamilies().
        if (F.baseBlock != Base) offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, F.baseBlock, Base)
        F.Chiseled?.let { offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, it, Base) }
        F.Slab?.let { offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, it, Base, 2) }
        F.Stairs?.let { offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, it, Base) }
        F.Wall?.let { offerStonecuttingRecipe(RecipeCategory.BUILDING_BLOCKS, it, Base) }
    }

    /** Add stonecutting recipes for a list of related families. */
    fun offerRelatedStonecuttingFamilies(Families: List<BlockFamily>) {
        for (I in Families.indices)
            for (J in I + 1..<Families.size)
                offerStonecuttingFamily(Families[J], Families[I].baseBlock)
    }

    /** Offer a lantern and chain recipe. */
    fun offerChainAndLantern(Chain: Block, Lantern: Block, Material: Item, Froglight: Item) {
        offerShaped(Chain) {
            pattern("N")
            pattern("A")
            pattern("N")
            cinput('A', Material)
            cinput('N', Items.IRON_NUGGET)
        }

        offerShaped(Lantern) {
            pattern("NAN")
            pattern("A#A")
            pattern("NNN")
            cinput('A', Material)
            cinput('N', Items.IRON_NUGGET)
            cinput('#', Froglight)
        }
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
        Suffix: String = "",
        Consumer: ShapedRecipeJsonBuilder.() -> Unit,
    ) {
        var Name: String = getItemPath(Output)
        if (!Suffix.isEmpty()) Name += "_$Suffix"
        val B = createShaped(RecipeCategory.MISC, Output, Count)
        B.Consumer()
        B.offerTo(E, Name)
    }

    // Helper function for smelting
    fun offerSmelting(Input: ItemConvertible, Output: ItemConvertible, Experience: Float = .2f)
            = offerSmelting(listOf(Input.asItem()), RecipeCategory.MISC, Output.asItem(), Experience, 200, null)

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