package xyz.xenondevs.nova.util

fun Float.toPackedByte(): Byte {
    return (this * 256.0f / 360.0f).toInt().toByte()
}

fun Byte.fromPackedByte(): Float {
    return this * 360.0f / 256.0f
}
