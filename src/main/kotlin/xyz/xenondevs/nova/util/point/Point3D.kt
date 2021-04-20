package xyz.xenondevs.nova.util.point

import org.bukkit.Axis
import org.bukkit.Axis.*
import org.bukkit.Location
import org.bukkit.World
import kotlin.math.max
import kotlin.math.min

fun List<Double>.toPoint3D() = Point3D(this[0], this[1], this[2])

fun Location.toPoint3D() = Point3D(x, y, z)

data class Point3D(var x: Double, var y: Double, var z: Double) {
    
    constructor(x: Int, y: Int, z: Int) : this(x.toDouble(), y.toDouble(), z.toDouble())
    
    fun getLocation(world: World) = Location(world, x, y, z)
    
    fun getBlock(world: World) = world.getBlockAt(x.toInt(), y.toInt(), z.toInt())
    
    fun rotateAroundYAxis(rotation: Int, origin: Point3D) {
        subtract(origin)
        val point2D = to2D(Y)
        repeat(rotation) { point2D.rotateClockwise() }
        setTo(point2D.to3D(Y, y))
        add(origin)
    }
    
    fun rotateAroundXAxis(rotation: Int, origin: Point3D) {
        subtract(origin)
        val point2D = to2D(X)
        repeat(rotation) { point2D.rotateClockwise() }
        setTo(point2D.to3D(X, x))
        add(origin)
    }
    
    fun to2D(ignore: Axis) =
        when (ignore) {
            X -> Point2D(z, y)
            Y -> Point2D(z, x)
            Z -> Point2D(x, y)
        }
    
    fun toDoubleArray() = doubleArrayOf(x, y, z)
    
    private fun subtract(point: Point3D) {
        x -= point.x
        y -= point.y
        z -= point.z
    }
    
    fun subtract(x: Double, y: Double, z: Double) {
        this.x -= x
        this.y -= y
        this.z -= z
    }
    
    private fun add(point: Point3D) {
        x += point.x
        y += point.y
        z += point.z
    }
    
    fun add(x: Double, y: Double, z: Double) {
        this.x += x
        this.y += y
        this.z += z
    }
    
    private fun setTo(point: Point3D) {
        x = point.x
        y = point.y
        z = point.z
    }
    
    companion object {
        
        fun sort(first: Point3D, second: Point3D): Pair<Point3D, Point3D> {
            val minX = min(first.x, second.x)
            val minY = min(first.y, second.y)
            val minZ = min(first.z, second.z)
            val maxX = max(first.x, second.x)
            val maxY = max(first.y, second.y)
            val maxZ = max(first.z, second.z)
            
            return Point3D(minX, minY, minZ) to Point3D(maxX, maxY, maxZ)
        }
        
    }
    
}