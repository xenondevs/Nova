package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonObject
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.parseJson
import java.io.File
import java.util.logging.Level

internal class LanguageContent : PackContent {
    
    private val languageLookup = HashMap<String, HashMap<String, String>>()
    
    init {
        try {
            ResourcePackBuilder.LANGUAGE_DIR.walkTopDown()
                .filter { it.isFile && it.extension == "json" }
                .forEach {
                    languageLookup[it.nameWithoutExtension] = 
                        it.parseJson().asJsonObject.entrySet().associateTo(HashMap()) { (key, value) -> key to value.asString }
                }
        } catch (t: Throwable) {
            LOGGER.log(Level.SEVERE, "Failed to read existing language files")
        }
    }
    
    override fun addFromPack(pack: AssetPack) {
        pack.langDir?.listFiles()?.forEach { lang ->
            if (lang.isFile && lang.extension.equals("json", true)) {
                val langObj = lang.parseJson() as JsonObject
                val langMap = languageLookup.getOrPut(lang.nameWithoutExtension) { HashMap() }
                langObj.entrySet().forEach { (key, value) -> langMap[key] = value.asString }
            }
        }
    }
    
    override fun write() {
        extractRomanNumerals(languageLookup["en_us"]!!)
        Resources.updateLanguageLookup(languageLookup)
        languageLookup.forEach { (name, content) ->
            val file = File(ResourcePackBuilder.LANGUAGE_DIR, "$name.json")
            file.parentFile.mkdirs()
            file.writeText(GSON.toJson(content))
        }
    }
    
    private fun extractRomanNumerals(map: HashMap<String, String>) {
        for (i in 6..254)
            map["potion.potency.$i"] = NumberFormatUtils.getRomanNumeral(i + 1)
    }
    
}