package org.nguh.nguhcraft.block

import com.mojang.serialization.Codec
import io.netty.buffer.ByteBuf
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents
import net.fabricmc.fabric.api.`object`.builder.v1.block.entity.FabricBlockEntityTypeBuilder
import net.fabricmc.fabric.api.`object`.builder.v1.block.type.BlockSetTypeBuilder
import net.fabricmc.fabric.api.`object`.builder.v1.block.type.WoodTypeBuilder
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry
import net.fabricmc.fabric.api.registry.StrippableBlockRegistry
import net.minecraft.block.*
import net.minecraft.block.entity.BlockEntity
import net.minecraft.block.entity.BlockEntityType
import net.minecraft.block.piston.PistonBehavior
import net.minecraft.component.ComponentType
import net.minecraft.data.family.BlockFamilies
import net.minecraft.data.family.BlockFamily
import net.minecraft.item.BlockItem
import net.minecraft.item.Item
import net.minecraft.item.ItemGroups
import net.minecraft.network.codec.PacketCodec
import net.minecraft.network.codec.PacketCodecs
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.text.Style
import net.minecraft.text.Text
import net.minecraft.util.StringIdentifiable
import net.minecraft.util.function.ValueLists
import org.nguh.nguhcraft.Nguhcraft.Companion.Id
import org.nguh.nguhcraft.flatten
import java.util.function.IntFunction

enum class ChestVariant : StringIdentifiable {
    CHRISTMAS,
    PALE_OAK;

    val DefaultName: Text = Text.translatable("chest_variant.nguhcraft.${asString()}")
        .setStyle(Style.EMPTY.withItalic(false))

    override fun asString() = name.lowercase()

    companion object {
        val BY_ID: IntFunction<ChestVariant> = ValueLists.createIndexToValueFunction(
            ChestVariant::ordinal,
            entries.toTypedArray(),
            ValueLists.OutOfBoundsHandling.ZERO
        )

        val CODEC: Codec<ChestVariant> = StringIdentifiable.createCodec(ChestVariant::values)
        val PACKET_CODEC: PacketCodec<ByteBuf, ChestVariant> = PacketCodecs.indexed(BY_ID, ChestVariant::ordinal)
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
    val CHEST_VARIANT_COMPONENT: ComponentType<ChestVariant> = Registry.register(
        Registries.DATA_COMPONENT_TYPE,
        CHEST_VARIANT_ID,
        ComponentType.builder<ChestVariant>()
            .codec(ChestVariant.CODEC)
            .packetCodec(ChestVariant.PACKET_CODEC)
            .build()
    )

    // All vertical slabs; this has to be declared before our custom block families.
    val VERTICAL_SLABS: List<VerticalSlabBlock> = mutableListOf()
    val SLABS_VSLABS_MAP: MutableMap<String, Block> = mutableMapOf()

    // =========================================================================
    //  Brocade Blocks
    // =========================================================================
    val BROCADE_WHITE = Register(
        "brocade_white",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.WHITE_WOOL)
    )

