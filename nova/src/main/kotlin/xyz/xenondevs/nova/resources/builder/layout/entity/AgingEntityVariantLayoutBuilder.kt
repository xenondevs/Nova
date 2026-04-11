package xyz.xenondevs.nova.resources.builder.layout.entity

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder

@RegistryElementBuilderDsl
class AgingEntityVariantLayoutBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) : EntityVariantLayoutBuilder<EntityVariantLayout.Aging>() {
    
    private var adultTexture: ResourcePath<ResourceType.Texture>? = null
    private var babyTexture: ResourcePath<ResourceType.Texture>? = null
    
    /**
     * Uses the adult texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [adultTexture].
     */
    fun adultTexture(adultTexture: ResourcePath<ResourceType.Texture>) {
        this.adultTexture = adultTexture
    }

    /**
     * Uses the adult texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [adultTexture].
     */
    fun adultTexture(adultTexture: String) {
        this.adultTexture = ResourcePath.of(ResourceType.Texture, adultTexture, namespace)
    }
    
    /**
     * Uses the baby texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [babyTexture].
     */
    fun babyTexture(babyTexture: ResourcePath<ResourceType.Texture>) {
        this.babyTexture = babyTexture
    }
    
    /**
     * Uses the baby texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [babyTexture].
     */
    fun babyTexture(babyTexture: String) {
        this.babyTexture = ResourcePath.of(ResourceType.Texture, babyTexture, namespace)
    }
    
    override fun build() = EntityVariantLayout.Aging(
        adultTexture ?: throw IllegalArgumentException("Adult texture is not defined"),
        babyTexture ?: throw IllegalArgumentException("Baby texture is not defined"),
    )
    
}
