package org.nguh.nguhcraft

import com.mojang.logging.LogUtils
import net.minecraft.component.ComponentChanges
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtOps
import net.minecraft.network.RegistryByteBuf
import net.minecraft.network.packet.CustomPayload
import net.minecraft.registry.Registries
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.text.MutableText
import net.minecraft.text.Text
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import org.nguh.nguhcraft.enchantment.NguhcraftEnchantments
import java.text.Normalizer
import java.util.*
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

typealias MojangPair<A, B> = com.mojang.datafixers.util.Pair<A, B>
operator fun <A, B> MojangPair<A, B>.component1(): A = this.first
operator fun <A, B> MojangPair<A, B>.component2(): B = this.second
operator fun Vec3d.unaryMinus() = Vec3d(-x, -y, -z)

/**
* Transform 'this' using a function iff 'Cond' is true and return
* 'this' unchanged otherwise.
*/
inline fun <T> T.mapIf(Cond: Boolean, Block: (T) -> T): T
    = if (Cond) Block(this) else this

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
    operator fun contains(Pos: Vec3d): Boolean = Contains(Pos.x, Pos.z)
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
    fun DistanceFrom(Pos: BlockPos) = DistanceFrom(Vec3d.of(Pos))
    fun DistanceFrom(Pos: Vec3d): Float {
        if (Pos in this) return 0f
        val C = Center
        val X = abs(Pos.x.toFloat() - C.x.toFloat())
        val Z = abs(Pos.z.toFloat() - C.z.toFloat())
        val Radius = Radius
        return Vec2f(max(X - Radius.x, 0f), max(Z - Radius.y, 0f)).length()
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

    fun Intersects(BB: Box) = Intersects(
        MinX = BB.minX,
        MinZ = BB.minZ,
        MaxX = BB.maxX,
        MaxZ = BB.maxZ
    )

    /** Get the radius of this rectangle. */
    val Radius: Vec2f get() {
        val X = (MaxX - MinX) / 2
        val Z = (MaxZ - MinZ) / 2
        return Vec2f(X.toFloat(), Z.toFloat())
    }

    fun SaveXZRect(Tag: NbtCompound) = Tag.apply {
        set(TAG_MIN_X, MinX)
        set(TAG_MIN_Z, MinZ)
        set(TAG_MAX_X, MaxX)
        set(TAG_MAX_Z, MaxZ)
    }

    fun WriteXZRect(buf: RegistryByteBuf) {
        buf.writeInt(MinX)
        buf.writeInt(MinZ)
        buf.writeInt(MaxX)
        buf.writeInt(MaxZ)
    }

    companion object {
        const val TAG_MIN_X = "MinX"
        const val TAG_MIN_Z = "MinZ"
        const val TAG_MAX_X = "MaxX"
        const val TAG_MAX_Z = "MaxZ"

        fun Load(Tag: NbtCompound) = XZRect(
            FromX = Tag.getInt(TAG_MIN_X),
            FromZ = Tag.getInt(TAG_MIN_Z),
            ToX = Tag.getInt(TAG_MAX_X),
            ToZ = Tag.getInt(TAG_MAX_Z)
        )

        fun ReadXZRect(B: RegistryByteBuf) = XZRect(
            FromX = B.readInt(),
            FromZ = B.readInt(),
            ToX = B.readInt(),
            ToZ = B.readInt()
        )
    }
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
    val LINK: Text = Text.literal("[").withColor(Constants.Blue)
        .append(Text.literal("Link").withColor(Constants.Green))
        .append(Text.literal("]").withColor(Constants.Blue))
        .append(Text.literal(".").withColor(Constants.Green))


    /** Coloured '[' and '] ' components (the latter includes a space). */
    val LBRACK_COMPONENT: Text = Text.literal("[").withColor(Constants.DeepKoamaru)
    val RBRACK_COMPONENT: Text = Text.literal("]").withColor(Constants.DeepKoamaru)

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
    fun BracketedLiteralComponent(Content: String): MutableText = Text.empty()
        .append(LBRACK_COMPONENT)
        .append(Text.literal(Content).withColor(Constants.Lavender))
        .append(RBRACK_COMPONENT)

    /**
     * The ItemStack constructor for this sucks so much it’s not even funny anymore.
     *
     * The callback has no default parameter because at that point you can literally
     * just use the ItemStack constructor instead.
     */
    fun BuildItemStack(I: Item, Count: Int = 1, ComponentBuilder: ComponentChanges.Builder.() -> Unit): ItemStack {
        val B = ComponentChanges.builder()
        B.ComponentBuilder()
        return ItemStack(
            Registries.ITEM.getEntry(I),
            Count,
            B.build()
        )
    }

    /** Calculate a player’s total saturation enchantment value. */
    @JvmStatic
    fun CalculateWeightedSaturationEnchantmentValue(P: PlayerEntity): Int {
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
        val W = P.world
        return P.armorItems.sumOf {
            val Lvl = EnchantLvl(W, it, NguhcraftEnchantments.SATURATION)
            if (Lvl == 0) 0 else 1 shl (Lvl - 1)
        }
    }

    /** Print a debug message. */
    @JvmStatic
    fun Debug(Message: String, vararg Objects : Any) = LOGGER.info(Message, *Objects)

    /** Deserialise a world from a registry key. */
    fun DeserialiseWorld(Nbt: NbtElement) =
        World.CODEC.parse(NbtOps.INSTANCE, Nbt).result().get()

    /** Get the level of an enchantment on an item stack. */
    @JvmStatic
    fun EnchantLvl(W: World, Stack: ItemStack, E: RegistryKey<Enchantment>): Int {
        val R = W.registryManager.getOrThrow(RegistryKeys.ENCHANTMENT)
        return EnchantmentHelper.getLevel(R.getOrThrow(E), Stack)
    }

    /** Normalise a string for fuzzy matching against another string  */
    fun NormaliseNFKCLower(S: String) = Normalizer.normalize(S, Normalizer.Form.NFKC).lowercase(Locale.getDefault())

    /** Create a packet id. */
    fun <T : CustomPayload> PacketId(Name: String) = CustomPayload.Id<T>(Nguhcraft.Id(Name))

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
    fun SerialiseWorld(W: RegistryKey<World>) = World.CODEC.encodeStart(NbtOps.INSTANCE, W).result().get()
}

/** Parse a string into a UUID, returning null on failure. */
fun String?.toUUID(): UUID? {
    if (this == null) return null
    return try { UUID.fromString(this) }
    catch (E: IllegalArgumentException) { null }
}