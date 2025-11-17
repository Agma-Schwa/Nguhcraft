package org.nguh.nguhcraft

import com.mojang.serialization.Codec
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import net.minecraft.util.ProblemReporter
import java.util.Optional
import kotlin.collections.toList
import kotlin.collections.toMutableSet
import kotlin.reflect.KMutableProperty1
import kotlin.text.uppercase


class NguhErrorReporter : ProblemReporter.PathElement {
    override fun get() = "Nguhcraft"
}

/**
 * Codec-like class that serialises a list of fields inline without
 * creating a wrapper object.
 */
class ClassSerialiser<R> private constructor(
    private val Fields: List<Field<R, *>>
) {
    private class Field<R, T: Any>(
        val Codec: Codec<T>,
        val Name: String,
        val Prop: KMutableProperty1<R, T>
    ) {
        fun Read(Object: R, RV: ValueInput) = RV.read(Name, Codec).ifPresent { Prop.set(Object, it) }
        fun Write(Object: R, WV: ValueOutput) = WV.store(Name, Codec, Prop.get(Object))
    }

    class BuilderImpl<R> {
        private val Fields = mutableListOf<Field<R, *>>()
        fun build(): ClassSerialiser<R> = ClassSerialiser(Fields)
        fun<T: Any> add(Codec: Codec<T>, Name: String, Prop: KMutableProperty1<R, T>) = also {
            if (Fields.find { it.Name == Name } != null)
                throw IllegalArgumentException("Duplicate field name: '$Name'")

            Fields.add(Field(Codec, Name, Prop))
        }
    }

    fun Read(Object: R, RV: ValueInput) = Fields.forEach { it.Read(Object, RV) }
    fun Write(Object: R, WV: ValueOutput) = Fields.forEach { it.Write(Object, WV) }
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
fun<T> ValueInput.Read(Codec: NamedCodec<T>): Optional<T> = read(Codec.Name, Codec.Codec)

/** Write a named codec. */
fun<T: Any> ValueOutput.Write(Codec: NamedCodec<T>, Val: T) = store(Codec.Name, Codec.Codec, Val)

/** Read from a child view. */
fun ValueInput.With(Name: String, Reader: ValueInput.() -> Unit) = childOrEmpty(Name).Reader()

/** Write to a child view. */
fun ValueOutput.With(Name: String, Writer: ValueOutput.() -> Unit) = child(Name).Writer()

/** Read from a child list. */
fun ValueInput.WithList(Name: String, Reader: ValueInput.ValueInputList.() -> Unit) = childrenListOrEmpty(Name).Reader()

/** Write to a child view. */
fun ValueOutput.WithList(Name: String, Writer: ValueOutput.ValueOutputList.() -> Unit) = childrenList(Name).Writer()

/** Create a mutable set codec. */
fun<T> Codec<T>.MutableSetOf() = listOf().xmap({ it.toMutableSet() }, { it.toList() })