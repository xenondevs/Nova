@file:OptIn(InternalResourcePackDTO::class)

package xyz.xenondevs.nova.resources.builder.layout.item

import io.papermc.paper.datacomponent.DataComponentType
import net.kyori.adventure.key.Key
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.InternalResourcePackDTO
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
    var onTrue: ItemModel = ItemModel.Empty
    
    /**
     * The model to display when the condition is `false`.
     */
    var onFalse: ItemModel = ItemModel.Empty
    
    internal fun build(): ItemModel.Condition =
        property.buildModel(onTrue, onFalse)
    
}

sealed class ConditionItemModelProperty(internal val property: ItemModel.Condition.Property) {
    
    internal open fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
        ItemModel.Condition(property, onTrue, onFalse)
    
    /**
     * Returns `true` if the player is currently using this item.
     */
    object UsingItem : ConditionItemModelProperty(ItemModel.Condition.Property.USING_ITEM)
    
    /**
     * Returns `true` if the item is damageable and has only one use remaining before breaking.
     */
    object Broken : ConditionItemModelProperty(ItemModel.Condition.Property.BROKEN)
    
    /**
     * Returns `true` if the item is damageable and has any amount of damage.
     */
    object Damaged : ConditionItemModelProperty(ItemModel.Condition.Property.DAMAGED)
    
    /**
     * Returns `true` if a dataComponent identified by [component] is present on the item.
     * To interpret default components as "no component", set [ignoreDefault] to `true`.
     */
    class HasComponent(
        private val component: Key,
        private val ignoreDefault: Boolean = false
    ) : ConditionItemModelProperty(ItemModel.Condition.Property.HAS_COMPONENT) {
        
        constructor(
            component: DataComponentType,
            ignoreDefault: Boolean = false
        ) : this(component.key(), ignoreDefault)
        
        override fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
            ItemModel.Condition(property, onTrue, onFalse, component = component, ignoreDefault = ignoreDefault)
        
    }
    
    /**
     * Returns `true` if the item is a fishing rod that is currently cast.
     */
    object FishingRodCast : ConditionItemModelProperty(ItemModel.Condition.Property.FISHING_ROD_CAST)
    
    /**
     * Returns `true` if the bundle is "open", i.e. it has a selected item visible in the GUI.
     */
    object BundleHasSelectedItem : ConditionItemModelProperty(ItemModel.Condition.Property.BUNDLE_HAS_SELECTED_ITEM)
    
    /**
     * Returns `true` if the item is selected in the hotbar.
     */
    object Selected : ConditionItemModelProperty(ItemModel.Condition.Property.SELECTED)
    
    /**
     * Returns `true` if the item is currently on the mouse cursor.
     */
    object Carried : ConditionItemModelProperty(ItemModel.Condition.Property.CARRIED)
    
    /**
     * Returns `true` if the player has requested extended details by holding the shift key down.
     * Only works for items displayed in the GUI.
     * Not a keybind, can't be rebound.
     */
    object ExtendedView : ConditionItemModelProperty(ItemModel.Condition.Property.EXTENDED_VIEW)
    
    /**
     * Returns `true` if the player is currently holding the keybind [keybind].
     */
    class KeybindDown(
        private val keybind: Keybind
    ) : ConditionItemModelProperty(ItemModel.Condition.Property.KEYBIND_DOWN) {
        
        override fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
            ItemModel.Condition(property, onTrue, onFalse, keybind = keybind)
        
    }
    
    /**
     * - If not spectating, returns `true` when the context entity is the local player entity, i.e. the one controlled by the client.
     * - If spectating, returns `true` when the context entity is the entity being spectated.
     * - If there is no context entity present, returns `false`.
     */
    object ViewEntity : ConditionItemModelProperty(ItemModel.Condition.Property.VIEW_ENTITY)
    
    /**
     * Returns the value from [index] in `flags` of the `minecraft:custom_model_data` component.
     */
    open class CustomModelData(
        private val index: Int = 0
    ) : ConditionItemModelProperty(ItemModel.Condition.Property.CUSTOM_MODEL_DATA) {
        
        override fun buildModel(onTrue: ItemModel, onFalse: ItemModel) =
            ItemModel.Condition(property, onTrue, onFalse, index = index)
        
        companion object : CustomModelData(0)
        
    }
    
}