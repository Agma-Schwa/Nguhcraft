package org.nguh.nguhcraft.block

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.enums.ChestType
import net.minecraft.client.data.*
import net.minecraft.client.data.BlockStateModelGenerator.createWeightedVariant
import net.minecraft.client.data.ModelIds.getBlockModelId
import net.minecraft.client.data.TextureKey.ALL
import net.minecraft.client.render.BlockRenderLayer
import net.minecraft.client.render.TexturedRenderLayers
import net.minecraft.client.render.item.model.special.ChestModelRenderer
import net.minecraft.client.render.item.property.select.SelectProperty
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.world.ClientWorld
import net.minecraft.data.family.BlockFamily
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemDisplayContext
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.state.property.Properties
import net.minecraft.util.Identifier
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.flatten
import java.util.*
import java.util.Optional.empty

@Environment(EnvType.CLIENT)
private fun MakeSprite(S: String) = SpriteIdentifier(
    TexturedRenderLayers.CHEST_ATLAS_TEXTURE,
    Id("entity/chest/$S")
)

@Environment(EnvType.CLIENT)
class LockedChestVariant(
    val Locked: SpriteIdentifier,
    val Unlocked: SpriteIdentifier
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
            Single = TexturedRenderLayers.CHEST,
            Left = TexturedRenderLayers.CHEST_LEFT,
            Right = TexturedRenderLayers.CHEST_RIGHT,
            Key = "chest"
        )


        @Environment(EnvType.CLIENT)
        private val OVERRIDES = mapOf(
            ChestVariant.CHRISTMAS to OverrideVanillaModel(
                Single = TexturedRenderLayers.CHRISTMAS_CHEST,
                Left = TexturedRenderLayers.CHRISTMAS_CHEST_LEFT,
                Right = TexturedRenderLayers.CHRISTMAS_CHEST_RIGHT,
                Key = "christmas"
            ),

            ChestVariant.PALE_OAK to ChestTextureOverride("pale_oak")
        )

        @Environment(EnvType.CLIENT)
        @JvmStatic
        fun GetTexture(CV: ChestVariant?, CT: ChestType, Locked: Boolean) =
            (CV?.let { OVERRIDES[CV] } ?: Normal).get(CT, Locked)

        internal fun OverrideVanillaModel(
            Single: SpriteIdentifier,
            Left: SpriteIdentifier,
            Right: SpriteIdentifier,
            Key: String,
        ) = ChestTextureOverride(
            Single = LockedChestVariant(MakeSprite("${Key}_locked"), Single),
            Left = LockedChestVariant(MakeSprite("${Key}_left_locked"), Left),
            Right = LockedChestVariant(MakeSprite("${Key}_right_locked"), Right)
        )
    }
}

@Environment(EnvType.CLIENT)
class ChestVariantProperty : SelectProperty<ChestVariant> {
    override fun getValue(
        St: ItemStack?,
        CW: ClientWorld?,
        LE: LivingEntity?,
        Seed: Int,
        Ctx: ItemDisplayContext?
    ) = St?.get(NguhBlocks.CHEST_VARIANT_COMPONENT)

    override fun valueCodec(): Codec<ChestVariant> = ChestVariant.CODEC
    override fun getType() = TYPE
    companion object {
        val TYPE: SelectProperty.Type<ChestVariantProperty, ChestVariant> = SelectProperty.Type.create(
            MapCodec.unit(ChestVariantProperty()),
            ChestVariant.CODEC
        )
    }
}

@Environment(EnvType.CLIENT)
object NguhBlockModels {
    @Environment(EnvType.CLIENT)
    fun ChainModelTemplate(): TexturedModel.Factory = TexturedModel.makeFactory(
        TextureMap::all,
        Model(Optional.of(Id("block/template_chain")), empty(), ALL)
    )

    // This model happens to be facing south.
    val VERTICAL_SLAB = Model(
        Optional.of(Id("block/vertical_slab")),
        empty(),
        ALL
    )

