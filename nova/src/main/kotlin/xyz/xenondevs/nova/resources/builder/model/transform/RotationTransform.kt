package xyz.xenondevs.nova.resources.builder.model.transform

import org.joml.Math
import org.joml.Matrix2d
import org.joml.Matrix4d
import org.joml.Vector2d
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4d
import org.joml.Vector4dc
import xyz.xenondevs.commons.collections.after
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.Model.*
import xyz.xenondevs.nova.resources.builder.model.Model.Element.Face
import xyz.xenondevs.nova.resources.builder.model.Model.Element.Rotation
import xyz.xenondevs.nova.util.component1
import xyz.xenondevs.nova.util.component2
import xyz.xenondevs.nova.util.component3
import xyz.xenondevs.nova.util.component4
import xyz.xenondevs.nova.util.firstNonZeroAxis
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.rotate
import xyz.xenondevs.nova.util.round
import xyz.xenondevs.nova.util.set
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign

// it may make sense to rewrite the elements transformation to convert into some intermediate representation that is easier to work with

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
        
        if (rotation != null) {
            // the rotation was either consumed, not present, or would've thrown before
            check(angle == 0.0)
            
            val direction = Vector3d()
            direction.set(rotation.axis, rotation.angle)
            direction.rotate(axis, Math.PI / 2 * fullRots)
            
            val newAxis = direction.firstNonZeroAxis()
            check(newAxis != null)
            
            rotation = rotation.copy(
                origin = rotated(rotation.origin, fullRots),
                axis = newAxis,
                angle = direction.get(newAxis)
            )
        } else if (angle != 0.0) {
            rotation = Rotation(angle, axis, pivot, rescale)
        }
        
        element.copy(
            from = from,
            to = to,
            rotation = rotation,
            faces = if (uvLock) rotatedFacesUvLocked(element, fullRots) else rotatedFaces(element.faces, fullRots)
        )
    }
    
    private fun rotatedFacesUvLocked(element: Element, rots: Int): Map<Direction, Face> {
        val faces = element.faces
        if (rots == 0)
            return faces
        
        val result = enumMap<Direction, Face>()
        
        for (dir in Direction.entries) {
            val face = faces[dir] ?: continue
            
            if (dir.axis != axis) {
                // swap face
                val newDir = rotatedDirection(dir, rots)
                
                // rotations related to the fact that the element rotation changes the relative position
                // of the from and to points in the object space
                val r = when (axis) {
                    Axis.X -> when {
                        dir == Direction.NORTH -> 2
                        newDir == Direction.NORTH -> -2
                        else -> 0
                    }
                    
                    Axis.Z -> rots
                    Axis.Y -> 0
                }
                
                result[newDir] = face.copy(
                    uv = rotatedUv(element, dir, face, r),
                    rotation = (face.rotation + r * 90).mod(360),
                    cullface = face.cullface?.let { rotatedDirection(dir, rots) }
                )
            } else {
                result[dir] = face.copy(uv = rotatedUvReordered(element, dir, face, rots * -dirSign(dir)))
            }
            
        }
        
        return result
    }
    
    /**
     * Rotates the uv of [element's][element] [face] at [dir] by 90*rots degrees around (8, 8) counter-clockwise.
     *
     */
    private fun rotatedUv(element: Element, dir: Direction, face: Face, rots: Int): Vector4dc? {
        if (rots == 0)
            return face.uv
        
        val uv = face.uv ?: element.generateUV(dir)
        return rotatedUv(uv, rots)
    }
    
    /**
     * Rotates the uv of [element's][element] [face] at [dir] by 90*rots degrees around (8, 8) counter-clockwise
     * without letting the rotation affect the draw direction (uv vertex order).
     */
    private fun rotatedUvReordered(element: Element, dir: Direction, face: Face, rots: Int): Vector4dc? {
        if (rots == 0)
            return face.uv
        
        val uv = face.uv ?: element.generateUV(dir)
        
        // remember mirroring to reapply later
        val mirrorU = uv.x() > uv.z()
        val mirrorV = uv.y() > uv.w()
        
        val (u0, v0, u1, v1) = rotatedUv(uv, rots)
        
        // sort uv vertices, then reapply original mirroring
        val minU = min(u0, u1)
        val minV = min(v0, v1)
        val maxU = max(u0, u1)
        val maxV = max(v0, v1)
        
        return Vector4d(
            if (mirrorU) maxU else minU,
            if (mirrorV) maxV else minV,
            if (mirrorU) minU else maxU,
            if (mirrorV) minV else maxV
        )
    }
    
    /**
     * Rotates the [uv] by 90*rots degrees around (8, 8) counter-clockwise
     */
    private fun rotatedUv(uv: Vector4dc, rots: Int): Vector4dc {
        val uv0 = Vector2d(uv.x() - 8, uv.y() - 8)
        val uv1 = Vector2d(uv.z() - 8, uv.w() - 8)
        
        val rot = Matrix2d().rotate(Math.PI / 2 * rots)
        uv0.mul(rot)
        uv1.mul(rot)
        
        return Vector4d(uv0.x + 8.0, uv0.y + 8.0, uv1.x + 8.0, uv1.y + 8.0)
    }
    
    private fun rotatedFaces(faces: Map<Direction, Face>, rots: Int): Map<Direction, Face> {
        if (rots == 0)
            return faces
        
        val result = enumMap<Direction, Face>()
        for (dir in Direction.entries) {
            val face = faces[dir] ?: continue
            if (dir.axis == axis) {
                // rotate texture
                result[dir] = face.copy(rotation = (face.rotation + rots * 90 * -dirSign(dir)).mod(360))
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
                
                result[newDir] = face.copy(
                    rotation = (face.rotation + rotation).mod(360),
                    cullface = face.cullface?.let { rotatedDirection(dir, rots) }
                )
            }
        }
        
        return result
    }
    
    private fun rotatedDirection(dir: Direction, rots: Int): Direction =
        directionsAround(axis).after(dir, rots)
    
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