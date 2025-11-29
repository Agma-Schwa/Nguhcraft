package org.nguh.nguhcraft.block

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.properties.ChestType
import net.minecraft.client.data.models.BlockModelGenerators
import net.minecraft.client.data.models.BlockModelGenerators.plainVariant
import net.minecraft.client.data.models.blockstates.MultiPartGenerator
import net.minecraft.client.data.models.blockstates.MultiVariantGenerator
import net.minecraft.client.data.models.blockstates.PropertyDispatch
import net.minecraft.client.data.models.model.ItemModelUtils
import net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation
import net.minecraft.client.data.models.model.ModelTemplate
import net.minecraft.client.data.models.model.ModelTemplates
import net.minecraft.client.data.models.model.TextureMapping
import net.minecraft.client.data.models.model.TextureSlot
import net.minecraft.client.data.models.model.TextureSlot.ALL
import net.minecraft.client.data.models.model.TexturedModel
import net.minecraft.client.renderer.chunk.ChunkSectionLayer
import net.minecraft.client.renderer.Sheets
import net.minecraft.client.renderer.special.ChestSpecialRenderer
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty
import net.minecraft.client.resources.model.Material
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.data.BlockFamily
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.CropBlock
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.IntegerProperty
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.flatten
import java.util.*
import java.util.Optional.empty

@Environment(EnvType.CLIENT)
private fun MakeSprite(S: String) = Material(
    Sheets.CHEST_SHEET,
    Id("entity/chest/$S")
)

@Environment(EnvType.CLIENT)
class LockedChestVariant(
    val Locked: Material,
    val Unlocked: Material
) {
    constructor(S: String) : this(
        Locked = MakeSprite("${S}_locked"),
        Unlocked = MakeSprite(S)
    )
}

@Environment(EnvType.CLIENT)
class ChestTextureOverride(
    val Single: LockedChestVariant,
    val Left: LockedChestVariant,
    val Right: LockedChestVariant,
) {
    internal constructor(S: String) : this(
        Single = LockedChestVariant(S),
        Left = LockedChestVariant("${S}_left"),
        Right = LockedChestVariant("${S}_right")
    )

    internal fun get(CT: ChestType, Locked: Boolean) = when (CT) {
        ChestType.LEFT -> if (Locked) Left.Locked else Left.Unlocked
        ChestType.RIGHT -> if (Locked) Right.Locked else Right.Unlocked
        else -> if (Locked) Single.Locked else Single.Unlocked
    }

    companion object {
        internal val Normal = OverrideVanillaModel(
            Single = Sheets.CHEST_LOCATION,
            Left = Sheets.CHEST_LOCATION_LEFT,
            Right = Sheets.CHEST_LOCATION_RIGHT,
            Key = "chest"
        )


        @Environment(EnvType.CLIENT)
        private val OVERRIDES = mapOf(
            ChestVariant.CHRISTMAS to OverrideVanillaModel(
                Single = Sheets.CHEST_XMAS_LOCATION,
                Left = Sheets.CHEST_XMAS_LOCATION_LEFT,
                Right = Sheets.CHEST_XMAS_LOCATION_RIGHT,
                Key = "christmas"
            ),

            ChestVariant.PALE_OAK to ChestTextureOverride("pale_oak")
        )

        @Environment(EnvType.CLIENT)
        @JvmStatic
        fun GetTexture(CV: ChestVariant?, CT: ChestType, Locked: Boolean) =
            (CV?.let { OVERRIDES[CV] } ?: Normal).get(CT, Locked)

        internal fun OverrideVanillaModel(
            Single: Material,
            Left: Material,
            Right: Material,
            Key: String,
        ) = ChestTextureOverride(
            Single = LockedChestVariant(MakeSprite("${Key}_locked"), Single),
            Left = LockedChestVariant(MakeSprite("${Key}_left_locked"), Left),
            Right = LockedChestVariant(MakeSprite("${Key}_right_locked"), Right)
        )
    }
}

@Environment(EnvType.CLIENT)
class ChestVariantProperty : SelectItemModelProperty<ChestVariant> {
    override fun get(
        St: ItemStack,
        CW: ClientLevel?,
        LE: LivingEntity?,
        Seed: Int,
        Ctx: ItemDisplayContext
    ) = St.get(NguhBlocks.CHEST_VARIANT_COMPONENT)

