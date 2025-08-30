package org.nguh.nguhcraft

import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList

fun NbtCompound.set(Key: String, Value: String) = putString(Key, Value)
fun NbtCompound.set(Key: String, Value: Int) = putInt(Key, Value)
fun NbtCompound.set(Key: String, Value: Long) = putLong(Key, Value)
fun NbtCompound.set(Key: String, Value: Float) = putFloat(Key, Value)
fun NbtCompound.set(Key: String, Value: Double) = putDouble(Key, Value)
fun NbtCompound.set(Key: String, Value: Boolean) = putBoolean(Key, Value)
fun NbtCompound.set(Key: String, Value: NbtElement) = put(Key, Value)

fun Nbt(Builder: NbtCompound.() -> Unit): NbtCompound {
    val T = NbtCompound()
    T.Builder()
    return T
}
