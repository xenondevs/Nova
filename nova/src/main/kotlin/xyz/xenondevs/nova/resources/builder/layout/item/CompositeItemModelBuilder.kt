package xyz.xenondevs.nova.resources.builder.layout.item

import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.CompositeItemModel
import xyz.xenondevs.nova.resources.builder.data.DefaultItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder

/**
 * A collection of [ItemModels][ItemModel].
 *
 * @see CompositeItemModelBuilder
 */
class ItemModelsCollection<S : ModelSelectorScope> internal constructor(
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) {
    
    internal val models = ArrayList<ItemModel>()
    
    /**
     * Adds the model created by [selectModel] to the collection.
     */
    operator fun plusAssign(selectModel: S.() -> ModelBuilder) {
        models += DefaultItemModel(selectAndBuild(selectModel))
    }
    
    /**
     * Adds the given [model] to the collection.
     */
    operator fun plusAssign(model: ItemModel) {
        models += model
    }
    
}

@RegistryElementBuilderDsl
class CompositeItemModelBuilder<S : ModelSelectorScope> internal constructor(
    resourcePackBuilder: ResourcePackBuilder,
    selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : ItemModelCreationScope<S>(resourcePackBuilder, selectAndBuild) {
    
    /**
     * The collection of models displayed in this composite model.
     */
    var models: ItemModelsCollection<S> = ItemModelsCollection(selectAndBuild)
    
    internal fun build() = CompositeItemModel(models.models)
    
}