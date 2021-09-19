package xyz.xenondevs.nova.i18n

import com.google.gson.JsonObject
import org.bukkit.entity.Player
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.config.DEFAULT_CONFIG
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.data.JSON_PARSER
import xyz.xenondevs.nova.util.runAsyncTask
import java.net.URL
import java.util.zip.ZipInputStream

object LocaleManager {
    
    private lateinit var translationProviders: Map<String, Map<String, String>>
    
    fun init() {
        val url = URL(DEFAULT_CONFIG.getString("resource_pack.url"))
        runAsyncTask {
            LOGGER.info("Loading translations")
            val translationProviders = HashMap<String, HashMap<String, String>>()
            ZipInputStream(url.openStream()).use { zis ->
                generateSequence { zis.nextEntry }
                    .filter { !it.isDirectory && it.name.startsWith("assets/minecraft/lang/") }
                    .forEach { entry ->
                        val fileContent = zis.readNBytes(entry.size.toInt())
                        val langName = entry.name.substringAfterLast('/').substringBeforeLast('.')
                        val langObject = JSON_PARSER.parse(String(fileContent)) as JsonObject
                        
                        translationProviders[langName] = langObject.entrySet().associateTo(HashMap()) { it.key to it.value.asString }
                    }
            }
            
            this.translationProviders = translationProviders
            LOGGER.info("Finished loading translations")
        }
    }
    
    fun getTranslation(lang: String, key: String): String {
        if (!::translationProviders.isInitialized) return ""
        return translationProviders[lang]?.get(key) ?: ""
    }
    
    fun getTranslation(player: Player, key: String): String {
        return getTranslation(player.locale, key)
    }
    
    fun getTranslatedName(lang: String, novaMaterial: NovaMaterial): String {
        return getTranslation(lang, novaMaterial.localizedName)
    }
    
    fun getTranslatedName(player: Player, novaMaterial: NovaMaterial): String {
        return getTranslation(player, novaMaterial.localizedName)
    }
    
}