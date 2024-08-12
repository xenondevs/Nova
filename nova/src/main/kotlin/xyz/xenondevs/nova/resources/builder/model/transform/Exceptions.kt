package xyz.xenondevs.nova.resources.builder.model.transform

import xyz.xenondevs.nova.resources.builder.model.Model

internal class TransformException(message: String, transformation: BuildAction) : Exception(
    "Cannot apply transformation $transformation: $message"
)

internal class ElementTransformException(message: String, transformation: BuildAction, element: Model.Element) : Exception(
    "Cannot apply transformation $transformation to $element: $message"
)