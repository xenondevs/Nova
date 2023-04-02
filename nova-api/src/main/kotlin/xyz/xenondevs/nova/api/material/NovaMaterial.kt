package xyz.xenondevs.nova.api.material

import xyz.xenondevs.nova.api.data.NamespacedId

@Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
interface NovaMaterial {
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * The [NamespacedId] of this [NovaMaterial].
     */
    val id: NamespacedId
    
    @Deprecated("Use NovaBlockRegistry and NovaItemRegistry instead")
    /**
     * Gets the localized name for this [NovaMaterial].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    fun getLocalizedName(locale: String): String
    
}