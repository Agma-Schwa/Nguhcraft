package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import com.mojang.serialization.Codec
import com.mojang.serialization.JavaOps
import io.netty.buffer.ByteBuf
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.nbt.Tag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceKey
import net.minecraft.core.registries.Registries
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Component
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec2
import net.minecraft.world.phys.Vec3
import net.minecraft.world.level.Level
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import org.nguh.nguhcraft.mixin.common.EntityEquipmentAccessor
import org.nguh.nguhcraft.mixin.common.LivingEntityEquipmentAccessor
import java.text.Normalizer
import java.util.*
import kotlin.enums.EnumEntries
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias MojangPair<A, B> = com.mojang.datafixers.util.Pair<A, B>
operator fun <A, B> MojangPair<A, B>.component1(): A = this.first
operator fun <A, B> MojangPair<A, B>.component2(): B = this.second
operator fun Vec3.unaryMinus(): Vec3 = reverse()
operator fun Vec3.plus(o: Vec3): Vec3 = add(o)
operator fun Vec3.minus(o: Vec3): Vec3 = subtract(o)

/**
* Transform 'this' using a function iff 'Cond' is true and return
* 'this' unchanged otherwise.
*/
inline fun <T> T.mapIf(Cond: Boolean, Block: (T) -> T): T
    = if (Cond) Block(this) else this

/** Flatten a list of pairs to a list. */
fun <A> List<Pair<A, A>>.flatten(): List<A> = flatMap { listOf(it.first, it.second) }

/** Serialisable EnumSet for small enums with less overhead. */
class SmallEnumSet<T : Enum<T>> private constructor(var Encoded: Long = 0L) {
    /** Encoded enum values.*/
    constructor(vararg Vals: T): this() { for (V in Vals) Set(V) }

    /** Deserialising constructor. */
    constructor(Vals: Map<T, Boolean>) : this() {
        for ((Flag, V) in Vals) Set(Flag, V)
    }

    /** Check whether a value is set. */
    fun IsSet(Flag: T, Value: Boolean = true) = (Encoded and Flag.Bit != 0L) == Value

    /** Set a value. */
    fun Set(Flag: T, To: Boolean = true) {
        val OldFlags = Encoded
        Encoded = if (To) OldFlags or Flag.Bit else OldFlags and Flag.Bit.inv()
    }

    companion object {
        private val <T: Enum<T>> T.Bit get() = 1L shl ordinal
        val PACKET_CODEC = ByteBufCodecs.LONG.map(::SmallEnumSet, SmallEnumSet<*>::Encoded)

        /** Create a codec for an enum set. */
        inline fun <reified T: Enum<T>> CreateCodec(Entries: EnumEntries<T>): Codec<SmallEnumSet<T>> {
            return Codec.unboundedMap(
                MakeEnumCodec<T>(),
                Codec.BOOL
            ).xmap(
                ::SmallEnumSet,
                { Set -> Entries.associateWith { Set.IsSet(it) } },
            )
        }

        /** Get the packet codec for an enum set. This does not create a new object. */
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T : Enum<T>> CreatePacketCodec()
            = PACKET_CODEC as StreamCodec<ByteBuf, SmallEnumSet<T>>
    }
}

/**
 * Rectangle that has X and Z bounds but ignores the Y axis. Used by regions
 * and in barrier rendering.
 */
open class XZRect(FromX: Int, FromZ: Int, ToX: Int, ToZ: Int) {
    /** Bounds of the rectangle. */
    val MinX: Int = min(FromX, ToX)
    val MinZ: Int = min(FromZ, ToZ)
    val MaxX: Int = max(FromX, ToX)
    val MaxZ: Int = max(FromZ, ToZ)

    /**
     * Coordinates in Minecraft are at the north-west (-X, -Z) corner of the block,
     * so for rendering, we need to add 1 to the maximum values to include the last
     * block within the region.
     */
    val OutsideMaxX get() = MaxX + 1
    val OutsideMaxZ get() = MaxZ + 1

    /** Get the centre of this rectangle. */
    val Center: BlockPos get() = BlockPos((MinX + MaxX) / 2, 0, (MinZ + MaxZ) / 2)

