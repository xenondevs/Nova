package xyz.xenondevs.nova.i18n

import com.google.gson.JsonObject
import net.minecraft.locale.Language
import net.minecraft.network.chat.FormattedText
import net.minecraft.util.FormattedCharSequence
import org.bukkit.entity.Player
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.initialize.Dispatcher
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.formatSafely
import xyz.xenondevs.nova.util.runAsyncTask

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dispatcher = Dispatcher.ASYNC,
    dependsOn = [ResourceGeneration.PostWorld::class]
)
object LocaleManager {
    
    private val loadedLangs = HashSet<String>()
    private val loadingLangs = HashSet<String>()
    
    private lateinit var translationProviders: MutableMap<String, HashMap<String, String>>
    
    @InitFun
    private fun init() {
        translationProviders = ResourceLookups.LANGUAGE.mapValuesTo(HashMap()) { HashMap(it.value) }
        loadLang("en_us")
        Language.inject(NovaLanguage)
    }
    
    private fun loadLang(lang: String) {
        if (lang in loadingLangs)
            return
        
        loadingLangs += lang
        
        if (Nova.isEnabled) runAsyncTask {
            val file = ResourcePackBuilder.MCASSETS_DIR.resolve("assets/minecraft/lang/$lang.json")
            val json = file.parseJson() as JsonObject
            val translations = json.entrySet().associateTo(HashMap()) { it.key to it.value.asString }
            
            synchronized(LocaleManager) {
                translationProviders.getOrPut(lang, ::HashMap) += translations
                loadedLangs += lang
                loadingLangs -= lang
            }
        }
    }
    
    @Synchronized
    fun hasTranslation(lang: String, key: String): Boolean {
        if (!::translationProviders.isInitialized) return false
        if (lang !in loadedLangs) loadLang(lang.lowercase())
        return translationProviders[lang.lowercase()]?.containsKey(key) ?: false
    }
    
    @Synchronized
    fun getFormatStringOrNull(lang: String, key: String): String? {
        if (!::translationProviders.isInitialized) return null
        if (lang !in loadedLangs) loadLang(lang.lowercase())
        return translationProviders[lang.lowercase()]?.get(key)
    }
    
    @Synchronized
    fun getFormatString(lang: String, key: String): String {
        var formatString = getFormatStringOrNull(lang, key)
        if (formatString == null && lang != "en_us")
            formatString = getFormatStringOrNull("en_us", key)
        return formatString ?: key
    }
    
    @Synchronized
    fun getAllFormatStrings(key: String): Set<String> {
        return loadedLangs.mapTo(HashSet()) { getFormatString(it, key) }
    }
    
    @Synchronized
    fun getTranslationOrNull(lang: String, key: String, vararg args: Any): String? {
        if (!::translationProviders.isInitialized) return null
        if (lang !in loadedLangs) loadLang(lang.lowercase())
        return translationProviders[lang.lowercase()]?.get(key)?.let { String.formatSafely(it, *args) }
    }
    
    @Synchronized
    fun getTranslation(lang: String, key: String, vararg args: Any): String {
        var translation = getTranslationOrNull(lang, key, *args)
        if (translation == null && lang != "en_us")
            translation = getTranslationOrNull("en_us", key, *args)
        return translation ?: key
    }
    
    @Synchronized
    fun getAllTranslations(key: String, vararg args: Any): Set<String> {
        return loadedLangs.mapTo(HashSet()) { getTranslation(it, key, *args) }
    }
    
    @Suppress("DEPRECATION")
    @Synchronized
    fun getTranslation(player: Player, key: String, vararg args: Any): String {
        return getTranslation(player.locale, key, *args)
    }
    
    private object NovaLanguage : Language() {
        
        private val delegate = getInstance()
        
        override fun getOrDefault(key: String): String {
            return getTranslationOrNull("en_us", key) ?: delegate.getOrDefault(key)
        }
        
        override fun getOrDefault(key: String, fallback: String): String {
            return getTranslationOrNull("en_us", key) ?: delegate.getOrDefault(key, fallback)
        }
        
        override fun has(key: String): Boolean {
            return hasTranslation("en_us", key) || delegate.has(key)
        }
        
        override fun isDefaultRightToLeft(): Boolean {
            return delegate.isDefaultRightToLeft
        }
        
        override fun getVisualOrder(text: FormattedText): FormattedCharSequence {
            return delegate.getVisualOrder(text)
        }
        
    }
    
}
