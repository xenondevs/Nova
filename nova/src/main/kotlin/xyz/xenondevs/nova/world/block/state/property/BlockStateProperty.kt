package xyz.xenondevs.nova.world.block.state.property

import ca.spottedleaf.moonrise.patches.blockstate_propertyaccess.PropertyAccess
import com.google.common.collect.ImmutableMap
import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.Property
import org.bukkit.Keyed
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.BlockPlace
import xyz.xenondevs.nova.util.toNamespacedKey
import java.util.*
import kotlin.jvm.optionals.getOrNull
import net.minecraft.world.level.block.state.properties.BooleanProperty as NmsBooleanProperty
import java.lang.reflect.Array as ReflectArray

private val REPLACE_REGEX = Regex("""[:_\-./]""")
private fun Key.asPropertyString(): String = asString().replace(REPLACE_REGEX, "_")

/**
 * Represents a property of a block state and its allowed values.
 *
 * The [defaultValue] is used for the block's registered default state. The [initializer] is evaluated
 * for each placement and may select a different value based on the placement context.
 */
abstract class BlockStateProperty<T : Comparable<T>>(
    /**
     * The namespaced identifier of this property.
     */
    val key: Key,
    /**
     * The value used for the block's registered default state.
     */
    val defaultValue: T,
    /**
     * Selects the property's value when a block is placed.
     */
    val initializer: (Context<BlockPlace>) -> T
) : Keyed {
    
    internal abstract val nmsProperty: Property<T>
    internal val name: String
        get() = nmsProperty.name
    
    /**
     * A list of all possible values that this property can have.
     */
    val values: List<T>
        get() = nmsProperty.possibleValues
    
    /**
     * Determines whether the given [value] is valid for this property.
     */
    open fun isValidValue(value: T): Boolean = value in nmsProperty.possibleValues
    
    /**
     * Determines whether the given [string] is valid for this property.
     */
    fun isValidString(string: String): Boolean = nmsProperty.getValue(string).isPresent
    
    /**
     * Converts the given [value] to a [String]
     * or throws an [IllegalArgumentException] if [value] is not valid for this property.
     */
    fun valueToString(value: T): String {
        require(isValidValue(value)) { "Value $value is not valid for property $this" }
        return nmsProperty.getName(value)
    }
    
    /**
     * Converts the given [string] to the corresponding value
     * or throws an [IllegalArgumentException] if [string] is not valid for this property.
     */
    fun stringToValue(string: String): T {
        return nmsProperty.getValue(string).getOrNull()
            ?: throw IllegalArgumentException("Value $string is not valid for property $this")
    }
    
    override fun key() = key
    override fun getKey() = key.toNamespacedKey()
    override fun toString(): String = key.asString()
    
}

/**
 * A block-state property with the values `true` and `false`.
 */
class BooleanProperty private constructor(
    id: Key,
    defaultValue: Boolean,
    initializer: (Context<BlockPlace>) -> Boolean,
    override val nmsProperty: NmsBooleanProperty,
) : BlockStateProperty<Boolean>(id, defaultValue, initializer) {
    
    /**
     * Creates a boolean property that defaults to `false` and uses [initializer] during placement.
     */
    constructor(id: Key, initializer: (Context<BlockPlace>) -> Boolean) :
        this(id, false, initializer, NmsBooleanProperty.create(id.asPropertyString()))
    
    /**
     * Creates a boolean property with the given [default] and placement [initializer].
     */
    constructor(id: Key, default: Boolean, initializer: (Context<BlockPlace>) -> Boolean) :
        this(id, default, initializer, NmsBooleanProperty.create(id.asPropertyString()))
    
    /**
     * Creates a boolean property that always uses [default].
     */
    constructor(id: Key, default: Boolean = false) :
        this(id, default, { default }, NmsBooleanProperty.create(id.asPropertyString()))
    
    internal constructor(
        nmsProperty: NmsBooleanProperty,
        default: Boolean = false,
        initializer: (Context<BlockPlace>) -> Boolean = { default }
    ) : this(Key.key("minecraft", nmsProperty.name), default, initializer, nmsProperty)
    
}

/**
 * A block-state property with integer values in a fixed range.
 */
class IntProperty private constructor(
    id: Key,
    defaultValue: Int,
    initializer: (Context<BlockPlace>) -> Int,
    override val nmsProperty: IntegerProperty
) : BlockStateProperty<Int>(id, defaultValue, initializer) {
    
    init {
        require(defaultValue in nmsProperty.possibleValues) {
            "Default value $defaultValue is not valid for property ${id.asString()}"
        }
    }
    
    /**
     * Creates an integer property whose registered default is the first value in [range].
     * The [initializer] selects the value used during placement.
     */
    constructor(
        id: Key,
        range: IntRange,
        initializer: (Context<BlockPlace>) -> Int = { range.first }
    ) : this(id, range.first, initializer, IntegerProperty.create(id.asPropertyString(), range.first, range.last))
    
    /**
     * Creates an integer property with the given [default] and placement [initializer].
     *
     * The [default] must be contained in [range].
     */
    constructor(
        id: Key,
        range: IntRange,
        default: Int,
        initializer: (Context<BlockPlace>) -> Int
    ) : this(id, default, initializer, IntegerProperty.create(id.asPropertyString(), range.first, range.last))
    
    internal constructor(
        nmsProperty: IntegerProperty,
        default: Int = nmsProperty.possibleValues.first(),
        initializer: (Context<BlockPlace>) -> Int = { default }
    ) : this(Key.key("minecraft", nmsProperty.name), default, initializer, nmsProperty)
    
}

