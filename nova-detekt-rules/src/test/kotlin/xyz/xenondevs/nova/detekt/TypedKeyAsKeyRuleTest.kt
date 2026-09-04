package xyz.xenondevs.nova.detekt

import dev.detekt.api.Config
import dev.detekt.test.lintWithContext
import dev.detekt.test.utils.createEnvironment
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TypedKeyAsKeyRuleTest {
    
    private val env = createEnvironment()
    private val typeStubs = arrayOf(
        """
        package net.kyori.adventure.key
        
        interface Key {
            fun key(): Key = this
            fun namespace(): String
            fun value(): String
            fun asString(): String
        }
        """,
        """
        package io.papermc.paper.registry
        
        import net.kyori.adventure.key.Key
        
        interface TypedKey<T> : Key {
            override fun key(): Key
            fun registryKey(): String
        }
        """
    )
    
    @Test
    fun `reports implicit conversions to key`() {
        val code = """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key
            
            fun accept(key: Key) = Unit
            fun returnKey(key: TypedKey<String>): Key = key
            class Holder(val key: TypedKey<String>)
            fun returnQualifiedKey(holder: Holder): Key = holder.key
            
            fun test(key: TypedKey<String>) {
                val assigned: Key = key
                accept(key)
            }
        """
        
        val findings = TypedKeyAsKeyRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assertEquals(4, findings.size, findings.toString())
    }
    
    @Test
    fun `reports a qualified conversion once`() {
        val code = """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key
            
            class Holder(val key: TypedKey<String>)
            fun returnKey(holder: Holder): Key = holder.key
        """
        
        val findings = TypedKeyAsKeyRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assertEquals(1, findings.size, findings.toString())
    }
    
    @Test
    fun `reports inherited key members`() {
        val code = """
            import io.papermc.paper.registry.TypedKey
            
            class Holder(val key: TypedKey<String>)
            
            fun test(key: TypedKey<String>, holder: Holder) {
                key.namespace()
                key.value()
                key.asString()
                holder.key.asString()
            }
        """
        
        val findings = TypedKeyAsKeyRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assertEquals(4, findings.size, findings.toString())
    }
    
    @Test
    fun `reports covariant key overrides`() {
        val code = """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key
            
            interface KeyOwner {
                val key: Key
            }
            
            class TypedKeyOwner(
                override val key: TypedKey<String>
            ) : KeyOwner
            
            class TypedKeyOwnerBody(
                typedKey: TypedKey<String>
            ) : KeyOwner {
                override val key: TypedKey<String> = typedKey
            }
        """
        
        val findings = TypedKeyAsKeyRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assertEquals(2, findings.size, findings.toString())
    }
    
    @Test
    fun `reports casts to key`() {
        val code = """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key
            
            fun test(key: TypedKey<String>) {
                key as Key
                key as? Key
            }
        """
        
        val findings = TypedKeyAsKeyRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assertEquals(2, findings.size, findings.toString())
    }
    
    @Test
    fun `allows typed key api and explicit key access`() {
        val code = """
            import io.papermc.paper.registry.TypedKey
            import net.kyori.adventure.key.Key
            
            fun accept(key: Key) = Unit
            fun acceptTyped(key: TypedKey<String>) = Unit
            
            fun test(key: TypedKey<String>) {
                acceptTyped(key)
                accept(key.key())
                key.key().namespace()
                key.registryKey()
                val any: Any = key
            }
        """
        
        val findings = TypedKeyAsKeyRule(Config.empty).lintWithContext(env, code, *typeStubs)
        assertTrue(findings.isEmpty(), findings.toString())
    }
    
}
