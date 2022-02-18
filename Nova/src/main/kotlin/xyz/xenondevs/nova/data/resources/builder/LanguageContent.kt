package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.util.data.GSON
import java.io.File

internal class LanguageContent(private val builder: ResourcePackBuilder) : PackContent {
    
    private val languageLookup = HashMap<String, HashMap<String, String>>()
    
    override fun addFromPack(pack: AssetPack) {
        pack.langDir?.listFiles()?.forEach { lang ->
            if (lang.isFile && lang.extension.equals("json", true)) {
                val langObj = JsonParser.parseReader(lang.reader()) as JsonObject
                val langMap = languageLookup.getOrPut(lang.nameWithoutExtension) { HashMap() }
                langObj.entrySet().forEach { (key, value) -> langMap[key] = value.asString }
            }
        }
    }
    
    override fun write() {
        Resources.updateLanguageLookup(languageLookup)
        languageLookup.forEach { (name, content) ->
            val file = File(builder.languageDir, "$name.json")
            file.parentFile.mkdirs()
            file.writeText(GSON.toJson(content))
        }
    }
    
}