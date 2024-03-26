package xyz.xenondevs.nova.data.resources.builder.model.transform

import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.xenondevs.nova.data.resources.builder.model.Model

/**
 * A transformation that scales a model by [scale] around [pivot].
 */
internal data class ScaleTransform(
    val pivot: Vector3dc,
    val scale: Vector3dc,
    val keepDisplaySize: Boolean = false
) : NonContextualModelBuildAction, Transform {
    
    override fun apply(model: Model): Model {
        var elements = model.elements ?: throw IllegalArgumentException("Model does not define elements list")
        elements = elements.map { element ->
            element.copy(
                from = scaled(element.from), to = scaled(element.to),
                rotation = element.rotation?.let { it.copy(origin = scaled(it.origin)) },
                faces = element.faces.mapValues { (direction, face) -> face.copy(uv = face.uv ?: element.generateUV(direction)) }
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
    
    override fun apply(matrix: Matrix4d) {
        matrix.translate(-(8 - pivot.x()) / 16, -(8 - pivot.y()) / 16, -(8 - pivot.z()) / 16)
        matrix.scale(scale)
        matrix.translate((8 - pivot.x()) / 16, (8 - pivot.y()) / 16, (8 - pivot.z()) / 16)
    }
    
}