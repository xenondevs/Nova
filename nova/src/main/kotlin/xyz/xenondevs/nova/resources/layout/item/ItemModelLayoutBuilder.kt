package xyz.xenondevs.nova.resources.layout.item

import org.bukkit.Material
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.world.item.vanilla.VanillaMaterialProperty
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl

private typealias ItemModelSelector = ItemModelSelectorScope.() -> ModelBuilder
private typealias NumberedItemModelSelector = ItemModelSelectorScope.(Int) -> ModelBuilder

private val DEFAULT_MODEL_SELECTOR_MAP: Map<String, ItemModelSelector> =
    mapOf("default" to { defaultModel })

internal data class RequestedItemModelLayout(
    val itemType: Material?,
    val models: Map<String, ItemModelSelector>
) {
    
    companion object {
        val DEFAULT = RequestedItemModelLayout(null, DEFAULT_MODEL_SELECTOR_MAP)
    }
    
}

@RegistryElementBuilderDsl
class ItemModelLayoutBuilder internal constructor() {
    
    private var itemType: Material? = null
    private var models = LinkedHashMap<String, ItemModelSelector>()
    
    /**
     * Configures the underlying item type of this item model, which is responsible for client-side behavior.
     *
     * If this is not set, Nova will generate custom model data entries of this item's models to all item types
     * that can be targeted using [VanillaMaterialProperty].
     */
    fun itemType(material: Material) {
        require(material.isItem) { "Material must be an item" }
        this.itemType = material
    }
    
    /**
     * Configures the model for the specified [name].
     */
    fun selectModel(name: String = "default", modelSelector: ItemModelSelector) {
        models[name] = modelSelector
    }
    
    /**
     * Configures the models for each number in the specified [range] by invoking the given [modelSelector].
     *
     * If [setDefault] is true, the first model in the range will be the default model.
     */
    fun selectModels(range: IntRange, setDefault: Boolean = false, modelSelector: NumberedItemModelSelector) {
        if (setDefault)
            models["default"] = { modelSelector(range.first) }
        
        val start = if (setDefault) range.first + 1 else range.first
        for (i in start..range.last)
            models[i.toString()] = { modelSelector(i) }
    }
    
    /**
     * Configures the models for each number in the specified [range] by invoking the given [format] with the
     * number as the argument.
     *
     * If [setDefault] is true, the first model in the range will be the default model.
     */
    fun selectModels(range: IntRange, format: String, setDefault: Boolean = false) {
        if (setDefault)
            models["default"] = { getModel(format.format(range.first)) }
        
        val start = if (setDefault) range.first + 1 else range.first
        for (i in start..range.last)
            models[i.toString()] = { getModel(format.format(i)) }
    }
    
    internal fun build() =
        RequestedItemModelLayout(itemType, models.takeUnlessEmpty() ?: DEFAULT_MODEL_SELECTOR_MAP)
    
}