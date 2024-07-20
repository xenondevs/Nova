@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.data.resources.lookup

import org.bukkit.Material
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.resources.builder.task.font.FontChar
import xyz.xenondevs.nova.data.resources.builder.task.font.GuiTextureData
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.armor.Armor
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.LinkedBlockModelProvider
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
    var BLOCK_MODEL_LOOKUP: ResourceLookup<Map<NovaBlockState, LinkedBlockModelProvider<*>>> =
        resourceLookup("block_models", emptyMap(), typeOf<HashMap<NovaBlockState, LinkedBlockModelProvider<*>>>())
    
    /**
     * Map of [NovaBlockState] to the relevant [LinkedBlockModelProvider].
     */
    var BLOCK_MODEL: Map<NovaBlockState, LinkedBlockModelProvider<*>> by BLOCK_MODEL_LOOKUP
    
    /**
     * Lookup for getting the relevant custom-model-data using named item models.
     *
     * Format: ``Map<NovaItem, Map<Vanilla Material, Map<Model Name, Custom Model Data>>>``
     */
    var NAMED_ITEM_MODEL_LOOKUP: ResourceLookup<Map<NovaItem, Map<Material, Map<String, Int>>>> =
        resourceLookup("named_item_models", emptyMap(), typeOf<HashMap<NovaItem, EnumMap<Material, HashMap<String, Int>>>>())
    
    /**
     * Map of [NovaItem] to the relevant custom-model-data using named item models.
     *
     * Format: ``Map<VanillaMaterial, Map<ModelName, CustomModelData>``
     */
    var NAMED_ITEM_MODEL: Map<NovaItem, Map<Material, Map<String, Int>>> by NAMED_ITEM_MODEL_LOOKUP
    
    /**
     * Lookup for getting the relevant custom-model-data using unnamed item models.
     *
     * Format: ``Map<NovaItem, Map<VanillaMaterial, CustomModelData[]>>``
     */
    var UNNAMED_ITEM_MODEL_LOOKUP: ResourceLookup<Map<NovaItem, Map<Material, IntArray>>> =
        resourceLookup("unnamed_item_models", emptyMap(), typeOf<HashMap<NovaItem, EnumMap<Material, IntArray>>>())
    
    /**
     * Map of [NovaItem] to the relevant custom-model-data using unnamed item models.
     *
     * Format: ``Map<NovaItem, Map<VanillaMaterial, CustomModelData[]>>``
     */
    var UNNAMED_ITEM_MODEL: Map<NovaItem, Map<Material, IntArray>> by UNNAMED_ITEM_MODEL_LOOKUP
    
    /**
     * Lookup for getting the leather armor color value for every custom [Armor] type.
     */
    val ARMOR_COLOR_LOOKUP: ResourceLookup<Map<Armor, Int>> =
        resourceLookup("armor_color", emptyMap(), typeOf<HashMap<Armor, Int>>())
    
    /**
     * Map of [Armor] to the leather armor color value.
     */
    var ARMOR_COLOR: Map<Armor, Int> by ARMOR_COLOR_LOOKUP
    
    /**
     * Lookup for getting translations.
     *
     * Format: ``Map<Language, Map<TranslationKey, Translation>>``
     */
    var LANGUAGE_LOOKUP: ResourceLookup<Map<String, Map<String, String>>> =
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
    val GUI_TEXTURE_LOOKUP: ResourceLookup<Map<GuiTexture, GuiTextureData>> =
        resourceLookup("gui_texture_lookup", emptyMap(), typeOf<HashMap<GuiTexture, GuiTextureData>>())
    
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
        val lookup = ResourceLookup<T>(key, typeOf<T>(), empty)
        lookups += lookup
        return lookup
    }
    
    private fun <T : Any> resourceLookup(key: String, empty: T, type: KType): ResourceLookup<T> {
        val lookup = ResourceLookup<T>(key, type, empty)
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