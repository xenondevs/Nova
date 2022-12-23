package xyz.xenondevs.nova.data.resources.builder.content

import com.google.gson.JsonObject
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.builder.AssetPack
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.listFileHeaders
import xyz.xenondevs.nova.util.data.parseJson
import xyz.xenondevs.nova.util.data.path
import java.util.logging.Level
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk
import kotlin.io.path.writeText

internal class LanguageContent : PackContent {
    
    override val stage = ResourcePackBuilder.BuildingStage.PRE_WORLD
    
    private val languageLookup = HashMap<String, HashMap<String, String>>()
    
    init {
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
        pack.zip.listFileHeaders(langDir)
            .filter { !it.isDirectory }
            .forEach { lang ->
                if (lang.path.extension == "json") {
                    val langObj = pack.zip.getInputStream(lang).parseJson() as JsonObject
                    val langMap = languageLookup.getOrPut(lang.path.nameWithoutExtension, ::HashMap)
                    langObj.entrySet().forEach { (key, value) -> langMap[key] = value.asString }
                }
            }
    }
    
    override fun write() {
        extractRomanNumerals(languageLookup["en_us"]!!)
        Resources.updateLanguageLookup(languageLookup)
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