package xyz.xenondevs.nova.api

import xyz.xenondevs.nova.api.data.NamespacedId
import xyz.xenondevs.nova.api.item.NovaItem
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.material.NovaBlock
import xyz.xenondevs.nova.util.namespacedId
import xyz.xenondevs.nova.api.block.NovaBlock as INovaBlock

internal class ApiBlockWrapper(val block: NovaBlock): INovaBlock {
    
    /**
     * The [NamespacedId] of this [NovaMaterial].
     */
    override val id = block.id.namespacedId
    
    /**
     * The [NovaItem] of this [NovaBlock] or null if there is none.
     */
    override val item: NovaItem? = block.item?.let(::ApiItemWrapper)
    
    /**
     * Gets the localized name for this [NovaBlock].
     * The [locale] should be in the same format as the language file names in
     * resource packs (e.g. en_us).
     */
    override fun getLocalizedName(locale: String): String {
        return LocaleManager.getTranslation(locale, block.localizedName)
    }
}