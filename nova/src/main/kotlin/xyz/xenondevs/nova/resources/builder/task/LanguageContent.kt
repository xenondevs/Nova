package xyz.xenondevs.nova.resources.builder.task

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.data.readJson
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.walk

/**
 * Contains all translations of the resource pack.
 */
class LanguageContent(private val builder: ResourcePackBuilder) : PackBuildData {
    
    lateinit var vanillaLangs: Map<ResourcePath<ResourceType.Lang>, Map<String, String>>
        private set
    lateinit var customLangs: MutableMap<ResourcePath<ResourceType.Lang>, MutableMap<String, String>>
        private set
    
    /**
     * Gets the custom translation map for [path].
     */
    operator fun get(path: ResourcePath<ResourceType.Lang>): MutableMap<String, String> =
        customLangs.getOrPut(path) { loadLangFile(builder.resolve(path)) }
    
    /**
     * Retrieves the vanilla translation map for [path].
     */
    fun getVanilla(path: ResourcePath<ResourceType.Lang>): Map<String, String> =
        vanillaLangs[path] ?: emptyMap()
    
    /**
     * Gets a translation for [key] in [lang] from either [customLangs] or [vanillaLangs].
     * If the translation neither found in [lang] nor `en_us`, the key is returned.
     */
    fun getTranslation(lang: ResourcePath<ResourceType.Lang>, key: String): String {
        val path = ResourcePath.of(ResourceType.Lang, lang)
        
        val customLang = get(path)
        if (key in customLang)
            return customLang[key]!!
        
        val vanillaLang = getVanilla(path)
        if (key in vanillaLang)
            return vanillaLang[key]!!
        
        if (lang != EN_US) {
            val customEnUs = get(EN_US)
            if (key in customEnUs)
                return customEnUs[key]!!
            
            val vanillaEnUs = getVanilla(EN_US)
            if (key in vanillaEnUs)
                return vanillaEnUs[key]!!
        }
        
        return key
    }
    
    /**
     * Sets the translation of [key] in [lang] to [value].
     */
    fun setTranslation(lang: ResourcePath<ResourceType.Lang>, key: String, value: String) {
        val path = ResourcePath.of(ResourceType.Lang, lang)
        val customLang = customLangs.getOrPut(path, ::HashMap)
        customLang[key] = value
    }
    
    private fun loadLangFile(path: Path): MutableMap<String, String> =
        if (path.exists()) path.readJson<MutableMap<String, String>>() else mutableMapOf()
    
    /**
     * Loads all lang files, both vanilla and custom.
     */
    inner class LoadAll : PackTask {
        
        override val runsBefore = setOf(Write::class)
        
        override suspend fun run() = coroutineScope {
            val vanilla = loadLangs(builder.resolveVanilla("assets/minecraft/lang/"))
            val custom = builder.assetPacks.mapNotNull { pack -> pack.langDir?.let { loadLangs(it) } }
            
            val mergedCustom = mutableMapOf<ResourcePath<ResourceType.Lang>, MutableMap<String, String>>()
            for (map in custom) {
                for ((path, deferred) in map) {
                    mergedCustom.getOrPut(path) { mutableMapOf() } += deferred.await()
                }
            }
            
            vanillaLangs = vanilla.mapValues { (_, v) -> v.await() }
            customLangs = mergedCustom
        }
        
        private fun CoroutineScope.loadLangs(
            dir: Path,
        ): MutableMap<ResourcePath<ResourceType.Lang>, Deferred<MutableMap<String, String>>> {
            val map = mutableMapOf<ResourcePath<ResourceType.Lang>, Deferred<MutableMap<String, String>>>()
            dir.walk()
                .filter { !it.isDirectory() && it.extension.equals("json", true) }
                .filter { it.name != "deprecated.json" }
                .forEach { file ->
                    val path = ResourcePath.of(ResourceType.Lang, file.nameWithoutExtension)
                    map[path] = async { loadLangFile(file) }
                }
            return map
        }
        
    }
    
    /**
     * Writes all translations of [LanguageContent] to the resource pack.
     */
    inner class Write : PackTask {
        
        override val stage = BuildStage.PRE_WORLD
        
        override suspend fun run() {
            extractRomanNumerals()
            
            // create language lookup
            ResourceLookups.LANGUAGE = customLangs.mapKeys { (path, _) -> path.path }
            
            // write custom langs to disk
            for ((path, content) in customLangs) {
                if (content.isEmpty())
                    continue
                builder.writeJson(path, content)
            }
        }
        
        private fun extractRomanNumerals() {
            val enUs = get(EN_US)
            for (i in 6..254)
                enUs["potion.potency.$i"] = NumberFormatUtils.getRomanNumeral(i + 1)
        }
        
    }
    
    private companion object {
         val EN_US = ResourcePath.of(ResourceType.Lang, "en_us")
    }
    
}