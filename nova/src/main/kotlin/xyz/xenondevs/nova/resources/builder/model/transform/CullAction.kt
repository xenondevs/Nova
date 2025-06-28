package xyz.xenondevs.nova.resources.builder.model.transform

import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.resources.builder.model.Model

internal class CullAction(private val cull: Set<Model.Direction>) : NonContextualModelBuildAction {
    
    override fun apply(model: Model): Model {
        return model.copy(elements = model.elements?.map { element ->
            element.copy(faces = element.faces.filterTo(enumMap()) { (_, face) -> face.cullface !in cull })
        })
    }
    
}