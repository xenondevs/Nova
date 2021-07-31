package xyz.xenondevs.nova.util

fun Double.toFixedPoint(): Short = (this * 4069).toInt().toShort()

fun Short.fromFixedPoint(): Double = this / 4096.0

fun Float.toPackedByte(): Byte = (this * 256.0f / 360.0f).toInt().toByte()

fun Byte.fromPackedByte(): Float = this * 360.0f / 256.0f

object MathUtils {
    
    fun convertBooleanArrayToInt(array: BooleanArray): Int {
        var i = 0
        for (element in array)
            i = (i shl 1) or if (element) 1 else 0
        
        return i
    }
    
}