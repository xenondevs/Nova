@file:OptIn(InternalResourcePackDTO::class)

package xyz.xenondevs.nova.resources.builder.layout.item

import com.mojang.serialization.JsonOps
import io.papermc.paper.datacomponent.DataComponentType
import io.papermc.paper.datacomponent.PaperDataComponentType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import net.kyori.adventure.key.Key
import net.minecraft.resources.RegistryOps
import org.bukkit.entity.EntityType
import xyz.xenondevs.nova.registry.RegistryElementBuilderDsl
import xyz.xenondevs.nova.resources.ResourcePath
import xyz.xenondevs.nova.resources.ResourceType
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.builder.data.InternalResourcePackDTO
import xyz.xenondevs.nova.resources.builder.data.ItemModel
import xyz.xenondevs.nova.resources.builder.layout.ModelSelectorScope
import xyz.xenondevs.nova.resources.builder.model.ModelBuilder
import xyz.xenondevs.nova.serialization.kotlinx.googleToKotlinxJson
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.resources.builder.layout.item.ChargedType as ChargedTypeEnum
import xyz.xenondevs.nova.resources.builder.layout.item.DisplayContext as DisplayContextEnum

/**
 * A collection of cases for a select item model.
 *
 * @see SelectItemModelBuilder
 */
class SelectCases<T, S : ModelSelectorScope> internal constructor(
    private val property: SelectItemModelProperty<T>,
    private val selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) {
    
    internal val cases = ArrayList<ItemModel.Select.Case>()
    
    /**
     * Adds a case that displays [model] when any of ([value], [values]) is present.
     */
    operator fun set(value: T, vararg values: T, model: ItemModel) {
        set(listOf(value, *values), model)
    }
    
    /**
     * Adds a case that displays the model created by [selectModel] when any of ([value], [values]) is present.
     */
    operator fun set(value: T, vararg values: T, selectModel: S.() -> ModelBuilder) {
        set(listOf(value, *values), selectModel)
    }
    
    /**
     * Adds a case that displays [model] when any of [values] is present.
     */
    operator fun set(values: List<T>, model: ItemModel) {
        cases += ItemModel.Select.Case(
            values.map(property::toJson),
            model
        )
    }
    
    /**
     * Adds a case that displays the model created by [selectModel] when any of [values] is present.
     */
    operator fun set(values: List<T>, selectModel: S.() -> ModelBuilder) {
        cases += ItemModel.Select.Case(
            values.map(property::toJson),
            ItemModel.Default(selectAndBuild(selectModel))
        )
    }
    
}

@RegistryElementBuilderDsl
class SelectItemModelBuilder<T, S : ModelSelectorScope> internal constructor(
    private val property: SelectItemModelProperty<T>,
    resourcePackBuilder: ResourcePackBuilder,
    selectAndBuild: (S.() -> ModelBuilder) -> ResourcePath<ResourceType.Model>
) : ItemModelCreationScope<S>(resourcePackBuilder, selectAndBuild) {
    
    /**
     * The cases for this select item model.
     */
    val case = SelectCases<T, S>(property, selectAndBuild)
    
    /**
     * The fallback model if no case matches.
     * Can be `null`, but will render a "missing" error model instead.
     */
    var fallback: ItemModel? = null
    
    internal fun build(): ItemModel.Select {
        return property.buildModel(case.cases, fallback)
    }
    
}

sealed class SelectItemModelProperty<T>(internal val property: ItemModel.Select.Property) {
    
    internal abstract fun toJson(value: T): JsonElement
    
    internal open fun buildModel(
        cases: List<ItemModel.Select.Case>,
        fallback: ItemModel?
    ): ItemModel.Select {
        return ItemModel.Select(property, cases, fallback)
    }
    
    /**
     * Returns the main hand of the holding player.
     */
    object MainHand : SelectItemModelProperty<MainHandPosition>(ItemModel.Select.Property.MAIN_HAND) {
        override fun toJson(value: MainHandPosition) = JsonPrimitive(value.name.lowercase())
    }
    
    /**
     * Returns the [charge type][ChargedTypeEnum] stored in the `minecraft:charged_projectiles` component.
     */
    object ChargedType : SelectItemModelProperty<ChargedTypeEnum>(ItemModel.Select.Property.CHARGE_TYPE) {
        override fun toJson(value: ChargedTypeEnum) = JsonPrimitive(value.name.lowercase())
    }
    
    /**
     * Returns the value of a component.
     */
    class Component<T : Any>(
        private val type: DataComponentType.Valued<T>
    ) : SelectItemModelProperty<T>(ItemModel.Select.Property.COMPONENT) {
        
        override fun buildModel(cases: List<ItemModel.Select.Case>, fallback: ItemModel?): ItemModel.Select {
            return ItemModel.Select(property, cases, fallback, component = type.key())
        }
        
        override fun toJson(value: T): JsonElement {
            return toJson<Any>(value)
        }
        
        @Suppress("UNCHECKED_CAST")
        private fun <NMS : Any> toJson(value: T): JsonElement {
            type as PaperDataComponentType.ValuedImpl<T, NMS>
            return googleToKotlinxJson(
                PaperDataComponentType.bukkitToMinecraft<NMS>(type)
                    .codecOrThrow()
                    .encodeStart(
                        RegistryOps.create(JsonOps.INSTANCE, REGISTRY_ACCESS),
                        type.adapter.toVanilla(value, type.holder)
                    )
                    .orThrow
            )
        }
        
    }
    
