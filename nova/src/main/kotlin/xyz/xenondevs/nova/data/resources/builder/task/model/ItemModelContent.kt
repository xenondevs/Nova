package xyz.xenondevs.nova.data.resources.builder.task.model

import it.unimi.dsi.fastutil.ints.Int2ObjectRBTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import org.bukkit.Material
import xyz.xenondevs.commons.collections.enumMap
import xyz.xenondevs.nova.data.resources.ResourcePath
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.builder.model.Model.Override
import xyz.xenondevs.nova.data.resources.builder.task.PackTask
import xyz.xenondevs.nova.data.resources.builder.task.PackTaskHolder
import xyz.xenondevs.nova.data.resources.layout.item.ItemModelSelectorScope
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.registry.NovaRegistries

private const val CUSTOM_MODEL_DATA_PREDICATE_KEY = "custom_model_data"

/**
 * A [PackTaskHolder] that deals with generating and assigning custom models to vanilla items
 * via custom-model-data.
 */
class ItemModelContent internal constructor(val builder: ResourcePackBuilder) : PackTaskHolder {
    
    private val overridesById: MutableMap<Material, Int2ObjectSortedMap<ResourcePath>> = enumMap()
    private val overridesByPath: MutableMap<Material, MutableMap<ResourcePath, IntList>> = enumMap()
    private val currentModelData: MutableMap<Material, Int> = enumMap()
    
    private val modelContent by builder.getHolderLazily<ModelContent>()
    
    /**
     * Creates a custom model data entry for [path] under all materials that can be targeted via [VanillaMaterialProperty]
     * and returns a map of the materials to the custom model data.
     */
    fun registerAll(path: ResourcePath): Map<Material, Int> =
        VanillaMaterialTypes.MATERIALS.associateWithTo(enumMap()) { registerCustom(path, it) }
    
    /**
     * Creates a custom model data entry for [path] under the default material and returns the default material and the
     * custom model data.
     */
    fun registerDefault(path: ResourcePath): Pair<Material, Int> =
        VanillaMaterialTypes.DEFAULT_MATERIAL to registerCustom(path, VanillaMaterialTypes.DEFAULT_MATERIAL)
    
    /**
     * Retrieves the first id that registers [path] using the default material, or registers the path.
     */
    fun getOrRegisterDefault(path: ResourcePath): Pair<Material, Int> {
        val ids = overridesByPath[VanillaMaterialTypes.DEFAULT_MATERIAL]?.get(path)
        if (!ids.isNullOrEmpty())
            return VanillaMaterialTypes.DEFAULT_MATERIAL to ids.getInt(0)
        
        return registerDefault(path)
    }
    
    /**
     * Creates a custom model data entry for [path] under the specified [material] and returns the custom model data.
     */
    fun registerCustom(path: ResourcePath, material: Material): Int {
        val customModelData = nextCustomModelData(material)
        overridesById.getOrPut(material, ::Int2ObjectRBTreeMap).put(customModelData, path)
        overridesByPath.getOrPut(material, ::HashMap).getOrPut(path, ::IntArrayList).add(customModelData)
        modelContent.rememberUsage(path)
        return customModelData
    }
    
    /**
     * Checks whether the given [customModelData] is occupied under the specified [material].
     */
    fun isOccupied(material: Material, customModelData: Int): Boolean =
        overridesById[material]?.keys?.contains(customModelData) ?: false
    
    /**
     * Gets the next unused custom model data for the specified [material].
     */
    fun nextCustomModelData(material: Material): Int {
        var customModelData = currentModelData[material] ?: 1
        while (isOccupied(material, customModelData))
            customModelData++
        currentModelData[material] = customModelData
        return customModelData
    }
    
    @PackTask(runAfter = ["ModelContent#discoverAllModels"])
    private fun loadOverrides() {
        for ((id, model) in modelContent) {
            if (id.namespace != "minecraft" || !id.path.startsWith("item/")) continue
            val material = Material.getMaterial(id.path.substringAfter("item/").uppercase()) ?: continue
            
            val itemOverridesById = Int2ObjectRBTreeMap<ResourcePath>()
            val itemOverridesByPath = HashMap<ResourcePath, IntList>()
            for (override in model.overrides) {
                val customModelData = override.predicate[CUSTOM_MODEL_DATA_PREDICATE_KEY] ?: continue
                itemOverridesById.put(customModelData.toInt(), override.model)
                itemOverridesByPath.getOrPut(override.model, ::IntArrayList).add(customModelData.toInt())
            }
            overridesById[material] = itemOverridesById
            overridesByPath[material] = itemOverridesByPath
        }
    }
    
    @PackTask(runAfter = ["ItemModelContent#loadOverrides"])
    private fun assignItemModels() {
        // Map<NovaItem, Map<Material, Map<ModelName, CustomModelData>>>
        val lookup = HashMap<NovaItem, Map<Material, Map<String, Int>>>()
        
        for (item in NovaRegistries.ITEM) {
            val itemLookup = enumMap<Material, MutableMap<String, Int>>()
            val requestedLayout = item.requestedLayout
            for ((modelName, modelSelector) in requestedLayout.models) {
                val (model, _) = modelSelector(ItemModelSelectorScope(item, builder, modelContent)).buildScaled(modelContent)
                val path = modelContent.getOrPutGenerated(model)
                
                val reqItemType = requestedLayout.itemType
                if (reqItemType != null) {
                    itemLookup.getOrPut(reqItemType, ::LinkedHashMap)[modelName] = registerCustom(path, reqItemType)
                } else {
                    for ((itemType, customModelData) in registerAll(path)) {
                        itemLookup.getOrPut(itemType, ::LinkedHashMap)[modelName] = customModelData
                    }
                }
                
                lookup[item] = itemLookup
            }
        }
        
        ResourceLookups.NAMED_ITEM_MODEL = lookup
        ResourceLookups.UNNAMED_ITEM_MODEL = lookup.mapValues { itemModels ->
            itemModels.value.mapValues { materials ->
                materials.value.values.toIntArray()
            }
        }
    }
    
    @PackTask(
        runAfter = ["ItemModelContent#loadOverrides", "ItemModelContent#assignItemModels"],
        runBefore = ["ModelContent#write"]
    )
    private fun writeOverrides() {
        for ((material, materialOverrides) in overridesById) {
            val path = ResourcePath("minecraft", "item/${material.name.lowercase()}")
            val model = modelContent[path] ?: throw IllegalStateException("No model found for $path")
            
            val overrides = materialOverrides.map { (customModelData, path) ->
                Override(mapOf(CUSTOM_MODEL_DATA_PREDICATE_KEY to customModelData), path)
            }
            
            modelContent[path] = model.copy(overrides = overrides)
            modelContent.rememberUsage(path)
        }
    }
    
}