    /** Check if this rectangle contains a block or another rectangle. */
    operator fun contains(Pos: BlockPos): Boolean = Contains(Pos.x, Pos.z)
    operator fun contains(Pos: Vec3): Boolean = Contains(Pos.x, Pos.z)
    operator fun contains(XZ: XZRect) = Contains(XZ.MinX, XZ.MinZ) && Contains(XZ.MaxX, XZ.MaxZ)
    fun Contains(X: Int, Z: Int): Boolean = X in MinX..MaxX && Z in MinZ..MaxZ
    fun Contains(X: Double, Z: Double): Boolean =
        X in MinX.toDouble()..OutsideMaxX.toDouble() &&
        Z in MinZ.toDouble()..OutsideMaxZ.toDouble()

    /**
     * Get the distance of a position to the nearest side of the
     * rectangle, or 0 if the position is inside the rectangle.
     *
     * This is a 2D distance calculation that uses the distance field
     * of a rectangle.
     */
    fun DistanceFrom(Pos: BlockPos) = DistanceFrom(Vec3.atLowerCornerOf(Pos))
    fun DistanceFrom(Pos: Vec3): Float {
        if (Pos in this) return 0f
        val C = Center
        val X = abs(Pos.x.toFloat() - C.x.toFloat())
        val Z = abs(Pos.z.toFloat() - C.z.toFloat())
        val Radius = Radius
        return Vec2(max(X - Radius.x, 0f), max(Z - Radius.y, 0f)).length()
    }

    /** Check if a rectangle intersects another. */
    fun Intersects(XZ: XZRect) = Intersects(
        MinX = XZ.MinX.toDouble(),
        MinZ = XZ.MinZ.toDouble(),
        MaxX = XZ.OutsideMaxX.toDouble(),
        MaxZ = XZ.OutsideMaxZ.toDouble()
    )

    fun Intersects(MinX: Double, MinZ: Double, MaxX: Double, MaxZ: Double) =
        MinX <= OutsideMaxX.toDouble() &&
        MaxX >= this.MinX.toDouble() &&
        MinZ <= OutsideMaxX.toDouble() &&
        MaxZ >= this.MinZ.toDouble()

    fun Intersects(BB: AABB) = Intersects(
        MinX = BB.minX,
        MinZ = BB.minZ,
        MaxX = BB.maxX,
        MaxZ = BB.maxZ
    )

    /** Get the radius of this rectangle. */
    val Radius: Vec2 get() {
        val X = (MaxX - MinX) / 2
        val Z = (MaxZ - MinZ) / 2
        return Vec2(X.toFloat(), Z.toFloat())
    }
}

/** Get an entity’s equipped items. */
fun LivingEntity.Equipment(): List<ItemStack> {
    val E = (this as LivingEntityEquipmentAccessor).equipment
    val Map = (E as EntityEquipmentAccessor).map
    return Map.values.filter { !it.isEmpty }
}

object Utils {
    val LOGGER = LogUtils.getLogger()

    /**
     * [Text] containing the text '[Link]'.
     *
     *
     * Used to communicate to players that a message contains a clickable
     * link. Purely cosmetic since the entire message is clickable anyway.
     */
    val LINK: Component = Component.literal("[").withColor(Constants.Blue)
        .append(Component.literal("Link").withColor(Constants.Green))
        .append(Component.literal("]").withColor(Constants.Blue))
        .append(Component.literal(".").withColor(Constants.Green))


    /** Coloured '[' and '] ' components (the latter includes a space). */
    val LBRACK_COMPONENT: Component = Component.literal("[").withColor(Constants.DeepKoamaru)
    val RBRACK_COMPONENT: Component = Component.literal("]").withColor(Constants.DeepKoamaru)

