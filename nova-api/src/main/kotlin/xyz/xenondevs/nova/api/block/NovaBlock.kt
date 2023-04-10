package xyz.xenondevs.nova.api.block

import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.api.item.NovaItem
import xyz.xenondevs.nova.api.material.NovaMaterial

interface NovaBlock {
    
    /**
     * The [NamespacedId] of this [NovaMaterial].
     */
    val id: NamespacedId
    
    /**
     * The [NovaItem] of this [NovaBlock] or null if there is none.
     */
    val item: NovaItem?
    
    /**
     * Gets the localized name for this [NovaBlock].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    fun getLocalizedName(locale: String): String
    
}