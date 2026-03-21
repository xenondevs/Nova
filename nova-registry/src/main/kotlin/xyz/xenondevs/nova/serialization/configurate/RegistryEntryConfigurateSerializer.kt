package xyz.xenondevs.nova.serialization.configurate

import io.leangen.geantyref.TypeToken
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import io.papermc.paper.registry.TypedKey
import net.kyori.adventure.key.Key
import org.bukkit.Keyed
import org.spongepowered.configurate.serialize.ScalarSerializer
import xyz.xenondevs.nova.registry.NovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistryElement
import xyz.xenondevs.nova.registry.RegistryEntry
import java.lang.reflect.Type
import java.util.function.Predicate
import kotlin.reflect.javaType
import kotlin.reflect.typeOf

/**
 * Creates a [NovaRegistryEntryConfigurateSerializer] for the given [registry].
 */
inline fun <reified T : NovaRegistryElement<T>> NovaRegistryEntryConfigurateSerializer(
    registry: NovaRegistry<T>
): NovaRegistryEntryConfigurateSerializer<T> =
    NovaRegistryEntryConfigurateSerializer(registry, geantyrefTypeTokenOf())

/**
 * A configurate serializer for [RegistryEntry.Nova] that serializes the entry by its key.
 */
class NovaRegistryEntryConfigurateSerializer<T : NovaRegistryElement<T>>(
    private val registry: NovaRegistry<T>,
    type: TypeToken<RegistryEntry.Nova<T>>
) : ScalarSerializer<RegistryEntry.Nova<T>>(type) {
    
    override fun deserialize(type: Type, obj: Any): RegistryEntry.Nova<T> {
        val key = Key.key(obj as String)
        return registry[key]
    }
    
    override fun serialize(item: RegistryEntry.Nova<T>, typeSupported: Predicate<Class<*>>): Any {
        return item.key.toString()
    }
    
}

/**
 * Creates a [PaperRegistryEntryConfigurateSerializer] for the given [registryKey].
 */
inline fun <reified T : Keyed> PaperRegistryEntryConfigurateSerializer(
    registryKey: RegistryKey<T>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): PaperRegistryEntryConfigurateSerializer<T> =
    PaperRegistryEntryConfigurateSerializer(registryKey, geantyrefTypeTokenOf(), registryAccess)

/**
 * A configurate serializer for [RegistryEntry.Paper] that serializes the entry by its key.
 */
class PaperRegistryEntryConfigurateSerializer<T : Keyed>(
    private val registryKey: RegistryKey<T>,
    type: TypeToken<RegistryEntry.Paper<T>>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : ScalarSerializer<RegistryEntry.Paper<T>>(type) {
    
    override fun deserialize(type: Type, obj: Any): RegistryEntry.Paper<T> {
        val key = Key.key(obj as String)
        return RegistryEntry.paper(TypedKey.create(registryKey, key), registryAccess)
    }
    
    override fun serialize(item: RegistryEntry.Paper<T>, typeSupported: Predicate<Class<*>>): Any {
        return item.key.asString()
    }
    
}

@OptIn(ExperimentalStdlibApi::class)
@PublishedApi
@Suppress("UNCHECKED_CAST")
internal inline fun <reified T> geantyrefTypeTokenOf() =
    TypeToken.get(typeOf<T>().javaType) as TypeToken<T>

/**
 * Creates an [EitherRegistryEntryConfigurateSerializer] for the given [novaRegistry] and [registryKey].
 */
inline fun <reified N : NovaRegistryElement<N>, reified P : Keyed> EitherRegistryEntryConfigurateSerializer(
    novaRegistry: NovaRegistry<N>,
    registryKey: RegistryKey<P>,
    registryAccess: RegistryAccess = RegistryAccess.registryAccess()
): EitherRegistryEntryConfigurateSerializer<N, P> =
    EitherRegistryEntryConfigurateSerializer(novaRegistry, registryKey, geantyrefTypeTokenOf(), registryAccess)

/**
 * A configurate serializer for [RegistryEntry.Either] that serializes the entry by its key.
 *
 * In case an entry exists in both the Nova and Paper registry, the Nova registry takes precedence.
 */
class EitherRegistryEntryConfigurateSerializer<N : NovaRegistryElement<N>, P : Keyed>(
    private val novaRegistry: NovaRegistry<N>,
    private val registryKey: RegistryKey<P>,
    type: TypeToken<RegistryEntry.Either<N, P>>,
    private val registryAccess: RegistryAccess = RegistryAccess.registryAccess()
) : ScalarSerializer<RegistryEntry.Either<N, P>>(type) {
    
    override fun deserialize(type: Type, obj: Any): RegistryEntry.Either<N, P> {
        val key = Key.key(obj as String)
        return RegistryEntry.either(key, novaRegistry, registryKey, registryAccess)
    }
    
    override fun serialize(item: RegistryEntry.Either<N, P>, typeSupported: Predicate<Class<*>>): Any {
        return item.key.asString()
    }
    
}

