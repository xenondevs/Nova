package xyz.xenondevs.nova.compiler

import org.junit.jupiter.api.Test

internal class KeyToStringTest : CompilerPluginTest(
    "NOVA_KEY_TO_STRING",
    KEY_SOURCE,
    "NamespacedKey.kt" to """
        package org.bukkit
        abstract class NamespacedKey : net.kyori.adventure.key.Key
    """
) {
    
    @Test
    fun `reports direct nullable inferred and subtype calls`() = assertDiagnostics(
        """
            import net.kyori.adventure.key.CustomKey
            import net.kyori.adventure.key.Key

            fun direct(key: Key) = key.toString() // warn
            fun safe(key: Key?) = key?.toString() // warn
            fun nullable(key: Key?) = key.toString() // warn
            fun inferred() = Key.key("example:key").toString() // warn
            fun subtype(key: CustomKey) = key.toString() // warn

            class AttachmentType(val key: Key) {
                override fun toString(): String = this@AttachmentType.key.toString() // warn
            }
        """
    )
    
    @Test
    fun `reports string interpolation`() = assertDiagnostics(
        $$"""
            import net.kyori.adventure.key.Key

            fun test(key: Key, nullable: Key?) {
                println("$key") // warn
                println("${key}") // warn
                println("$nullable") // warn
            }
        """
    )
    
    @Test
    fun `allows asString unrelated values and NamespacedKey`() = assertDiagnostics(
        $$"""
            import net.kyori.adventure.key.Key
            import org.bukkit.NamespacedKey

            fun test(key: Key, named: NamespacedKey, nullable: NamespacedKey?, text: String) {
                key.asString()
                text.toString()
                named.toString()
                nullable.toString()
                nullable?.toString()
                println("$named $nullable $text")
            }
        """
    )
    
    @Test
    fun `supports local suppression`() = assertDiagnostics(
        """
            import net.kyori.adventure.key.Key

            @Suppress("NOVA_KEY_TO_STRING")
            fun suppressed(key: Key) = key.toString()

            fun reported(key: Key) = key.toString() // warn
        """
    )
    
    @Test
    fun `supports file suppression`() = assertDiagnostics(
        $$"""
            @file:Suppress("NOVA_KEY_TO_STRING")
            import net.kyori.adventure.key.Key
            fun test(key: Key) = "$key ${key.toString()}"
        """
    )
}
