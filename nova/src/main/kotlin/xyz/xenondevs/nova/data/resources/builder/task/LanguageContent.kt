package xyz.xenondevs.nova.data.resources.builder.task

import xyz.xenondevs.commons.gson.fromJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.util.NumberFormatUtils
import java.nio.file.Path
import java.util.logging.Level
import kotlin.io.path.bufferedReader
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.notExists
import kotlin.io.path.walk
import kotlin.io.path.writeText

class LanguageContent internal constructor(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val _vanillaLangs = HashMap<String, HashMap<String, String>>()
    val vanillaLangs: Map<String, Map<String, String>> get() = _vanillaLangs
    val customLangs = HashMap<String, HashMap<String, String>>()
    
    fun getOrCreate(lang: String): HashMap<String, String> {
        return customLangs.getOrPut(lang) { HashMap() }
    }
    
    /**
     * Gets a translation for [key] in [lang] from either [customLangs] or [vanillaLangs].
     * If the translation neither found in [lang] nor `en_us`, the key is returned.
     */
    fun getTranslation(lang: String, key: String): String {
        val customLang = customLangs[lang]
        if (customLang != null && key in customLang)
            return customLang[key]!!
        
        val vanillaLang = vanillaLangs[lang]
        if (vanillaLang != null && key in vanillaLang)
            return vanillaLang[key]!!
        
        if (lang != "en_us") {
            val customEnUs = customLangs["en_us"]
            if (customEnUs != null && key in customEnUs)
                return customEnUs[key]!!
            
            val vanillaEnUs = vanillaLangs["en_us"]
            if (vanillaEnUs != null && key in vanillaEnUs)
                return vanillaEnUs[key]!!
        }
        
        return key
    }
    
    fun setTranslation(lang: String, key: String, value: String) {
        val customLang = customLangs.getOrPut(lang, ::HashMap)
        customLang[key] = value
    }
    
    @PackTask(runAfter = ["ExtractTask#extractAll"])
    private fun loadLangFiles() {
        try {
            // Load vanilla lang files
            loadLangs(ResourcePackBuilder.MCASSETS_ASSETS_DIR.resolve("minecraft/lang/"), _vanillaLangs)
            // load lang files from base packs
            loadLangs(ResourcePackBuilder.LANGUAGE_DIR, customLangs)
            // load lang files from asset packs
            for (pack in builder.assetPacks) loadLangs(pack.langDir, customLangs)
        } catch (t: Throwable) {
            LOGGER.log(Level.SEVERE, "Failed to read existing language files", t)
        }
    }
    
    private fun loadLangs(dir: Path?, map: HashMap<String, HashMap<String, String>>) {
        if (dir == null || dir.notExists())
            return
        
        dir.walk()
            .filter { !it.isDirectory() && it.extension.equals("json", true) }
            .forEach {
                val langMap = map.getOrPut(it.nameWithoutExtension, ::HashMap)
                langMap += GSON.fromJson<HashMap<String, String>>(it.bufferedReader())!!
            }
    }
    
    @PackTask(runAfter = ["LanguageContent#loadLangFiles"])
    private fun write() {
        extractRomanNumerals(customLangs.getOrPut("en_us", ::HashMap))
        ResourceLookups.LANGUAGE_LOOKUP = customLangs
        customLangs.forEach { (name, content) ->
            val file = ResourcePackBuilder.LANGUAGE_DIR.resolve("$name.json")
            file.parent.createDirectories()
            file.writeText(GSON.toJson(content))
        }
    }
    
    private fun extractRomanNumerals(map: HashMap<String, String>) {
        for (i in 6..254)
            map["potion.potency.$i"] = NumberFormatUtils.getRomanNumeral(i + 1)
    }
    
}