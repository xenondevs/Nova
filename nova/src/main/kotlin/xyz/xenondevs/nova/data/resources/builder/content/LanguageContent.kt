package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonObject
import net.minecraft.resources.ResourceLocation
import xyz.xenondevs.commons.gson.parseJson
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.serialization.json.GSON
import xyz.xenondevs.nova.util.NumberFormatUtils
import java.util.logging.Level
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk
import kotlin.io.path.writeText

class LanguageContent private constructor() : PackContent {
    
    companion object : PackContentType<LanguageContent> {
        override fun create(builder: ResourcePackBuilder) = LanguageContent()
    }
    
    val languageLookup = HashMap<String, HashMap<String, String>>()
    
    override fun init() {
        try {
            ResourcePackBuilder.LANGUAGE_DIR.walk()
                .filter { !it.isDirectory() && it.extension == "json" }
                .forEach {
                    languageLookup[it.nameWithoutExtension] =
                        it.parseJson().asJsonObject.entrySet().associateTo(HashMap()) { (key, value) -> key to value.asString }
                }
        } catch (t: Throwable) {
            LOGGER.log(Level.SEVERE, "Failed to read existing language files")
        }
    }
    
    override fun includePack(pack: AssetPack) {
        val langDir = pack.langDir ?: return
        
        langDir.walk().forEach { lang ->
            if (lang.extension.equals("json", true)) {
                val langObj = lang.parseJson() as JsonObject
                val langMap = languageLookup.getOrPut(lang.nameWithoutExtension, ::HashMap)
                langObj.entrySet().forEach { (key, value) -> langMap[key] = value.asString }
            }
        }
    }
    
    override fun write() {
        extractRomanNumerals(languageLookup["en_us"]!!)
        ResourceGeneration.updateLanguageLookup(languageLookup.mapKeys { ResourceLocation(it.key) })
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