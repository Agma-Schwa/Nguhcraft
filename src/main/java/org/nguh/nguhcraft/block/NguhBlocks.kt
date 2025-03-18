package org.nguh.nguhcraft.block

import com.mojang.serialization.Codec
import com.mojang.serialization.MapCodec
import io.netty.buffer.ByteBuf
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.enums.ChestType
import net.minecraft.block.piston.PistonBehavior
import net.minecraft.client.data.*
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.TexturedRenderLayers
import net.minecraft.client.render.item.model.special.ChestModelRenderer
import net.minecraft.client.render.item.property.select.SelectProperty
import net.minecraft.client.util.SpriteIdentifier
import net.minecraft.client.world.ClientWorld
import net.minecraft.component.ComponentType
import net.minecraft.data.family.BlockFamilies
import net.minecraft.data.family.BlockFamily
import net.minecraft.entity.LivingEntity
import net.minecraft.item.*
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.state.property.Properties
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.function.ValueLists
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import java.util.function.IntFunction


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
            Single = TexturedRenderLayers.NORMAL,
            Left = TexturedRenderLayers.NORMAL_LEFT,
            Right = TexturedRenderLayers.NORMAL_RIGHT,
            Key = "chest"
        )


        @Environment(EnvType.CLIENT)
        private val OVERRIDES = mapOf(
            ChestVariant.CHRISTMAS to OverrideVanillaModel(
                Single = TexturedRenderLayers.CHRISTMAS,
                Left = TexturedRenderLayers.CHRISTMAS_LEFT,
                Right = TexturedRenderLayers.CHRISTMAS_RIGHT,
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

enum class ChestVariant : StringIdentifiable {
    CHRISTMAS,
    PALE_OAK;

    val DefaultName: Text = Text.translatable("chest_variant.nguhcraft.${asString()}")
        .setStyle(Style.EMPTY.withItalic(false))

    override fun asString() = name.lowercase()

    companion object {
        val BY_ID: IntFunction<ChestVariant> = ValueLists.createIdToValueFunction(
            ChestVariant::ordinal,
            entries.toTypedArray(),
            ValueLists.OutOfBoundsHandling.ZERO
        )

        val CODEC: Codec<ChestVariant> = StringIdentifiable.createCodec(ChestVariant::values)
        val PACKET_CODEC: PacketCodec<ByteBuf, ChestVariant> = PacketCodecs.indexed(BY_ID, ChestVariant::ordinal)
    }
}

@Environment(EnvType.CLIENT)
class ChestVariantProperty : SelectProperty<ChestVariant> {
    override fun getValue(
        St: ItemStack,
        CW: ClientWorld?,
        LE: LivingEntity?,
        Seed: Int,
        MTM: ModelTransformationMode
    ) = St.get(NguhBlocks.CHEST_VARIANT_COMPONENT)

    override fun getType() = TYPE
    companion object {
        val TYPE: SelectProperty.Type<ChestVariantProperty, ChestVariant> = SelectProperty.Type.create(
            MapCodec.unit(ChestVariantProperty()),
            ChestVariant.CODEC
        )
    }
}

val BlockFamily.Chiseled get() = this.variants[BlockFamily.Variant.CHISELED]
val BlockFamily.Polished get() = this.variants[BlockFamily.Variant.POLISHED]
val BlockFamily.Slab get() = this.variants[BlockFamily.Variant.SLAB]
val BlockFamily.Stairs get() = this.variants[BlockFamily.Variant.STAIRS]
val BlockFamily.Wall get() = this.variants[BlockFamily.Variant.WALL]

object NguhBlocks {
    // Components.
    @JvmField val CHEST_VARIANT_ID = Id("chest_variant")

    @JvmField
    val CHEST_VARIANT_COMPONENT: ComponentType<ChestVariant> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        CHEST_VARIANT_ID,
        ComponentType.builder<ChestVariant>()
            .codec(ChestVariant.CODEC)
            .packetCodec(ChestVariant.PACKET_CODEC)
            .build()
    )

    // Blocks.
    val DECORATIVE_HOPPER = Register(
        "decorative_hopper",
        ::DecorativeHopperBlock,
        AbstractBlock.Settings.copy(Blocks.HOPPER)
    )

    val LOCKED_DOOR =  Register(
        "locked_door",
        ::LockedDoorBlock,
        AbstractBlock.Settings.create()
            .mapColor(MapColor.GOLD)
            .requiresTool().strength(5.0f, 3600000.0F)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.IGNORE)
    )

    val PEARLESCENT_LANTERN = Register(
        "pearlescent_lantern",
        ::LanternBlock,
        AbstractBlock.Settings.copy(Blocks.LANTERN)
            .mapColor(MapColor.DULL_PINK)
    )

    val PEARLESCENT_CHAIN = Register(
        "pearlescent_chain",
        ::ChainBlock,
        AbstractBlock.Settings.copy(Blocks.CHAIN)
            .mapColor(MapColor.GRAY)
    )

    val WROUGHT_IRON_BLOCK = Register(
        "wrought_iron_block",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.GRAY)
    )

    val WROUGHT_IRON_BARS = Register(
        "wrought_iron_bars",
        ::PaneBlock,
        AbstractBlock.Settings.copy(Blocks.IRON_BARS)
            .mapColor(MapColor.GRAY)
    )

    val GOLD_BARS = Register(
        "gold_bars",
        ::PaneBlock,
        AbstractBlock.Settings.copy(Blocks.IRON_BARS)
            .mapColor(MapColor.YELLOW)
    )

    val COMPRESSED_STONE = Register(
        "compressed_stone",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.STONE)
            .mapColor(MapColor.STONE_GRAY)
    )

    // =========================================================================
    // Cinnabar blocks
    // =========================================================================
    val CINNABAR = Register(
        "cinnabar",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.TUFF)
            .mapColor(MapColor.DARK_RED)
    )

    val CINNABAR_SLAB = RegisterVariant(CINNABAR, "slab", ::SlabBlock)
    val CINNABAR_STAIRS = RegisterStairs(CINNABAR)

    val POLISHED_CINNABAR = Register(
        "polished_cinnabar",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.STONE)
            .mapColor(MapColor.DARK_RED)
    )

    val POLISHED_CINNABAR_SLAB = RegisterVariant(POLISHED_CINNABAR, "slab", ::SlabBlock)
    val POLISHED_CINNABAR_STAIRS = RegisterStairs(POLISHED_CINNABAR)
    val POLISHED_CINNABAR_WALL = RegisterVariant(POLISHED_CINNABAR, "wall", ::WallBlock)

    val CINNABAR_BRICKS = Register(
        "cinnabar_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.STONE)
            .mapColor(MapColor.DARK_RED)
    )

    val CINNABAR_BRICK_SLAB = RegisterVariant(CINNABAR_BRICKS, "slab", ::SlabBlock)
    val CINNABAR_BRICK_STAIRS = RegisterStairs(CINNABAR_BRICKS)
    val CINNABAR_BRICK_WALL = RegisterVariant(CINNABAR_BRICKS, "wall", ::WallBlock)

    // =========================================================================
    // Calcite blocks
    // =========================================================================
    val POLISHED_CALCITE = Register(
        "polished_calcite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val POLISHED_CALCITE_SLAB = RegisterVariant(POLISHED_CALCITE, "slab", ::SlabBlock)
    val POLISHED_CALCITE_STAIRS = RegisterStairs(POLISHED_CALCITE)
    val POLISHED_CALCITE_WALL = RegisterVariant(POLISHED_CALCITE, "wall", ::WallBlock)

    val CHISELED_CALCITE = Register(
        "chiseled_calcite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val CALCITE_BRICKS = Register(
        "calcite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val CALCITE_BRICK_SLAB = RegisterVariant(CALCITE_BRICKS, "slab", ::SlabBlock)
    val CALCITE_BRICK_STAIRS = RegisterStairs(CALCITE_BRICKS)
    val CALCITE_BRICK_WALL = RegisterVariant(CALCITE_BRICKS, "wall", ::WallBlock)

    val CHISELED_CALCITE_BRICKS = Register(
        "chiseled_calcite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    // =========================================================================
    // Gilded calcite
    // =========================================================================
    val GILDED_CALCITE = Register(
        "gilded_calcite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_CALCITE_SLAB = RegisterVariant(GILDED_CALCITE, "slab", ::SlabBlock)
    val GILDED_CALCITE_STAIRS = RegisterStairs(GILDED_CALCITE)

    val GILDED_POLISHED_CALCITE = Register(
        "gilded_polished_calcite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_POLISHED_CALCITE_SLAB = RegisterVariant(GILDED_POLISHED_CALCITE, "slab", ::SlabBlock)
    val GILDED_POLISHED_CALCITE_STAIRS = RegisterStairs(GILDED_POLISHED_CALCITE)
    val GILDED_POLISHED_CALCITE_WALL = RegisterVariant(GILDED_POLISHED_CALCITE, "wall", ::WallBlock)

    val GILDED_CHISELED_CALCITE = Register(
        "gilded_chiseled_calcite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_CALCITE_BRICKS = Register(
        "gilded_calcite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_CALCITE_BRICK_SLAB = RegisterVariant(GILDED_CALCITE_BRICKS, "slab", ::SlabBlock)
    val GILDED_CALCITE_BRICK_STAIRS = RegisterStairs(GILDED_CALCITE_BRICKS)
    val GILDED_CALCITE_BRICK_WALL = RegisterVariant(GILDED_CALCITE_BRICKS, "wall", ::WallBlock)

    val GILDED_CHISELED_CALCITE_BRICKS = Register(
        "gilded_chiseled_calcite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    // =========================================================================
    // Block entities.
    // =========================================================================
    val LOCKED_DOOR_BLOCK_ENTITY = RegisterEntity(
        "lockable_door",
        FabricBlockEntityTypeBuilder
            .create(::LockedDoorBlockEntity, LOCKED_DOOR)
            .build()
    )

    // =========================================================================
    // Block families
    // =========================================================================
    val CINNABAR_FAMILY: BlockFamily = BlockFamilies.register(CINNABAR)
        .polished(POLISHED_CINNABAR)
        .slab(CINNABAR_SLAB)
        .stairs(CINNABAR_STAIRS)
        .build()

    val POLISHED_CINNABAR_FAMILY: BlockFamily = BlockFamilies.register(POLISHED_CINNABAR)
        .slab(POLISHED_CINNABAR_SLAB)
        .stairs(POLISHED_CINNABAR_STAIRS)
        .wall(POLISHED_CINNABAR_WALL)
        .build()

    val CINNABAR_BRICK_FAMILY: BlockFamily = BlockFamilies.register(CINNABAR_BRICKS)
        .slab(CINNABAR_BRICK_SLAB)
        .stairs(CINNABAR_BRICK_STAIRS)
        .wall(CINNABAR_BRICK_WALL)
        .build()

    val POLISHED_CALCITE_FAMILY: BlockFamily = BlockFamilies.register(POLISHED_CALCITE)
        .slab(POLISHED_CALCITE_SLAB)
        .stairs(POLISHED_CALCITE_STAIRS)
        .wall(POLISHED_CALCITE_WALL)
        .chiseled(CHISELED_CALCITE)
        .build()

    val CALCITE_BRICK_FAMILY: BlockFamily = BlockFamilies.register(CALCITE_BRICKS)
        .slab(CALCITE_BRICK_SLAB)
        .stairs(CALCITE_BRICK_STAIRS)
        .wall(CALCITE_BRICK_WALL)
        .chiseled(CHISELED_CALCITE_BRICKS)
        .build()

    val GILDED_CALCITE_FAMILY: BlockFamily = BlockFamilies.register(GILDED_CALCITE)
        .polished(GILDED_POLISHED_CALCITE)
        .slab(GILDED_CALCITE_SLAB)
        .stairs(GILDED_CALCITE_STAIRS)
        .build()

    val GILDED_POLISHED_CALCITE_FAMILY: BlockFamily = BlockFamilies.register(GILDED_POLISHED_CALCITE)
        .slab(GILDED_POLISHED_CALCITE_SLAB)
        .stairs(GILDED_POLISHED_CALCITE_STAIRS)
        .wall(GILDED_POLISHED_CALCITE_WALL)
        .chiseled(GILDED_CHISELED_CALCITE)
        .build()

    val GILDED_CALCITE_BRICK_FAMILY: BlockFamily = BlockFamilies.register(GILDED_CALCITE_BRICKS)
        .slab(GILDED_CALCITE_BRICK_SLAB)
        .stairs(GILDED_CALCITE_BRICK_STAIRS)
        .wall(GILDED_CALCITE_BRICK_WALL)
        .chiseled(GILDED_CHISELED_CALCITE_BRICKS)
        .build()

    val CINNABAR_FAMILIES = listOf(CINNABAR_FAMILY, POLISHED_CINNABAR_FAMILY, CINNABAR_BRICK_FAMILY)
    val CALCITE_FAMILIES = listOf(POLISHED_CALCITE_FAMILY, CALCITE_BRICK_FAMILY)
    val GILDED_CALCITE_FAMILIES = listOf(GILDED_CALCITE_FAMILY, GILDED_POLISHED_CALCITE_FAMILY, GILDED_CALCITE_BRICK_FAMILY)
    val FAMILY_GROUPS = listOf(CINNABAR_FAMILIES, CALCITE_FAMILIES, GILDED_CALCITE_FAMILIES)

    val STONE_VARIANT_FAMILIES = arrayOf(
        CINNABAR_FAMILY,
        POLISHED_CINNABAR_FAMILY,
        CINNABAR_BRICK_FAMILY,
        POLISHED_CALCITE_FAMILY,
        CALCITE_BRICK_FAMILY,
        GILDED_CALCITE_FAMILY,
        GILDED_POLISHED_CALCITE_FAMILY,
        GILDED_CALCITE_BRICK_FAMILY
    )

    val STONE_VARIANT_FAMILY_BLOCKS = mutableSetOf<Block>().also {
        for (F in STONE_VARIANT_FAMILIES) {
            it.add(F.baseBlock)
            it.addAll(F.variants.values)
        }
    }.toTypedArray()

    // =========================================================================
    // Tags
    // =========================================================================
    // Note: These are seemingly randomly shuffled everytime datagen runs; I have
    // no idea why, but they all seem to be there so I don’t care.
    val PICKAXE_MINEABLE = mutableSetOf(
        DECORATIVE_HOPPER,
        LOCKED_DOOR,
        PEARLESCENT_LANTERN,
        PEARLESCENT_CHAIN,
        WROUGHT_IRON_BLOCK,
        WROUGHT_IRON_BARS,
        GOLD_BARS,
        COMPRESSED_STONE,
    ).also { it.addAll(STONE_VARIANT_FAMILY_BLOCKS) }.toTypedArray()

    val DROPS_SELF = mutableSetOf(
        DECORATIVE_HOPPER,
        PEARLESCENT_LANTERN,
        PEARLESCENT_CHAIN,
        WROUGHT_IRON_BLOCK,
        WROUGHT_IRON_BARS,
        GOLD_BARS,
        COMPRESSED_STONE,
    ).also {
        // Slabs may drop 2 or 1 and are thus handled separately.
        it.addAll(STONE_VARIANT_FAMILY_BLOCKS.filter { it !is SlabBlock })
    }.toTypedArray()

    // =========================================================================
    // Models
    // =========================================================================
    fun BootstrapModels(G: BlockStateModelGenerator) {
        // The door and hopper block state models are very complicated and not exposed
        // as helper functions (the door is actually exposed but our door has an extra
        // block state), so those are currently hard-coded as JSON files instead of being
        // generated here.
        G.registerItemModel(DECORATIVE_HOPPER.asItem())
        G.registerItemModel(LOCKED_DOOR.asItem())

        // Simple blocks.
        G.registerLantern(PEARLESCENT_LANTERN)
        G.registerItemModel(PEARLESCENT_CHAIN.asItem())
        G.registerSimpleCubeAll(WROUGHT_IRON_BLOCK)
        G.registerSimpleCubeAll(COMPRESSED_STONE)
        G.registerAxisRotated(PEARLESCENT_CHAIN, ModelIds.getBlockModelId(PEARLESCENT_CHAIN))

        // Bars.
        RegisterBarsModel(G, WROUGHT_IRON_BARS)
        RegisterBarsModel(G, GOLD_BARS)

        // Block families.
        STONE_VARIANT_FAMILIES
            .filter(BlockFamily::shouldGenerateModels)
            .forEach { G.registerCubeAllModelTexturePool(it.baseBlock).family(it) }

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

    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register {
            it.add(DECORATIVE_HOPPER)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register {
            it.add(LOCKED_DOOR)
            it.add(COMPRESSED_STONE)
            it.add(WROUGHT_IRON_BLOCK)
            it.add(WROUGHT_IRON_BARS)
            it.add(GOLD_BARS)
            for (B in STONE_VARIANT_FAMILY_BLOCKS) it.add(B)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register {
            it.add(PEARLESCENT_LANTERN)
            it.add(PEARLESCENT_CHAIN)
        }
    }

    @Environment(EnvType.CLIENT)
    fun InitRenderLayers() {
        val Cutout = RenderLayer.getCutout()
        val CutoutMipped = RenderLayer.getCutoutMipped()
        BlockRenderLayerMap.INSTANCE.putBlock(LOCKED_DOOR, Cutout)
        BlockRenderLayerMap.INSTANCE.putBlock(PEARLESCENT_LANTERN, Cutout)
        BlockRenderLayerMap.INSTANCE.putBlock(PEARLESCENT_CHAIN, Cutout)
        BlockRenderLayerMap.INSTANCE.putBlock(WROUGHT_IRON_BARS, CutoutMipped)
        BlockRenderLayerMap.INSTANCE.putBlock(GOLD_BARS, CutoutMipped)
    }

    @Suppress("DEPRECATION")
    private fun RegisterVariant(
        Parent: Block,
        Suffix: String,
        Ctor: (AbstractBlock.Settings) -> Block
    ) = Register(
        "${Registries.BLOCK.getKey(Parent).get().value.path}_$Suffix",
        Ctor,
        AbstractBlock.Settings.copyShallow(Parent)
    )

    @Suppress("DEPRECATION")
    private fun RegisterStairs(Parent: Block) = Register(
        "${Registries.BLOCK.getKey(Parent).get().value.path}_stairs",
        { StairsBlock(Parent.defaultState, it) },
        AbstractBlock.Settings.copyShallow(Parent)
    )

    private fun Register(
        Key: String,
        Ctor: (S: AbstractBlock.Settings) -> Block,
        S: AbstractBlock.Settings,
        ItemCtor: (B: Block, S: Item.Settings) -> Item = ::BlockItem
    ): Block {
        // Create registry keys.
        val ItemKey = RegistryKey.of(RegistryKeys.ITEM, Id(Key))
        val BlockKey = RegistryKey.of(RegistryKeys.BLOCK, Id(Key))

        // Set the registry key for the block settings.
        S.registryKey(BlockKey)

        // Create and register the block.
        val B = Ctor(S)
        Registry.register(Registries.BLOCK, BlockKey, B)

        // Create and register the item.
        val ItemSettings = Item.Settings()
            .useBlockPrefixedTranslationKey()
            .registryKey(ItemKey)
        val I = ItemCtor(B, ItemSettings)
        Registry.register(Registries.ITEM, ItemKey, I)
        return B
    }

    private fun <C : BlockEntity> RegisterEntity(
        Key: String,
        Type: BlockEntityType<C>
    ): BlockEntityType<C> = Registry.register(
        Registries.BLOCK_ENTITY_TYPE,
        Id(Key),
        Type
    )

    // Copied from ::registerIronBars()
    fun RegisterBarsModel(G: BlockStateModelGenerator, B: Block) {
        val identifier = ModelIds.getBlockSubModelId(B, "_post_ends")
        val identifier2 = ModelIds.getBlockSubModelId(B, "_post")
        val identifier3 = ModelIds.getBlockSubModelId(B, "_cap")
        val identifier4 = ModelIds.getBlockSubModelId(B, "_cap_alt")
        val identifier5 = ModelIds.getBlockSubModelId(B, "_side")
        val identifier6 = ModelIds.getBlockSubModelId(B, "_side_alt")
        G.blockStateCollector
            .accept(
                MultipartBlockStateSupplier.create(B)
                    .with(BlockStateVariant.create().put(VariantSettings.MODEL, identifier))
                    .with(
                        When.create().set(Properties.NORTH, false).set(Properties.EAST, false).set(Properties.SOUTH, false)
                            .set(Properties.WEST, false),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier2)
                    )
                    .with(
                        When.create().set(Properties.NORTH, true).set(Properties.EAST, false).set(Properties.SOUTH, false)
                            .set(Properties.WEST, false),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier3)
                    )
                    .with(
                        When.create().set(Properties.NORTH, false).set(Properties.EAST, true).set(Properties.SOUTH, false)
                            .set(Properties.WEST, false),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier3)
                            .put(VariantSettings.Y, VariantSettings.Rotation.R90)
                    )
                    .with(
                        When.create().set(Properties.NORTH, false).set(Properties.EAST, false).set(Properties.SOUTH, true)
                            .set(Properties.WEST, false),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier4)
                    )
                    .with(
                        When.create().set(Properties.NORTH, false).set(Properties.EAST, false).set(Properties.SOUTH, false)
                            .set(Properties.WEST, true),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier4)
                            .put(VariantSettings.Y, VariantSettings.Rotation.R90)
                    )
                    .with(
                        When.create().set(Properties.NORTH, true),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier5)
                    )
                    .with(
                        When.create().set(Properties.EAST, true),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier5)
                            .put(VariantSettings.Y, VariantSettings.Rotation.R90)
                    )
                    .with(
                        When.create().set(Properties.SOUTH, true),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier6)
                    )
                    .with(
                        When.create().set(Properties.WEST, true),
                        BlockStateVariant.create().put(VariantSettings.MODEL, identifier6)
                            .put(VariantSettings.Y, VariantSettings.Rotation.R90)
                    )
            )
        G.registerItemModel(B)
    }
}