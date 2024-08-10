package xyz.xenondevs.nova.resources.builder.model.transform

import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.resources.builder.task.model.ModelContent

/**
 * A combination of two models.
 */
internal data class CombinationAction(val other: ModelBuilder) : ContextualModelBuildAction {
    
    override fun apply(model: Model, context: ModelContent): Model {
        val otherModel = other.build(context).flattened(context)

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
    
}