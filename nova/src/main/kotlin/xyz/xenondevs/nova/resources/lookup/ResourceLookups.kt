@file:Suppress("MemberVisibilityCanBePrivate")
@file:UseSerializers(BlockStateSerializer::class)

package xyz.xenondevs.nova.resources.lookup

import kotlinx.serialization.KSerializer
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.serialization.kotlinx.BlockStateSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ResourceKeySerializer
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.BlockModelProvider
import xyz.xenondevs.nova.world.item.Equipment

/**
 * Resource lookups store data that is generated during resource pack build, but relevant for the game.
 */
internal object ResourceLookups {
    
    private val lookups = ArrayList<ResourceLookup<*>>()
    
    /**
     * Lookup for getting the relevant [BlockModelProvider] for every [NovaBlockState].
     */
    val BLOCK_MODEL_LOOKUP: ResourceLookup<Map<NovaBlockState, BlockModelProvider>> =
        resourceLookup("block_models", emptyMap())
    
    /**
     * Map of [NovaBlockState] to the relevant [BlockModelProvider].
     */
    var BLOCK_MODEL: Map<NovaBlockState, BlockModelProvider> by BLOCK_MODEL_LOOKUP
    
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
        resourceLookup("language_lookup", emptyMap())
    
    /**
     * Map of translations.
     *
     * Format: ``Map<Language, Map<TranslationKey, Translation>>``
     */
    var LANGUAGE: Map<String, Map<String, String>> by LANGUAGE_LOOKUP
    
    /**
     * Lookup for getting the [FontChar] for every [GuiTexture].
     */
    val GUI_TEXTURE_LOOKUP: MapResourceLookup<GuiTexture, GuiTextureData> = mapResourceLookup("gui_texture_lookup")
    
    /**
     * Map of [GuiTexture] to the [FontChar].
     */
    var GUI_TEXTURE: Map<GuiTexture, GuiTextureData> by GUI_TEXTURE_LOOKUP
    
    /**
     * Lookup for getting the [GuiTexture] by its corresponding [FontChar].
     */
    val GUI_TEXTURE_BY_FONT_CHAR_LOOKUP: MapResourceLookup<FontChar, GuiTexture> = mapResourceLookup("gui_texture_by_font_char_lookup")
    
    /**
     * Map of [FontChar] to the corresponding [GuiTexture].
     */
    var GUI_TEXTURE_BY_FONT_CHAR: Map<FontChar, GuiTexture> by GUI_TEXTURE_BY_FONT_CHAR_LOOKUP
    
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
    
    /**
     * Lookup containing all block states that are in use by base packs.
     */
    val OCCUPIED_BLOCK_STATES_LOOKUP: ResourceLookup<Set<BlockState>> =
        resourceLookup("occupied_block_states", emptySet(), SetSerializer(BlockStateSerializer))
    
    /**
     * Set of all block states that are in use by base packs.
     */
    var OCCUPIED_BLOCK_STATES: Set<BlockState> by OCCUPIED_BLOCK_STATES_LOOKUP
    
    /**
     * Lookup for entity variant layouts.
     */
    val ENTITY_VARIANT_ASSETS_LOOKUP: ResourceLookup<Map<ResourceKey<*>, EntityVariantLayout>> =
        resourceLookup("entity_variant_lookup", emptyMap(), MapSerializer(ResourceKeySerializer, EntityVariantLayout.serializer()))
    
    /**
     * Entity variant layouts.
     */
    val ENTITY_VARIANT_ASSETS: Map<ResourceKey<*>, EntityVariantLayout> by ENTITY_VARIANT_ASSETS_LOOKUP
    
    /**
     * Lookup for sound overrides (ids of sound that were moved to the Nova namespace).
     */
    val SOUND_OVERRIDES_LOOKUP: ResourceLookup<Set<String>> =
        resourceLookup("sound_overrides", emptySet(), SetSerializer(String.serializer()))
    
    /**
     * Sound overrides (ids of sound that were moved to the Nova namespace).
     */
    var SOUND_OVERRIDES: Set<String> by SOUND_OVERRIDES_LOOKUP
    
    private inline fun <reified T : Any> resourceLookup(key: String, empty: T): ResourceLookup<T> {
        val lookup = ResourceLookup(key, PermanentStorage::retrieve, PermanentStorage::store, empty)
        lookups += lookup
        return lookup
    }
    
    private fun <T : Any> resourceLookup(key: String, empty: T, serializer: KSerializer<T>): ResourceLookup<T> {
        val lookup = ResourceLookup(
            key,
            { PermanentStorage.retrieve(it, serializer) },
            { k, v -> PermanentStorage.store(k, serializer, v) },
            empty
        )
        lookups += lookup
        return lookup
    }
    
    private inline fun <reified K : Any, reified V : Any> mapResourceLookup(key: String): MapResourceLookup<K, V> {
        val lookup = MapResourceLookup<K, V>(key, PermanentStorage::retrieve, PermanentStorage::store)
        lookups += lookup
        return lookup
    }
    
    private inline fun <reified T : Any> idResourceLookup(key: String): IdResourceLookup<T> {
        val lookup = IdResourceLookup<T>(key, PermanentStorage::retrieve, PermanentStorage::store)
        lookups += lookup
        return lookup
    }
    
    internal fun tryLoadAll(): Boolean =
        runCatching { loadAll() }.isSuccess
    
    internal fun loadAll() =
        lookups.forEach(ResourceLookup<*>::load)
    
}