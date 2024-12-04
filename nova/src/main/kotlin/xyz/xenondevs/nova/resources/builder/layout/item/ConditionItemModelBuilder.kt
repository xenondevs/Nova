package xyz.xenondevs.nova.resources.builder.layout.item

import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.ConditionItemModel
import xyz.xenondevs.nova.resources.builder.data.EmptyItemModel
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.data.Keybind
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder

@RegistryElementBuilderDsl
class ConditionItemModelBuilder<S : ModelSelectorScope> internal constructor(
    private val property: ConditionItemModelProperty,
    resourcePackBuilder: ResourcePackBuilder,
    selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : ItemModelCreationScope<S>(resourcePackBuilder, selectAndBuild) {
    
    /**
     * The model to display when the condition is `true`.
     */
    var onTrue: ItemModel = EmptyItemModel
    
    /**
     * The model to display when the condition is `false`.
     */
    var onFalse: ItemModel = EmptyItemModel
    
    internal fun build(): ConditionItemModel =
        property.buildModel(onTrue, onFalse)
    
}

sealed class ConditionItemModelProperty(internal val property: ConditionItemModel.Property) {
    
    internal open fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
        ConditionItemModel(property, onTrue, onFalse)
    
    /**
     * Returns `true` if the player is currently using this item.
     */
    object UsingItem : ConditionItemModelProperty(ConditionItemModel.Property.USING_ITEM)
    
    /**
     * Returns `true` if the item is damageable and has only one use remaining before breaking.
     */
    object Broken : ConditionItemModelProperty(ConditionItemModel.Property.BROKEN)
    
    /**
     * Returns `true` if the item is damageable and has any amount of damage.
     */
    object Damaged : ConditionItemModelProperty(ConditionItemModel.Property.DAMAGED)
    
    /**
     * Returns `true` if a dataComponent identified by [component] is present on the item.
     * To interpret default components as "no component", set [ignoreDefault] to `true`.
     */
    class HasComponent(
        private val component: Key,
        private val ignoreDefault: Boolean = false
    ) : ConditionItemModelProperty(ConditionItemModel.Property.HAS_COMPONENT) {
        
        override fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
            ConditionItemModel(property, onTrue, onFalse, component = component, ignoreDefault = ignoreDefault)
        
    }
    
    /**
     * Returns `true` if the item is a fishing rod that is currently cast.
     */
    object FishingRodCast : ConditionItemModelProperty(ConditionItemModel.Property.FISHING_ROD_CAST)
    
    /**
     * Returns `true` if the bundle is "open", i.e. it has a selected item visible in the GUI.
     */
    object BundleHasSelectedItem : ConditionItemModelProperty(ConditionItemModel.Property.BUNDLE_HAS_SELECTED_ITEM)
    
    /**
     * Returns `true` if the item is selected in the hotbar.
     */
    object Selected : ConditionItemModelProperty(ConditionItemModel.Property.SELECTED)
    
    /**
     * Returns `true` if the item is currently on the mouse cursor.
     */
    object Carried : ConditionItemModelProperty(ConditionItemModel.Property.CARRIED)
    
    /**
     * Returns `true` if the player has requested extended details by holding the shift key down.
     * Only works for items displayed in the GUI.
     * Not a keybind, can't be rebound.
     */
    object ExtendedView : ConditionItemModelProperty(ConditionItemModel.Property.EXTENDED_VIEW)
    
    /**
     * Returns `true` if the player is currently holding the keybind [keybind].
     */
    class KeybindDown(
        private val keybind: Keybind
    ) : ConditionItemModelProperty(ConditionItemModel.Property.KEYBIND_DOWN) {
        
        override fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
            ConditionItemModel(property, onTrue, onFalse, keybind = keybind)
        
    }
    
    /**
     * - If not spectating, returns `true` when the context entity is the local player entity, i.e. the one controlled by the client.
     * - If spectating, returns `true` when the context entity is the entity being spectated.
     * - If there is no context entity present, returns `false`.
     */
    object ViewEntity : ConditionItemModelProperty(ConditionItemModel.Property.VIEW_ENTITY)
    
    /**
     * Returns the value from [index] in `flags` of the `minecraft:custom_model_data` component.
     */
    open class CustomModelData(
        private val index: Int = 0
    ) : ConditionItemModelProperty(ConditionItemModel.Property.CUSTOM_MODEL_DATA) {
        
        override fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
            ConditionItemModel(property, onTrue, onFalse, index = index)
        
        companion object : CustomModelData(0)
        
    }
    
}