package xyz.xenondevs.nova.util

import org.bukkit.util.BoundingBox
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4dc
import org.joml.primitives.AABBd
import org.joml.primitives.AABBdc
import xyz.xenondevs.nova.resources.builder.model.Model
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
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

/**
 * Rotates [this][Vector3d] by [angleRad] radians around [axis].
 */
internal fun Vector3d.rotate(axis: Model.Axis, angleRad: Double): Vector3d =
    when (axis) {
        Model.Axis.X -> rotateX(angleRad)
        Model.Axis.Y -> rotateY(angleRad)
        Model.Axis.Z -> rotateZ(angleRad)
    }

/**
 * Sets [this][Vector3d] component at [axis] to [value].
 */
internal fun Vector3d.set(axis: Model.Axis, value: Double): Vector3d =
    setComponent(axis.ordinal, value)

/**
 * Gets the component of [this][Vector3d] at [axis].
 */
internal fun Vector3dc.get(axis: Model.Axis): Double =
    get(axis.ordinal)

/**
 * Gets the first axis of [this][Vector3d] whose value is larger than 1e-6.
 */
internal fun Vector3dc.firstNonZeroAxis(): Model.Axis? =
    Model.Axis.entries.firstOrNull { axis -> abs(get(axis)) > 1e-6 }

/**
 * Returns the [Vector4dc.x] component of [this][Vector4dc].
 */
internal operator fun Vector4dc.component1(): Double = x()

/**
 * Returns the [Vector4dc.y] component of [this][Vector4dc].
 */
internal operator fun Vector4dc.component2(): Double = y()

/**
 * Returns the [Vector4dc.z] component of [this][Vector4dc].
 */
internal operator fun Vector4dc.component3(): Double = z()

/**
 * Returns the [Vector4dc.w] component of [this][Vector4dc].
 */
internal operator fun Vector4dc.component4(): Double = w()

/**
 * Converts a Bukkit [BoundingBox] to a JOML [AABBdc].
 */
internal fun BoundingBox.toAabb(): AABBdc =
    AABBd(minX, minY, minZ, maxX, maxY, maxZ)

internal object MathUtils {
    
    /**
     * Returns the greatest common divisor of the given numbers.
     */
    fun gcd(a: Int, b: Int): Int {
        // Euclidean algorithm
        var a = a
        var b = b
        while (b > 0) {
            val temp = b
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
    
    /**
     * Returns the greatest common divisor of all numbers in [numbers].
     */
    fun gcd(numbers: Iterable<Int>): Int {
        return gcd(numbers.iterator())
    }
    
    /**
     * Returns the greatest common divisor of all numbers in [numbers].
     */
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
    
    /**
     * Encodes [array] as an [Int] by treating each element as a bit,
     * where the first element is the least significant bit.
     */
    fun convertBooleanArrayToInt(array: BooleanArray): Int {
        var i = 0
        for (element in array) {
            i = (i shl 1) or if (element) 1 else 0
        }
        
        return i
    }
    
}