@file:Suppress("unused", "CanBeParameter")

package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.kyori.adventure.key.Key
import org.bukkit.DyeColor
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.serialization.kotlinx.ValueOrListSerializer

@Serializable
internal data class ItemModelDefinition(
    val model: ItemModel,
    @SerialName("hand_animation_on_swap")
    val handAnimationOnSwap: Boolean = true
)

@Serializable
sealed interface ItemModel

@Serializable
@SerialName("minecraft:model")
internal data class DefaultItemModel(
    val model: ResourcePath<ResourceType.Model>,
    val tints: List<TintSource>? = null
) : ItemModel

@Serializable
@SerialName("minecraft:composite")
internal data class CompositeItemModel(val models: List<ItemModel>) : ItemModel

@Serializable
@SerialName("minecraft:condition")
internal data class ConditionItemModel(
    val property: Property,
    @SerialName("on_true")
    val onTrue: ItemModel,
    @SerialName("on_false")
    val onFalse: ItemModel,
    
    // has_component
    val component: Key? = null,
    val ignoreDefault: Boolean = false,
    
    // keybind_down
    val keybind: Keybind? = null,
    
    // custom_model_data
    val index: Int = 0
) : ItemModel {
    
    @Serializable
    enum class Property {
        
        @SerialName("minecraft:using_item")
        USING_ITEM,
        
        @SerialName("minecraft:broken")
        BROKEN,
        
        @SerialName("minecraft:damaged")
        DAMAGED,
        
        @SerialName("minecraft:has_component")
        HAS_COMPONENT,
        
        @SerialName("minecraft:fishing_rod/cast")
        FISHING_ROD_CAST,
        
        @SerialName("minecraft:bundle/has_selected_item")
        BUNDLE_HAS_SELECTED_ITEM,
        
        @SerialName("minecraft:selected")
        SELECTED,
        
        @SerialName("minecraft:carried")
        CARRIED,
        
        @SerialName("minecraft:extended_view")
        EXTENDED_VIEW,
        
        @SerialName("minecraft:keybind_down")
        KEYBIND_DOWN,
        
        @SerialName("minecraft:custom_model_data")
        CUSTOM_MODEL_DATA,
        
        @SerialName("minecraft:view_entity")
        VIEW_ENTITY
        
    }
    
}

@Serializable
@SerialName("minecraft:select")
internal data class SelectItemModel(
    val property: Property,
    val cases: List<Case>,
    val fallback: ItemModel? = null,
    
    // block_state
    @SerialName("block_state_property")
    val blockStateProperty: String? = null,
    
    // local_time
    val locale: String = "",
    @SerialName("time_zone")
    val timeZone: String? = null,
    val pattern: String? = null,
    
    // custom_model_data
    val index: Int = 0

) : ItemModel {
    
    @Serializable
    enum class Property {
        
        @SerialName("minecraft:main_hand")
        MAIN_HAND,
        
        @SerialName("minecraft:charge_type")
        CHARGE_TYPE,
        
        @SerialName("minecraft:trim_material")
        TRIM_MATERIAL,
        
        @SerialName("minecraft:block_state")
        BLOCK_STATE,
        
        @SerialName("minecraft:display_context")
        DISPLAY_CONTEXT,
        
        @SerialName("minecraft:context_dimension")
        CONTEXT_DIMENSION,
        
        @SerialName("minecraft:context_entity_type")
        CONTEXT_ENTITY_TYPE,
        
        @SerialName("minecraft:local_time")
        LOCAL_TIME,
        
        @SerialName("minecraft:custom_model_data")
        CUSTOM_MODEL_DATA
        
    }
    
    @Serializable
    data class Case(
        @Serializable(with = ValueOrListSerializer::class)
        val `when`: List<String>,
        val model: ItemModel
    )
    
}

@Serializable
@SerialName("minecraft:range_dispatch")
internal data class RangeDispatchItemModel(
    val property: Property,
    val scale: Double = 1.0,
    val entries: List<Entry>,
    val fallback: ItemModel? = null,
    
    // damage, count
    val normalize: Boolean = true,
    
    // time, compass
    val wobble: Boolean = true,
    
    // time
    val source: TimeSource? = null,
    
    // compass
    val target: CompassTarget? = null,
    
    // use_duration
    val remaining: Boolean = false,
    
    // use_cycle
    val period: Double = 1.0,
    
    // custom_model_data
    val index: Int = 0
) : ItemModel {
    
    init {
        require(period >= 0) { "Period must be >= 0" }
    }
    
    @Serializable
    enum class Property {
        
        @SerialName("minecraft:custom_model_data")
        CUSTOM_MODEL_DATA,
        
        @SerialName("minecraft:bundle/fullness")
        BUNDLE_FULLNESS,
        
        @SerialName("minecraft:damage")
        DAMAGE,
        
        @SerialName("minecraft:count")
        COUNT,
        
        @SerialName("minecraft:cooldown")
        COOLDOWN,
        
        @SerialName("minecraft:time")
        TIME,
        
        @SerialName("minecraft:compass")
        COMPASS,
        
        @SerialName("minecraft:crossbow/pull")
        CROSSBOW_PULL,
        
        @SerialName("minecraft:use_duration")
        USE_DURATION,
        
        @SerialName("minecraft:use_cycle")
        USE_CYCLE
        
    }
    
    @Serializable
    data class Entry(
        val threshold: Double,
        val model: ItemModel
    )
    
}

@Serializable
@SerialName("minecraft:special")
internal data class SpecialItemModel(
    val model: SpecialModel,
    val base: ResourcePath<ResourceType.Model>
) : ItemModel {
    
    @Serializable
    sealed interface SpecialModel {
        
        @Serializable
        data class Bed(
            val texture: ResourcePath<ResourceType.BedTexture>
        ) : SpecialModel
        
        @Serializable
        data class Banner(
            val color: DyeColor // TODO: lowercase enum required?
        ) : SpecialModel
        
        @Serializable
        data object Conduit : SpecialModel
        
        @Serializable
        data class Chest(
            val texture: ResourcePath<ResourceType.ChestTexture>,
            val openness: Double = 0.0
        ) : SpecialModel
        
        @Serializable
        data object DecoratedPot : SpecialModel
        
        @Serializable
        data class Head(
            val kind: HeadKind,
            val texture: ResourcePath<ResourceType.EntityTexture>,
            val animation: Double = 0.0
        ) : SpecialModel
        
        @Serializable
        data class ShulkerBox(
            val name: ResourcePath<ResourceType.ShulkerTexture>,
            val openness: Double = 0.0,
            val orientation: Orientation = Orientation.UP
        ) : SpecialModel
        
        @Serializable
        data object Shield : SpecialModel
        
        @Serializable
        data class StandingSign(
            val woodType: WoodType,
            val texture: ResourcePath<ResourceType.SignTexture>? = null
        ) : SpecialModel
        
        @Serializable
        data object Trident : SpecialModel
        
        @Serializable
        data class HangingSign(
            val woodType: WoodType,
            val texture: ResourcePath<ResourceType.SignTexture>? = null
        ) : SpecialModel
        
    }
    
}

@Serializable
@SerialName("minecraft:bundle/selected_item")
internal data object BundleSelectedItemItemModel : ItemModel

@Serializable
@SerialName("minecraft:empty")
internal data object EmptyItemModel : ItemModel