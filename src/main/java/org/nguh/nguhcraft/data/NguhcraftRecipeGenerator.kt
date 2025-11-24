package org.nguh.nguhcraft.data

import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.data.BlockFamilies
import net.minecraft.data.BlockFamily
import net.minecraft.data.recipes.SpecialRecipeBuilder
import net.minecraft.data.recipes.RecipeOutput
import net.minecraft.data.recipes.RecipeProvider
import net.minecraft.data.recipes.ShapedRecipeBuilder
import net.minecraft.world.item.Item
import net.minecraft.world.level.ItemLike
import net.minecraft.world.item.Items
import net.minecraft.data.recipes.RecipeCategory
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.core.HolderLookup
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagKey
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.block.*
import org.nguh.nguhcraft.item.KeyDuplicationRecipe
import org.nguh.nguhcraft.item.KeyLockPairingRecipe
import org.nguh.nguhcraft.item.NguhItems
import kotlin.collections.iterator

@Environment(EnvType.CLIENT)
class NguhcraftRecipeGenerator(
    val WL: HolderLookup.Provider,
    val E: RecipeOutput
) : RecipeProvider(WL, E) {
    val Lookup = WL.lookupOrThrow(Registries.ITEM)
    val BlockFamily.IsWood get() = this in NguhBlocks.WOOD_VARIANT_FAMILIES

    override fun buildRecipes() {
        // =========================================================================
        //  Armour Trims
        // =========================================================================
        NguhItems.ALL_NGUHCRAFT_ARMOUR_TRIMS.forEach { trimSmithing(
            it.Template,
            it.Trim,
            ResourceKey.create(Registries.RECIPE, Id("${getItemName(it)}_smithing"))
        ) }

        copySmithingTemplate(NguhItems.ATLANTIC_ARMOUR_TRIM, Items.NAUTILUS_SHELL)
        copySmithingTemplate(NguhItems.CENRAIL_ARMOUR_TRIM, tag(ItemTags.IRON_ORES))
        copySmithingTemplate(NguhItems.ICE_COLD_ARMOUR_TRIM, Items.SNOW_BLOCK)
        copySmithingTemplate(NguhItems.VENEFICIUM_ARMOUR_TRIM, Items.SLIME_BALL)

        // =========================================================================
        //  Slabs and Slablets
        // =========================================================================
        for ((Lesser, Greater) in SLABLETS) {
            oneToOneConversionRecipe(Lesser, Greater, "slablets", 2)
            offerShapelessRecipe(Greater, 1, Lesser to 2)
        }

        offerShapelessRecipe(NguhItems.SLAB_SHAVINGS_1, 8, NguhItems.SLAB_SHAVINGS_8 to 1)
        offerShapelessRecipe(NguhItems.SLAB_SHAVINGS_8, 1, NguhItems.SLAB_SHAVINGS_1 to 8)
        offerShapelessRecipe(NguhItems.SLABLET_1, 1, NguhItems.SLAB_SHAVINGS_8 to 8)
        offerShapelessRecipe(NguhItems.SLAB_SHAVINGS_8, 8, NguhItems.SLABLET_1 to 1)

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

        // =========================================================================
        // Vanilla Block Decompositions
        // =========================================================================
        offerShapelessRecipe(Items.STRING, 4, ItemTags.WOOL to 1)
        offerShapelessRecipe(Items.AMETHYST_SHARD, 4, Items.AMETHYST_BLOCK to 1)
        offerShapelessRecipe(Items.QUARTZ, 4, Items.QUARTZ_BLOCK to 1)
        offerShapelessRecipe(Items.NETHER_WART, 9, Items.NETHER_WART_BLOCK to 1)
        offerShapelessRecipe(Items.BAMBOO, 18, Items.BAMBOO_BLOCK to 2)
        offerShapelessRecipe(Items.SOUL_SOIL, 2, Items.SOUL_SAND to 1, Items.DIRT to 1)

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

        offerShaped(NguhBlocks.IRON_GRATE, 16) {
            pattern(" # ")
            pattern("# #")
            pattern(" # ")
            cinput('#', Items.IRON_BLOCK)
        }

        offerShaped(NguhBlocks.WROUGHT_IRON_GRATE, 16) {
            pattern(" # ")
            pattern("# #")
            pattern(" # ")
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

        offerShaped(NguhBlocks.DRIPSTONE_BRICKS, 4) {
            pattern("##")
            pattern("##")
            cinput('#', Items.DRIPSTONE_BLOCK)
        }

        offerShaped(NguhBlocks.CHARCOAL_BLOCK, 1) {
            pattern("ccc")
            pattern("ccc")
            pattern("ccc")
            cinput('c', Items.CHARCOAL)
        }

        offerChainAndLantern(NguhBlocks.OCHRE_CHAIN, NguhBlocks.OCHRE_LANTERN, Items.RESIN_CLUMP, Items.OCHRE_FROGLIGHT)
        offerChainAndLantern(NguhBlocks.PEARLESCENT_CHAIN, NguhBlocks.PEARLESCENT_LANTERN, Items.AMETHYST_SHARD, Items.PEARLESCENT_FROGLIGHT)
        offerChainAndLantern(NguhBlocks.VERDANT_CHAIN, NguhBlocks.VERDANT_LANTERN, Items.EMERALD, Items.VERDANT_FROGLIGHT)

        offerShapelessRecipe(Items.HOPPER, 1, NguhBlocks.DECORATIVE_HOPPER to 1, Items.CHEST to 1)
        offerShapelessRecipe(NguhBlocks.DECORATIVE_HOPPER, 1, Items.HOPPER to 1)
        offerShapelessRecipe(Items.CHARCOAL, 9, NguhBlocks.CHARCOAL_BLOCK to 1)

        // =========================================================================
        //  Block Families
        // =========================================================================
        for ((Base, Gilded) in listOf(
            Blocks.CALCITE to NguhBlocks.GILDED_CALCITE,
            NguhBlocks.POLISHED_CALCITE to NguhBlocks.GILDED_POLISHED_CALCITE,
            NguhBlocks.CALCITE_BRICKS to NguhBlocks.GILDED_CALCITE_BRICKS,
            NguhBlocks.CHISELED_CALCITE to NguhBlocks.GILDED_CHISELED_CALCITE,
            NguhBlocks.CHISELED_CALCITE_BRICKS to NguhBlocks.GILDED_CHISELED_CALCITE_BRICKS
        )) offerShaped(Gilded, 2, "from_${BuiltInRegistries.BLOCK.getResourceKey(Base).get().location().path.lowercase()}_and_gold_ingot") {
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

        offerShaped(NguhBlocks.CALCITE_BRICKS, 4) {
            pattern("##")
            pattern("##")
            cinput('#', NguhBlocks.POLISHED_CALCITE)
        }

        offerShaped(NguhBlocks.GILDED_CALCITE_BRICKS, 4) {
            pattern("##")
            pattern("##")
            cinput('#', NguhBlocks.GILDED_POLISHED_CALCITE)
        }

        // Usual crafting recipes for custom stone types.
        for (F in NguhBlocks.ALL_VARIANT_FAMILIES) {
            F.Fence?.let {
                offerShaped(it, 3) {
                    pattern("#s#")
                    pattern("#s#")
                    cinput('#', F.baseBlock)
                    cinput('s', Items.STICK)
                    if (F.IsWood) { group("wooden_fence") }
                }
            }

            F.FenceGate?.let {
                offerShaped(it, 1, Category = RecipeCategory.REDSTONE) {
                    pattern("s#s")
                    pattern("s#s")
                    cinput('#', F.baseBlock)
                    cinput('s', Items.STICK)
                    if (F in NguhBlocks.WOOD_VARIANT_FAMILIES) { group("wooden_fence_gate") }
                }
            }

            F.Door?.let {
                offerShaped(it, 3, Category = RecipeCategory.REDSTONE) {
                    pattern("##")
                    pattern("##")
                    pattern("##")
                    cinput('#', F.baseBlock)
                    if (F in NguhBlocks.WOOD_VARIANT_FAMILIES) { group("wooden_door") }
                }
            }

            F.Trapdoor?.let {
                offerShaped(it, 2, Category = RecipeCategory.REDSTONE) {
                    pattern("###")
                    pattern("###")
                    cinput('#', F.baseBlock)
                    if (F in NguhBlocks.WOOD_VARIANT_FAMILIES) { group("wooden_trapdoor") }
                }
            }

            F.Slab?.let {
                offerShaped(it, 6, Category = RecipeCategory.BUILDING_BLOCKS) {
                    pattern("###")
                    cinput('#', F.baseBlock)

                    F.Chiseled?.let {
                        offerShaped(it, 4) {
                            pattern("#")
                            pattern("#")
                            cinput('#', F.Slab!!)
                        }
                    }
                    if (F in NguhBlocks.WOOD_VARIANT_FAMILIES) { group("wooden_slab") }
                }
            }

            F.Stairs?.let {
                offerShaped(it, 4, Category = RecipeCategory.BUILDING_BLOCKS) {
                    pattern("#  ")
                    pattern("## ")
                    pattern("###")
                    cinput('#', F.baseBlock)
                    if (F in NguhBlocks.WOOD_VARIANT_FAMILIES) { group("wooden_stairs") }
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
        //  Tinted Oak
        // =========================================================================
        offerShaped(NguhBlocks.TINTED_OAK_PLANKS, 4, "tinting") {
            pattern(" # ")
            pattern("#A#")
            pattern(" # ")
            cinput('#', Items.PALE_OAK_PLANKS)
            cinput('A', Items.AMETHYST_SHARD)
        }

        offerShaped(NguhBlocks.TINTED_OAK_LOG, 2, "tinting") {
            pattern("P")
            pattern("A")
            pattern("P")
            cinput('P', Blocks.PALE_OAK_LOG)
            cinput('A', Items.AMETHYST_SHARD)
        }

        offerShaped(NguhBlocks.TINTED_OAK_WOOD, 2, "tinting") {
            pattern("P")
            pattern("A")
            pattern("P")
            cinput('P', Blocks.PALE_OAK_WOOD)
            cinput('A', Items.AMETHYST_SHARD)
        }

        offerShaped(NguhBlocks.STRIPPED_TINTED_OAK_LOG, 2, "tinting") {
            pattern("P")
            pattern("A")
            pattern("P")
            cinput('P', Blocks.STRIPPED_PALE_OAK_LOG)
            cinput('A', Items.AMETHYST_SHARD)
        }

        offerShaped(NguhBlocks.STRIPPED_TINTED_OAK_WOOD, 2, "tinting") {
            pattern("P")
            pattern("A")
            pattern("P")
            cinput('P', Blocks.STRIPPED_PALE_OAK_WOOD)
            cinput('A', Items.AMETHYST_SHARD)
        }

        planksFromLogs(NguhBlocks.TINTED_OAK_PLANKS, NguhItems.TINTED_LOGS, 4)
        woodFromLogs(NguhBlocks.TINTED_OAK_WOOD, NguhBlocks.TINTED_OAK_LOG)
        woodFromLogs(NguhBlocks.STRIPPED_TINTED_OAK_WOOD, NguhBlocks.STRIPPED_TINTED_OAK_LOG)
        
        offerShaped(NguhBlocks.TINTED_OAK_PRESSURE_PLATE, 1, Category = RecipeCategory.REDSTONE) {
            pattern("PP")
            cinput('P', NguhBlocks.TINTED_OAK_PLANKS)
            group("wooden_pressure_plate")
        }

        offerShaped(NguhBlocks.TINTED_OAK_BUTTON, 1, Category = RecipeCategory.REDSTONE) {
            pattern("P")
            cinput('P', NguhBlocks.TINTED_OAK_PLANKS)
            group("wooden_button")
        }

        // =========================================================================
        //  Crop Stuff
        // =========================================================================
        offerShapelessRecipe(NguhItems.GRAPE_SEEDS, 1, NguhItems.GRAPES to 1)
        offerShapelessRecipe(NguhItems.GRAPE_JUICE, 1, NguhItems.GRAPES to 1, Items.GLASS_BOTTLE to 1, Items.SUGAR to 1)

        // =========================================================================
        //  Brocade Blocks
        // =========================================================================
        offerBrocade(NguhBlocks.BROCADE_BLACK, Blocks.BLACK_WOOL)
        offerBrocade(NguhBlocks.BROCADE_BLUE, Blocks.BLUE_WOOL)
        offerBrocade(NguhBlocks.BROCADE_BROWN, Blocks.BROWN_WOOL)
        offerBrocade(NguhBlocks.BROCADE_CYAN, Blocks.CYAN_WOOL)
        offerBrocade(NguhBlocks.BROCADE_GREEN, Blocks.GREEN_WOOL)
        offerBrocade(NguhBlocks.BROCADE_GREY, Blocks.GRAY_WOOL)
        offerBrocade(NguhBlocks.BROCADE_LIGHT_BLUE, Blocks.LIGHT_BLUE_WOOL)
        offerBrocade(NguhBlocks.BROCADE_LIGHT_GREY, Blocks.LIGHT_GRAY_WOOL)
        offerBrocade(NguhBlocks.BROCADE_LIME, Blocks.LIME_WOOL)
        offerBrocade(NguhBlocks.BROCADE_MAGENTA, Blocks.MAGENTA_WOOL)
        offerBrocade(NguhBlocks.BROCADE_ORANGE, Blocks.ORANGE_WOOL)
        offerBrocade(NguhBlocks.BROCADE_PINK, Blocks.PINK_WOOL)
        offerBrocade(NguhBlocks.BROCADE_PURPLE, Blocks.PURPLE_WOOL)
        offerBrocade(NguhBlocks.BROCADE_RED, Blocks.RED_WOOL)
        offerBrocade(NguhBlocks.BROCADE_WHITE, Blocks.WHITE_WOOL)
        offerBrocade(NguhBlocks.BROCADE_YELLOW, Blocks.YELLOW_WOOL)

        // =========================================================================
        //  Vertical Slabs
        // =========================================================================
        for (V in NguhBlockModels.VERTICAL_SLABS) offerShaped(V.VerticalSlab, 6) {
            pattern("#")
            pattern("#")
            pattern("#")
            cinput('#', V.Base)
            if (V.Wood) { group("wooden_vertical_slab") }
        }

        // =========================================================================
        //  Stone Cutting
        // =========================================================================
        for (F in NguhBlocks.STONE_VARIANT_FAMILIES) offerStonecuttingFamily(F)
        for (F in NguhBlocks.STONE_FAMILY_GROUPS) offerRelatedStonecuttingFamilies(F)
        offerStonecuttingFamily(NguhBlocks.POLISHED_CALCITE_FAMILY, Blocks.CALCITE)
        offerStonecuttingFamily(NguhBlocks.CALCITE_BRICK_FAMILY, Blocks.CALCITE)
        offerStonecuttingRecipe(Out = NguhBlocks.PYRITE_BRICKS, In = NguhBlocks.PYRITE)
        offerStonecuttingRecipe(Out = NguhBlocks.DRIPSTONE_BRICKS, In = Blocks.DRIPSTONE_BLOCK)

        for (V in NguhBlockModels.VERTICAL_SLABS)
            offerStonecuttingRecipe(Out = V.VerticalSlab, In = V.Base, 2)

        // =========================================================================
        //  ‘Wood Cutting’
        // =========================================================================
        for (F in NguhBlocks.VANILLA_AND_NGUHCRAFT_EXTENDED_WOOD_FAMILIES) {
            offerStonecuttingFamily(F.PlanksFamily)

            // Logs -> Planks
            offerStonecuttingRecipe(Out = F.PlanksFamily.baseBlock, In = F.Wood, 4)
            offerStonecuttingRecipe(Out = F.PlanksFamily.baseBlock, In = F.Log, 4)
            offerStonecuttingRecipe(Out = F.PlanksFamily.baseBlock, In = F.StrippedLog, 4)
            offerStonecuttingRecipe(Out = F.PlanksFamily.baseBlock, In = F.StrippedWood, 4)

            // Logs -> Stripped Logs
            offerStonecuttingRecipe(Out = F.StrippedLog, In = F.Log)
            offerStonecuttingRecipe(Out = F.StrippedWood, In = F.Wood)
        }

        // Bamboo (this is separate, bc bamboo doesn't have wood and therefore can't fit
        // in the usual wood families).
        offerStonecuttingFamily(BlockFamilies.BAMBOO_PLANKS)
        offerStonecuttingFamily(BlockFamilies.BAMBOO_MOSAIC)

        for (B in BlockFamilies.BAMBOO_MOSAIC.variants) { offerStonecuttingRecipe(Out = B.value, In = Blocks.BAMBOO_PLANKS) }
        offerStonecuttingRecipe(Out = Blocks.BAMBOO_MOSAIC, In = Blocks.BAMBOO_PLANKS)

        offerStonecuttingRecipe(Out = Blocks.BAMBOO_PLANKS, In = Blocks.BAMBOO_BLOCK)
        offerStonecuttingRecipe(Out = Blocks.BAMBOO_PLANKS, In = Blocks.STRIPPED_BAMBOO_BLOCK)

        offerStonecuttingRecipe(Out = Blocks.STRIPPED_BAMBOO_BLOCK, In = Blocks.BAMBOO_BLOCK)

        // =========================================================================
        //  Smelting
        // =========================================================================
        offerSmelting(Items.BONE_BLOCK, Items.CALCITE)

        // =========================================================================
        //  Special Recipes
        // =========================================================================
        SpecialRecipeBuilder.special(::KeyLockPairingRecipe).save(E, "key_lock_pairing")
        SpecialRecipeBuilder.special(::KeyDuplicationRecipe).save(E, "key_duplication")
    }

    /** Add a recipe for a brocade block. */
    fun offerBrocade(B: Block, Wool: Block) {
        offerShaped(B, 4) {
            pattern("SWS")
            pattern("WSW")
            pattern("SWS")
            cinput('W', Wool)
            cinput('S', Items.STRING)
        }
    }

    /** Add a stonecutting recipe. */
    fun offerStonecuttingRecipe(Out: Block, In: Block, Count: Int = 1)
        = stonecutterResultFromBase(RecipeCategory.BUILDING_BLOCKS, Out, In, Count)

    /** Add stonecutting recipes for a family. */
    fun offerStonecuttingFamily(F: BlockFamily, Base: Block = F.baseBlock) {
        // Do NOT include 'Polished' here; that is handled in offerRelatedStonecuttingFamilies().
        if (F.baseBlock != Base) offerStonecuttingRecipe(Out = F.baseBlock, In = Base)
        F.Chiseled?.let { offerStonecuttingRecipe(Out = it, In = Base) }
        F.Door?.let { offerStonecuttingRecipe(Out = it, In = Base) }
        F.Fence?.let { offerStonecuttingRecipe(Out = it, In = Base) }
        F.FenceGate?.let { offerStonecuttingRecipe(Out = it, In = Base) }
        F.Slab?.let { offerStonecuttingRecipe(Out = it, In = Base, 2) }
        F.Stairs?.let { offerStonecuttingRecipe(Out = it, In = Base) }
        F.Trapdoor?.let { offerStonecuttingRecipe(Out = it, In = Base) }
        F.Wall?.let { offerStonecuttingRecipe(Out = it, In = Base) }
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
    fun ShapedRecipeBuilder.cinput(C: Char, I: ItemLike): ShapedRecipeBuilder {
        define(C, I)
        unlockedBy("has_${getItemName(I)}", has(I))
        return this
    }

    inline fun offerShaped(
        Output: ItemLike,
        Count: Int = 1,
        Suffix: String = "",
        Category: RecipeCategory = RecipeCategory.MISC,
        Consumer: ShapedRecipeBuilder.() -> Unit,
    ) {
        var Name: String = getItemName(Output)
        if (!Suffix.isEmpty()) Name += "_$Suffix"
        val B = shaped(Category, Output, Count)
        B.Consumer()
        B.save(E, Name)
    }

    // Helper function for smelting
    fun offerSmelting(Input: ItemLike, Output: ItemLike, Experience: Float = .2f)
        = oreSmelting(listOf(Input.asItem()), RecipeCategory.MISC, Output.asItem(), Experience, 200, null)

    // offerShapelessRecipe() sucks, so this is a better version.
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> offerShapelessRecipe(Output: ItemLike, Count: Int, vararg Inputs: Pair<T, Int>) {
        val B = shapeless(RecipeCategory.MISC, Output, Count)
        for ((I, C) in Inputs) when (I) {
            is ItemLike -> B.requires(I, C).unlockedBy("has_${getItemName(I)}", has(I))
            is TagKey<*> -> B.requires(tag(I as TagKey<Item>), C).unlockedBy("has_${I.location.path}", has(I))
            else -> throw IllegalArgumentException("Invalid input type: ${I::class.simpleName}")
        }

        B.save(E, "${getItemName(Output)}_from_${Inputs.joinToString("_and_") { 
            (I, _) -> when (I) {
                is ItemLike -> getItemName(I)
                is TagKey<*> -> I.location.path
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