    override fun valueCodec(): Codec<ChestVariant> = ChestVariant.CODEC
    override fun type() = TYPE
    companion object {
        val TYPE: SelectItemModelProperty.Type<ChestVariantProperty, ChestVariant> = SelectItemModelProperty.Type.create(
            MapCodec.unit(ChestVariantProperty()),
            ChestVariant.CODEC
        )
    }
}

@Environment(EnvType.CLIENT)
object NguhBlockModels {
    @Environment(EnvType.CLIENT)
    fun ChainModelTemplate(): TexturedModel.Provider = TexturedModel.createDefault(
        TextureMapping::cube,
        ModelTemplate(Optional.of(Id("block/template_chain")), empty(), ALL)
    )

    // This model happens to be facing south.
    val VERTICAL_SLAB = ModelTemplate(
        Optional.of(Id("block/vertical_slab")),
        empty(),
        ALL
    )

    @Environment(EnvType.CLIENT)
    class VSlab(
        val VerticalSlab: VerticalSlabBlock,
        val Base: Block,
        val Wood: Boolean = false,
        val TextureId: ResourceLocation = TextureMapping.getBlockTexture(Base)
    )

    // Thank you, Minecraft, for doing weird nonsense with your block models
    // such as reusing the top or bottom of existing block textures for the
    // ‘smooth’ blocks, which prevents us from generating vertical slab models
    // from their corresponding full block models in a sensible manner, and
    // instead, we have to duplicate all of this data here.
    val VERTICAL_SLABS = mutableListOf(
        // Vanilla.
        VSlab(NguhBlocks.ACACIA_SLAB_VERTICAL, Blocks.ACACIA_PLANKS, true) ,
        VSlab(NguhBlocks.ANDESITE_SLAB_VERTICAL, Blocks.ANDESITE),
        VSlab(NguhBlocks.BAMBOO_MOSAIC_SLAB_VERTICAL, Blocks.BAMBOO_MOSAIC, true) ,
        VSlab(NguhBlocks.BAMBOO_SLAB_VERTICAL, Blocks.BAMBOO_BLOCK, true) ,
        VSlab(NguhBlocks.BIRCH_SLAB_VERTICAL , Blocks.BIRCH_PLANKS, true) ,
        VSlab(NguhBlocks.BLACKSTONE_SLAB_VERTICAL, Blocks.BLACKSTONE),
        VSlab(NguhBlocks.BRICK_SLAB_VERTICAL , Blocks.BRICKS),
        VSlab(NguhBlocks.CHERRY_SLAB_VERTICAL, Blocks.CHERRY_PLANKS, true) ,
        VSlab(NguhBlocks.COBBLED_DEEPSLATE_SLAB_VERTICAL, Blocks.COBBLED_DEEPSLATE),
        VSlab(NguhBlocks.COBBLESTONE_SLAB_VERTICAL, Blocks.COBBLESTONE),
        VSlab(NguhBlocks.CRIMSON_SLAB_VERTICAL, Blocks.CRIMSON_PLANKS, true) ,
        VSlab(NguhBlocks.CUT_COPPER_SLAB_VERTICAL, Blocks.CUT_COPPER),
        VSlab(NguhBlocks.CUT_RED_SANDSTONE_SLAB_VERTICAL, Blocks.CUT_RED_SANDSTONE),
        VSlab(NguhBlocks.CUT_SANDSTONE_SLAB_VERTICAL, Blocks.CUT_SANDSTONE),
        VSlab(NguhBlocks.DARK_OAK_SLAB_VERTICAL, Blocks.DARK_OAK_PLANKS, true) ,
        VSlab(NguhBlocks.DARK_PRISMARINE_SLAB_VERTICAL, Blocks.DARK_PRISMARINE),
        VSlab(NguhBlocks.DEEPSLATE_BRICK_SLAB_VERTICAL, Blocks.DEEPSLATE_BRICKS),
        VSlab(NguhBlocks.DEEPSLATE_TILE_SLAB_VERTICAL, Blocks.DEEPSLATE_TILES),
        VSlab(NguhBlocks.DIORITE_SLAB_VERTICAL, Blocks.DIORITE),
        VSlab(NguhBlocks.END_STONE_BRICK_SLAB_VERTICAL, Blocks.END_STONE_BRICKS),
        VSlab(NguhBlocks.EXPOSED_CUT_COPPER_SLAB_VERTICAL, Blocks.EXPOSED_CUT_COPPER),
        VSlab(NguhBlocks.GRANITE_SLAB_VERTICAL, Blocks.GRANITE),
        VSlab(NguhBlocks.JUNGLE_SLAB_VERTICAL, Blocks.JUNGLE_PLANKS, true) ,
        VSlab(NguhBlocks.MANGROVE_SLAB_VERTICAL, Blocks.MANGROVE_PLANKS, true) ,
        VSlab(NguhBlocks.MOSSY_COBBLESTONE_SLAB_VERTICAL, Blocks.MOSSY_COBBLESTONE),
        VSlab(NguhBlocks.MOSSY_STONE_BRICK_SLAB_VERTICAL, Blocks.MOSSY_STONE_BRICKS),
        VSlab(NguhBlocks.MUD_BRICK_SLAB_VERTICAL, Blocks.MUD_BRICKS),
        VSlab(NguhBlocks.NETHER_BRICK_SLAB_VERTICAL, Blocks.NETHER_BRICKS),
        VSlab(NguhBlocks.OAK_SLAB_VERTICAL, Blocks.OAK_PLANKS, true) ,
        VSlab(NguhBlocks.OXIDIZED_CUT_COPPER_SLAB_VERTICAL, Blocks.OXIDIZED_CUT_COPPER),
        VSlab(NguhBlocks.PALE_OAK_SLAB_VERTICAL, Blocks.PALE_OAK_PLANKS, true) ,
        VSlab(NguhBlocks.POLISHED_ANDESITE_SLAB_VERTICAL, Blocks.POLISHED_ANDESITE),
        VSlab(NguhBlocks.POLISHED_BLACKSTONE_BRICK_SLAB_VERTICAL, Blocks.POLISHED_BLACKSTONE_BRICKS) ,
        VSlab(NguhBlocks.POLISHED_BLACKSTONE_SLAB_VERTICAL, Blocks.POLISHED_BLACKSTONE),
        VSlab(NguhBlocks.POLISHED_DEEPSLATE_SLAB_VERTICAL, Blocks.POLISHED_DEEPSLATE),
        VSlab(NguhBlocks.POLISHED_DIORITE_SLAB_VERTICAL, Blocks.POLISHED_DIORITE),
        VSlab(NguhBlocks.POLISHED_GRANITE_SLAB_VERTICAL, Blocks.POLISHED_GRANITE),
        VSlab(NguhBlocks.POLISHED_TUFF_SLAB_VERTICAL, Blocks.POLISHED_TUFF),
        VSlab(NguhBlocks.PRISMARINE_BRICK_SLAB_VERTICAL, Blocks.PRISMARINE_BRICKS),
        VSlab(NguhBlocks.PRISMARINE_SLAB_VERTICAL, Blocks.PRISMARINE),
        VSlab(NguhBlocks.PURPUR_SLAB_VERTICAL, Blocks.PURPUR_BLOCK),
        VSlab(NguhBlocks.QUARTZ_SLAB_VERTICAL, Blocks.QUARTZ_BLOCK, false , TextureMapping.getBlockTexture(Blocks.QUARTZ_BLOCK, "_top")),
        VSlab(NguhBlocks.RED_NETHER_BRICK_SLAB_VERTICAL, Blocks.RED_NETHER_BRICKS),
        VSlab(NguhBlocks.RED_SANDSTONE_SLAB_VERTICAL, Blocks.RED_SANDSTONE),
        VSlab(NguhBlocks.SANDSTONE_SLAB_VERTICAL, Blocks.SANDSTONE),
        VSlab(NguhBlocks.SMOOTH_QUARTZ_SLAB_VERTICAL, Blocks.SMOOTH_QUARTZ, false , TextureMapping.getBlockTexture(Blocks.QUARTZ_BLOCK, "_bottom")) ,
        VSlab(NguhBlocks.SMOOTH_RED_SANDSTONE_SLAB_VERTICAL, Blocks.SMOOTH_RED_SANDSTONE, false , TextureMapping.getBlockTexture(Blocks.RED_SANDSTONE , "_top")),
        VSlab(NguhBlocks.SMOOTH_SANDSTONE_SLAB_VERTICAL, Blocks.SMOOTH_SANDSTONE, false , TextureMapping.getBlockTexture(Blocks.SANDSTONE, "_top")),
        VSlab(NguhBlocks.SMOOTH_STONE_SLAB_VERTICAL, Blocks.SMOOTH_STONE),
        VSlab(NguhBlocks.SPRUCE_SLAB_VERTICAL, Blocks.SPRUCE_PLANKS, true) ,
        VSlab(NguhBlocks.STONE_BRICK_SLAB_VERTICAL, Blocks.STONE_BRICKS),
        VSlab(NguhBlocks.STONE_SLAB_VERTICAL , Blocks.STONE) ,
        VSlab(NguhBlocks.TUFF_BRICK_SLAB_VERTICAL, Blocks.TUFF_BRICKS),
        VSlab(NguhBlocks.TUFF_SLAB_VERTICAL, Blocks.TUFF),
        VSlab(NguhBlocks.WARPED_SLAB_VERTICAL, Blocks.WARPED_PLANKS, true) ,
        VSlab(NguhBlocks.WAXED_CUT_COPPER_SLAB_VERTICAL, Blocks.CUT_COPPER),
        VSlab(NguhBlocks.WAXED_EXPOSED_CUT_COPPER_SLAB_VERTICAL, Blocks.EXPOSED_CUT_COPPER),
        VSlab(NguhBlocks.WAXED_OXIDIZED_CUT_COPPER_SLAB_VERTICAL, Blocks.OXIDIZED_CUT_COPPER),
        VSlab(NguhBlocks.WAXED_WEATHERED_CUT_COPPER_SLAB_VERTICAL , Blocks.WEATHERED_CUT_COPPER),
        VSlab(NguhBlocks.WEATHERED_CUT_COPPER_SLAB_VERTICAL, Blocks.WEATHERED_CUT_COPPER),

        // Custom.
        VSlab(NguhBlocks.CALCITE_BRICK_SLAB_VERTICAL, NguhBlocks.CALCITE_BRICKS),
        VSlab(NguhBlocks.CINNABAR_BRICK_SLAB_VERTICAL, NguhBlocks.CINNABAR_BRICKS),
        VSlab(NguhBlocks.CINNABAR_SLAB_VERTICAL, NguhBlocks.CINNABAR),
        VSlab(NguhBlocks.GILDED_CALCITE_BRICK_SLAB_VERTICAL, NguhBlocks.GILDED_CALCITE_BRICKS),
        VSlab(NguhBlocks.GILDED_CALCITE_SLAB_VERTICAL, NguhBlocks.GILDED_CALCITE),
        VSlab(NguhBlocks.GILDED_POLISHED_CALCITE_SLAB_VERTICAL, NguhBlocks.GILDED_POLISHED_CALCITE),
        VSlab(NguhBlocks.POLISHED_CALCITE_SLAB_VERTICAL, NguhBlocks.POLISHED_CALCITE),
        VSlab(NguhBlocks.POLISHED_CINNABAR_SLAB_VERTICAL, NguhBlocks.POLISHED_CINNABAR),
        VSlab(NguhBlocks.TINTED_OAK_SLAB_VERTICAL, NguhBlocks.TINTED_OAK_PLANKS, true),
        VSlab(NguhBlocks.PYRITE_BRICK_SLAB_VERTICAL, NguhBlocks.PYRITE_BRICKS),
        VSlab(NguhBlocks.DRIPSTONE_BRICK_SLAB_VERTICAL, NguhBlocks.DRIPSTONE_BRICKS),
    ).toTypedArray()

