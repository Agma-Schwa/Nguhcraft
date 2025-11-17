package org.nguh.nguhcraft

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.nbt.ListTag

fun CompoundTag.set(Key: String, Value: String) = putString(Key, Value)
fun CompoundTag.set(Key: String, Value: Int) = putInt(Key, Value)
fun CompoundTag.set(Key: String, Value: Long) = putLong(Key, Value)
fun CompoundTag.set(Key: String, Value: Float) = putFloat(Key, Value)
fun CompoundTag.set(Key: String, Value: Double) = putDouble(Key, Value)
fun CompoundTag.set(Key: String, Value: Boolean) = putBoolean(Key, Value)
fun CompoundTag.set(Key: String, Value: Tag) = put(Key, Value)

fun Nbt(Builder: CompoundTag.() -> Unit): CompoundTag {
    val T = CompoundTag()
    T.Builder()
    return T
}
