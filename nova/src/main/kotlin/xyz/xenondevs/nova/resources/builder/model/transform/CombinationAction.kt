package xyz.xenondevs.nova.resources.builder.model.transform

import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.ModelContent

/**
 * A combination of two models.
 */
internal data class CombinationAction(val other: ModelBuilder) : ContextualModelBuildAction {
    
    override fun apply(model: Model, context: ModelContent): Model {
        val prefix = (0..Int.MAX_VALUE).first { i -> model.textures.keys.none { it.startsWith(i.toString()) } }.toString()
        
        val otherModel = other.build(context).flattened(context).withPrefixedTextureKeys(prefix)
        
        val textures = HashMap<String, String>()
        textures.putAll(model.textures)
        textures.putAll(otherModel.textures)
        
        val elements = if (model.elements != null || otherModel.elements != null)
            (model.elements ?: emptyList()) + (otherModel.elements ?: emptyList())
        else null
        
        return model.copy(
            textures = textures,
            elements = elements
        )
    }
    
    // assumes flattened model
    private fun Model.withPrefixedTextureKeys(prefix: String): Model = copy(
        textures = textures.entries.associate { (key, value) ->
            if (value.startsWith('#')) {
                prefix + key to "#" + prefix + value.substring(1)
            } else {
                prefix + key to value
            }
        },
        elements = elements?.map { element ->
            element.copy(faces = element.faces.mapValues { (_, face) ->
                face.copy(texture = "#" + prefix + face.texture.substring(1))
            })
        }
    )
    
}