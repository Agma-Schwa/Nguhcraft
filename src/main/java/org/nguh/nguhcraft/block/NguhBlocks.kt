package org.nguh.nguhcraft.block

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.`object`.builder.v1.block.type.BlockSetTypeBuilder
import net.fabricmc.fabric.api.`object`.builder.v1.block.type.WoodTypeBuilder
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry
import net.minecraft.core.Registry
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.data.BlockFamilies
import net.minecraft.data.BlockFamily
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.resources.ResourceKey
import net.minecraft.tags.TagKey
import net.minecraft.util.ByIdMap
import net.minecraft.util.StringRepresentable
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.CreativeModeTabs
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.*
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.block.state.properties.BlockSetType
import net.minecraft.world.level.block.state.properties.WoodType
import net.minecraft.world.level.material.MapColor
import net.minecraft.world.level.material.PushReaction
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.flatten
import java.util.function.IntFunction

enum class ChestVariant : StringRepresentable {
    CHRISTMAS,
    PALE_OAK;

    val DefaultName: Component = Component.translatable("chest_variant.nguhcraft.${getSerializedName()}")
        .setStyle(Style.EMPTY.withItalic(false))

    override fun getSerializedName() = name.lowercase()

    companion object {
        val BY_ID: IntFunction<ChestVariant> = ByIdMap.continuous(
            ChestVariant::ordinal,
            entries.toTypedArray(),
            ByIdMap.OutOfBoundsStrategy.ZERO
        )

        val CODEC: Codec<ChestVariant> = StringRepresentable.fromEnum(ChestVariant::values)
        val PACKET_CODEC: StreamCodec<ByteBuf, ChestVariant> = ByteBufCodecs.idMapper(BY_ID, ChestVariant::ordinal)
    }
}

val BlockFamily.Chiseled get() = this.variants[BlockFamily.Variant.CHISELED]
val BlockFamily.Door get() = this.variants[BlockFamily.Variant.DOOR]
val BlockFamily.Fence get() = this.variants[BlockFamily.Variant.FENCE]
val BlockFamily.FenceGate get() = this.variants[BlockFamily.Variant.FENCE_GATE]
val BlockFamily.Polished get() = this.variants[BlockFamily.Variant.POLISHED]
val BlockFamily.Slab get() = this.variants[BlockFamily.Variant.SLAB]
val BlockFamily.Stairs get() = this.variants[BlockFamily.Variant.STAIRS]
val BlockFamily.Trapdoor get() = this.variants[BlockFamily.Variant.TRAPDOOR]
val BlockFamily.Wall get() = this.variants[BlockFamily.Variant.WALL]

data class WoodFamily(
    val PlanksFamily: BlockFamily,
    val Log: Block,
    val Wood: Block,
    val StrippedLog: Block,
    val StrippedWood: Block,
)

object NguhBlocks {
    // Components.
    @JvmField val CHEST_VARIANT_ID = Id("chest_variant")

    @JvmField
    val CHEST_VARIANT_COMPONENT: DataComponentType<ChestVariant> = Registry.register(
        BuiltInRegistries.DATA_COMPONENT_TYPE,
        CHEST_VARIANT_ID,
        DataComponentType.builder<ChestVariant>()
            .persistent(ChestVariant.CODEC)
            .networkSynchronized(ChestVariant.PACKET_CODEC)
            .build()
    )

    // All vertical slabs; this has to be declared before our custom block families.
    val VERTICAL_SLABS: List<VerticalSlabBlock> = mutableListOf()

