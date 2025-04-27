package xyz.xenondevs.nova.util

import org.joml.Vector3d
import java.util.concurrent.atomic.AtomicInteger
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
    var sum = 0L
    for (element in this) {
        val element = selector(element)
        val result = sum + element
        
        // if the sign of both arguments is different from the result, an overflow occurred
        if ((sum xor result) and (element xor result) < 0)
            return Long.MAX_VALUE
        
        sum = result
    }
    
    return sum
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

internal fun AtomicInteger.decrementIfGreaterThanZero(): Boolean {
    while (true) {
        val current = get()
        if (current <= 0)
            return false
        if (compareAndSet(current, current - 1))
            return true
    }
}

internal object MathUtils {
    
    /**
     * Returns the greatest common divisor of the given numbers.
     */
    fun gcd(a: Int, b: Int): Int {
        // Euclidean algorithm
        var a = a
        var b = b
        while (b > 0) {
            var temp = b
            b = a % b
            a = temp
        }
        return a
    }
    
    /**
     * Returns the greatest common divisor of the given [numbers].
     */
    fun gcd(vararg numbers: Int): Int {
        var gcd = numbers[0]
        for (i in 1..<numbers.size) {
            gcd = gcd(gcd, numbers[i])
        }
        return gcd
    }
    
    /**
     * Returns the greatest common divisor of all numbers provided by [numbers].
     */
    fun gcd(numbers: Iterator<Int>): Int {
        if (!numbers.hasNext())
            throw IllegalArgumentException("Iterator has no elements")
        
        var gcd = numbers.next()
        while (numbers.hasNext()) {
            gcd = gcd(gcd, numbers.next())
        }
        
        return gcd
    }
    
    fun gcd(numbers: Iterable<Int>): Int {
        return gcd(numbers.iterator())
    }
    
    fun gcd(numbers: Sequence<Int>): Int {
        return gcd(numbers.iterator())
    }
    
    /**
     * Returns the least common multiple of the given numbers.
     */
    fun lcm(a: Int, b: Int): Int {
        return a * (b / gcd(a, b))
    }
    
    /**
     * Returns the least common multiple of the given [numbers].
     */
    fun lcm(vararg numbers: Int): Int {
        var lcm = numbers[0]
        for (i in 1..<numbers.size) {
            lcm = lcm(lcm, numbers[i])
        }
        return lcm
    }
    
    fun convertBooleanArrayToInt(array: BooleanArray): Int {
        var i = 0
        for (element in array) {
            i = (i shl 1) or if (element) 1 else 0
        }
        
        return i
    }
    
}