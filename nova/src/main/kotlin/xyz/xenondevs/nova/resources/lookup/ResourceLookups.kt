@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.resources.lookup

import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.state.BlockState
import xyz.xenondevs.commons.provider.MutableProvider
import xyz.xenondevs.commons.provider.mutableProvider
import xyz.xenondevs.nova.config.PermanentStorage
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.builder.layout.entity.EntityVariantLayout
import xyz.xenondevs.nova.resources.builder.task.FontChar
import xyz.xenondevs.nova.resources.builder.task.GuiTextureData
import xyz.xenondevs.nova.resources.builder.task.RuntimeEquipmentData
import xyz.xenondevs.nova.ui.overlay.guitexture.GuiTexture
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.state.model.BlockModelProvider
import xyz.xenondevs.nova.world.item.Equipment
import kotlin.reflect.typeOf

/**
 * Resource lookups store data that is generated during resource pack build, but relevant for the game.
 */
internal object ResourceLookups {
    
    private val lookups = HashMap<String, ResourceLookup<*>>()
    
    /**
     * Lookup for getting the relevant [BlockModelProvider] for every stringified [NovaBlockState].
     */
    val blockModelLookup: MutableProvider<Map<NovaBlockState, BlockModelProvider>> =
        resourceLookup("block_models", emptyMap())
    
    /**
     * Map of stringified [NovaBlockState] to the relevant [BlockModelProvider].
     */
    var blockModel: Map<NovaBlockState, BlockModelProvider>
        by blockModelLookup
    
    /**
     * Lookup containing texture and camera overlay locations for every [Equipment].
     */
    val equipmentLookup: MutableProvider<Map<RegistryEntry.Nova<Equipment>, RuntimeEquipmentData>> =
        resourceLookup("equipment", emptyMap())
    
    /**
     * Map of [Equipment] to the relevant [RuntimeEquipmentData].
     */
    var equipment: Map<RegistryEntry.Nova<Equipment>, RuntimeEquipmentData>
        by equipmentLookup
    
    /**
     * Lookup for getting translations.
     *
     * Format: ``Map<Language, Map<TranslationKey, Translation>>``
     */
    val languageLookup: MutableProvider<Map<String, Map<String, String>>> =
        resourceLookup("language_lookup", emptyMap())
    
    /**
     * Map of translations.
     *
     * Format: ``Map<Language, Map<TranslationKey, Translation>>``
     */
    var language: Map<String, Map<String, String>>
        by languageLookup
    
    /**
     * Lookup for getting the [FontChar] for every [GuiTexture].
     */
    val guiTextureLookup: MutableProvider<Map<RegistryEntry.Nova<GuiTexture>, GuiTextureData>> =
        resourceLookup("gui_texture_lookup", emptyMap())
    
    /**
     * Map of [GuiTexture] to the [FontChar].
     */
    var guiTexture: Map<RegistryEntry.Nova<GuiTexture>, GuiTextureData>
        by guiTextureLookup
    
    /**
     * Lookup for getting the [GuiTexture] by its corresponding [FontChar].
     */
    val guiTextureByFontCharLookup: MutableProvider<Map<FontChar, RegistryEntry.Nova<GuiTexture>>> =
        resourceLookup("gui_texture_by_font_char_lookup", emptyMap())
    
    /**
     * Map of [FontChar] to the corresponding [GuiTexture].
     */
    var guiTextureByFontChar: Map<FontChar, RegistryEntry.Nova<GuiTexture>>
        by guiTextureByFontCharLookup
    
    /**
     * The first code-point that is a move character in the minecraft:default font.
     */
    val moveCharactersOffsetLookup: MutableProvider<Int> =
        resourceLookup("move_characters_offset")
    
    /**
     * The first code-point that is a move character in the minecraft:default font.
     */
    var moveCharactersOffset: Int
        by moveCharactersOffsetLookup
    
    /**
     * Lookup for Waila icons.
     */
    val wailaDataLookup: MutableProvider<Map<Key, FontChar>> =
        resourceLookup("waila_data_lookup", emptyMap())
    
    /**
     * Map of Waila icon id to [FontChar].
     */
    var wailaData: Map<Key, FontChar>
        by wailaDataLookup
    
    /**
     * Lookup for texture icons.
     */
    val textureIconLookup: MutableProvider<Map<Key, FontChar>> =
        resourceLookup("texture_icon_lookup", emptyMap())
    
    /**
     * Map of texture icon id to [FontChar].
     */
    var textureIcon: Map<Key, FontChar>
        by textureIconLookup
    
    /**
     * Lookup containing all block states that are in use by base packs.
     */
    val occupiedBlockStatesLookup: MutableProvider<Set<BlockState>> =
        resourceLookup("occupied_block_states", emptySet())
    
    /**
     * Set of all block states that are in use by base packs.
     */
    var occupiedBlockStates: Set<BlockState>
        by occupiedBlockStatesLookup
    
    /**
     * Lookup for entity variant layouts.
     */
    val entityVariantAssetsLookup: MutableProvider<Map<Pair<Key, Key>, EntityVariantLayout>> =
        resourceLookup("entity_variant_lookup", emptyMap())
    
    /**
     * Entity variant layouts.
     */
    val entityVariantAssets: Map<Pair<Key, Key>, EntityVariantLayout>
        by entityVariantAssetsLookup
    
    /**
     * Lookup for sound overrides (ids of sound that were moved to the Nova namespace).
     */
    val soundOverridesLookup: MutableProvider<Set<String>> =
        resourceLookup("sound_overrides", emptySet())
    
    /**
     * Sound overrides (ids of sound that were moved to the Nova namespace).
     */
    var soundOverrides: Set<String>
        by soundOverridesLookup
    
    private inline fun <reified T : Any> resourceLookup(key: String, default: T? = null): MutableProvider<T> {
        val provider = mutableProvider<T> { loadAll(key) }
        lookups[key] = ResourceLookup(key, typeOf<T>(), provider, default)
        return provider
    }
    
    /**
     * Loads all resource lookups and returns the value of the lookup under [initiator].
     */
    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> loadAll(initiator: String): T {
        loadAll()
        return (lookups[initiator] as ResourceLookup<T>).provider.get()
    }
    
    /**
     * Loads all resource lookups.
     */
    fun loadAll() {
        for (lookup in lookups.values) {
            try {
                lookup.load()
            } catch(e: Exception) {
                // clear invalid lookups and force pack rebuild on the next startup
                lookups.values.forEach(ResourceLookup<*>::remove)
                PermanentStorage.remove(ResourceGeneration.RESOURCES_HASH)
                throw ResourceLookupException(lookup.key, e)
            }
        }
    }
    
    /**
     * Checks whether there is serialized data for all lookups.
     */
    fun hasAll(): Boolean {
        return lookups.values.all(ResourceLookup<*>::exists)
    }
    
    /**
     * Serializes and writes all lookups to disk.
     */
    fun storeAll() {
        lookups.values.forEach(ResourceLookup<*>::store)
    }
    
}