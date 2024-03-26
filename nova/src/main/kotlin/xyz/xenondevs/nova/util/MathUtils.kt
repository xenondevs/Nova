package xyz.xenondevs.nova.util

import org.joml.Vector3d
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round

internal val Boolean.intValue: Int
    get() = if (this) 1 else 0

internal fun Double.toFixedPoint(): Short = (this * 4069).toInt().toShort()

internal fun Short.fromFixedPoint(): Double = this / 4096.0

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

internal inline fun <T> Iterable<T>.sumOfNoOverflow(selector: (T) -> Long): Long {
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

internal fun Int.ceilDiv(other: Int): Int = (this + other - 1) / other

internal fun Vector3d.round(decimalPlaces: Int): Vector3d {
    val multiplier = 10.0.pow(decimalPlaces.toDouble())
    mul(multiplier)
    round()
    div(multiplier)
    return this
}

internal fun max(a: Vector3d, b: Vector3d): Vector3d {
    return Vector3d(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))
}

internal object MathUtils {
    
    fun convertBooleanArrayToInt(array: BooleanArray): Int {
        var i = 0
        for (element in array)
            i = (i shl 1) or if (element) 1 else 0
        
        return i
    }
    
}