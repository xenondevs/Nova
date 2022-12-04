package xyz.xenondevs.nova.util

import kotlin.math.pow
import kotlin.math.round

val Boolean.intValue: Int
    get() = if (this) 1 else 0

fun Double.toFixedPoint(): Short = (this * 4069).toInt().toShort()

fun Short.fromFixedPoint(): Double = this / 4096.0

internal fun Float.toPackedByte(): Byte = (this * 256.0f / 360.0f).toInt().toByte()

internal fun Byte.fromPackedByte(): Float = this * 360.0f / 256.0f

internal fun Float.roundToDecimalPlaces(n: Int): Float {
    val multiplier = 10.0.pow(n.toDouble())
    return (round(this * multiplier) / multiplier).toFloat()
}

internal fun Double.roundToDecimalPlaces(n: Int): Double {
    val multiplier = 10.0.pow(n.toDouble())
    return round(this * multiplier) / multiplier
}

fun <T> Iterable<T>.sumOfNoOverflow(selector: (T) -> Long): Long {
    return try {
        var sum = 0L
        for (element in this) {
            sum = Math.addExact(sum, selector(element))
        }
        
        sum
    } catch (e: ArithmeticException) {
        Long.MAX_VALUE
    }
}

object MathUtils {
    
    fun convertBooleanArrayToInt(array: BooleanArray): Int {
        var i = 0
        for (element in array)
            i = (i shl 1) or if (element) 1 else 0
        
        return i
    }
    
}