package xyz.xenondevs.nova.compiler

import org.junit.jupiter.api.Test

internal class TypedKeyAsKeyTest : CompilerPluginTest(
    "NOVA_TYPED_KEY_AS_KEY",
    KEY_SOURCE,
    TYPED_KEY_SOURCE
) {
    
    @Test
    fun `reports implicit conversions and qualified conversions once`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun accept(key: Key) = Unit
            fun returnKey(key: TypedKey<String>): Key = key // warn
            class Holder(val key: TypedKey<String>)
            fun returnQualifiedKey(holder: Holder): Key = holder.key // warn

            fun test(key: TypedKey<String>, holder: Holder) {
                var assigned: Key = key // warn
                assigned = holder.key // warn
                accept(key) // warn
                accept(key = holder.key) // warn
            }
        """
    )
    
    @Test
    fun `reports inherited key members`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey

            class Holder(val key: TypedKey<String>)

            fun test(key: TypedKey<String>, holder: Holder) {
                key.namespace() // warn
                key.value() // warn
                key.asString() // warn
                holder.key.asString() // warn
            }
        """
    )
    
    @Test
    fun `reports covariant constructor and body property overrides`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            interface KeyOwner {
                val key: Key
            }

            class TypedKeyOwner(
                override val key: TypedKey<String> // warn
            ) : KeyOwner

            class TypedKeyOwnerBody(typedKey: TypedKey<String>) : KeyOwner {
                override val key: TypedKey<String> = typedKey // warn
            }
        """
    )
    
    @Test
    fun `reports unsafe safe and nullable casts`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun test(key: TypedKey<String>, nullable: TypedKey<String>?) {
                key as Key // warn
                key as? Key // warn
                key as Key? // warn
                nullable as Key? // warn
            }
        """
    )
    
    @Test
    fun `reports conversions independently of nullability`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun accept(key: Key?) = Unit
            fun returnKey(key: TypedKey<String>?): Key? = key // warn
            fun test(key: TypedKey<String>, nullable: TypedKey<String>?) {
                val first: Key? = key // warn
                val second: Key? = nullable // warn
                accept(key) // warn
                accept(nullable) // warn
                nullable?.namespace() // warn
            }
        """
    )
    
    @Test
    fun `substitutes explicit and inferred generic function arguments`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun <T> consume(value: T) = Unit
            fun <T> choose(first: T, second: T): T = first

            fun test(typed: TypedKey<String>, plain: Key) {
                consume<Key>(typed) // warn
                consume<Key?>(typed) // warn
                choose(plain, typed) // warn
                consume(typed)
                consume<Any>(typed)
                consume<TypedKey<String>>(typed)
            }
        """
    )
    
    @Test
    fun `substitutes generic member constructor and extension arguments`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            class Box<T>(val value: T) {
                fun consume(value: T) = Unit
            }
            interface Consumer<T> {
                fun consume(value: T)
            }
            interface Keys : Consumer<Key>
            fun <T> List<T>.consume(value: T) = Unit

            fun test(typed: TypedKey<String>, map: MutableMap<Key, String>, box: Box<Key>, keys: Keys) {
                map[typed] = "value" // warn
                box.consume(typed) // warn
                keys.consume(typed) // warn
                Box<Key>(typed) // warn
                emptyList<Key>().consume(typed) // warn
            }
        """
    )
    
    @Test
    fun `checks individual vararg elements with substituted element types`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun accept(vararg keys: Key) = Unit
            fun <T> generic(vararg values: T) = Unit

            fun test(typed: TypedKey<String>, plain: Key, keys: Array<Key>) {
                accept(typed) // warn
                accept(typed, plain, typed) // warn 2
                accept(*keys, typed) // warn
                generic<Key>(typed) // warn
                generic(plain, typed) // warn
                accept(plain, typed.key())
                accept(*keys)
                accept(keys = keys)
                generic(typed)
            }
        """
    )
    
    @Test
    fun `checks if and when branch results in declarations assignments arguments and returns`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun accept(key: Key) = Unit
            fun returned(flag: Boolean, typed: TypedKey<String>, plain: Key): Key =
                if (flag) typed else plain // warn

            fun test(flag: Boolean, typed: TypedKey<String>, plain: Key) {
                var key: Key = if (flag) typed else plain // warn
                key = if (flag) plain else typed // warn
                accept(if (flag) typed else plain) // warn
                val nested: Key = when {
                    flag -> if (flag) typed else plain // warn
                    else -> {
                        println("branch")
                        typed // warn
                    }
                }
                accept(if (flag) typed else typed) // warn
            }
        """
    )
    
    @Test
    fun `checks elvis and try branch results`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun test(typed: TypedKey<String>?, plain: Key): Key {
                val elvis: Key = typed ?: plain // warn
                return try {
                    typed // warn
                } catch (exception: Exception) {
                    plain
                } ?: plain
            }
        """
    )
    
    @Test
    fun `checks function and constructor parameter defaults`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun makeTypedKey(): TypedKey<String> = TODO()
            fun accept(key: Key = makeTypedKey()) = Unit // warn
            fun nullable(key: Key? = makeTypedKey()) = Unit // warn
            class Holder(val key: Key = makeTypedKey()) // warn
            fun branches(flag: Boolean, plain: Key, key: Key = if (flag) makeTypedKey() else plain) = Unit // warn
            fun allowed(key: TypedKey<String> = makeTypedKey()) = Unit
        """
    )
    
    @Test
    fun `allows bottom types and null literals`() = assertDiagnostics(
        """
            import net.kyori.adventure.key.Key

            fun accept(key: Key = error("missing")) = Unit
            fun missing(): Key = error("missing")
            fun thrown(): Key = throw IllegalStateException()
            fun absent(): Key? = null
            fun test(flag: Boolean, plain: Key) {
                val key: Key = error("missing")
                val nullable: Key? = null
                accept(error("missing"))
                val branch: Key = if (flag) error("missing") else plain
            }
        """
    )
    
    @Test
    fun `allows typed key api explicit key access and unrelated expected types`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun accept(key: Key) = Unit
            fun acceptTyped(key: TypedKey<String>) = Unit

            fun test(flag: Boolean, key: TypedKey<String>, plain: Key) {
                acceptTyped(key)
                accept(key.key())
                key.key().namespace()
                key.registryKey()
                val any: Any = key
                val branch: Any = if (flag) key else plain
                val typed: TypedKey<String> = if (flag) key else key
                val inferred = key
            }
        """
    )
    
    @Test
    fun `reports delegated constructor arguments`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            open class Base(key: Key)
            class Primary(typed: TypedKey<String>) : Base(typed) // warn
            class Secondary : Base {
                constructor(typed: TypedKey<String>) : super(typed) // warn
            }
            class Chained(key: Key) {
                constructor(typed: TypedKey<String>, marker: Int) : this(typed) // warn
            }
            open class GenericBase<T>(key: T)
            class Generic(typed: TypedKey<String>) : GenericBase<Key>(typed) // warn
            open class VarargBase(vararg keys: Key)
            class Vararg(typed: TypedKey<String>) : VarargBase(typed, typed) // warn 2

            class Explicit(typed: TypedKey<String>) : Base(typed.key())
            class Typed(typed: TypedKey<String>) : GenericBase<TypedKey<String>>(typed)
        """
    )
    
    @Test
    fun `reports interface delegate conversions`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            class Wrapped(typed: TypedKey<String>) : Key by typed // warn
            class Conditional(flag: Boolean, typed: TypedKey<String>, plain: Key) :
                Key by (if (flag) typed else plain) // warn

            class Explicit(typed: TypedKey<String>) : Key by typed.key()
            class Typed(typed: TypedKey<String>) : TypedKey<String> by typed
            class Plain(key: Key) : Key by key

            fun anonymous(typed: TypedKey<String>) = object : Key by typed {} // warn
        """
    )
    
    @Test
    fun `checks subject based when branch expectations`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun test(key: Key, nullable: Key?, typed: TypedKey<String>, any: Any) {
                when (key) {
                    typed -> Unit // warn
                    else -> Unit
                }
                when (nullable) {
                    null -> Unit
                    typed -> Unit // warn
                    else -> Unit
                }
                when (typed) {
                    key -> Unit
                    else -> Unit
                }
                when (any) {
                    typed -> Unit
                    else -> Unit
                }
                when {
                    typed == key -> Unit
                    else -> Unit
                }
            }
        """
    )
    
    @Test
    fun `honors suppression inside conditional results`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            fun test(flag: Boolean, typed: TypedKey<String>, plain: Key) {
                val assigned: Key = if (flag) {
                    @Suppress("NOVA_TYPED_KEY_AS_KEY")
                    typed
                } else {
                    typed
                    val explicit: Key = typed // warn
                    plain
                }
                val returned: () -> Key = {
                    if (flag) {
                        @Suppress("NOVA_TYPED_KEY_AS_KEY")
                        typed
                    } else plain
                }
            }
        """
    )
    
    @Test
    fun `reports wrapped conversions once`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            class Holder(val typed: TypedKey<String>)
            fun accept(key: Key?) = Unit
            fun test(flag: Boolean, typed: TypedKey<String>?, holder: Holder?) {
                accept(holder?.typed) // warn
                if (typed != null) {
                    accept(typed) // warn
                }
                accept(if (flag) holder?.typed else typed) // warn
                val key: Key? = run { holder?.typed } // warn
            }
        """
    )
    
    @Test
    fun `supports suppression including covariant overrides`() = assertDiagnostics(
        """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key

            interface BaseHolder {
                val key: Key
            }
            interface Holder : BaseHolder {
                @Suppress("NOVA_TYPED_KEY_AS_KEY")
                override val key: TypedKey<String>
            }

            @Suppress("NOVA_TYPED_KEY_AS_KEY")
            fun suppressed(key: TypedKey<String>): Key = key

            fun test(holder: Holder) {
                holder.key.key().asString()
            }
        """
    )
}
