package xyz.xenondevs.nova.resources.builder.model.transform

import org.joml.Matrix4d
import org.joml.Vector2d
import org.joml.Vector2dc
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector4d
import org.joml.Vector4dc
import xyz.xenondevs.nova.resources.builder.model.Model

/**
 * A transformation that scales a model by [scale] around [pivot].
 *
 * @param pivot the pivot point around which the model is scaled
 * @param scale the scale factor
 * @param scaleUV whether to scale the UV coordinates of the model,
 * trying to preserve the pixel size
 * @param keepDisplaySize whether the display size properties should be
 * inversely scaled in order to keep the actual displayed size constant
 */
internal data class ScaleTransform(
    val pivot: Vector3dc,
    val scale: Vector3dc,
    val scaleUV: Boolean = false,
    val keepDisplaySize: Boolean = false
) : NonContextualModelBuildAction, Transform {
    
    override fun apply(model: Model): Model {
        var elements = model.elements ?: throw IllegalArgumentException("Model does not define elements list")
        elements = elements.map { element ->
            element.copy(
                from = scaled(element.from), to = scaled(element.to),
                rotation = element.rotation?.let { it.copy(origin = scaled(it.origin)) },
                faces = element.faces.mapValues { (direction, face) ->
                    var uv = face.uv ?: element.generateUV(direction)
                    if (scaleUV)
                        uv = scaledUV(direction, element.from, element.to, uv, face.rotation)
                    face.copy(uv = uv)
                }
            )
        }
        
        var display = model.display
        if (keepDisplaySize) {
            val inverseScale = Vector3d(1.0, 1.0, 1.0).div(scale, Vector3d())
            display = display.mapValues { (_, dp) -> dp.copy(scale = dp.scale.mul(inverseScale, Vector3d())) }
        }
        
        return model.copy(elements = elements, display = display)
    }
    
    private fun scaled(v: Vector3dc): Vector3dc =
        Vector3d(v).sub(pivot).mul(scale).add(pivot)
    
    /**
     * Adjusts the [uv] for [face] with [rotation] of an element with positions [from] and [to].
     */
    private fun scaledUV(face: Model.Direction, from: Vector3dc, to: Vector3dc, uv: Vector4dc, rotation: Int): Vector4dc {
        val (uv0, uAxis, vAxis) = getUVAxes(face, rotation)
        
        val elementSize = Vector3d(to).sub(from)
        val relPivot = Vector3d(pivot)
            .div(elementSize)
            .sub(uv0)
        val uvSize = Vector2d(uv.z(), uv.w()).sub(uv.x(), uv.y())
        val uvScale = Vector2d(uAxis.dot(scale), vAxis.dot(scale)).absolute()
        val uvPivot = Vector2d(uAxis.dot(relPivot), vAxis.dot(relPivot))
            .mul(uvSize)
            .add(Vector2d(uv.x(), uv.y()))
        
        return scaledUV(uv, uvPivot, uvScale)
    }
    
    /**
     * Adjusts the [uv] based on the [uvPivot] and [uvScale].
     */
    private fun scaledUV(uv: Vector4dc, uvPivot: Vector2dc, uvScale: Vector2dc): Vector4dc {
        val from = Vector2d(uv.x(), uv.y())
        val to = Vector2d(uv.z(), uv.w())
        
        from.sub(uvPivot).mul(uvScale).add(uvPivot)
        to.sub(uvPivot).mul(uvScale).add(uvPivot)
        
        return Vector4d(from.x(), from.y(), to.x(), to.y())
    }
    
    /**
     * Gets a tuple the uv axes and normalized UV origin for the given face and rotation. (uv0, uAxis, vAxis)
     */
    private fun getUVAxes(face: Model.Direction, rotation: Int): Triple<Vector3d, Vector3d, Vector3d> {
        val uv0 = Vector3d(0.0, 1.0, 1.0)
        val u = Vector3d(1.0, 0.0, 0.0)
        val v = Vector3d(0.0, -1.0, 0.0)
        
        fun rotate(axis: Model.Axis, angleDeg: Double) {
            val angleRad = Math.toRadians(angleDeg)
            uv0.sub(0.5, 0.5, 0.5).rotate(axis, angleRad).add(0.5, 0.5, 0.5)
            v.rotate(axis, angleRad)
            u.rotate(axis, angleRad)
        }
        
        when (face) {
            Model.Direction.SOUTH -> {
                rotate(Model.Axis.Z, -rotation.toDouble())
            }
            
            Model.Direction.EAST -> {
                rotate(Model.Axis.Y, 90.0)
                rotate(Model.Axis.X, -rotation.toDouble())
            }
            
            Model.Direction.NORTH -> {
                rotate(Model.Axis.Y, 180.0)
                rotate(Model.Axis.Z, rotation.toDouble())
            }
            
            Model.Direction.WEST -> {
                rotate(Model.Axis.Y, 270.0)
                rotate(Model.Axis.X, rotation.toDouble())
            }
            
            Model.Direction.UP -> {
                rotate(Model.Axis.X, -90.0)
                rotate(Model.Axis.Y, -rotation.toDouble())
            }
            
            Model.Direction.DOWN -> {
                rotate(Model.Axis.X, 90.0)
                rotate(Model.Axis.Y, rotation.toDouble())
            }
        }
        
        return Triple(uv0, u, v)
    }
    
    /**
     * Rotates [this][Vector3d] by [angleRad] radians around [axis].
     */
    private fun Vector3d.rotate(axis: Model.Axis, angleRad: Double): Vector3d =
        when (axis) {
            Model.Axis.X -> rotateX(angleRad)
            Model.Axis.Y -> rotateY(angleRad)
            Model.Axis.Z -> rotateZ(angleRad)
        }
    
    override fun apply(matrix: Matrix4d) {
        if (scaleUV)
            throw UnsupportedOperationException("Cannot apply UV adjustments to a matrix")
        
        matrix.translate(-(8 - pivot.x()) / 16, -(8 - pivot.y()) / 16, -(8 - pivot.z()) / 16)
        matrix.scale(scale)
        matrix.translate((8 - pivot.x()) / 16, (8 - pivot.y()) / 16, (8 - pivot.z()) / 16)
    }
    
}