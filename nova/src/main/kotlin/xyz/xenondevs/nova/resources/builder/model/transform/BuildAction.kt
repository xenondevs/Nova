package xyz.xenondevs.nova.resources.builder.model.transform

import org.joml.Matrix4d
import xyz.xenondevs.nova.resources.builder.model.Model
import xyz.xenondevs.nova.resources.builder.task.ModelContent

/**
 * Something that can be applied to a model to modify it.
 */
internal sealed interface BuildAction

/**
 * A [BuildAction] that requires a [ModelContent] context.
 */
internal sealed interface ContextualModelBuildAction : BuildAction {
    
    /**
     * Applies this action to [model] with the given [context] and returns the modified model.
     */
    fun apply(model: Model, context: ModelContent): Model
    
}

/**
 * A [BuildAction] that does not require a [ModelContent] context.
 */
internal sealed interface NonContextualModelBuildAction : BuildAction {
    
    /**
     * Applies this action to [model] without any context and returns the modified model.
     */
    fun apply(model: Model): Model
    
}

/**
 * A transformation that can be applied to a model.
 */
internal sealed interface Transform : BuildAction {
    
    /**
     * Applies this transformation the given [matrix].
     */
    fun apply(matrix: Matrix4d)
    
}