    @Environment(EnvType.CLIENT)
    class VSlab(
        val VerticalSlab: VerticalSlabBlock,
        val Base: Block,
        val Wood: Boolean = false,
        val TextureId: Identifier = TextureMap.getId(Base)
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
        VSlab(NguhBlocks.QUARTZ_SLAB_VERTICAL, Blocks.QUARTZ_BLOCK, false , TextureMap.getSubId(Blocks.QUARTZ_BLOCK, "_top")),
        VSlab(NguhBlocks.RED_NETHER_BRICK_SLAB_VERTICAL, Blocks.RED_NETHER_BRICKS),
        VSlab(NguhBlocks.RED_SANDSTONE_SLAB_VERTICAL, Blocks.RED_SANDSTONE),
        VSlab(NguhBlocks.SANDSTONE_SLAB_VERTICAL, Blocks.SANDSTONE),
        VSlab(NguhBlocks.SMOOTH_QUARTZ_SLAB_VERTICAL, Blocks.SMOOTH_QUARTZ, false , TextureMap.getSubId(Blocks.QUARTZ_BLOCK, "_bottom")) ,
        VSlab(NguhBlocks.SMOOTH_RED_SANDSTONE_SLAB_VERTICAL, Blocks.SMOOTH_RED_SANDSTONE, false , TextureMap.getSubId(Blocks.RED_SANDSTONE , "_top")),
        VSlab(NguhBlocks.SMOOTH_SANDSTONE_SLAB_VERTICAL, Blocks.SMOOTH_SANDSTONE, false , TextureMap.getSubId(Blocks.SANDSTONE, "_top")),
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
    fun BootstrapModels(G: BlockStateModelGenerator) {
        // The door and hopper block state models are very complicated and not exposed
        // as helper functions (the door is actually exposed but our door has an extra
        // block state), so those are currently hard-coded as JSON files instead of being
        // generated here.
        G.registerItemModel(NguhBlocks.DECORATIVE_HOPPER.asItem())
        G.registerItemModel(NguhBlocks.LOCKED_DOOR.asItem())

        // Simple blocks.
        G.registerSimpleCubeAll(NguhBlocks.WROUGHT_IRON_BLOCK)
        G.registerSimpleCubeAll(NguhBlocks.IRON_GRATE)
        G.registerSimpleCubeAll(NguhBlocks.WROUGHT_IRON_GRATE)
        G.registerSimpleCubeAll(NguhBlocks.COMPRESSED_STONE)
        G.registerSimpleCubeAll(NguhBlocks.PYRITE)
        G.registerSimpleCubeAll(NguhBlocks.CHARCOAL_BLOCK)

        // Chains and lanterns.
        for ((Chain, Lantern) in NguhBlocks.CHAINS_AND_LANTERNS) {
            G.registerLantern(Lantern)
            G.registerItemModel(Chain.asItem())
            G.registerAxisRotated(Chain, createWeightedVariant(getBlockModelId(Chain)))
            ChainModelTemplate().upload(Chain, G.modelCollector)
        }

        // Tinted oak logs.
        G.createLogTexturePool(NguhBlocks.TINTED_OAK_LOG)
            .log(NguhBlocks.TINTED_OAK_LOG)
            .wood(NguhBlocks.TINTED_OAK_WOOD)

        G.createLogTexturePool(NguhBlocks.STRIPPED_TINTED_OAK_LOG)
            .log(NguhBlocks.STRIPPED_TINTED_OAK_LOG)
            .wood(NguhBlocks.STRIPPED_TINTED_OAK_WOOD)

        // Brocade blocks.
        for (B in NguhBlocks.ALL_BROCADE_BLOCKS) G.registerSimpleCubeAll(B)

        // Bars.
        RegisterBarsModel(G, NguhBlocks.WROUGHT_IRON_BARS)
        RegisterBarsModel(G, NguhBlocks.GOLD_BARS)

        // Block families.
        NguhBlocks.ALL_VARIANT_FAMILIES
            .filter(BlockFamily::shouldGenerateModels)
            .forEach { G.registerCubeAllModelTexturePool(it.baseBlock).family(it) }

        // Vertical slabs.
        for (V in VERTICAL_SLABS) RegisterVerticalSlab(G, V)

        // Chest variants. Copied from registerChest().
        val Template = Models.TEMPLATE_CHEST.upload(Items.CHEST, TextureMap.particle(Blocks.OAK_PLANKS), G.modelCollector)
        val Normal = ItemModels.special(Template, ChestModelRenderer.Unbaked(ChestModelRenderer.NORMAL_ID))
        val Christmas = ItemModels.special(Template, ChestModelRenderer.Unbaked(ChestModelRenderer.CHRISTMAS_ID))
        val ChristmasOrNormal = ItemModels.christmasSelect(Christmas, Normal)
        val PaleOak = ItemModels.special(Template, ChestModelRenderer.Unbaked(Id("pale_oak")))
        G.itemModelOutput.accept(Items.CHEST, ItemModels.select(
            ChestVariantProperty(),
            ChristmasOrNormal,
            ItemModels.switchCase(ChestVariant.CHRISTMAS, Christmas),
            ItemModels.switchCase(ChestVariant.PALE_OAK, PaleOak),
        ))
    }

    @Environment(EnvType.CLIENT)
    fun InitRenderLayers() {
        BlockRenderLayer.CUTOUT.let {
            BlockRenderLayerMap.putBlock(NguhBlocks.LOCKED_DOOR, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.IRON_GRATE, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.WROUGHT_IRON_GRATE, it)
            for (B in NguhBlocks.CHAINS_AND_LANTERNS.flatten()) BlockRenderLayerMap.putBlock(B, it)
        }

        BlockRenderLayer.CUTOUT_MIPPED.let {
            BlockRenderLayerMap.putBlock(NguhBlocks.WROUGHT_IRON_BARS, it)
            BlockRenderLayerMap.putBlock(NguhBlocks.GOLD_BARS, it)
        }
    }

    // Copied from ::registerIronBars()
    @Environment(EnvType.CLIENT)
    fun RegisterBarsModel(G: BlockStateModelGenerator, B: Block) {
        val weightedVariant = createWeightedVariant(ModelIds.getBlockSubModelId(B, "_post_ends"))
        val weightedVariant2 = createWeightedVariant(ModelIds.getBlockSubModelId(B, "_post"))
        val weightedVariant3 = createWeightedVariant(ModelIds.getBlockSubModelId(B, "_cap"))
        val weightedVariant4 = createWeightedVariant(ModelIds.getBlockSubModelId(B, "_cap_alt"))
        val weightedVariant5 = createWeightedVariant(ModelIds.getBlockSubModelId(B, "_side"))
        val weightedVariant6 = createWeightedVariant(ModelIds.getBlockSubModelId(B, "_side_alt"))
        G.blockStateCollector
            .accept(
                MultipartBlockModelDefinitionCreator.create(B)
                    .with(weightedVariant)
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.NORTH, false)
                            .put(Properties.EAST, false).put(Properties.SOUTH, false)
                            .put(Properties.WEST, false),
                        weightedVariant2
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.NORTH, true)
                            .put(Properties.EAST, false).put(Properties.SOUTH, false)
                            .put(Properties.WEST, false),
                        weightedVariant3
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.NORTH, false)
                            .put(Properties.EAST, true).put(Properties.SOUTH, false)
                            .put(Properties.WEST, false),
                        weightedVariant3.apply(BlockStateModelGenerator.ROTATE_Y_90)
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.NORTH, false)
                            .put(Properties.EAST, false).put(Properties.SOUTH, true)
                            .put(Properties.WEST, false),
                        weightedVariant4
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.NORTH, false)
                            .put(Properties.EAST, false).put(Properties.SOUTH, false)
                            .put(Properties.WEST, true),
                        weightedVariant4.apply(BlockStateModelGenerator.ROTATE_Y_90)
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.NORTH, true),
                        weightedVariant5
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.EAST, true),
                        weightedVariant5.apply(BlockStateModelGenerator.ROTATE_Y_90)
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.SOUTH, true),
                        weightedVariant6
                    )
                    .with(
                        BlockStateModelGenerator.createMultipartConditionBuilder().put(Properties.WEST, true),
                        weightedVariant6.apply(BlockStateModelGenerator.ROTATE_Y_90)
                    )
            )
        G.registerItemModel(B)
    }

    @Environment(EnvType.CLIENT)
    fun RegisterVerticalSlab(G: BlockStateModelGenerator, S: VSlab) {
        val Model = VERTICAL_SLAB.upload(S.VerticalSlab, TextureMap.all(S.TextureId), G.modelCollector)
        val South = createWeightedVariant(Model)
        G.blockStateCollector.accept(VariantsBlockModelDefinitionCreator.of(S.VerticalSlab)
            .with(BlockStateVariantMap.models(VerticalSlabBlock.TYPE)
                .register(
                    VerticalSlabBlock.Type.DOUBLE,
                    createWeightedVariant(getBlockModelId(S.Base))
                )
                .register(
                    VerticalSlabBlock.Type.NORTH,
                    South.apply(BlockStateModelGenerator.ROTATE_Y_180).apply(BlockStateModelGenerator.UV_LOCK)
                )
                .register(
                    VerticalSlabBlock.Type.SOUTH,
                    South
                )
                .register(
                    VerticalSlabBlock.Type.WEST,
                    South.apply(BlockStateModelGenerator.ROTATE_Y_90).apply(BlockStateModelGenerator.UV_LOCK)
                )
                .register(
                    VerticalSlabBlock.Type.EAST,
                    South.apply(BlockStateModelGenerator.ROTATE_Y_270).apply(BlockStateModelGenerator.UV_LOCK)
                )
            )
        )

        G.registerParentedItemModel(S.VerticalSlab, Model)
    }
}