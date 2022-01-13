package xyz.xenondevs.nova.api

interface NovaMaterial {
    
    /**
     * Gets the id of this [NovaMaterial] in the format of nova:name
     */
    val id: String
    
    /**
     * Gets the localized name for this [NovaMaterial].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    fun getLocalizedName(locale: String): String
    
}