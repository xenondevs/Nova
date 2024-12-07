package xyz.xenondevs.nova.resources.builder.model.transform

import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.task.model.ModelContent

internal class FlatteningAction : ContextualModelBuildAction {
    
    override fun apply(model: Model, context: ModelContent): Model {
        return model.flattened(context)
    }

}