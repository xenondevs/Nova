package xyz.xenondevs.nova.data.resources.builder.content.material.info

internal class RegisteredMaterial(
    val id: String,
    val itemInfo: ItemModelInformation,
    val blockInfo: BlockModelInformation
)

internal interface ModelInformation {
    val id: String
    val models: List<String>
}