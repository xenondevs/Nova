package xyz.xenondevs.nmsutils.internal.util

internal fun Double.toFixedPoint(): Short = (this * 4069).toInt().toShort()

internal fun Short.fromFixedPoint(): Double = this / 4096.0

internal fun Float.toPackedByte(): Byte = (this * 256.0f / 360.0f).toInt().toByte()

internal fun Byte.fromPackedByte(): Float = this * 360.0f / 256.0f