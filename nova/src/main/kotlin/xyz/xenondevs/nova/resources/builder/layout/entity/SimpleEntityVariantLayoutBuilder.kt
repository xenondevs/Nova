package xyz.xenondevs.nova.resources.builder.layout.entity

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder

@RegistryElementBuilderDsl
class SimpleEntityVariantLayoutBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) : EntityVariantLayoutBuilder<EntityVariantLayout.Simple>() {
    
    private var texture: ResourcePath<ResourceType.Texture>? = null
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [texture].
     */
    fun texture(texture: ResourcePath<ResourceType.Texture>) {
        this.texture = texture
    }
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [texture].
     */
    fun texture(texture: String) {
        this.texture = ResourcePath.Companion.of(ResourceType.Texture, texture, namespace)
    }
    
    override fun build() = EntityVariantLayout.Simple(
        texture ?: throw IllegalArgumentException("Texture is not defined")
    )
    
}