package xyz.xenondevs.nova.resources.builder.model.transform

import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.Model.*
import xyz.xenondevs.nova.resources.builder.model.Model.Element.Face
import xyz.xenondevs.nova.resources.builder.model.Model.Element.Rotation
import xyz.xenondevs.nova.util.round
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

/**
 * A transformation that rotates  a model around [axis] and [pivot] by [rot] degrees
 * with [uvLock] and [rescale] parameters.
 */
internal data class RotationTransform(
    val pivot: Vector3dc,
    val axis: Axis, val rot: Double,
    val uvLock: Boolean, val rescale: Boolean
) : NonContextualModelBuildAction, Transform {
    
    override fun apply(matrix: Matrix4d) {
        if (uvLock)
            throw UnsupportedOperationException("UV lock is not supported in matrix transformations")
        if (rescale) // TODO: this can be implemented
            throw UnsupportedOperationException("Rescale is not supported in matrix transformations")
        
        matrix.translate(-(8 - pivot.x()) / 16, -(8 - pivot.y()) / 16, -(8 - pivot.z()) / 16)
        when (axis) {
            Axis.X -> matrix.rotateX(Math.toRadians(rot))
            Axis.Y -> matrix.rotateY(Math.toRadians(rot))
            Axis.Z -> matrix.rotateZ(Math.toRadians(rot))
        }
        matrix.translate((8 - pivot.x()) / 16, (8 - pivot.y()) / 16, (8 - pivot.z()) / 16)
    }
    
    override fun apply(model: Model): Model {
        if (rot % 22.5 != 0.0)
            throw TransformException("Illegal rotation: $rot (needs to a multiple of 22.5)", this)
        
        val elements = model.elements ?: throw IllegalArgumentException("Model does not define elements list")
        return model.copy(elements = rotatedElements(elements))
    }
    
    private fun rotatedElements(elements: List<Element>): List<Element> = elements.map { element ->
        var rotation = element.rotation
        
        var rot = rot % 360
        if (rotation != null) {
            if (rotation.axis == axis && rotation.origin == pivot) {
                rot += rotation.angle
                rotation = null // consumed
            } else if (rot % 90 != 0.0) {
                throw ElementTransformException("Rotation angle $rot requires rotation property, " +
                    "but it is already occupied by a different axis or pivot.", this, element)
            }
        }
        
        var fullRots = (rot / 90).toInt()
        var angle = rot % 90
        
        if (abs(angle) > 45) {
            fullRots = (fullRots + angle.sign.toInt()) % 4
            angle -= angle.sign * 90
        }
        
        val from: Vector3dc
        val to: Vector3dc
        if (fullRots != 0) {
            val sorted = sort(rotated(element.from, fullRots), rotated(element.to, fullRots))
            from = sorted.first
            to = sorted.second
        } else {
            from = element.from
            to = element.to
        }
        
        element.copy(
            from = from,
            to = to,
            rotation = if (angle != 0.0) Rotation(angle, axis, pivot, rescale) else rotation,
            faces = if (uvLock) rotatedFacesUvLocked(element.faces, fullRots) else rotatedFaces(element.faces, fullRots)
        )
    }
    
    private fun rotatedFaces(faces: Map<Direction, Face>, rots: Int): Map<Direction, Face> {
        if (rots == 0)
            return faces
        
        val result = enumMap<Direction, Face>()
        for (dir in Direction.entries) {
            val face = faces[dir] ?: continue
            if (dir.axis == axis) {
                // rotate texture
                val rotation = (face.rotation + rots * 90 * -dirSign(dir)).mod(360)
                result[dir] = face.copy(rotation = rotation)
            } else {
                // swap face
                val newDir = rotatedDirection(dir, rots)
                
                // rotations related to the fact that the element rotation changes the relative position
                // of the from and to points in the object space
                val rotation = when (axis) {
                    Axis.X -> when {
                        dir == Direction.NORTH -> -180
                        newDir == Direction.NORTH -> 180
                        else -> 0
                    }
                    
                    Axis.Z -> -90 * rots
                    Axis.Y -> 0
                }
                
                result[newDir] = face.copy(rotation = (face.rotation + rotation).mod(360))
            }
        }
        
        return result
    }
    
    private fun rotatedFacesUvLocked(faces: Map<Direction, Face>, rots: Int): Map<Direction, Face> {
        if (rots == 0)
            return faces
        
        val result = enumMap<Direction, Face>()
        for (dir in Direction.entries) {
            if (dir.axis != axis) {
                val newDir = rotatedDirection(dir, rots)
                result[newDir] = faces[dir] ?: continue
            }
        }
        
        return result
    }
    
    private fun rotatedDirection(dir: Direction, rots: Int): Direction {
        val rotatingFaces = directionsAround(axis)
        return rotatingFaces[(rotatingFaces.indexOf(dir) + rots).mod(rotatingFaces.size)]
    }
    
    private fun rotated(v: Vector3dc, fullRots: Int): Vector3dc {
        val result = Vector3d(v)
        result.sub(pivot)
        
        val radians = Math.toRadians(90.0 * fullRots)
        when (axis) {
            Axis.X -> result.rotateX(radians)
            Axis.Y -> result.rotateY(radians)
            Axis.Z -> result.rotateZ(radians)
        }
        
        result.add(pivot)
        result.round(10)
        
        return result
    }
    
    companion object {
        
        private val AROUND_X = listOf(Direction.NORTH, Direction.UP, Direction.SOUTH, Direction.DOWN)
        private val AROUND_Y = listOf(Direction.NORTH, Direction.WEST, Direction.SOUTH, Direction.EAST)
        private val AROUND_Z = listOf(Direction.UP, Direction.WEST, Direction.DOWN, Direction.EAST)
        
        private fun directionsAround(axis: Axis) = when (axis) {
            Axis.X -> AROUND_X
            Axis.Y -> AROUND_Y
            Axis.Z -> AROUND_Z
        }
        
        private fun dirSign(dir: Direction) = when (dir) {
            Direction.SOUTH, Direction.EAST, Direction.UP -> 1
            Direction.NORTH, Direction.WEST, Direction.DOWN -> -1
        }
        
        private fun sort(a: Vector3dc, b: Vector3dc): Pair<Vector3dc, Vector3dc> {
            val min = Vector3d(
                min(a.x(), b.x()),
                min(a.y(), b.y()),
                min(a.z(), b.z())
            )
            
            val max = Vector3d(
                max(a.x(), b.x()),
                max(a.y(), b.y()),
                max(a.z(), b.z())
            )
            
            return min to max
        }
        
    }
    
}