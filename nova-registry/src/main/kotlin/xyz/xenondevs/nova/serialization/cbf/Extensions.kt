package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import io.papermc.paper.registry.set.RegistryKeySet
import io.papermc.paper.registry.tag.TagKey
import org.bukkit.Keyed
import xyz.xenondevs.cbf.Cbf
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import xyz.xenondevs.nova.registry.RegistryEntrySet

/**
 * Registers [NovaRegistryElementBinarySerializer], [NovaRegistryEntryBinarySerializer] and [NovaRegistryEntrySetBinarySerializer] for the given [registry]
 * to [Cbf].
 */
inline fun <reified T : NovaRegistryElement<T>> Cbf.registerRegistrySerializers(registry: NovaRegistry<T>) {
    registerSerializer<T>(NovaRegistryElementBinarySerializer(registry))
    registerSerializer<RegistryEntry.Nova<T>>(NovaRegistryEntryBinarySerializer(registry))
    registerSerializer<RegistryEntrySet.Nova<T>>(NovaRegistryEntrySetBinarySerializer(registry))
}

/**
 * Registers [PaperRegistryElementBinarySerializer], [PaperRegistryEntryBinarySerializer], [PaperRegistryEntrySetBinarySerializer],
 * [TypedKeyBinarySerializer], [TagKeyBinarySerializer] and [RegistryKeySetBinarySerializer] for the given [registry] to [Cbf].
 */
inline fun <reified T : Keyed> Cbf.registerRegistrySerializers(registry: RegistryKey<T>) {
    registerSerializer<T>(PaperRegistryElementBinarySerializer(registry))
    registerSerializer<RegistryEntry.Paper<T>>(PaperRegistryEntryBinarySerializer(registry))
    registerSerializer<RegistryEntrySet.Paper<T>>(PaperRegistryEntrySetBinarySerializer(registry))
    registerSerializer<TypedKey<T>>(TypedKeyBinarySerializer(registry))
    registerSerializer<TagKey<T>>(TagKeyBinarySerializer(registry))
    registerSerializer<RegistryKeySet<T>>(RegistryKeySetBinarySerializer(registry))
}