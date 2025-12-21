@file:Suppress("unused", "CanBeParameter")
@file:UseSerializers(KeySerializer::class)

package xyz.xenondevs.nova.resources.builder.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.JsonElement
import net.kyori.adventure.key.Key
import org.bukkit.DyeColor
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.serialization.kotlinx.KeySerializer
import xyz.xenondevs.nova.serialization.kotlinx.LowercaseDyeColorSerializer
import xyz.xenondevs.nova.serialization.kotlinx.ValueOrListSerializer

/**
 * An [Item Model Definition](https://minecraft.wiki/w/Items_model_definition).
 *
 * @param model The model to use.
 * @param handAnimationOnSwap Whether a down-and-up animation should be played in first-person view
 * when the item slot is changed (either type, count, components or by swapping the item into the other hand).
 */
@Serializable
data class ItemModelDefinition(
    val model: ItemModel,
    @SerialName("hand_animation_on_swap")
    val handAnimationOnSwap: Boolean = true,
    @SerialName("oversized_in_gui")
    val oversizedInGui: Boolean = false,
    @SerialName("swap_animation_scale")
    val swapAnimationScale: Double = 1.0
)

/**
 * An [Item Model](https://minecraft.wiki/w/Items_model_definition#Items_model_types).
 */
@Serializable
sealed interface ItemModel {
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:model")
    data class Default(
        val model: ResourcePath<ResourceType.Model>,
        val tints: List<TintSource>? = null
    ) : ItemModel
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:composite")
    data class Composite(val models: List<ItemModel>) : ItemModel
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:condition")
    data class Condition(
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
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:select")
    data class Select(
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
        val index: Int = 0,
        
        // component
        val component: Key? = null
    ) : ItemModel {
        
        @Serializable
        enum class Property {
            
            @SerialName("minecraft:block_state")
            BLOCK_STATE,
            
            @SerialName("minecraft:charge_type")
            CHARGE_TYPE,
            
            @SerialName("minecraft:component")
            COMPONENT,
            
            @SerialName("minecraft:context_dimension")
            CONTEXT_DIMENSION,
            
            @SerialName("minecraft:context_entity_type")
            CONTEXT_ENTITY_TYPE,
            
            @SerialName("minecraft:custom_model_data")
            CUSTOM_MODEL_DATA,
            
            @SerialName("minecraft:display_context")
            DISPLAY_CONTEXT,
            
            @SerialName("minecraft:local_time")
            LOCAL_TIME,
            
            @SerialName("minecraft:main_hand")
            MAIN_HAND,
            
            @SerialName("minecraft:trim_material")
            TRIM_MATERIAL
            
        }
        
        @Serializable
        data class Case(
            @Serializable(with = ValueOrListSerializer::class)
            val `when`: List<JsonElement>,
            val model: ItemModel
        )
        
    }
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:range_dispatch")
    data class RangeDispatch(
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
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:special")
    data class Special(
        val model: SpecialModel,
        val base: ResourcePath<ResourceType.Model>
    ) : ItemModel {
        
        @Serializable
        sealed interface SpecialModel {
            
            @Serializable
            @SerialName("minecraft:bed")
            data class Bed(
                val texture: ResourcePath<ResourceType.BedTexture>
            ) : SpecialModel
            
            @Serializable
            @SerialName("minecraft:banner")
            data class Banner(
                @Serializable(with = LowercaseDyeColorSerializer::class)
                val color: DyeColor
            ) : SpecialModel
            
            @Serializable
            @SerialName("minecraft:conduit")
            data object Conduit : SpecialModel
            
            @Serializable
            @SerialName("minecraft:chest")
            data class Chest(
                val texture: ResourcePath<ResourceType.ChestTexture>,
                val openness: Double = 0.0
            ) : SpecialModel
            
            @Serializable
            @SerialName("minecraft:copper_golem_statue")
            data class CopperGolemStatue(
                val pose: Pose,
                val texture: ResourcePath<ResourceType.CopperGolemStatueTexture>
            ) : SpecialModel {
                
                @Serializable
                enum class Pose {
                    
                    @SerialName("sitting")
                    SITTING,
                    
                    @SerialName("running")
                    RUNNING,
                    
                    @SerialName("star")
                    STAR,
                    
                    @SerialName("standing")
                    STANDING
                    
                }
                
            }
            
            @Serializable
            @SerialName("minecraft:decorated_pot")
            data object DecoratedPot : SpecialModel
            
            @Serializable
            @SerialName("minecraft:head")
            data class Head(
                val kind: HeadKind,
                val texture: ResourcePath<ResourceType.EntityTexture>? = kind.defaultTexture,
                val animation: Double = 0.0
            ) : SpecialModel
            
            @Serializable
            @SerialName("minecraft:player_head")
            data object PlayerHead : SpecialModel
            
            @Serializable
            @SerialName("minecraft:shulker_box")
            data class ShulkerBox(
                val texture: ResourcePath<ResourceType.ShulkerTexture>,
                val openness: Double = 0.0,
                val orientation: Orientation = Orientation.UP
            ) : SpecialModel
            
            @Serializable
            @SerialName("minecraft:shield")
            data object Shield : SpecialModel
            
            @Serializable
            @SerialName("minecraft:standing_sign")
            data class StandingSign(
                val woodType: WoodType,
                val texture: ResourcePath<ResourceType.SignTexture>? = null
            ) : SpecialModel
            
            @Serializable
            @SerialName("minecraft:trident")
            data object Trident : SpecialModel
            
            @Serializable
            @SerialName("minecraft:hanging_sign")
            data class HangingSign(
                val woodType: WoodType,
                val texture: ResourcePath<ResourceType.SignTexture>? = null
            ) : SpecialModel
            
        }
        
    }
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:bundle/selected_item")
    data object BundleSelectedItem : ItemModel
    
    @InternalResourcePackDTO
    @Serializable
    @SerialName("minecraft:empty")
    data object Empty : ItemModel
    
}