package xyz.xenondevs.nova.world.block.state.property

import ca.spottedleaf.moonrise.patches.blockstate_propertyaccess.PropertyAccess
import com.google.common.collect.ImmutableMap
import net.kyori.adventure.key.Key
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.IntegerProperty
import net.minecraft.world.level.block.state.properties.Property
import org.bukkit.Keyed
import org.bukkit.craftbukkit.block.data.CraftBlockData
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
    
    internal abstract val nmsProperty: Property<*>
    
    @Suppress("UNCHECKED_CAST")
    internal open fun fromNmsValue(value: Any): T = value as T
    
    internal open fun toNmsValue(value: T): Any = value
    
    @Suppress("UNCHECKED_CAST")
    private fun nmsPropertyTyped(): Property<Comparable<Any>> = nmsProperty as Property<Comparable<Any>>
    
    internal fun get(state: BlockState): T? =
        state.getOptionalValue(nmsPropertyTyped()).getOrNull()?.let(::fromNmsValue)
    
    @Suppress("UNCHECKED_CAST")
    internal fun set(state: BlockState, value: T): BlockState =
        state.setValue(nmsPropertyTyped(), toNmsValue(value) as Comparable<Any>)
    
    @Suppress("UNCHECKED_CAST")
    internal fun set(data: CraftBlockData, value: T) {
        data.set(nmsPropertyTyped(), toNmsValue(value) as Comparable<Any>)
    }
    
    internal val name: String
        get() = nmsProperty.name
    
    /**
     * A list of all possible values that this property can have.
     */
    val values: List<T>
        get() = nmsProperty.possibleValues.map(::fromNmsValue)
    
    /**
     * A list of all possible values that this property can have, as strings.
     */
    val stringValues: List<String>
        get() = values.map(::valueToString)
    
    /**
     * Determines whether the given [value] is valid for this property.
     */
    open fun isValidValue(value: T): Boolean = toNmsValue(value) in nmsProperty.possibleValues
    
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
        @Suppress("UNCHECKED_CAST")
        return nmsPropertyTyped().getName(toNmsValue(value) as Comparable<Any>)
    }
    
    /**
     * Converts the given [string] to the corresponding value
     * or throws an [IllegalArgumentException] if [string] is not valid for this property.
     */
    fun stringToValue(string: String): T {
        return nmsProperty.getValue(string).getOrNull()?.let(::fromNmsValue)
            ?: throw IllegalArgumentException("Value $string is not valid for property $this")
    }
    
    override fun key() = key
    override fun getKey() = key.toNamespacedKey()
    override fun toString(): String = key.asString()
    
}

internal class MappedProperty<T : Comparable<T>, N : Comparable<N>>(
    override val nmsProperty: Property<N>,
    default: T,
    private val fromNms: (N) -> T,
    private val toNms: (T) -> N,
    initializer: (Context<BlockPlace>) -> T
) : BlockStateProperty<T>(Key.key("minecraft", nmsProperty.name), default, initializer) {
    
    @Suppress("UNCHECKED_CAST")
    override fun fromNmsValue(value: Any): T = fromNms(value as N)
    
    override fun toNmsValue(value: T): Any = toNms(value)
    
    override fun isValidValue(value: T): Boolean = value in values
    
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

@Suppress("UNCHECKED_CAST")
private class CustomInternalEnumProperty<T : Enum<T>>(
    name: String,
    clazz: Class<T>,
    values: List<T>
) : Property<T>(name, clazz), PropertyAccess<T> {
    
    private val values = values.toList()
    private val names: Map<String, T>
    private val ordinalToIndex: IntArray
    
    init {
        require(values.isNotEmpty()) { "Trying to make empty EnumProperty '$name'" }
        
        val names = ImmutableMap.builder<String, T>()
        val byId = ReflectArray.newInstance(clazz, this.values.size) as Array<T>
        
        ordinalToIndex = IntArray(clazz.enumConstants.size) { -1 }
        for ([id, value] in this.values.withIndex()) {
            ordinalToIndex[value.ordinal] = id
            byId[id] = value
            names.put(getName(value), value)
        }
        
        this.names = names.buildOrThrow()
        
        `moonrise$setById`(byId)
    }
    
    override fun getPossibleValues(): List<T> = values
    override fun getValue(name: String): Optional<T> = Optional.ofNullable(names[name])
    override fun getName(value: T): String = value.name.lowercase(Locale.ROOT)
    override fun getInternalIndex(value: T): Int = ordinalToIndex[value.ordinal]
    
    override fun `moonrise$getIdFor`(value: T): Int {
        val target = valueClass
        if (value.javaClass !== target && value.declaringJavaClass !== target)
            return -1
        return ordinalToIndex[value.ordinal]
    }
    
    override fun generateHashCode(): Int = 31 * super.generateHashCode() + values.hashCode()
    
}

// string-based property for unknown block states
internal class UnknownProperty(key: Key, name: String, strings: List<String>) : BlockStateProperty<String>(key, strings[0], { strings[0] }) {
    
    override val nmsProperty = object : Property<String>(name, String::class.java) {
        
        init {
            `moonrise$setById`(strings.toTypedArray())
        }
        
        override fun getPossibleValues(): List<String> = strings
        override fun getName(s: String): String = s
        override fun getValue(s: String): Optional<String> = if (s in strings) Optional.of(s) else Optional.empty()
        override fun `moonrise$getIdFor`(s: String): Int = getInternalIndex(s)
        override fun getInternalIndex(s: String): Int = strings.indexOf(s)
        
    }
    
}