    @Environment(EnvType.CLIENT)
    fun BootstrapModels(G: BlockModelGenerators) {
        // The door and hopper block state models are very complicated and not exposed
        // as helper functions (the door is actually exposed but our door has an extra
        // block state), so those are currently hard-coded as JSON files instead of being
        // generated here.
        G.registerSimpleFlatItemModel(NguhBlocks.DECORATIVE_HOPPER.asItem())
        G.registerSimpleFlatItemModel(NguhBlocks.LOCKED_DOOR.asItem())

        // Simple blocks.
        G.createTrivialCube(NguhBlocks.WROUGHT_IRON_BLOCK)
        G.createTrivialCube(NguhBlocks.IRON_GRATE)
        G.createTrivialCube(NguhBlocks.WROUGHT_IRON_GRATE)
        G.createTrivialCube(NguhBlocks.COMPRESSED_STONE)
        G.createTrivialCube(NguhBlocks.PYRITE)
        G.createTrivialCube(NguhBlocks.CHARCOAL_BLOCK)

        // Chains and lanterns.
        for ((Chain, Lantern) in NguhBlocks.CHAINS_AND_LANTERNS) {
            G.createLantern(Lantern)
            G.registerSimpleFlatItemModel(Chain.asItem())
            G.createAxisAlignedPillarBlockCustomModel(Chain, plainVariant(getModelLocation(Chain)))
            ChainModelTemplate().create(Chain, G.modelOutput)
        }

        // Tinted oak logs.
        G.woodProvider(NguhBlocks.TINTED_OAK_LOG)
            .logWithHorizontal(NguhBlocks.TINTED_OAK_LOG)
            .wood(NguhBlocks.TINTED_OAK_WOOD)

        G.woodProvider(NguhBlocks.STRIPPED_TINTED_OAK_LOG)
            .logWithHorizontal(NguhBlocks.STRIPPED_TINTED_OAK_LOG)
            .wood(NguhBlocks.STRIPPED_TINTED_OAK_WOOD)

        // Brocade blocks.
        for (B in NguhBlocks.ALL_BROCADE_BLOCKS) G.createTrivialCube(B)

        // Bars.
        RegisterBarsModel(G, NguhBlocks.WROUGHT_IRON_BARS)
        RegisterBarsModel(G, NguhBlocks.GOLD_BARS)

        // Crop blocks
        RegisterCropWithStick(G, NguhBlocks.GRAPE_CROP, GrapeCropBlock.STICK_LOGGED, GrapeCropBlock.AGE, 0, 1, 2, 3, 4)
        G.createCropBlock(NguhBlocks.PEANUT_CROP, CropBlock.AGE, 0, 1, 2, 3, 4, 5, 6, 7)

        // Block families.
        NguhBlocks.ALL_VARIANT_FAMILIES
            .filter(BlockFamily::shouldGenerateModel)
            .forEach { G.family(it.baseBlock).generateFor(it) }

        // Vertical slabs.
        for (V in VERTICAL_SLABS) RegisterVerticalSlab(G, V)

        // Chest variants. Copied from registerChest().
        val Template = ModelTemplates.CHEST_INVENTORY.create(Items.CHEST, TextureMapping.particle(Blocks.OAK_PLANKS), G.modelOutput)
        val Normal = ItemModelUtils.specialModel(Template, ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.NORMAL_CHEST_TEXTURE))
        val Christmas = ItemModelUtils.specialModel(Template, ChestSpecialRenderer.Unbaked(ChestSpecialRenderer.GIFT_CHEST_TEXTURE))
        val ChristmasOrNormal = ItemModelUtils.isXmas(Christmas, Normal)
        val PaleOak = ItemModelUtils.specialModel(Template, ChestSpecialRenderer.Unbaked(Id("pale_oak")))
        G.itemModelOutput.accept(Items.CHEST, ItemModelUtils.select(
            ChestVariantProperty(),
            ChristmasOrNormal,
            ItemModelUtils.`when`(ChestVariant.CHRISTMAS, Christmas),
            ItemModelUtils.`when`(ChestVariant.PALE_OAK, PaleOak),
        ))
    }

    @Environment(EnvType.CLIENT)
    fun InitRenderLayers() {
        ChunkSectionLayer.CUTOUT.let {
            BlockRenderLayerMap.putBlock(NguhBlocks.LOCKED_DOOR, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.IRON_GRATE, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.WROUGHT_IRON_GRATE, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.GRAPE_CROP, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.PEANUT_CROP, it)
            for (B in NguhBlocks.CHAINS_AND_LANTERNS.flatten()) BlockRenderLayerMap.putBlock(B, it)
        }

        ChunkSectionLayer.CUTOUT_MIPPED.let {
            BlockRenderLayerMap.putBlock(NguhBlocks.WROUGHT_IRON_BARS, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.GOLD_BARS, it)
        }
    }

    // Copied from ::registerIronBars()
    @Environment(EnvType.CLIENT)
    fun RegisterBarsModel(G: BlockModelGenerators, B: Block) {
        val plainVariant = plainVariant(getModelLocation(B, "_post_ends"))
        val plainVariant2 = plainVariant(getModelLocation(B, "_post"))
        val plainVariant3 = plainVariant(getModelLocation(B, "_cap"))
        val plainVariant4 = plainVariant(getModelLocation(B, "_cap_alt"))
        val plainVariant5 = plainVariant(getModelLocation(B, "_side"))
        val plainVariant6 = plainVariant(getModelLocation(B, "_side_alt"))
        G.blockStateOutput
            .accept(
                MultiPartGenerator.multiPart(B)
                    .with(plainVariant)
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, false),
                        plainVariant2
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true)
                            .term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, false),
                        plainVariant3
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, true).term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, false),
                        plainVariant3.with(BlockModelGenerators.Y_ROT_90)
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, true)
                            .term(BlockStateProperties.WEST, false),
                        plainVariant4
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.NORTH, false)
                            .term(BlockStateProperties.EAST, false).term(BlockStateProperties.SOUTH, false)
                            .term(BlockStateProperties.WEST, true),
                        plainVariant4.with(BlockModelGenerators.Y_ROT_90)
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.NORTH, true),
                        plainVariant5
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.EAST, true),
                        plainVariant5.with(BlockModelGenerators.Y_ROT_90)
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.SOUTH, true),
                        plainVariant6
                    )
                    .with(
                        BlockModelGenerators.condition().term(BlockStateProperties.WEST, true),
                        plainVariant6.with(BlockModelGenerators.Y_ROT_90)
                    )
            )
        G.registerSimpleFlatItemModel(B)
    }

    @Environment(EnvType.CLIENT)
    fun RegisterVerticalSlab(G: BlockModelGenerators, S: VSlab) {
        val Model = VERTICAL_SLAB.create(S.VerticalSlab, TextureMapping.cube(S.TextureId), G.modelOutput)
        val South = plainVariant(Model)
        G.blockStateOutput.accept(MultiVariantGenerator.dispatch(S.VerticalSlab)
            .with(PropertyDispatch.initial(VerticalSlabBlock.TYPE)
                .select(
                    VerticalSlabBlock.Type.DOUBLE,
                    plainVariant(getModelLocation(S.Base))
                )
                .select(
                    VerticalSlabBlock.Type.NORTH,
                    South.with(BlockModelGenerators.Y_ROT_180).with(BlockModelGenerators.UV_LOCK)
                )
                .select(
                    VerticalSlabBlock.Type.SOUTH,
                    South
                )
                .select(
                    VerticalSlabBlock.Type.WEST,
                    South.with(BlockModelGenerators.Y_ROT_90).with(BlockModelGenerators.UV_LOCK)
                )
                .select(
                    VerticalSlabBlock.Type.EAST,
                    South.with(BlockModelGenerators.Y_ROT_270).with(BlockModelGenerators.UV_LOCK)
                )
            )
        )

        G.registerSimpleItemModel(S.VerticalSlab, Model)
    }

    @Environment(EnvType.CLIENT)
    fun RegisterCropWithStick(
        G: BlockModelGenerators,
        Crop: Block,
        StickLoggedProperty: BooleanProperty,
        AgeProperty: IntegerProperty,
        vararg AgeIndices: Int
    ) {
        val StickSide = TextureSlot.create("stick_side")
        val StickTop = TextureSlot.create("stick_top")

        G.registerSimpleFlatItemModel(Crop.asItem())
        require(AgeProperty.possibleValues.size == AgeIndices.size)
        val Map1 = Int2ObjectOpenHashMap<ResourceLocation>()
        val Map2 = Int2ObjectOpenHashMap<ResourceLocation>()
        G.blockStateOutput.accept(MultiVariantGenerator.dispatch(Crop)
            .with(PropertyDispatch.initial(StickLoggedProperty, AgeProperty).generate { StickLogged: Boolean, Age: Int ->
                val I = AgeIndices[Age]
                if (StickLogged) {
                    plainVariant(
                        Map1.computeIfAbsent(I) {
                            ModelTemplate(
                                Optional.of(ResourceLocation.parse("nguhcraft:block/crop_with_stick")),
                                empty(),
                                TextureSlot.CROP,
                                StickSide,
                                StickTop
                            ).createWithSuffix(
                                Crop,
                                "_stage$it",
                                TextureMapping()
                                    .put(
                                        TextureSlot.CROP,
                                        TextureMapping.getBlockTexture(Crop, "_stage$it")
                                    )
                                    .put(
                                        StickSide,
                                        TextureMapping.getBlockTexture(
                                            Crop,
                                            "_coiled_stick_stage$it"
                                        )
                                    )
                                    .put(
                                        StickTop,
                                        ResourceLocation.parse("nguhcraft:block/stick_top")
                                    ),
                                G.modelOutput
                            )
                        }
                    )
                } else {
                    plainVariant(
                        Map2.computeIfAbsent(I) {
                            if (it == 0) {
                                ModelTemplates.CROP.create(
                                    Crop,
                                    TextureMapping().put(TextureSlot.CROP, TextureMapping.getBlockTexture(Crop)),
                                    G.modelOutput
                                )
                            } else {
                                ModelTemplates.CROP.getDefaultModelLocation(Crop)
                            }
                        }
                    )
                }
            })
        )
    }
}