    /**
     * Returns the value of `material` from the `minecraft:trim` component, if present.
     */
    object TrimMaterial : SelectItemModelProperty<Key>(ItemModel.Select.Property.TRIM_MATERIAL) {
        override fun toJson(value: Key) = JsonPrimitive(value.asString())
    }
    
    /**
     * Returns the value of [propertyName] from the block state stored in the `minecraft:block_state` component.
     */
    class BlockState(
        private val propertyName: String
    ) : SelectItemModelProperty<String>(ItemModel.Select.Property.BLOCK_STATE) {
        
        override fun toJson(value: String) = JsonPrimitive(value)
        
        override fun buildModel(cases: List<ItemModel.Select.Case>, fallback: ItemModel?) =
            ItemModel.Select(
                property,
                cases,
                fallback,
                blockStateProperty = propertyName
            )
        
    }
    
    /**
     * Returns the [context][DisplayContextEnum] the item is rendered in.
     */
    object DisplayContext : SelectItemModelProperty<DisplayContextEnum>(ItemModel.Select.Property.DISPLAY_CONTEXT) {
        override fun toJson(value: DisplayContextEnum) = JsonPrimitive(value.name.lowercase())
    }
    
    /**
     * Returns the current time in [timeZone] formatted according to the given [pattern]
     *
     * @param timeZone The time zone to use for formatting the time. If not present, defaults to the timezone set by the client.
     * Examples:
     * - `Europe/Stockholm`
     * - `GMT+0:45`
     * @param locale The locale to use for formatting the time, or `""` for the root locale.
     * Examples:
     * - `en_US`: English language (used for things like week names), formating as in the USA.
     * - `cs_AU@numbers=thai;calendar=japanese`: Czech language, Australian formatting, Thai numerals and Japanese calendar
     * @param pattern The pattern to be used for formatting the time.
     * Examples:
     * - `yyyy-MM-dd`: 4-digit year number, then 2-digit month number, then 2-digit day of month number, all zero-padded if needed, separated by `-`.
     * - `HH:mm:ss`: current time (hours, minutes, seconds), 24-hour cycle, all zero-padded to 2 digits of needed, separated by `:`.
     */
    class LocalTime(
        private val pattern: String,
        private val timeZone: String? = null,
        private val locale: String = ""
    ) : SelectItemModelProperty<String>(ItemModel.Select.Property.LOCAL_TIME) {
        
        override fun toJson(value: String) = JsonPrimitive(value)
        
        override fun buildModel(cases: List<ItemModel.Select.Case>, fallback: ItemModel?) =
            ItemModel.Select(
                property,
                cases,
                fallback,
                locale = locale,
                timeZone = timeZone,
                pattern = pattern
            )
        
    }
    
    /**
     * Returns the id of the dimension in context, if any.
     */
    object ContextDimension : SelectItemModelProperty<Key>(ItemModel.Select.Property.CONTEXT_DIMENSION) {
        override fun toJson(value: Key) = JsonPrimitive(value.asString())
    }
    
    /**
     * Returns the holding entity type, if present.
     */
    object ContextEntityType : SelectItemModelProperty<EntityType>(ItemModel.Select.Property.CONTEXT_ENTITY_TYPE) {
        override fun toJson(value: EntityType) = JsonPrimitive(value.key().asString())
    }
    
    /**
     * Returns the value at [index] from `strings` of the `minecraft:custom_model_data` component.
     */
    open class CustomModelData(private val index: Int) : SelectItemModelProperty<String>(ItemModel.Select.Property.CUSTOM_MODEL_DATA) {
        
        override fun toJson(value: String) = JsonPrimitive(value.toString())
        
        override fun buildModel(cases: List<ItemModel.Select.Case>, fallback: ItemModel?) =
            ItemModel.Select(
                property,
                cases,
                fallback,
                index = index
            )
        
        companion object : CustomModelData(0)
        
    }
    
}

/**
 * Position of the main hand.
 */
enum class MainHandPosition { LEFT, RIGHT }

/**
 * Charged projectile type.
 */
enum class ChargedType { NONE, ROCKET, ARROW }

/**
 * Context in which an item is rendered.
 */
enum class DisplayContext {
    NONE,
    THIRDPERSON_LEFTHAND,
    THIRDPERSON_RIGHTHAND,
    FIRSTPERSON_LEFTHAND,
    FIRSTPERSON_RIGHTHAND,
    HEAD,
    GUI,
    GROUND,
    FIXED
}