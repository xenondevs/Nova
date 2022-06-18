package xyz.xenondevs.nova.i18n

import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence
import org.bukkit.entity.Player
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.util.formatSafely

object LocaleManager : Initializable() {
    
    override val inMainThread = false
    override val dependsOn = setOf(Resources)
    
    private lateinit var translationProviders: Map<String, Map<String, String>>
    
    override fun init() {
        LOGGER.info("Injecting translations")
        translationProviders = Resources.languageLookup
        Language.inject(NovaLanguage)
    }
    
    fun getTranslation(lang: String, key: String, vararg args: Any): String {
        var translation = getTranslationOrNull(lang, key, *args)
        if (translation == null && lang != "en_us")
            translation = getTranslationOrNull("en_us", key, *args)
        return translation ?: key
    }
    
    fun getTranslationOrNull(lang: String, key: String, vararg args: Any): String? {
        if (!::translationProviders.isInitialized) return null
        return translationProviders[lang]?.get(key)?.let { String.formatSafely(it, *args) }
    }
    
    fun hasTranslation(lang: String, key: String): Boolean {
        if (!::translationProviders.isInitialized) return false
        return translationProviders[lang]?.containsKey(key) ?: false
    }
    
    fun getTranslation(player: Player, key: String, vararg args: Any): String {
        return getTranslation(player.locale, key, *args)
    }
    
    fun getTranslatedName(lang: String, material: ItemNovaMaterial): String {
        return getTranslation(lang, material.localizedName)
    }
    
    fun getTranslatedName(player: Player, material: ItemNovaMaterial): String {
        return getTranslation(player, material.localizedName)
    }
    
    private object NovaLanguage : Language() {
        
        private val delegate = getInstance()
        
        override fun getOrDefault(key: String): String {
            return getTranslationOrNull("en_us", key) ?: delegate.getOrDefault(key)
        }
        
        override fun has(key: String): Boolean {
            return hasTranslation("en_us", key) || delegate.has(key)
        }
        
        override fun isDefaultRightToLeft(): Boolean {
            return delegate.isDefaultRightToLeft
        }
        
        override fun getVisualOrder(text: FormattedText?): FormattedCharSequence {
            return delegate.getVisualOrder(text)
        }
        
    }
    
}
