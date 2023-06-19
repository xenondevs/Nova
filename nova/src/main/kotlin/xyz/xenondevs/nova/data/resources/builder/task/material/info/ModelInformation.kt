package xyz.xenondevs.nova.data.resources.builder.task.material.info

import net.minecraft.resources.ResourceLocation

internal class RegisteredMaterial(
    val id: ResourceLocation,
    val itemInfo: ItemModelInformation,
    val blockInfo: BlockModelInformation,
    val armor: ResourceLocation?
)

internal interface ModelInformation {
    val id: ResourceLocation
    val models: List<String>
}