    val BROCADE_LIGHT_GREY = Register(
        "brocade_light_grey",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.LIGHT_GRAY_WOOL)
    )

    val BROCADE_GREY = Register(
        "brocade_grey",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.GRAY_WOOL)
    )

    val BROCADE_BLACK = Register(
        "brocade_black",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.BLACK_WOOL)
    )

    val BROCADE_BROWN = Register(
        "brocade_brown",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.BROWN_WOOL)
    )

    val BROCADE_RED = Register(
        "brocade_red",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.RED_WOOL)
    )

    val BROCADE_ORANGE = Register(
        "brocade_orange",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.ORANGE_WOOL)
    )

    val BROCADE_YELLOW = Register(
        "brocade_yellow",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.YELLOW_WOOL)
    )

    val BROCADE_LIME = Register(
        "brocade_lime",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.LIME_WOOL)
    )

    val BROCADE_GREEN = Register(
        "brocade_green",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.GREEN_WOOL)
    )

    val BROCADE_CYAN = Register(
        "brocade_cyan",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CYAN_WOOL)
    )

    val BROCADE_LIGHT_BLUE = Register(
        "brocade_light_blue",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.LIGHT_BLUE_WOOL)
    )

    val BROCADE_BLUE = Register(
        "brocade_blue",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.BLUE_WOOL)
    )

    val BROCADE_PURPLE = Register(
        "brocade_purple",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.PURPLE_WOOL)
    )

    val BROCADE_MAGENTA = Register(
        "brocade_magenta",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.MAGENTA_WOOL)
    )

    val BROCADE_PINK = Register(
        "brocade_pink",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.PINK_WOOL)
    )

    val ALL_BROCADE_BLOCKS = arrayOf(
        BROCADE_WHITE,
        BROCADE_LIGHT_GREY,
        BROCADE_GREY,
        BROCADE_BLACK,
        BROCADE_BROWN,
        BROCADE_RED,
        BROCADE_ORANGE,
        BROCADE_YELLOW,
        BROCADE_LIME,
        BROCADE_GREEN,
        BROCADE_CYAN,
        BROCADE_LIGHT_BLUE,
        BROCADE_BLUE,
        BROCADE_PURPLE,
        BROCADE_MAGENTA,
        BROCADE_PINK
    )

    // =========================================================================
    //  Miscellaneous Blocks
    // =========================================================================
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

    val IRON_GRATE = Register(
        "iron_grate",
        ::GrateBlock,
        AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.GRAY)
            .nonOpaque()
    )

    val WROUGHT_IRON_GRATE = Register(
        "wrought_iron_grate",
        ::GrateBlock,
        AbstractBlock.Settings.copy(Blocks.IRON_BLOCK)
            .mapColor(MapColor.GRAY)
            .nonOpaque()
    )

    val COMPRESSED_STONE = Register(
        "compressed_stone",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.STONE)
            .mapColor(MapColor.STONE_GRAY)
    )

    val PYRITE = Register(
        "pyrite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.GOLD_BLOCK)
            .mapColor(MapColor.GOLD)
    )

    val PYRITE_BRICKS = Register(
        "pyrite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.GOLD_BLOCK)
            .mapColor(MapColor.GOLD)
    )

    val PYRITE_BRICK_SLAB = RegisterVariant(PYRITE_BRICKS, "slab", ::SlabBlock)
    val PYRITE_BRICK_SLAB_VERTICAL = RegisterVSlab("pyrite_bricks", PYRITE_BRICK_SLAB)
    val PYRITE_BRICK_STAIRS = RegisterStairs(PYRITE_BRICKS)
    val PYRITE_BRICK_WALL = RegisterVariant(PYRITE_BRICKS, "wall", ::WallBlock)

    val CHISELED_PYRITE_BRICKS = Register(
        "chiseled_pyrite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.GOLD_BLOCK)
            .mapColor(MapColor.GOLD)
    )

    val DRIPSTONE_BRICKS = Register(
        "dripstone_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.DRIPSTONE_BLOCK)
            .mapColor(MapColor.TERRACOTTA_BROWN)
    )

    val DRIPSTONE_BRICK_SLAB = RegisterVariant(DRIPSTONE_BRICKS, "slab", ::SlabBlock)
    val DRIPSTONE_BRICK_SLAB_VERTICAL = RegisterVSlab("dripstone_bricks", DRIPSTONE_BRICK_SLAB)
    val DRIPSTONE_BRICK_STAIRS = RegisterStairs(DRIPSTONE_BRICKS)
    val DRIPSTONE_BRICK_WALL = RegisterVariant(DRIPSTONE_BRICKS, "wall", ::WallBlock)

    val CHARCOAL_BLOCK = Register(
        "charcoal_block",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.COAL_BLOCK)
            .mapColor(MapColor.TERRACOTTA_GRAY)
    )

    // =========================================================================
    //  Lanterns and Chains
    // =========================================================================
    val OCHRE_LANTERN = Register(
        "ochre_lantern",
        ::LanternBlock,
        AbstractBlock.Settings.copy(Blocks.LANTERN)
            .mapColor(MapColor.ORANGE)
    )

    val OCHRE_CHAIN = Register(
        "ochre_chain",
        ::ChainBlock,
        AbstractBlock.Settings.copy(Blocks.CHAIN)
            .mapColor(MapColor.GRAY)
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

    val VERDANT_LANTERN = Register(
        "verdant_lantern",
        ::LanternBlock,
        AbstractBlock.Settings.copy(Blocks.LANTERN)
            .mapColor(MapColor.PALE_GREEN)
    )

    val VERDANT_CHAIN = Register(
        "verdant_chain",
        ::ChainBlock,
        AbstractBlock.Settings.copy(Blocks.CHAIN)
            .mapColor(MapColor.GRAY)
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
        AbstractBlock.Settings.copy(Blocks.TUFF)
            .mapColor(MapColor.DARK_RED)
    )

    val CINNABAR_SLAB = RegisterVariant(CINNABAR, "slab", ::SlabBlock)
    val CINNABAR_SLAB_VERTICAL = RegisterVSlab("cinnabar", CINNABAR_SLAB)
    val CINNABAR_STAIRS = RegisterStairs(CINNABAR)

    val POLISHED_CINNABAR = Register(
        "polished_cinnabar",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.STONE)
            .mapColor(MapColor.DARK_RED)
    )

    val POLISHED_CINNABAR_SLAB = RegisterVariant(POLISHED_CINNABAR, "slab", ::SlabBlock)
    val POLISHED_CINNABAR_SLAB_VERTICAL = RegisterVSlab("polished_cinnabar", POLISHED_CINNABAR_SLAB)
    val POLISHED_CINNABAR_STAIRS = RegisterStairs(POLISHED_CINNABAR)
    val POLISHED_CINNABAR_WALL = RegisterVariant(POLISHED_CINNABAR, "wall", ::WallBlock)

    val CINNABAR_BRICKS = Register(
        "cinnabar_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.STONE)
            .mapColor(MapColor.DARK_RED)
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
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val POLISHED_CALCITE_SLAB = RegisterVariant(POLISHED_CALCITE, "slab", ::SlabBlock)
    val POLISHED_CALCITE_SLAB_VERTICAL = RegisterVSlab("polished_calcite", POLISHED_CALCITE_SLAB)
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
    val CALCITE_BRICK_SLAB_VERTICAL = RegisterVSlab("calcite_bricks", CALCITE_BRICK_SLAB)
    val CALCITE_BRICK_STAIRS = RegisterStairs(CALCITE_BRICKS)
    val CALCITE_BRICK_WALL = RegisterVariant(CALCITE_BRICKS, "wall", ::WallBlock)

    val CHISELED_CALCITE_BRICKS = Register(
        "chiseled_calcite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    // =========================================================================
    //  Gilded calcite
    // =========================================================================
    val GILDED_CALCITE = Register(
        "gilded_calcite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_CALCITE_SLAB = RegisterVariant(GILDED_CALCITE, "slab", ::SlabBlock)
    val GILDED_CALCITE_SLAB_VERTICAL = RegisterVSlab("gilded_calcite", GILDED_CALCITE_SLAB)
    val GILDED_CALCITE_STAIRS = RegisterStairs(GILDED_CALCITE)

    val GILDED_POLISHED_CALCITE = Register(
        "gilded_polished_calcite",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
            .mapColor(MapColor.TERRACOTTA_WHITE)
    )

    val GILDED_POLISHED_CALCITE_SLAB = RegisterVariant(GILDED_POLISHED_CALCITE, "slab", ::SlabBlock)
    val GILDED_POLISHED_CALCITE_SLAB_VERTICAL = RegisterVSlab("gilded_polished_calcite", GILDED_POLISHED_CALCITE_SLAB)
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
    val GILDED_CALCITE_BRICK_SLAB_VERTICAL = RegisterVSlab("gilded_calcite_bricks", GILDED_CALCITE_BRICK_SLAB)
    val GILDED_CALCITE_BRICK_STAIRS = RegisterStairs(GILDED_CALCITE_BRICKS)
    val GILDED_CALCITE_BRICK_WALL = RegisterVariant(GILDED_CALCITE_BRICKS, "wall", ::WallBlock)

    val GILDED_CHISELED_CALCITE_BRICKS = Register(
        "gilded_chiseled_calcite_bricks",
        ::Block,
        AbstractBlock.Settings.copy(Blocks.CALCITE)
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
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_PLANKS)
            .mapColor(MapColor.PALE_PURPLE)
    )

    val TINTED_OAK_SLAB = RegisterVariant(TINTED_OAK_PLANKS, "slab", ::SlabBlock)
    val TINTED_OAK_SLAB_VERTICAL = RegisterVSlab("tinted_oak", TINTED_OAK_SLAB)
    val TINTED_OAK_STAIRS = RegisterStairs(TINTED_OAK_PLANKS)
    val TINTED_OAK_FENCE = RegisterVariant(TINTED_OAK_PLANKS, "fence", ::FenceBlock)

    val TINTED_OAK_FENCE_GATE = Register(
        "tinted_oak_fence_gate",
        { s -> FenceGateBlock(TINTED_OAK_WOODTYPE, s) },
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_FENCE_GATE).mapColor(MapColor.PALE_PURPLE)
    )

    val TINTED_OAK_DOOR = Register(
        "tinted_oak_door",
        { s -> DoorBlock(TINTED_OAK_BLOCKSETTYPE, s) },
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_DOOR).mapColor(MapColor.PALE_PURPLE)
    )

    val TINTED_OAK_TRAPDOOR = Register(
        "tinted_oak_trapdoor",
        { s -> TrapdoorBlock(TINTED_OAK_BLOCKSETTYPE, s) },
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_TRAPDOOR).mapColor(MapColor.PALE_PURPLE)
    )

    val TINTED_OAK_PRESSURE_PLATE = Register(
        "tinted_oak_pressure_plate",
        { s -> PressurePlateBlock(TINTED_OAK_BLOCKSETTYPE, s) },
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_PRESSURE_PLATE).mapColor(MapColor.PALE_PURPLE)
    )

    val TINTED_OAK_BUTTON = Register(
        "tinted_oak_button",
        { s -> ButtonBlock(TINTED_OAK_BLOCKSETTYPE, 30, s) },
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_BUTTON).mapColor(MapColor.PALE_PURPLE)
    )

    val TINTED_OAK_LOG = Register(
        "tinted_oak_log",
        ::PillarBlock,
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_LOG).mapColor(MapColor.PALE_PURPLE)
    )

    val TINTED_OAK_WOOD = Register(
        "tinted_oak_wood",
        ::PillarBlock,
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_WOOD).mapColor(MapColor.PALE_PURPLE)
    )

    val STRIPPED_TINTED_OAK_LOG = Register(
        "stripped_tinted_oak_log",
        ::PillarBlock,
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_LOG).mapColor(MapColor.PALE_PURPLE)
    )

    val STRIPPED_TINTED_OAK_WOOD = Register(
        "stripped_tinted_oak_wood",
        ::PillarBlock,
        AbstractBlock.Settings.copy(Blocks.PALE_OAK_WOOD).mapColor(MapColor.PALE_PURPLE)
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

    val PYRITE_BRICK_FAMILY: BlockFamily = BlockFamilies.register(PYRITE_BRICKS)
        .slab(PYRITE_BRICK_SLAB)
        .stairs(PYRITE_BRICK_STAIRS)
        .wall(PYRITE_BRICK_WALL)
        .chiseled(CHISELED_PYRITE_BRICKS)
        .build()

    val DRIPSTONE_BRICK_FAMILY: BlockFamily = BlockFamilies.register(DRIPSTONE_BRICKS)
        .slab(DRIPSTONE_BRICK_SLAB)
        .stairs(DRIPSTONE_BRICK_STAIRS)
        .wall(DRIPSTONE_BRICK_WALL)
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

    val TINTED_OAK_FAMILY: BlockFamily = BlockFamilies.register(TINTED_OAK_PLANKS)
        .slab(TINTED_OAK_SLAB)
        .stairs(TINTED_OAK_STAIRS)
        .fence(TINTED_OAK_FENCE)
        .fenceGate(TINTED_OAK_FENCE_GATE)
        .door(TINTED_OAK_DOOR)
        .trapdoor(TINTED_OAK_TRAPDOOR)
        .pressurePlate(TINTED_OAK_PRESSURE_PLATE)
        .button(TINTED_OAK_BUTTON)
        .build()

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
            BlockFamilies.ACACIA,
            Log = Blocks.ACACIA_LOG,
            Wood = Blocks.ACACIA_WOOD,
            StrippedLog = Blocks.STRIPPED_ACACIA_LOG,
            StrippedWood = Blocks.STRIPPED_ACACIA_WOOD,
        ),

        WoodFamily(
            BlockFamilies.BIRCH,
            Log = Blocks.BIRCH_LOG,
            Wood = Blocks.BIRCH_WOOD,
            StrippedLog = Blocks.STRIPPED_BIRCH_LOG,
            StrippedWood = Blocks.STRIPPED_BIRCH_WOOD,
        ),

        WoodFamily(
            BlockFamilies.CHERRY,
            Log = Blocks.CHERRY_LOG,
            Wood = Blocks.CHERRY_WOOD,
            StrippedLog = Blocks.STRIPPED_CHERRY_LOG,
            StrippedWood = Blocks.STRIPPED_CHERRY_WOOD,
        ),

        WoodFamily(
            BlockFamilies.CRIMSON,
            Log = Blocks.CRIMSON_STEM,
            Wood = Blocks.CRIMSON_HYPHAE,
            StrippedLog = Blocks.STRIPPED_CRIMSON_STEM,
            StrippedWood = Blocks.STRIPPED_CRIMSON_HYPHAE,
        ),

        WoodFamily(
            BlockFamilies.DARK_OAK,
            Log = Blocks.DARK_OAK_LOG,
            Wood = Blocks.DARK_OAK_WOOD,
            StrippedLog = Blocks.STRIPPED_DARK_OAK_LOG,
            StrippedWood = Blocks.STRIPPED_DARK_OAK_WOOD,
        ),

        WoodFamily(
            BlockFamilies.JUNGLE,
            Log = Blocks.JUNGLE_LOG,
            Wood = Blocks.JUNGLE_WOOD,
            StrippedLog = Blocks.STRIPPED_JUNGLE_LOG,
            StrippedWood = Blocks.STRIPPED_JUNGLE_WOOD,
        ),

        WoodFamily(
            BlockFamilies.MANGROVE,
            Log = Blocks.MANGROVE_LOG,
            Wood = Blocks.MANGROVE_WOOD,
            StrippedLog = Blocks.STRIPPED_MANGROVE_LOG,
            StrippedWood = Blocks.STRIPPED_MANGROVE_WOOD,
        ),

        WoodFamily(
            BlockFamilies.OAK,
            Log = Blocks.OAK_LOG,
            Wood = Blocks.OAK_WOOD,
            StrippedLog = Blocks.STRIPPED_OAK_LOG,
            StrippedWood = Blocks.STRIPPED_OAK_WOOD,
        ),

        WoodFamily(
            BlockFamilies.PALE_OAK,
            Log = Blocks.PALE_OAK_LOG,
            Wood = Blocks.PALE_OAK_WOOD,
            StrippedLog = Blocks.STRIPPED_PALE_OAK_LOG,
            StrippedWood = Blocks.STRIPPED_PALE_OAK_WOOD,
        ),

        WoodFamily(
            BlockFamilies.SPRUCE,
            Log = Blocks.SPRUCE_LOG,
            Wood = Blocks.SPRUCE_WOOD,
            StrippedLog = Blocks.STRIPPED_SPRUCE_LOG,
            StrippedWood = Blocks.STRIPPED_SPRUCE_WOOD,
        ),

        WoodFamily(
            BlockFamilies.WARPED,
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
    val CAN_DUPLICATE_WITH_BONEMEAL = TagKey.of(RegistryKeys.BLOCK, Id("can_duplicate_with_bonemeal"))

    // =========================================================================
    //  Initialisation
    // =========================================================================
    fun Init() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.REDSTONE).register {
            it.addAfter(Blocks.HOPPER, DECORATIVE_HOPPER)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.BUILDING_BLOCKS).register {
            it.addAfter(Blocks.GOLD_BLOCK, LOCKED_DOOR)
            it.addAfter(Blocks.SMOOTH_STONE_SLAB, COMPRESSED_STONE)
            it.addAfter(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, WROUGHT_IRON_BLOCK, WROUGHT_IRON_GRATE, WROUGHT_IRON_BARS)
            it.addAfter(Blocks.IRON_BLOCK, IRON_GRATE)
            it.addAfter(Blocks.GOLD_BLOCK, GOLD_BARS)
            it.addAfter(Blocks.POLISHED_BLACKSTONE_BRICK_WALL, PYRITE)
            it.addAfter(Blocks.WARPED_BUTTON, TINTED_OAK_LOG, TINTED_OAK_WOOD, STRIPPED_TINTED_OAK_LOG, STRIPPED_TINTED_OAK_WOOD)
            it.addAfter(Blocks.COAL_BLOCK, CHARCOAL_BLOCK)

            // Family Base Blocks
            it.addAfter(STRIPPED_TINTED_OAK_WOOD, TINTED_OAK_PLANKS)
            it.addAfter(Blocks.RED_NETHER_BRICK_WALL, CINNABAR, CINNABAR_BRICKS)
            it.addAfter(Blocks.POLISHED_ANDESITE_SLAB, Blocks.CALCITE, POLISHED_CALCITE, CALCITE_BRICKS, GILDED_CALCITE, GILDED_CALCITE_BRICKS,Blocks.DRIPSTONE_BLOCK, DRIPSTONE_BRICKS)
            it.addAfter(PYRITE, PYRITE_BRICKS)

            for (F in ALL_VARIANT_FAMILIES.iterator()) for (B in F.variants.toSortedMap( compareBy<BlockFamily.Variant> { c -> getVarientPlacement(c) }).values.reversed()) it.addAfter(F.baseBlock, B)
            for (B in ALL_BROCADE_BLOCKS) it.add(B)
            for (B in CHAINS_AND_LANTERNS) it.addAfter(Blocks.CHAIN, B.first)
            for (B in VERTICAL_SLABS) it.addBefore(SLABS_VSLABS_MAP.get(B.translationKey.substringAfterLast('.')), B)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COLORED_BLOCKS).register {
            for (B in ALL_BROCADE_BLOCKS) it.add(B)
        }

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register {
            for (B in CHAINS_AND_LANTERNS) {
                it.addAfter(Blocks.CHAIN, B.first)
                it.addAfter(Blocks.SOUL_LANTERN, B.second)
            }
        }

        registerStrippable(TINTED_OAK_LOG, STRIPPED_TINTED_OAK_LOG)
        registerStrippable(TINTED_OAK_WOOD, STRIPPED_TINTED_OAK_WOOD)

        for (B in WOOD_VARIANT_FAMILY_BLOCKS) { registerFlammable(B, 5, 5) }
        registerFlammable(TINTED_OAK_LOG, 5, 5)
        registerFlammable(TINTED_OAK_WOOD, 5, 5)
        registerFlammable(STRIPPED_TINTED_OAK_LOG, 5, 5)
        registerFlammable(STRIPPED_TINTED_OAK_WOOD, 5, 5)
        for (V in NguhBlockModels.VERTICAL_SLABS) { if (V.Wood) { registerFlammable(V.VerticalSlab, 5, 5) } }
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

    private fun <T : Block> Register(
        Key: String,
        Ctor: (S: AbstractBlock.Settings) -> T,
        S: AbstractBlock.Settings,
        ItemCtor: (B: Block, S: Item.Settings) -> Item = ::BlockItem
    ): T {
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

    fun RegisterVSlab(Name: String, SlabBlock: Block) = Register(
        "${Name}_slab_vertical",
        ::VerticalSlabBlock,
        AbstractBlock.Settings.copy(SlabBlock)
    ).also { (VERTICAL_SLABS as MutableList).add(it) }.also { SLABS_VSLABS_MAP.put("${Name}_slab_vertical", SlabBlock) }

    private fun getVarientPlacement(varient: BlockFamily.Variant): Int {
        var list = listOf("CRACKED", "STAIRS", "SLAB", "WALL", "FENCE", "FENCE_GATE", "DOOR", "TRAPDOOR", "PRESSURE_PLATE", "BUTTON", "SIGN", "WALL_SIGN", "MOSAIC", "CHISELED", "POLISHED", "CUT")
        var index = list.indexOf(varient.name)
        return index
    }

    fun RegisterStrippable(L: Block, S: Block) = StrippableBlockRegistry.register(L, S)

    fun registerFlammable(b: Block, burn: Int, spread: Int) = FlammableBlockRegistry.getDefaultInstance().add(b, burn, spread)
}
