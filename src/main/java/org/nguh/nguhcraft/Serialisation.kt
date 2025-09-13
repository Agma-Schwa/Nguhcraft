package org.nguh.nguhcraft

import com.mojang.serialization.Codec
import net.minecraft.storage.ReadView
import net.minecraft.storage.WriteView
import net.minecraft.util.ErrorReporter
import java.util.Optional
import kotlin.collections.toList
import kotlin.collections.toMutableSet
import kotlin.reflect.KMutableProperty1
import kotlin.text.uppercase


class NguhErrorReporter : ErrorReporter.Context {
    override fun getName() = "Nguhcraft"
}

/**
 * Codec-like class that serialises a list of fields inline without
 * creating a wrapper object.
 */
class ClassSerialiser<R> private constructor(
    private val Fields: List<Field<R, *>>
) {
    private class Field<R, T>(
        val Codec: Codec<T>,
        val Name: String,
        val Prop: KMutableProperty1<R, T>
    ) {
        fun Read(Object: R, RV: ReadView) = RV.read(Name, Codec).ifPresent { Prop.set(Object, it) }
        fun Write(Object: R, WV: WriteView) = WV.put(Name, Codec, Prop.get(Object))
    }

    class BuilderImpl<R> {
        private val Fields = mutableListOf<Field<R, *>>()
        fun build(): ClassSerialiser<R> = ClassSerialiser(Fields)
        fun<T> add(Codec: Codec<T>, Name: String, Prop: KMutableProperty1<R, T>) = also {
            if (Fields.find { it.Name == Name } != null)
                throw IllegalArgumentException("Duplicate field name: '$Name'")

            Fields.add(Field(Codec, Name, Prop))
        }
    }

    fun Read(Object: R, RV: ReadView) = Fields.forEach { it.Read(Object, RV) }
    fun Write(Object: R, WV: WriteView) = Fields.forEach { it.Write(Object, WV) }
    companion object { fun<R> Builder(): BuilderImpl<R> = BuilderImpl() }
}

/** A codec for serialising enums. */
inline fun <reified T : Enum<T>> MakeEnumCodec(): Codec<T> = Codec.stringResolver(
    { it.name.lowercase() },
    { enumValueOf<T>(it.uppercase()) }
)

/** A named codec. */
data class NamedCodec<T>(val Name: String, val Codec: Codec<T>)

/** Create a named codec. */
fun<T> Codec<T>.Named(Name: String) = NamedCodec(Name, this)

/** Read a named codec. */
fun<T> ReadView.Read(Codec: NamedCodec<T>): Optional<T> = read(Codec.Name, Codec.Codec)

/** Write a named codec. */
fun<T> WriteView.Write(Codec: NamedCodec<T>, Val: T) = put(Codec.Name, Codec.Codec, Val)

/** Read from a child view. */
fun ReadView.With(Name: String, Reader: ReadView.() -> Unit) = getReadView(Name).Reader()

/** Write to a child view. */
fun WriteView.With(Name: String, Writer: WriteView.() -> Unit) = get(Name).Writer()

/** Read from a child list. */
fun ReadView.WithList(Name: String, Reader: ReadView.ListReadView.() -> Unit) = getListReadView(Name).Reader()

/** Write to a child view. */
fun WriteView.WithList(Name: String, Writer: WriteView.ListView.() -> Unit) = getList(Name).Writer()

/** Create a mutable set codec. */
fun<T> Codec<T>.MutableSetOf() = listOf().xmap({ it.toMutableSet() }, { it.toList() })