/**
 * Creates an enum property from the supplied [values].
 *
 * If no values are supplied, all enum constants are allowed. By default, the first allowed value is
 * used for the registered default state and during placement. Use [default] and [initializer] to
 * configure those decisions separately.
 */
inline fun <reified E : Enum<E>> EnumProperty(
    id: Key,
    vararg values: E,
    default: E = values.firstOrNull() ?: E::class.java.enumConstants[0],
    noinline initializer: (Context<BlockPlace>) -> E = { default }
): EnumProperty<E> = EnumProperty(id, E::class.java, values.toSet(), default, initializer)

/**
 * A block-state property backed by enum values.
 */
class EnumProperty<E : Enum<E>> private constructor(
    id: Key,
    defaultValue: E,
    initializer: (Context<BlockPlace>) -> E,
    override val nmsProperty: Property<E>
) : BlockStateProperty<E>(id, defaultValue, initializer) {
    
    init {
        require(defaultValue in nmsProperty.possibleValues) {
            "Default value $defaultValue is not valid for property ${id.asString()}"
        }
    }
    
    /**
     * Creates an enum property whose registered default is the first value in [values].
     * If [values] is empty, all constants from [enumClass] are allowed.
     */
    constructor(
        id: Key,
        enumClass: Class<E>,
        values: Set<E>,
        initializer: (Context<BlockPlace>) -> E
    ) : this(
        id,
        values.firstOrNull() ?: enumClass.enumConstants[0],
        initializer,
        CustomInternalEnumProperty(
            name = id.asPropertyString(),
            clazz = enumClass,
            values = values.ifEmpty { EnumSet.allOf(enumClass) }.toList()
        )
    )
    
    /**
     * Creates an enum property with the given [default] and placement [initializer].
     *
     * The [default] must be contained in [values], unless [values] is empty and therefore allows all constants.
     */
    constructor(
        id: Key,
        enumClass: Class<E>,
        values: Set<E>,
        default: E,
        initializer: (Context<BlockPlace>) -> E
    ) : this(
        id,
        default,
        initializer,
        CustomInternalEnumProperty(
            name = id.asPropertyString(),
            clazz = enumClass,
            values = values.ifEmpty { EnumSet.allOf(enumClass) }.toList()
        )
    )
    
    /**
     * Creates an enum property containing all constants from [enumClass].
     */
    constructor(id: Key, enumClass: Class<E>) : this(id, enumClass, emptySet(), { enumClass.enumConstants[0] })
    
    internal constructor(
        nmsProperty: Property<E>,
        default: E = nmsProperty.possibleValues.first(),
        initializer: (Context<BlockPlace>) -> E = { default }
    ) : this(
        Key.key("minecraft", nmsProperty.name),
        default,
        initializer,
        nmsProperty
    )
    
}

private class CustomInternalEnumProperty<T : Enum<T>>(
    name: String,
    clazz: Class<T>,
    values: List<T>
) : Property<T>(name, clazz), PropertyAccess<T> {
    
    private val values: List<T>
    private val names: Map<String, T>
    private val ordinalToIndex: IntArray
    private val idLookupTable: IntArray
    
    init {
        require(values.isNotEmpty()) { "Trying to make empty EnumProperty '$name'" }
        
        this.values = values.toList()
        
        val allEnumValues = clazz.enumConstants
        ordinalToIndex = IntArray(allEnumValues.size) { ordinal ->
            this.values.indexOf(allEnumValues[ordinal])
        }
        
        val names = ImmutableMap.builder<String, T>()
        for (value in this.values) {
            names.put(value.name.lowercase(Locale.ROOT), value)
        }
        this.names = names.buildOrThrow()
        
        idLookupTable = IntArray(allEnumValues.size) { -1 }
        @Suppress("UNCHECKED_CAST")
        val byId = ReflectArray.newInstance(clazz, this.values.size) as Array<T>
        for ([id, value] in this.values.withIndex()) {
            idLookupTable[value.ordinal] = id
            byId[id] = value
        }
        `moonrise$setById`(byId)
    }
    
    override fun getPossibleValues(): List<T> =
        values
    
    override fun getValue(name: String): Optional<T> =
        Optional.ofNullable(names[name])
    
    override fun getName(value: T): String =
        value.name.lowercase(Locale.ROOT)
    
    override fun getInternalIndex(value: T): Int =
        ordinalToIndex[value.ordinal]
    
    override fun `moonrise$getIdFor`(value: T): Int {
        val target = valueClass
        return if (value.javaClass != target && value.javaClass.declaringClass != target) {
            -1
        } else {
            idLookupTable[value.ordinal]
        }
    }
    
    override fun generateHashCode(): Int =
        31 * super.generateHashCode() + values.hashCode()
    
}