    // =========================================================================
    //  Brocade Blocks
    // =========================================================================
    val BROCADE_WHITE = Register(
        "brocade_white",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.WHITE_WOOL)
    )

    val BROCADE_LIGHT_GREY = Register(
        "brocade_light_grey",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.LIGHT_GRAY_WOOL)
    )

    val BROCADE_GREY = Register(
        "brocade_grey",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.GRAY_WOOL)
    )

    val BROCADE_BLACK = Register(
        "brocade_black",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.BLACK_WOOL)
    )

    val BROCADE_BROWN = Register(
        "brocade_brown",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_WOOL)
    )

    val BROCADE_RED = Register(
        "brocade_red",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.RED_WOOL)
    )

    val BROCADE_ORANGE = Register(
        "brocade_orange",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.ORANGE_WOOL)
    )

    val BROCADE_YELLOW = Register(
        "brocade_yellow",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.YELLOW_WOOL)
    )

    val BROCADE_LIME = Register(
        "brocade_lime",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.LIME_WOOL)
    )

    val BROCADE_GREEN = Register(
        "brocade_green",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.GREEN_WOOL)
    )

    val BROCADE_CYAN = Register(
        "brocade_cyan",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CYAN_WOOL)
    )

    val BROCADE_LIGHT_BLUE = Register(
        "brocade_light_blue",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.LIGHT_BLUE_WOOL)
    )

    val BROCADE_BLUE = Register(
        "brocade_blue",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.BLUE_WOOL)
    )

    val BROCADE_PURPLE = Register(
        "brocade_purple",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.PURPLE_WOOL)
    )

    val BROCADE_MAGENTA = Register(
        "brocade_magenta",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.MAGENTA_WOOL)
    )

    val BROCADE_PINK = Register(
        "brocade_pink",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_WOOL)
    )

    val ALL_BROCADE_BLOCKS = arrayOf(
        BROCADE_BLACK,
        BROCADE_BLUE,
        BROCADE_BROWN,
        BROCADE_CYAN,
        BROCADE_GREEN,
        BROCADE_GREY,
        BROCADE_LIGHT_BLUE,
        BROCADE_LIGHT_GREY,
        BROCADE_LIME,
        BROCADE_MAGENTA,
        BROCADE_ORANGE,
        BROCADE_PINK,
        BROCADE_PURPLE,
        BROCADE_RED,
        BROCADE_WHITE,
        BROCADE_YELLOW,
    )

    // =========================================================================
    //  Miscellaneous Blocks
    // =========================================================================
    val DECORATIVE_HOPPER = Register(
        "decorative_hopper",
        ::DecorativeHopperBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)
    )

    val LOCKED_DOOR =  Register(
        "locked_door",
        ::LockedDoorBlock,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.GOLD)
            .requiresCorrectToolForDrops().strength(5.0f, 3600000.0F)
            .noOcclusion()
            .pushReaction(PushReaction.IGNORE)
    )

    val WROUGHT_IRON_BLOCK = Register(
        "wrought_iron_block",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.COLOR_GRAY)
    )

    val WROUGHT_IRON_BARS = Register(
        "wrought_iron_bars",
        ::IronBarsBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)
            .mapColor(MapColor.COLOR_GRAY)
    )

    val GOLD_BARS = Register(
        "gold_bars",
        ::IronBarsBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BARS)
            .mapColor(MapColor.COLOR_YELLOW)
    )

    val IRON_GRATE = Register(
        "iron_grate",
        ::WaterloggedTransparentBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.COLOR_GRAY)
            .noOcclusion()
    )

    val WROUGHT_IRON_GRATE = Register(
        "wrought_iron_grate",
        ::WaterloggedTransparentBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.COLOR_GRAY)
            .noOcclusion()
    )

    val COMPRESSED_STONE = Register(
        "compressed_stone",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
            .mapColor(MapColor.STONE)
    )

    val PYRITE = Register(
        "pyrite",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
            .mapColor(MapColor.GOLD)
    )

    val PYRITE_BRICKS = Register(
        "pyrite_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
            .mapColor(MapColor.GOLD)
    )

    val PYRITE_BRICK_SLAB = RegisterVariant(PYRITE_BRICKS, "slab", ::SlabBlock)
    val PYRITE_BRICK_SLAB_VERTICAL = RegisterVSlab("pyrite_bricks", PYRITE_BRICK_SLAB)
    val PYRITE_BRICK_STAIRS = RegisterStairs(PYRITE_BRICKS)
    val PYRITE_BRICK_WALL = RegisterVariant(PYRITE_BRICKS, "wall", ::WallBlock)

    val CHISELED_PYRITE_BRICKS = Register(
        "chiseled_pyrite_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
            .mapColor(MapColor.GOLD)
    )

    val DRIPSTONE_BRICKS = Register(
        "dripstone_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.DRIPSTONE_BLOCK)
            .mapColor(MapColor.TERRACOTTA_BROWN)
    )

    val DRIPSTONE_BRICK_SLAB = RegisterVariant(DRIPSTONE_BRICKS, "slab", ::SlabBlock)
    val DRIPSTONE_BRICK_SLAB_VERTICAL = RegisterVSlab("dripstone_bricks", DRIPSTONE_BRICK_SLAB)
    val DRIPSTONE_BRICK_STAIRS = RegisterStairs(DRIPSTONE_BRICKS)
    val DRIPSTONE_BRICK_WALL = RegisterVariant(DRIPSTONE_BRICKS, "wall", ::WallBlock)

    val CHARCOAL_BLOCK = Register(
        "charcoal_block",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.COAL_BLOCK)
            .mapColor(MapColor.TERRACOTTA_GRAY)
    )

    // =========================================================================
    //  Lanterns and Chains
    // =========================================================================
    val OCHRE_LANTERN = Register(
        "ochre_lantern",
        ::LanternBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
            .mapColor(MapColor.COLOR_ORANGE)
    )

    val OCHRE_CHAIN = Register(
        "ochre_chain",
        ::ChainBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN)
            .mapColor(MapColor.COLOR_GRAY)
    )

    val PEARLESCENT_LANTERN = Register(
        "pearlescent_lantern",
        ::LanternBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
            .mapColor(MapColor.CRIMSON_STEM)
    )

    val PEARLESCENT_CHAIN = Register(
        "pearlescent_chain",
        ::ChainBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN)
            .mapColor(MapColor.COLOR_GRAY)
    )

    val VERDANT_LANTERN = Register(
        "verdant_lantern",
        ::LanternBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN)
            .mapColor(MapColor.GRASS)
    )

    val VERDANT_CHAIN = Register(
        "verdant_chain",
        ::ChainBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CHAIN)
            .mapColor(MapColor.COLOR_GRAY)
    )

    val CHAINS_AND_LANTERNS = listOf(
        OCHRE_CHAIN to OCHRE_LANTERN,
        PEARLESCENT_CHAIN to PEARLESCENT_LANTERN,
        VERDANT_CHAIN to VERDANT_LANTERN
    )

    // =========================================================================
    //  Cinnabar Blocks
    // =========================================================================
    val CINNABAR = Register(
        "cinnabar",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.TUFF)
            .mapColor(MapColor.NETHER)
    )

    val CINNABAR_SLAB = RegisterVariant(CINNABAR, "slab", ::SlabBlock)
    val CINNABAR_SLAB_VERTICAL = RegisterVSlab("cinnabar", CINNABAR_SLAB)
    val CINNABAR_STAIRS = RegisterStairs(CINNABAR)

    val POLISHED_CINNABAR = Register(
        "polished_cinnabar",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
            .mapColor(MapColor.NETHER)
    )

    val POLISHED_CINNABAR_SLAB = RegisterVariant(POLISHED_CINNABAR, "slab", ::SlabBlock)
    val POLISHED_CINNABAR_SLAB_VERTICAL = RegisterVSlab("polished_cinnabar", POLISHED_CINNABAR_SLAB)
    val POLISHED_CINNABAR_STAIRS = RegisterStairs(POLISHED_CINNABAR)
    val POLISHED_CINNABAR_WALL = RegisterVariant(POLISHED_CINNABAR, "wall", ::WallBlock)

    val CINNABAR_BRICKS = Register(
        "cinnabar_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)
            .mapColor(MapColor.NETHER)
    )

    val CINNABAR_BRICK_SLAB = RegisterVariant(CINNABAR_BRICKS, "slab", ::SlabBlock)
    val CINNABAR_BRICK_SLAB_VERTICAL = RegisterVSlab("cinnabar_bricks", CINNABAR_BRICK_SLAB)
    val CINNABAR_BRICK_STAIRS = RegisterStairs(CINNABAR_BRICKS)
    val CINNABAR_BRICK_WALL = RegisterVariant(CINNABAR_BRICKS, "wall", ::WallBlock)

    // =========================================================================
    //  Calcite blocks
    // =========================================================================
    val POLISHED_CALCITE = Register(
        "polished_calcite",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val POLISHED_CALCITE_SLAB = RegisterVariant(POLISHED_CALCITE, "slab", ::SlabBlock)
    val POLISHED_CALCITE_SLAB_VERTICAL = RegisterVSlab("polished_calcite", POLISHED_CALCITE_SLAB)
    val POLISHED_CALCITE_STAIRS = RegisterStairs(POLISHED_CALCITE)
    val POLISHED_CALCITE_WALL = RegisterVariant(POLISHED_CALCITE, "wall", ::WallBlock)

    val CHISELED_CALCITE = Register(
        "chiseled_calcite",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val CALCITE_BRICKS = Register(
        "calcite_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val CALCITE_BRICK_SLAB = RegisterVariant(CALCITE_BRICKS, "slab", ::SlabBlock)
    val CALCITE_BRICK_SLAB_VERTICAL = RegisterVSlab("calcite_bricks", CALCITE_BRICK_SLAB)
    val CALCITE_BRICK_STAIRS = RegisterStairs(CALCITE_BRICKS)
    val CALCITE_BRICK_WALL = RegisterVariant(CALCITE_BRICKS, "wall", ::WallBlock)

    val CHISELED_CALCITE_BRICKS = Register(
        "chiseled_calcite_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    // =========================================================================
    //  Gilded calcite
    // =========================================================================
    val GILDED_CALCITE = Register(
        "gilded_calcite",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_CALCITE_SLAB = RegisterVariant(GILDED_CALCITE, "slab", ::SlabBlock)
    val GILDED_CALCITE_SLAB_VERTICAL = RegisterVSlab("gilded_calcite", GILDED_CALCITE_SLAB)
    val GILDED_CALCITE_STAIRS = RegisterStairs(GILDED_CALCITE)

    val GILDED_POLISHED_CALCITE = Register(
        "gilded_polished_calcite",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_POLISHED_CALCITE_SLAB = RegisterVariant(GILDED_POLISHED_CALCITE, "slab", ::SlabBlock)
    val GILDED_POLISHED_CALCITE_SLAB_VERTICAL = RegisterVSlab("gilded_polished_calcite", GILDED_POLISHED_CALCITE_SLAB)
    val GILDED_POLISHED_CALCITE_STAIRS = RegisterStairs(GILDED_POLISHED_CALCITE)
    val GILDED_POLISHED_CALCITE_WALL = RegisterVariant(GILDED_POLISHED_CALCITE, "wall", ::WallBlock)

    val GILDED_CHISELED_CALCITE = Register(
        "gilded_chiseled_calcite",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_CALCITE_BRICKS = Register(
        "gilded_calcite_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_CALCITE_BRICK_SLAB = RegisterVariant(GILDED_CALCITE_BRICKS, "slab", ::SlabBlock)
    val GILDED_CALCITE_BRICK_SLAB_VERTICAL = RegisterVSlab("gilded_calcite_bricks", GILDED_CALCITE_BRICK_SLAB)
    val GILDED_CALCITE_BRICK_STAIRS = RegisterStairs(GILDED_CALCITE_BRICKS)
    val GILDED_CALCITE_BRICK_WALL = RegisterVariant(GILDED_CALCITE_BRICKS, "wall", ::WallBlock)

    val GILDED_CHISELED_CALCITE_BRICKS = Register(
        "gilded_chiseled_calcite_bricks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    // =========================================================================
    //  Tinted Oak
    // =========================================================================
    val TINTED_OAK_BLOCK_SET_TYPE = BlockSetTypeBuilder.copyOf(BlockSetType.PALE_OAK).register(Id("tinted_oak"))
    val TINTED_OAK_WOOD_TYPE = WoodTypeBuilder.copyOf(WoodType.PALE_OAK).register(Id("tinted_oak"), TINTED_OAK_BLOCK_SET_TYPE)

    val TINTED_OAK_PLANKS = Register(
        "tinted_oak_planks",
        ::Block,
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_PLANKS)
            .mapColor(MapColor.ICE)
    )

    val TINTED_OAK_SLAB = RegisterVariant(TINTED_OAK_PLANKS, "slab", ::SlabBlock)
    val TINTED_OAK_SLAB_VERTICAL = RegisterVSlab("tinted_oak", TINTED_OAK_SLAB)
    val TINTED_OAK_STAIRS = RegisterStairs(TINTED_OAK_PLANKS)
    val TINTED_OAK_FENCE = RegisterVariant(TINTED_OAK_PLANKS, "fence", ::FenceBlock)

    val TINTED_OAK_FENCE_GATE = Register(
        "tinted_oak_fence_gate",
        { s -> FenceGateBlock(TINTED_OAK_WOOD_TYPE, s) },
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_FENCE_GATE).mapColor(MapColor.ICE)
    )

    val TINTED_OAK_DOOR = Register(
        "tinted_oak_door",
        { s -> DoorBlock(TINTED_OAK_BLOCK_SET_TYPE, s) },
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_DOOR).mapColor(MapColor.ICE)
    )

    val TINTED_OAK_TRAPDOOR = Register(
        "tinted_oak_trapdoor",
        { s -> TrapDoorBlock(TINTED_OAK_BLOCK_SET_TYPE, s) },
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_TRAPDOOR).mapColor(MapColor.ICE)
    )

    val TINTED_OAK_PRESSURE_PLATE = Register(
        "tinted_oak_pressure_plate",
        { s -> PressurePlateBlock(TINTED_OAK_BLOCK_SET_TYPE, s) },
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_PRESSURE_PLATE).mapColor(MapColor.ICE)
    )

    val TINTED_OAK_BUTTON = Register(
        "tinted_oak_button",
        { s -> ButtonBlock(TINTED_OAK_BLOCK_SET_TYPE, 30, s) },
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_BUTTON).mapColor(MapColor.ICE)
    )

    val TINTED_OAK_LOG = Register(
        "tinted_oak_log",
        ::RotatedPillarBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_LOG).mapColor(MapColor.ICE)
    )

    val TINTED_OAK_WOOD = Register(
        "tinted_oak_wood",
        ::RotatedPillarBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_WOOD).mapColor(MapColor.ICE)
    )

    val STRIPPED_TINTED_OAK_LOG = Register(
        "stripped_tinted_oak_log",
        ::RotatedPillarBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_LOG).mapColor(MapColor.ICE)
    )

    val STRIPPED_TINTED_OAK_WOOD = Register(
        "stripped_tinted_oak_wood",
        ::RotatedPillarBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_WOOD).mapColor(MapColor.ICE)
    )

    // =========================================================================
    //  New Crops
    // =========================================================================
    val GRAPE_CROP = RegisterWithoutItem(
        "grape_crop",
        ::GrapeCropBlock,
        BlockBehaviour.Properties.of()
            .mapColor(MapColor.PLANT)
            .noCollission()
            .randomTicks()
            .instabreak()
            .pushReaction(PushReaction.PUSH_ONLY)
            .sound(SoundType.CROP)
    )

    val PEANUT_CROP = RegisterWithoutItem(
        "peanut_crop",
        ::PeanutCropBlock,
        BlockBehaviour.Properties.ofFullCopy(Blocks.POTATOES)
    )

    // =========================================================================
    //  Block entities
    // =========================================================================
    val LOCKED_DOOR_BLOCK_ENTITY = RegisterEntity(
        "lockable_door",
        FabricBlockEntityTypeBuilder
            .create(::LockedDoorBlockEntity, LOCKED_DOOR)
            .build()
    )

    // =========================================================================
    //  Block families
    // =========================================================================
    val CINNABAR_FAMILY: BlockFamily = BlockFamilies.familyBuilder(CINNABAR)
        .polished(POLISHED_CINNABAR)
        .slab(CINNABAR_SLAB)
        .stairs(CINNABAR_STAIRS)
        .family

    val POLISHED_CINNABAR_FAMILY: BlockFamily = BlockFamilies.familyBuilder(POLISHED_CINNABAR)
        .slab(POLISHED_CINNABAR_SLAB)
        .stairs(POLISHED_CINNABAR_STAIRS)
        .wall(POLISHED_CINNABAR_WALL)
        .family

    val CINNABAR_BRICK_FAMILY: BlockFamily = BlockFamilies.familyBuilder(CINNABAR_BRICKS)
        .slab(CINNABAR_BRICK_SLAB)
        .stairs(CINNABAR_BRICK_STAIRS)
        .wall(CINNABAR_BRICK_WALL)
        .family

    val PYRITE_BRICK_FAMILY: BlockFamily = BlockFamilies.familyBuilder(PYRITE_BRICKS)
        .slab(PYRITE_BRICK_SLAB)
        .stairs(PYRITE_BRICK_STAIRS)
        .wall(PYRITE_BRICK_WALL)
        .chiseled(CHISELED_PYRITE_BRICKS)
        .family

    val DRIPSTONE_BRICK_FAMILY: BlockFamily = BlockFamilies.familyBuilder(DRIPSTONE_BRICKS)
        .slab(DRIPSTONE_BRICK_SLAB)
        .stairs(DRIPSTONE_BRICK_STAIRS)
        .wall(DRIPSTONE_BRICK_WALL)
        .family

    val POLISHED_CALCITE_FAMILY: BlockFamily = BlockFamilies.familyBuilder(POLISHED_CALCITE)
        .slab(POLISHED_CALCITE_SLAB)
        .stairs(POLISHED_CALCITE_STAIRS)
        .wall(POLISHED_CALCITE_WALL)
        .chiseled(CHISELED_CALCITE)
        .family

    val CALCITE_BRICK_FAMILY: BlockFamily = BlockFamilies.familyBuilder(CALCITE_BRICKS)
        .slab(CALCITE_BRICK_SLAB)
        .stairs(CALCITE_BRICK_STAIRS)
        .wall(CALCITE_BRICK_WALL)
        .chiseled(CHISELED_CALCITE_BRICKS)
        .family

    val GILDED_CALCITE_FAMILY: BlockFamily = BlockFamilies.familyBuilder(GILDED_CALCITE)
        .polished(GILDED_POLISHED_CALCITE)
        .slab(GILDED_CALCITE_SLAB)
        .stairs(GILDED_CALCITE_STAIRS)
        .family

    val GILDED_POLISHED_CALCITE_FAMILY: BlockFamily = BlockFamilies.familyBuilder(GILDED_POLISHED_CALCITE)
        .slab(GILDED_POLISHED_CALCITE_SLAB)
        .stairs(GILDED_POLISHED_CALCITE_STAIRS)
        .wall(GILDED_POLISHED_CALCITE_WALL)
        .chiseled(GILDED_CHISELED_CALCITE)
        .family

    val GILDED_CALCITE_BRICK_FAMILY: BlockFamily = BlockFamilies.familyBuilder(GILDED_CALCITE_BRICKS)
        .slab(GILDED_CALCITE_BRICK_SLAB)
        .stairs(GILDED_CALCITE_BRICK_STAIRS)
        .wall(GILDED_CALCITE_BRICK_WALL)
        .chiseled(GILDED_CHISELED_CALCITE_BRICKS)
        .family

    val TINTED_OAK_FAMILY: BlockFamily = BlockFamilies.familyBuilder(TINTED_OAK_PLANKS)
        .slab(TINTED_OAK_SLAB)
        .stairs(TINTED_OAK_STAIRS)
        .fence(TINTED_OAK_FENCE)
        .fenceGate(TINTED_OAK_FENCE_GATE)
        .door(TINTED_OAK_DOOR)
        .trapdoor(TINTED_OAK_TRAPDOOR)
        .pressurePlate(TINTED_OAK_PRESSURE_PLATE)
        .button(TINTED_OAK_BUTTON)
        .family

    val CINNABAR_FAMILIES = listOf(CINNABAR_FAMILY, POLISHED_CINNABAR_FAMILY, CINNABAR_BRICK_FAMILY)
    val CALCITE_FAMILIES = listOf(POLISHED_CALCITE_FAMILY, CALCITE_BRICK_FAMILY)
    val GILDED_CALCITE_FAMILIES = listOf(GILDED_CALCITE_FAMILY, GILDED_POLISHED_CALCITE_FAMILY, GILDED_CALCITE_BRICK_FAMILY)
    val STONE_FAMILY_GROUPS = listOf(CINNABAR_FAMILIES, CALCITE_FAMILIES, GILDED_CALCITE_FAMILIES)

    val STONE_VARIANT_FAMILIES = arrayOf(
        CINNABAR_FAMILY,
        POLISHED_CINNABAR_FAMILY,
        CINNABAR_BRICK_FAMILY,
        POLISHED_CALCITE_FAMILY,
        CALCITE_BRICK_FAMILY,
        GILDED_CALCITE_FAMILY,
        GILDED_POLISHED_CALCITE_FAMILY,
        GILDED_CALCITE_BRICK_FAMILY,
        PYRITE_BRICK_FAMILY,
        DRIPSTONE_BRICK_FAMILY
    )

    val WOOD_VARIANT_FAMILIES = arrayOf(
        TINTED_OAK_FAMILY
    )

    val ALL_VARIANT_FAMILIES = STONE_VARIANT_FAMILIES + WOOD_VARIANT_FAMILIES

    val STONE_VARIANT_FAMILY_BLOCKS = mutableSetOf<Block>().also {
        for (F in STONE_VARIANT_FAMILIES) {
            it.add(F.baseBlock)
            it.addAll(F.variants.values)
        }
    }.toTypedArray()

    val WOOD_VARIANT_FAMILY_BLOCKS = mutableSetOf<Block>().also {
        for (F in WOOD_VARIANT_FAMILIES) {
            it.add(F.baseBlock)
            it.addAll(F.variants.values)
        }
    }.toTypedArray()

    val ALL_VARIANT_FAMILY_BLOCKS = STONE_VARIANT_FAMILY_BLOCKS + WOOD_VARIANT_FAMILY_BLOCKS

    // Information about a wood family and the logs and wood blocks that belong to it.
    val VANILLA_AND_NGUHCRAFT_EXTENDED_WOOD_FAMILIES = arrayOf(
        WoodFamily(
            BlockFamilies.ACACIA_PLANKS,
            Log = Blocks.ACACIA_LOG,
            Wood = Blocks.ACACIA_WOOD,
            StrippedLog = Blocks.STRIPPED_ACACIA_LOG,
            StrippedWood = Blocks.STRIPPED_ACACIA_WOOD,
        ),

        WoodFamily(
            BlockFamilies.BIRCH_PLANKS,
            Log = Blocks.BIRCH_LOG,
            Wood = Blocks.BIRCH_WOOD,
            StrippedLog = Blocks.STRIPPED_BIRCH_LOG,
            StrippedWood = Blocks.STRIPPED_BIRCH_WOOD,
        ),

        WoodFamily(
            BlockFamilies.CHERRY_PLANKS,
            Log = Blocks.CHERRY_LOG,
            Wood = Blocks.CHERRY_WOOD,
            StrippedLog = Blocks.STRIPPED_CHERRY_LOG,
            StrippedWood = Blocks.STRIPPED_CHERRY_WOOD,
        ),

        WoodFamily(
            BlockFamilies.CRIMSON_PLANKS,
            Log = Blocks.CRIMSON_STEM,
            Wood = Blocks.CRIMSON_HYPHAE,
            StrippedLog = Blocks.STRIPPED_CRIMSON_STEM,
            StrippedWood = Blocks.STRIPPED_CRIMSON_HYPHAE,
        ),

        WoodFamily(
            BlockFamilies.DARK_OAK_PLANKS,
            Log = Blocks.DARK_OAK_LOG,
            Wood = Blocks.DARK_OAK_WOOD,
            StrippedLog = Blocks.STRIPPED_DARK_OAK_LOG,
            StrippedWood = Blocks.STRIPPED_DARK_OAK_WOOD,
        ),

        WoodFamily(
            BlockFamilies.JUNGLE_PLANKS,
            Log = Blocks.JUNGLE_LOG,
            Wood = Blocks.JUNGLE_WOOD,
            StrippedLog = Blocks.STRIPPED_JUNGLE_LOG,
            StrippedWood = Blocks.STRIPPED_JUNGLE_WOOD,
        ),

        WoodFamily(
            BlockFamilies.MANGROVE_PLANKS,
            Log = Blocks.MANGROVE_LOG,
            Wood = Blocks.MANGROVE_WOOD,
            StrippedLog = Blocks.STRIPPED_MANGROVE_LOG,
            StrippedWood = Blocks.STRIPPED_MANGROVE_WOOD,
        ),

        WoodFamily(
            BlockFamilies.OAK_PLANKS,
            Log = Blocks.OAK_LOG,
            Wood = Blocks.OAK_WOOD,
            StrippedLog = Blocks.STRIPPED_OAK_LOG,
            StrippedWood = Blocks.STRIPPED_OAK_WOOD,
        ),

        WoodFamily(
            BlockFamilies.PALE_OAK_PLANKS,
            Log = Blocks.PALE_OAK_LOG,
            Wood = Blocks.PALE_OAK_WOOD,
            StrippedLog = Blocks.STRIPPED_PALE_OAK_LOG,
            StrippedWood = Blocks.STRIPPED_PALE_OAK_WOOD,
        ),

        WoodFamily(
            BlockFamilies.SPRUCE_PLANKS,
            Log = Blocks.SPRUCE_LOG,
            Wood = Blocks.SPRUCE_WOOD,
            StrippedLog = Blocks.STRIPPED_SPRUCE_LOG,
            StrippedWood = Blocks.STRIPPED_SPRUCE_WOOD,
        ),

        WoodFamily(
            BlockFamilies.WARPED_PLANKS,
            Log = Blocks.WARPED_STEM,
            Wood = Blocks.WARPED_HYPHAE,
            StrippedLog = Blocks.STRIPPED_WARPED_STEM,
            StrippedWood = Blocks.STRIPPED_WARPED_HYPHAE,
        ),

        WoodFamily(
            TINTED_OAK_FAMILY,
            Log = TINTED_OAK_LOG,
            Wood = TINTED_OAK_WOOD,
            StrippedLog = STRIPPED_TINTED_OAK_LOG,
            StrippedWood = STRIPPED_TINTED_OAK_WOOD,
        ),
    )

    // =========================================================================
    //  Vertical Slabs for Vanilla Blocks
    // =========================================================================
    val ACACIA_SLAB_VERTICAL = RegisterVSlab("acacia", Blocks.ACACIA_SLAB)
    val ANDESITE_SLAB_VERTICAL = RegisterVSlab("andesite", Blocks.ANDESITE_SLAB)
    val BAMBOO_MOSAIC_SLAB_VERTICAL = RegisterVSlab("bamboo_mosaic", Blocks.BAMBOO_MOSAIC_SLAB)
    val BAMBOO_SLAB_VERTICAL = RegisterVSlab("bamboo", Blocks.BAMBOO_SLAB)
    val BIRCH_SLAB_VERTICAL = RegisterVSlab("birch", Blocks.BIRCH_SLAB)
    val BLACKSTONE_SLAB_VERTICAL = RegisterVSlab("blackstone", Blocks.BLACKSTONE_SLAB)
    val BRICK_SLAB_VERTICAL = RegisterVSlab("brick", Blocks.BRICK_SLAB)
    val CHERRY_SLAB_VERTICAL = RegisterVSlab("cherry", Blocks.CHERRY_SLAB)
    val COBBLED_DEEPSLATE_SLAB_VERTICAL = RegisterVSlab("cobbled_deepslate", Blocks.COBBLED_DEEPSLATE_SLAB)
    val COBBLESTONE_SLAB_VERTICAL = RegisterVSlab("cobblestone", Blocks.COBBLESTONE_SLAB)
    val CRIMSON_SLAB_VERTICAL = RegisterVSlab("crimson", Blocks.CRIMSON_SLAB)
    val CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("cut_copper", Blocks.CUT_COPPER_SLAB)
    val CUT_RED_SANDSTONE_SLAB_VERTICAL = RegisterVSlab("cut_red_sandstone", Blocks.CUT_RED_SANDSTONE_SLAB)
    val CUT_SANDSTONE_SLAB_VERTICAL = RegisterVSlab("cut_sandstone", Blocks.CUT_SANDSTONE_SLAB)
    val DARK_OAK_SLAB_VERTICAL = RegisterVSlab("dark_oak", Blocks.DARK_OAK_SLAB)
    val DARK_PRISMARINE_SLAB_VERTICAL = RegisterVSlab("dark_prismarine", Blocks.DARK_PRISMARINE_SLAB)
    val DEEPSLATE_BRICK_SLAB_VERTICAL = RegisterVSlab("deepslate_brick", Blocks.DEEPSLATE_BRICK_SLAB)
    val DEEPSLATE_TILE_SLAB_VERTICAL = RegisterVSlab("deepslate_tile", Blocks.DEEPSLATE_TILE_SLAB)
    val DIORITE_SLAB_VERTICAL = RegisterVSlab("diorite", Blocks.DIORITE_SLAB)
    val END_STONE_BRICK_SLAB_VERTICAL = RegisterVSlab("end_stone_brick", Blocks.END_STONE_BRICK_SLAB)
    val EXPOSED_CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("exposed_cut_copper", Blocks.EXPOSED_CUT_COPPER_SLAB)
    val GRANITE_SLAB_VERTICAL = RegisterVSlab("granite", Blocks.GRANITE_SLAB)
    val JUNGLE_SLAB_VERTICAL = RegisterVSlab("jungle", Blocks.JUNGLE_SLAB)
    val MANGROVE_SLAB_VERTICAL = RegisterVSlab("mangrove", Blocks.MANGROVE_SLAB)
    val MOSSY_COBBLESTONE_SLAB_VERTICAL = RegisterVSlab("mossy_cobblestone", Blocks.MOSSY_COBBLESTONE_SLAB)
    val MOSSY_STONE_BRICK_SLAB_VERTICAL = RegisterVSlab("mossy_stone_brick", Blocks.MOSSY_STONE_BRICK_SLAB)
    val MUD_BRICK_SLAB_VERTICAL = RegisterVSlab("mud_brick", Blocks.MUD_BRICK_SLAB)
    val NETHER_BRICK_SLAB_VERTICAL = RegisterVSlab("nether_brick", Blocks.NETHER_BRICK_SLAB)
    val OAK_SLAB_VERTICAL = RegisterVSlab("oak", Blocks.OAK_SLAB)
    val OXIDIZED_CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("oxidized_cut_copper", Blocks.OXIDIZED_CUT_COPPER_SLAB)
    val PALE_OAK_SLAB_VERTICAL = RegisterVSlab("pale_oak", Blocks.PALE_OAK_SLAB)
    val POLISHED_ANDESITE_SLAB_VERTICAL = RegisterVSlab("polished_andesite", Blocks.POLISHED_ANDESITE_SLAB)
    val POLISHED_BLACKSTONE_BRICK_SLAB_VERTICAL = RegisterVSlab("polished_blackstone_brick", Blocks.POLISHED_BLACKSTONE_BRICK_SLAB)
    val POLISHED_BLACKSTONE_SLAB_VERTICAL = RegisterVSlab("polished_blackstone", Blocks.POLISHED_BLACKSTONE_SLAB)
    val POLISHED_DEEPSLATE_SLAB_VERTICAL = RegisterVSlab("polished_deepslate", Blocks.POLISHED_DEEPSLATE_SLAB)
    val POLISHED_DIORITE_SLAB_VERTICAL = RegisterVSlab("polished_diorite", Blocks.POLISHED_DIORITE_SLAB)
    val POLISHED_GRANITE_SLAB_VERTICAL = RegisterVSlab("polished_granite", Blocks.POLISHED_GRANITE_SLAB)
    val POLISHED_TUFF_SLAB_VERTICAL = RegisterVSlab("polished_tuff", Blocks.POLISHED_TUFF_SLAB)
    val PRISMARINE_BRICK_SLAB_VERTICAL = RegisterVSlab("prismarine_brick", Blocks.PRISMARINE_BRICK_SLAB)
    val PRISMARINE_SLAB_VERTICAL = RegisterVSlab("prismarine", Blocks.PRISMARINE_SLAB)
    val PURPUR_SLAB_VERTICAL = RegisterVSlab("purpur", Blocks.PURPUR_SLAB)
    val QUARTZ_SLAB_VERTICAL = RegisterVSlab("quartz", Blocks.QUARTZ_SLAB)
    val RED_NETHER_BRICK_SLAB_VERTICAL = RegisterVSlab("red_nether_brick", Blocks.RED_NETHER_BRICK_SLAB)
    val RED_SANDSTONE_SLAB_VERTICAL = RegisterVSlab("red_sandstone", Blocks.RED_SANDSTONE_SLAB)
    val SANDSTONE_SLAB_VERTICAL = RegisterVSlab("sandstone", Blocks.SANDSTONE_SLAB)
    val SMOOTH_QUARTZ_SLAB_VERTICAL = RegisterVSlab("smooth_quartz", Blocks.SMOOTH_QUARTZ_SLAB)
    val SMOOTH_RED_SANDSTONE_SLAB_VERTICAL = RegisterVSlab("smooth_red_sandstone", Blocks.SMOOTH_RED_SANDSTONE_SLAB)
    val SMOOTH_SANDSTONE_SLAB_VERTICAL = RegisterVSlab("smooth_sandstone", Blocks.SMOOTH_SANDSTONE_SLAB)
    val SMOOTH_STONE_SLAB_VERTICAL = RegisterVSlab("smooth_stone", Blocks.SMOOTH_STONE_SLAB)
    val SPRUCE_SLAB_VERTICAL = RegisterVSlab("spruce", Blocks.SPRUCE_SLAB)
    val STONE_BRICK_SLAB_VERTICAL = RegisterVSlab("stone_brick", Blocks.STONE_BRICK_SLAB)
    val STONE_SLAB_VERTICAL = RegisterVSlab("stone", Blocks.STONE_SLAB)
    val TUFF_BRICK_SLAB_VERTICAL = RegisterVSlab("tuff_brick", Blocks.TUFF_BRICK_SLAB)
    val TUFF_SLAB_VERTICAL = RegisterVSlab("tuff", Blocks.TUFF_SLAB)
    val WARPED_SLAB_VERTICAL = RegisterVSlab("warped", Blocks.WARPED_SLAB)
    val WAXED_CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("waxed_cut_copper", Blocks.WAXED_CUT_COPPER_SLAB)
    val WAXED_EXPOSED_CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("waxed_exposed_cut_copper", Blocks.WAXED_EXPOSED_CUT_COPPER_SLAB)
    val WAXED_OXIDIZED_CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("waxed_oxidized_cut_copper", Blocks.WAXED_OXIDIZED_CUT_COPPER_SLAB)
    val WAXED_WEATHERED_CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("waxed_weathered_cut_copper", Blocks.WAXED_WEATHERED_CUT_COPPER_SLAB)
    val WEATHERED_CUT_COPPER_SLAB_VERTICAL = RegisterVSlab("weathered_cut_copper", Blocks.WEATHERED_CUT_COPPER_SLAB)

    // =========================================================================
    // Tags
    // =========================================================================
    // Note: These are seemingly randomly shuffled everytime datagen runs; I have
    // no idea why, but they all seem to be there so I don’t care.
    //
    // Note: Tags for vertical slabs are set directly in the datagen code.
    val PICKAXE_MINEABLE = mutableSetOf(
        DECORATIVE_HOPPER,
        LOCKED_DOOR,
        WROUGHT_IRON_BLOCK,
        WROUGHT_IRON_BARS,
        GOLD_BARS,
        COMPRESSED_STONE,
        PYRITE,
        IRON_GRATE,
        WROUGHT_IRON_GRATE,
        CHARCOAL_BLOCK
    ).also {
        it.addAll(CHAINS_AND_LANTERNS.flatten())
        it.addAll(STONE_VARIANT_FAMILY_BLOCKS)
    }.toTypedArray()

    val DROPS_SELF = mutableSetOf(
        DECORATIVE_HOPPER,
        WROUGHT_IRON_BLOCK,
        WROUGHT_IRON_BARS,
        GOLD_BARS,
        COMPRESSED_STONE,
        PYRITE,
        TINTED_OAK_LOG,
        TINTED_OAK_WOOD,
        STRIPPED_TINTED_OAK_LOG,
        STRIPPED_TINTED_OAK_WOOD,
        IRON_GRATE,
        WROUGHT_IRON_GRATE,
        CHARCOAL_BLOCK
    ).also {
        it.addAll(CHAINS_AND_LANTERNS.flatten())
        it.addAll(ALL_BROCADE_BLOCKS)

        // Slabs may drop 2 or 1 and are thus handled separately. Same for doors
        it.addAll(ALL_VARIANT_FAMILY_BLOCKS.filter { it !is SlabBlock && it !is DoorBlock })
    }.toTypedArray()

    @JvmField
    val CAN_DUPLICATE_WITH_BONEMEAL = TagKey.create(Registries.BLOCK, Id("can_duplicate_with_bonemeal"))

    // =========================================================================
    //  Initialisation
    // =========================================================================
    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register {
            it.accept(DECORATIVE_HOPPER)
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register {
            it.accept(LOCKED_DOOR)
            it.accept(COMPRESSED_STONE)
            it.accept(WROUGHT_IRON_BLOCK)
            it.accept(WROUGHT_IRON_BARS)
            it.accept(IRON_GRATE)
            it.accept(WROUGHT_IRON_GRATE)
            it.accept(GOLD_BARS)
            it.accept(PYRITE)
            it.accept(TINTED_OAK_LOG)
            it.accept(TINTED_OAK_WOOD)
            it.accept(STRIPPED_TINTED_OAK_LOG)
            it.accept(STRIPPED_TINTED_OAK_WOOD)
            it.accept(CHARCOAL_BLOCK)
            for (B in ALL_VARIANT_FAMILY_BLOCKS) it.accept(B)
            for (B in VERTICAL_SLABS) it.accept(B)
            for (B in ALL_BROCADE_BLOCKS) it.accept(B)
        }

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register {
            for (B in CHAINS_AND_LANTERNS.flatten()) it.accept(B)
        }

        RegisterStrippable(TINTED_OAK_LOG, STRIPPED_TINTED_OAK_LOG)
        RegisterStrippable(TINTED_OAK_WOOD, STRIPPED_TINTED_OAK_WOOD)

        for (B in WOOD_VARIANT_FAMILY_BLOCKS) {
            if (B !is DoorBlock && B !is TrapDoorBlock && B !is PressurePlateBlock && B !is ButtonBlock) {
                RegisterFlammable(B, 5, 5)
            }
        }
        RegisterFlammable(TINTED_OAK_LOG, 5, 5)
        RegisterFlammable(TINTED_OAK_WOOD, 5, 5)
        RegisterFlammable(STRIPPED_TINTED_OAK_LOG, 5, 5)
        RegisterFlammable(STRIPPED_TINTED_OAK_WOOD, 5, 5)
    }

    @Suppress("DEPRECATION")
    private fun RegisterVariant(
        Parent: Block,
        Suffix: String,
        Ctor: (BlockBehaviour.Properties) -> Block
    ) = Register(
        "${BuiltInRegistries.BLOCK.getResourceKey(Parent).get().location().path}_$Suffix",
        Ctor,
        BlockBehaviour.Properties.ofLegacyCopy(Parent)
    )

    @Suppress("DEPRECATION")
    private fun RegisterStairs(Parent: Block) = Register(
        "${BuiltInRegistries.BLOCK.getResourceKey(Parent).get().location().path}_stairs",
        { StairBlock(Parent.defaultBlockState(), it) },
        BlockBehaviour.Properties.ofLegacyCopy(Parent)
    )

    private fun <T : Block> RegisterWithoutItem(
        Key: String,
        Ctor: (S: BlockBehaviour.Properties) -> T,
        S: BlockBehaviour.Properties
    ): T {
        // Create registry key.
        val BlockKey = ResourceKey.create(Registries.BLOCK, Id(Key))

        // Set the registry key for the block settings.
        S.setId(BlockKey)

        // Create and register the block.
        val B = Ctor(S)
        Registry.register(BuiltInRegistries.BLOCK, BlockKey, B)
        return B
    }

    private fun <T : Block> Register(
        Key: String,
        Ctor: (S: BlockBehaviour.Properties) -> T,
        S: BlockBehaviour.Properties,
        ItemCtor: (B: Block, S: Item.Properties) -> Item = ::BlockItem
    ): T {
        // Create registry key.
        val ItemKey = ResourceKey.create(Registries.ITEM, Id(Key))

        // Create and register the block.
        val B = RegisterWithoutItem(Key, Ctor, S)

        // Create and register the item.
        val ItemSettings = Item.Properties()
            .useBlockDescriptionPrefix()
            .setId(ItemKey)
        val I = ItemCtor(B, ItemSettings)
        Registry.register(BuiltInRegistries.ITEM, ItemKey, I)
        return B
    }

    private fun <C : BlockEntity> RegisterEntity(
        Key: String,
        Type: BlockEntityType<C>
    ): BlockEntityType<C> = Registry.register(
        BuiltInRegistries.BLOCK_ENTITY_TYPE,
        Id(Key),
        Type
    )

    fun RegisterVSlab(Name: String, SlabBlock: Block) = Register(
        "${Name}_slab_vertical",
        ::VerticalSlabBlock,
        BlockBehaviour.Properties.ofFullCopy(SlabBlock)
    ).also { (VERTICAL_SLABS as MutableList).add(it) }

    fun RegisterStrippable(L: Block, S: Block) = StrippableBlockRegistry.register(L, S)

    fun RegisterFlammable(b: Block, burn: Int, spread: Int) = FlammableBlockRegistry.getDefaultInstance().add(b, burn, spread)
}
