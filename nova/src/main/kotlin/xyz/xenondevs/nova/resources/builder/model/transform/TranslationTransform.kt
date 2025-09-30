package xyz.xenondevs.nova.resources.builder.model.transform

import org.joml.Matrix4d
import org.joml.Vector3d
import org.joml.Vector3dc
import xyz.xenondevs.nova.resources.builder.model.Model

/**
 * A transformation that translates a model by [v].
 */
internal data class TranslationTransform(val v: Vector3dc) : NonContextualModelBuildAction, Transform {
    
    override fun apply(model: Model): Model {
        val elements = model.elements ?: throw IllegalArgumentException("Model does not define elements list")
        return model.copy(elements = elements.map { element ->
            element.copy(
                from = element.from.add(v, Vector3d()),
                to = element.to.add(v, Vector3d()),
                rotation = element.rotation?.copy(origin = element.rotation.origin.add(v, Vector3d())),
                faces = element.faces.mapValues { (direction, face) -> face.copy(uv = face.uv ?: element.generateUV(direction)) }
            )
        })
    }
    
    override fun apply(matrix: Matrix4d) {
        matrix.translateLocal(v)
    }
    
}