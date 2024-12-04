@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.resources.lookup

import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.resources.builder.task.font.FontChar
import xyz.xenondevs.nova.resources.builder.task.font.GuiTextureData
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.LinkedBlockModelProvider
import xyz.xenondevs.nova.world.item.Equipment
import java.util.*
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Resource lookups store data that is generated during resource pack build, but relevant for the game.
 */
internal object ResourceLookups {
    
    private val lookups = ArrayList<ResourceLookup<*>>()
    
    /**
     * Lookup for getting the relevant [LinkedBlockModelProvider] for every [NovaBlockState].
     */
    val BLOCK_MODEL_LOOKUP: ResourceLookup<Map<NovaBlockState, LinkedBlockModelProvider<*>>> =
        resourceLookup("block_models", emptyMap(), typeOf<HashMap<NovaBlockState, LinkedBlockModelProvider<*>>>())
    
    /**
     * Map of [NovaBlockState] to the relevant [LinkedBlockModelProvider].
     */
    var BLOCK_MODEL: Map<NovaBlockState, LinkedBlockModelProvider<*>> by BLOCK_MODEL_LOOKUP
    
    /**
     * Lookup containing texture and camera overlay locations for every [Equipment].
     */
    val EQUIPMENT_LOOKUP: MapResourceLookup<Equipment, RuntimeEquipmentData> = mapResourceLookup("equipment")
    
    /**
     * Map of [Equipment] to the relevant [RuntimeEquipmentData].
     */
    var EQUIPMENT: Map<Equipment, RuntimeEquipmentData> by EQUIPMENT_LOOKUP
    
    /**
     * Lookup for getting translations.
     *
     * Format: ``Map<Language, Map<TranslationKey, Translation>>``
     */
    val LANGUAGE_LOOKUP: ResourceLookup<Map<String, Map<String, String>>> =
        resourceLookup("language_lookup", emptyMap(), typeOf<HashMap<String, HashMap<String, String>>>())
    
    /**
     * Map of translations.
     *
     * Format: ``Map<Language, Map<TranslationKey, Translation>>``
     */
    var LANGUAGE: Map<String, Map<String, String>> by LANGUAGE_LOOKUP
    
    /**
     * Lookup for getting the [FontChar] for every [GuiTexture].
     */
    val GUI_TEXTURE_LOOKUP: MapResourceLookup<GuiTexture, GuiTextureData> = mapResourceLookup("gui_texture_lookup" )
    
    /**
     * Map of [GuiTexture] to the [FontChar].
     */
    var GUI_TEXTURE: Map<GuiTexture, GuiTextureData> by GUI_TEXTURE_LOOKUP
    
    /**
     * The first code-point that is a move character in the minecraft:default font.
     */
    var MOVE_CHARACTERS_OFFSET by resourceLookup<Int>("move_characters_offset", 0)
    
    /**
     * Lookup for Waila icons.
     */
    val WAILA_DATA_LOOKUP: IdResourceLookup<FontChar> =
        idResourceLookup<FontChar>("waila_data_lookup")
    
    /**
     * Lookup for texture icons.
     */
    val TEXTURE_ICON_LOOKUP: IdResourceLookup<FontChar> =
        idResourceLookup<FontChar>("texture_icon_lookup")
    
    private inline fun <reified T : Any> resourceLookup(key: String, empty: T): ResourceLookup<T> {
        val lookup = ResourceLookup(key, typeOf<T>(), empty)
        lookups += lookup
        return lookup
    }
    
    private fun <T : Any> resourceLookup(key: String, empty: T, type: KType): ResourceLookup<T> {
        val lookup = ResourceLookup(key, type, empty)
        lookups += lookup
        return lookup
    }
    
    private inline fun <reified K: Any, reified V: Any> mapResourceLookup(key: String): MapResourceLookup<K, V> {
        val lookup = MapResourceLookup<K, V>(key, typeOf<K>(), typeOf<V>())
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
    
    internal fun tryLoadAll(): Boolean =
        runCatching { loadAll() }.isSuccess
    
    internal fun loadAll() =
        lookups.forEach(ResourceLookup<*>::load)
    
}