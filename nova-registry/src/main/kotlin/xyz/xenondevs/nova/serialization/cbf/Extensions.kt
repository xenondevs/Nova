package xyz.xenondevs.nova.serialization.cbf

import io.papermc.paper.registry.RegistryAccess
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
import kotlin.enums.enumEntries

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
 * [TypedKeyBinarySerializer], [TagKeyBinarySerializer] and [RegistryKeySetBinarySerializer] for the given [registry] and [registryAccess] to [Cbf].
 */
inline fun <reified T : Keyed> Cbf.registerRegistrySerializers(
    registry: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) {
    registerSerializer<T>(PaperRegistryElementBinarySerializer(registry, registryAccess))
    registerRegistrySerializersCommon(registry, registryAccess)
}

/**
 * Registers [PaperRegistryElementBinarySerializer], [PaperRegistryEntryBinarySerializer], [PaperRegistryEntrySetBinarySerializer],
 * [TypedKeyBinarySerializer], [TagKeyBinarySerializer] and [RegistryKeySetBinarySerializer] for the given [registry] and [registryAccess] to [Cbf].
 */
@JvmName("registerRegistrySerializersEnum")
inline fun <reified T> Cbf.registerRegistrySerializers(
    registry: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) where T : Enum<T>, T : Keyed {
    val enumEntries = enumEntries<T>().associateBy { it.name }
    registerSerializer<T>(PaperRegistryElementBinarySerializer(registry, registryAccess, enumEntries))
    registerRegistrySerializersCommon(registry, registryAccess)
}

inline fun <reified T : Keyed> Cbf.registerRegistrySerializersCommon(registry: RegistryKey<T>, registryAccess: RegistryAccess) {
    registerSerializer<RegistryEntry.Paper<T>>(PaperRegistryEntryBinarySerializer(registry, registryAccess))
    registerSerializer<RegistryEntrySet.Paper<T>>(PaperRegistryEntrySetBinarySerializer(registry, registryAccess))
    registerSerializer<TypedKey<T>>(TypedKeyBinarySerializer(registry))
    registerSerializer<TagKey<T>>(TagKeyBinarySerializer(registry))
    registerSerializer<RegistryKeySet<T>>(RegistryKeySetBinarySerializer(registry, registryAccess))
}