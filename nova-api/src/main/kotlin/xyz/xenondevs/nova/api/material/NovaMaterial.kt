package xyz.xenondevs.nova.api.material

import xyz.xenondevs.nova.api.data.NamespacedId

interface NovaMaterial {
    
    /**
     * Gets the [NamespacedId] of this [NovaMaterial]
     */
    val id: NamespacedId
    
    /**
     * Gets the localized name for this [NovaMaterial].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    fun getLocalizedName(locale: String): String
    
}