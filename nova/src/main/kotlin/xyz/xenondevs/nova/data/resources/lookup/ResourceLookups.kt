package xyz.xenondevs.nova.data.resources.lookup

import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.task.EnchantmentData
import xyz.xenondevs.nova.data.resources.builder.task.armor.info.ArmorTexture
import xyz.xenondevs.nova.data.resources.builder.task.font.FontChar
import xyz.xenondevs.nova.data.resources.model.ModelData
import kotlin.reflect.typeOf

object ResourceLookups {
    
    private val lookups = ArrayList<ResourceLookup<*>>()
    
    val MODEL_DATA_LOOKUP = idResourceLookup<ModelData>("model_data_lookup")
    val ARMOR_DATA_LOOKUP = idResourceLookup<ArmorTexture>("armor_data_lookup")
    val GUI_DATA_LOOKUP = idResourceLookup<FontChar>("gui_data_lookup")
    val WAILA_DATA_LOOKUP = idResourceLookup<FontChar>("waila_data_lookup")
    val TEXTURE_ICON_LOOKUP = idResourceLookup<FontChar>("texture_icon_lookup")
    var LANGUAGE_LOOKUP by resourceLookup<Map<String, Map<String, String>>>("language_lookup")
    var ENCHANTMENT_DATA_LOOKUP by resourceLookup<Map<String, EnchantmentData>>("enchantment_data_lookup")
    var MOVE_CHARACTERS_OFFSET by resourceLookup<Int>("move_characters_offset")
    
    private inline fun <reified T : Any> resourceLookup(key: String): ResourceLookup<T> {
        val lookup= ResourceLookup<T>(key, typeOf<T>())
        lookups += lookup
        return lookup
    }
    
    private inline fun <reified T : Any> idResourceLookup(key: String): IdResourceLookup<T> {
        val lookup = IdResourceLookup<T>(key, typeOf<T>())
        lookups += lookup
        return lookup
    }
    
    internal fun hasAllLookups(): Boolean =
        lookups.all { PermanentStorage.has(it.key) }
    
    internal fun loadAll() {
        lookups.forEach(ResourceLookup<*>::load)
    }
    
}