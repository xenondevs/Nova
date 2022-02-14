package xyz.xenondevs.nova.data.resources.builder

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import xyz.xenondevs.nova.addon.assets.AssetPack
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.addAll
import java.io.File

internal class LanguageContent(private val builder: ResourcePackBuilder) : PackContent {
    
    private val languages = HashMap<String, JsonObject>()
    
    override fun addFromPack(pack: AssetPack) {
        pack.langDir?.listFiles()?.forEach { lang ->
            if (lang.isFile && lang.endsWith(".json")) {
                val mainLangJsonObj = languages.getOrPut(lang.nameWithoutExtension) { JsonObject() }
                val packLangJsonObj = JsonParser.parseReader(lang.reader()) as JsonObject
                mainLangJsonObj.addAll(packLangJsonObj)
            }
        }
    }
    
    override fun write() {
        languages.forEach { (name, content) ->
            val file = File(builder.languageDir, name)
            file.parentFile.mkdirs()
            file.writeText(GSON.toJson(content))
        }
    }
    
}