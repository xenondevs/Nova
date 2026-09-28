package xyz.xenondevs.nova.packetentity

import org.bukkit.Location
import org.bukkit.entity.Player

internal fun Location.positionEquals(other: Location): Boolean =
    world == other.world
        && x == other.x
        && y == other.y
        && z == other.z

internal fun Double.toFixedPoint(): Short = (this * 4096).toInt().toShort()

internal fun Short.fromFixedPoint(): Double = this / 4096.0

internal fun Float.toPackedByte(): Byte = (this * 256.0f / 360.0f).toInt().toByte()

internal fun Byte.fromPackedByte(): Float = this * 360.0f / 256.0f
