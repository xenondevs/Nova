package xyz.xenondevs.nova.data.resources.builder.task

import com.google.gson.JsonObject
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.util.NumberFormatUtils
import java.util.logging.Level
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk
import kotlin.io.path.writeText

class LanguageContent internal constructor(private val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val languageLookup = HashMap<String, HashMap<String, String>>()
    
    fun getLanguage(lang: String): HashMap<String, String> {
        return languageLookup.getOrPut(lang, ::HashMap)
    }
    
    fun setTranslation(lang: String, key: String, value: String) {
        getLanguage(lang)[key] = value
    }
    
    @PackTask(stage = BuildStage.POST_BASE_PACKS)
    private fun loadLangFiles() {
        try {
            // load existing lang files from base packs
            ResourcePackBuilder.LANGUAGE_DIR.walk()
                .filter { !it.isDirectory() && it.extension == "json" }
                .forEach {
                    languageLookup[it.nameWithoutExtension] =
                        it.parseJson().asJsonObject.entrySet().associateTo(HashMap()) { (key, value) -> key to value.asString }
                }
            
            // load lang files from asset packs
            builder.assetPacks.forEach { pack ->
                pack.langDir?.walk()?.forEach { lang ->
                    if (lang.extension.equals("json", true)) {
                        val langObj = lang.parseJson() as JsonObject
                        val langMap = languageLookup.getOrPut(lang.nameWithoutExtension, ::HashMap)
                        langObj.entrySet().forEach { (key, value) -> langMap[key] = value.asString }
                    }
                }
            }
        } catch (t: Throwable) {
            LOGGER.log(Level.SEVERE, "Failed to read existing language files", t)
        }
    }
    
    @PackTask(stage = BuildStage.PRE_WORLD_WRITE)
    private fun write() {
        extractRomanNumerals(getLanguage("en_us"))
        ResourceLookups.LANGUAGE_LOOKUP = languageLookup
        languageLookup.forEach { (name, content) ->
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