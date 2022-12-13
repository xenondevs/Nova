package xyz.xenondevs.nova.data.resources.builder.content.material.info

import xyz.xenondevs.nova.data.NamespacedId

internal class RegisteredMaterial(
    val id: NamespacedId,
    val itemInfo: ItemModelInformation,
    val blockInfo: BlockModelInformation,
    val armor: NamespacedId?
)

internal interface ModelInformation {
    val id: NamespacedId
    val models: List<String>
}