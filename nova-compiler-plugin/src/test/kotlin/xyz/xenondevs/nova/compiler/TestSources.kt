package xyz.xenondevs.nova.compiler

internal val KEY_SOURCE = "Key.kt" to """
    package net.kyori.adventure.key

    interface Key {
        fun key(): Key = this
        fun namespace(): String
        fun value(): String
        fun asString(): String

        companion object {
            fun key(value: String): Key = TODO()
        }
    }

    abstract class CustomKey : Key
"""

internal val TYPED_KEY_SOURCE = "TypedKey.kt" to """
    package io.papermc.paper.registry

    import net.kyori.adventure.key.Key

    interface TypedKey<T> : Key {
        override fun key(): Key
        fun registryKey(): String
    }
"""