    /** For RomanNumeral conversion. */
    private val M = arrayOf("", "M", "MM", "MMM")
    private val C = arrayOf("", "C", "CC", "CCC", "CD", "D", "DC", "DCC", "DCCC", "CM")
    private val X = arrayOf("", "X", "XX", "XXX", "XL", "L", "LX", "LXX", "LXXX", "XC")
    private val I = arrayOf("", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX")

    /** The weighted value at which the saturation enchantment is considered maxed out. */
    const val MAX_SATURATION_ENCHANTMENT_VALUE = 8

    /**
     * Get a component enclosed in brackets.
     *
     * For example, for an input of "foo", this will return '[foo]' with
     * appropriate formatting.
     */
    fun BracketedLiteralComponent(Content: String): MutableComponent = Component.empty()
        .append(LBRACK_COMPONENT)
        .append(Component.literal(Content).withColor(Constants.Lavender))
        .append(RBRACK_COMPONENT)

    /**
     * The ItemStack constructor for this sucks so much it’s not even funny anymore.
     *
     * The callback has no default parameter because at that point you can literally
     * just use the ItemStack constructor instead.
     */
    fun BuildItemStack(I: Item, Count: Int = 1, ComponentBuilder: DataComponentPatch.Builder.() -> Unit): ItemStack {
        val B = DataComponentPatch.builder()
        B.ComponentBuilder()
        return ItemStack(
            BuiltInRegistries.ITEM.wrapAsHolder(I),
            Count,
            B.build()
        )
    }

    /** Calculate a player’s total saturation enchantment value. */
    @JvmStatic
    fun CalculateWeightedSaturationEnchantmentValue(P: Player): Int {
        // Accumulate the total saturation level across all armour pieces.
        //
        // The formula for this is weighted, i.e. one armour piece with
        // saturation 4 is enough to prevent all hunger loss; but with
        // saturation 3, you need two pieces, and so on. In other words
        // we can model this as
        //
        //    Level 1 = 1 point,
        //    Level 2 = 2 points,
        //    Level 3 = 4 points,
        //    Level 4 = 8 points,
        //
        // where 8 points = 100%. This means the formula to map an enchantment
        // level to how many points it adds is 2^(L-1).
        val W = P.level()
        return P.Equipment().sumOf {
            val Lvl = EnchantLvl(W, it, NguhcraftEnchantments.SATURATION)
            if (Lvl == 0) 0 else 1 shl (Lvl - 1)
        }
    }

    /** Print a debug message. */
    @JvmStatic
    fun Debug(Message: String, vararg Objects : Any) = LOGGER.info(Message, *Objects)

    /** Deserialise a world from a registry key. */
    fun DeserialiseWorld(Nbt: Tag) =
        Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, Nbt).result().get()
    fun DeserialiseWorld(Str: String) =
        Level.RESOURCE_KEY_CODEC.parse(JavaOps.INSTANCE, Str).result().get()

    /** Get the level of an enchantment on an item stack. */
    @JvmStatic
    fun EnchantLvl(W: Level, Stack: ItemStack, E: ResourceKey<Enchantment>): Int {
        val R = W.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
        return EnchantmentHelper.getItemEnchantmentLevel(R.getOrThrow(E), Stack)
    }

    /** If this iterable contains a single element, return it, else return null. */
    fun<T> GetSingleElement(Iter: Iterable<T>?): T? {
        if (Iter == null) return null

        // For lists, just extract the first element directly.
        if (Iter is List) return if (Iter.size == 1) Iter.first() else null

        // For anything else, use an iterator.
        val It = Iter.iterator()
        if (!It.hasNext()) return null
        val First = It.next()
        return if (It.hasNext()) null else First
    }

    /** Normalise a string for fuzzy matching against another string  */
    fun NormaliseNFKCLower(S: String) = Normalizer.normalize(S, Normalizer.Form.NFKC).lowercase(Locale.getDefault())

    /** Create a packet id. */
    fun <T : CustomPacketPayload> PacketId(Name: String) = CustomPacketPayload.Type<T>(Nguhcraft.Id(Name))

    /** Format a number as a Roman numeral */
    @JvmStatic
    fun RomanNumeral(Number: Int): String {
        if (Number < 1 || Number > 3999) return Number.toString()
        val Thousands = M[Number / 1000]
        val Hundreds = C[Number % 1000 / 100]
        val Tens = X[Number % 100 / 10]
        val Ones = I[Number % 10]
        return "$Thousands$Hundreds$Tens$Ones"
    }

    /** Serialise a world as a registry key. */
    fun SerialiseWorld(W: ResourceKey<Level>) = Level.RESOURCE_KEY_CODEC.encodeStart(NbtOps.INSTANCE, W).result().get()
    fun SerialiseWorldToString(W: ResourceKey<Level>) = Level.RESOURCE_KEY_CODEC.encodeStart(JavaOps.INSTANCE, W).result().get() as String
}

/** Parse a string into a UUID, returning null on failure. */
fun String?.toUUID(): UUID? {
    if (this == null) return null
    return try { UUID.fromString(this) }
    catch (E: IllegalArgumentException) { null }
}

/** Decode a value. */
fun <T> Codec<T>.Decode(Val: Tag): T =
    parse(NbtOps.INSTANCE, Val).getOrThrow()

/** Encode a value. */
fun <T> Codec<T>.Encode(Val: T): Tag =
    encodeStart(NbtOps.INSTANCE, Val).getOrThrow()