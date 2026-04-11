package xyz.xenondevs.nova.resources.builder.layout.entity

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder

@RegistryElementBuilderDsl
class WolfEntityVariantLayoutBuilder internal constructor(
    private val namespace: String,
    val resourcePackBuilder: ResourcePackBuilder
) : EntityVariantLayoutBuilder<EntityVariantLayout.Wolf>() {
    
    private var configureAdultTextures: (WolfTextureSetBuilder.() -> Unit)? = null
    private var configureBabyTextures: (WolfTextureSetBuilder.() -> Unit)? = null
    
    /**
     * Configures the adult wolf textures.
     */
    fun adultTextures(adultTextures: WolfTextureSetBuilder.() -> Unit) {
        this.configureAdultTextures = adultTextures
    }
    
    /**
     * Configures the baby wolf textures.
     */
    fun babyTextures(babyTextures: WolfTextureSetBuilder.() -> Unit) {
        this.configureBabyTextures = babyTextures
    }

    override fun build(): EntityVariantLayout.Wolf {
        val adult = WolfTextureSetBuilder(namespace)
            .apply(configureAdultTextures ?: throw IllegalArgumentException("Adult textures are not defined"))
            .build()
        val baby = WolfTextureSetBuilder(namespace)
            .apply(configureBabyTextures ?: throw IllegalArgumentException("Baby textures are not defined"))
            .build()
        return EntityVariantLayout.Wolf(adult, baby)
    }
    
}

@RegistryElementBuilderDsl
class WolfTextureSetBuilder internal constructor(
    private val namespace: String,
) {
    
    private var wild: ResourcePath<ResourceType.Texture>? = null
    private var tame: ResourcePath<ResourceType.Texture>? = null
    private var angry: ResourcePath<ResourceType.Texture>? = null
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [wild].
     */
    fun wild(wild: ResourcePath<ResourceType.Texture>) {
        this.wild = wild
    }
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [wild].
     */
    fun wild(wild: String) {
        this.wild = ResourcePath.of(ResourceType.Texture, wild, namespace)
    }
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [tame].
     */
    fun tame(tame: ResourcePath<ResourceType.Texture>) {
        this.tame = tame
    }
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [tame].
     */
    fun tame(tame: String) {
        this.tame = ResourcePath.of(ResourceType.Texture, tame, namespace)
    }
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [angry].
     */
    fun angry(angry: ResourcePath<ResourceType.Texture>) {
        this.angry = angry
    }
    
    /**
     * Uses the texture under `assets/<namespace>/textures/<texture>.png`, where
     * `namespace` and `texture` are defined by [angry].
     */
    fun angry(angry: String) {
        this.angry = ResourcePath.of(ResourceType.Texture, angry, namespace)
    }
    
    internal fun build() = EntityVariantLayout.WolfTextureSet(
        wild ?: throw IllegalArgumentException("wild texture is not defined"),
        tame ?: throw IllegalArgumentException("tame texture is not defined"),
        angry ?: throw IllegalArgumentException("angry texture is not defined"),
    )
    
}
