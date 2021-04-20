package xyz.xenondevs.nova.util.point

import org.bukkit.Axis

fun List<Double>.toPoint2D() = Point2D(this[0], this[1])

data class Point2D(var x: Double, var y: Double) {
    
    fun rotateClockwise() {
        if (x == 0.0 && y == 0.0) return
        
        if (x > 0 && y > 0) { // top right
            val temp = x
            x = y
            y = -temp
        } else if (x > 0 && y < 0) { // bottom right
            val temp = y
            y = -x
            x = temp
        } else if (x < 0 && y < 0) { // bottom left
            val temp = y
            y = -x
            x = temp
        } else if (x < 0 && y > 0) { // top left
            val temp = y
            y = -x
            x = temp
        } else if (y == 0.0) { // on x axis
            y = -x
            x = 0.0
        } else if (x == 0.0) { // on y axis
            x = y
            y = 0.0
        }
    }
    
    fun to3D(missingAxis: Axis, value: Double) =
        when (missingAxis) {
            Axis.X -> Point3D(value, y, x)
            Axis.Y -> Point3D(y, value, x)
            Axis.Z -> Point3D(x, y, value)
        }
    
}