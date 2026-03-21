package xyz.xenondevs.nova.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.UnstableProviderApi
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.commons.provider.requireNotNull
import xyz.xenondevs.commons.provider.strongRequireNotNull
import java.lang.ref.WeakReference
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Gets a [ConfigProvider] scoped to the sub-node at [path].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
fun Provider<ConfigProvider>.node(vararg path: String): Provider<ConfigProvider> =
    map { it.node(*path) }

/**
 * Gets a [ConfigProvider] scoped to the sub-node at [path].
 */
fun Provider<ConfigProvider>.strongNode(vararg path: String): Provider<ConfigProvider> =
    map { it.strongNode(*path) }

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigProvider>.entry(default: T, vararg path: String): Provider<T> =
    flatMap { it.entry(default, *path) }

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigProvider>.entry(default: Provider<T>, vararg path: String): Provider<T> =
    flatMap { it.entry(default, *path) }

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigProvider>.entry(default: T, vararg paths: List<String>): Provider<T> =
    flatMap { it.entry(default, *paths) }

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigProvider>.entry(default: Provider<T>, vararg paths: List<String>): Provider<T> =
    flatMap { it.entry(default, *paths) }

/**
 * Gets an optional entry [Provider] for a value of type [T] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigProvider>.optionalEntry(vararg path: String): Provider<T?> =
    flatMap { it.optionalEntry(*path) }

/**
 * Gets an optional entry [Provider] for a value of type [T] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> Provider<ConfigProvider>.optionalEntry(vararg paths: List<String>): Provider<T?> =
    flatMap { it.optionalEntry(*paths) }

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntry(default: T, vararg path: String): Provider<T> =
    flatMap { it.strongEntry(default, *path) }

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntry(default: Provider<T>, vararg path: String): Provider<T> =
    flatMap { it.strongEntry(default, *path) }

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntry(default: T, vararg paths: List<String>): Provider<T> =
    flatMap { it.strongEntry(default, *paths) }

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntry(default: Provider<T>, vararg paths: List<String>): Provider<T> =
    flatMap { it.strongEntry(default, *paths) }

/**
 * Gets an optional entry [Provider] for a value of type [T] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> Provider<ConfigProvider>.strongOptionalEntry(vararg path: String): Provider<T?> =
    flatMap { it.strongOptionalEntry(*path) }

/**
 * Gets an optional entry [Provider] for a value of type [T] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> Provider<ConfigProvider>.strongOptionalEntry(vararg paths: List<String>): Provider<T?> =
    flatMap { it.strongOptionalEntry(*paths) }

//<editor-fold desc="deprecated Provider<ConfigProvider> extensions">
@Suppress("DEPRECATION")
@Deprecated("Use entry with a default value instead", ReplaceWith("entry(default, *path)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> Provider<ConfigProvider>.entry(vararg path: String): Provider<T> =
    flatMap { it.entry<T>(*path) }

@Deprecated("Use entry with a default value instead", ReplaceWith("entry(default, *paths)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> Provider<ConfigProvider>.entry(vararg paths: List<String>): Provider<T> =
    flatMap { cfg -> cfg.optionalEntry<T>(*paths).requireNotNull { cfg.missingEntryMessage(*paths) } }

@Suppress("DEPRECATION")
@Deprecated("Use entry instead", ReplaceWith("entry(default, *path)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> Provider<ConfigProvider>.entryOrElse(default: T?, vararg path: String): Provider<T> =
    flatMap { it.entryOrElse(default, *path) }

@Suppress("DEPRECATION")
@Deprecated("Use entry instead", ReplaceWith("entry(default, *path)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> Provider<ConfigProvider>.entryOrElse(default: Provider<T>?, vararg path: String): Provider<T> =
    flatMap { it.entryOrElse(default, *path) }

@Deprecated("Use entry instead", ReplaceWith("entry(default, *paths)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> Provider<ConfigProvider>.entryOrElse(default: T?, vararg paths: List<String>): Provider<T> =
    flatMap { cfg -> if (default != null) cfg.entry(default, *paths) else cfg.optionalEntry<T>(*paths).requireNotNull { cfg.missingEntryMessage(*paths) } }

@Deprecated("Use entry instead", ReplaceWith("entry(default, *paths)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> Provider<ConfigProvider>.entryOrElse(default: Provider<T>?, vararg paths: List<String>): Provider<T> =
    flatMap { cfg -> if (default != null) cfg.entry(default, *paths) else cfg.optionalEntry<T>(*paths).requireNotNull { cfg.missingEntryMessage(*paths) } }

@Suppress("DEPRECATION")
@Deprecated("Use strongEntry with a default value instead", ReplaceWith("strongEntry(default, *path)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntry(vararg path: String): Provider<T> =
    flatMap { it.strongEntry<T>(*path) }

@Deprecated("Use strongEntry with a default value instead", ReplaceWith("strongEntry(default, *paths)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntry(vararg paths: List<String>): Provider<T> =
    flatMap { cfg -> cfg.strongOptionalEntry<T>(*paths).strongRequireNotNull { cfg.missingEntryMessage(*paths) } }

@Suppress("DEPRECATION")
@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *path)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntryOrElse(default: T?, vararg path: String): Provider<T> =
    flatMap { it.strongEntryOrElse(default, *path) }

@Suppress("DEPRECATION")
@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *path)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntryOrElse(default: Provider<T>?, vararg path: String): Provider<T> =
    flatMap { it.strongEntryOrElse(default, *path) }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *paths)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntryOrElse(default: T?, vararg paths: List<String>): Provider<T> =
    flatMap { cfg -> if (default != null) cfg.strongEntry(default, *paths) else cfg.strongOptionalEntry<T>(*paths).strongRequireNotNull { cfg.missingEntryMessage(*paths) } }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *paths)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> Provider<ConfigProvider>.strongEntryOrElse(default: Provider<T>?, vararg paths: List<String>): Provider<T> =
    flatMap { cfg -> if (default != null) cfg.strongEntry(default, *paths) else cfg.strongOptionalEntry<T>(*paths).strongRequireNotNull { cfg.missingEntryMessage(*paths) } }
//</editor-fold>

/**
 * Gets a [ConfigProvider] scoped to the sub-node at [path].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
fun ConfigProvider.node(vararg path: String): ConfigProvider =
    node(path.asList())

/**
 * Gets a [ConfigProvider] scoped to the sub-node at [path].
 */
fun ConfigProvider.strongNode(vararg path: String): ConfigProvider =
    strongNode(path.asList())

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> ConfigProvider.entry(default: T, vararg path: String): Provider<T> =
    entry(provider(default), *path)

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> ConfigProvider.entry(default: Provider<T>, vararg path: String): Provider<T> =
    entry(typeOf<T>(), default, path.asList())

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> ConfigProvider.entry(default: T, vararg paths: List<String>): Provider<T> =
    entry(provider(default), *paths)

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> ConfigProvider.entry(default: Provider<T>, vararg paths: List<String>): Provider<T> =
    entry(typeOf<T>(), default, *paths)

/**
 * Gets an optional entry [Provider] for a value of type [T] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> ConfigProvider.optionalEntry(vararg path: String): Provider<T?> =
    optionalEntry(typeOf<T>(), path.asList())

/**
 * Gets an optional entry [Provider] for a value of type [T] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized due to [SerializationException].
 *
 * The returned provider will only be stored in a [WeakReference] in the parent provider.
 */
inline fun <reified T : Any> ConfigProvider.optionalEntry(vararg paths: List<String>): Provider<T?> =
    optionalEntry(typeOf<T>(), *paths)

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> ConfigProvider.strongEntry(default: T, vararg path: String): Provider<T> =
    strongEntry(provider(default), *path)

/**
 * Gets an entry [Provider] for a value of type [T] under [path].
 * Falls back to [default] if the entry does not exist or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> ConfigProvider.strongEntry(default: Provider<T>, vararg path: String): Provider<T> =
    strongEntry(typeOf<T>(), default, path.asList())

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> ConfigProvider.strongEntry(default: T, vararg paths: List<String>): Provider<T> =
    strongEntry(provider(default), *paths)

/**
 * Gets an entry [Provider] for a value of type [T] under the first existing path from [paths].
 * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> ConfigProvider.strongEntry(default: Provider<T>, vararg paths: List<String>): Provider<T> =
    strongEntry(typeOf<T>(), default, *paths)

/**
 * Gets an optional entry [Provider] for a value of type [T] under [path], whose value
 * will be null if the entry does not exist or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> ConfigProvider.strongOptionalEntry(vararg path: String): Provider<T?> =
    strongOptionalEntry(typeOf<T>(), path.asList())

/**
 * Gets an optional entry [Provider] for a value of type [T] under the first existing path from [paths],
 * whose value will be null if no entry exists or could not be deserialized due to [SerializationException].
 */
inline fun <reified T : Any> ConfigProvider.strongOptionalEntry(vararg paths: List<String>): Provider<T?> =
    strongOptionalEntry(typeOf<T>(), *paths)

//<editor-fold desc="deprecated">
@Deprecated("Use entry with a default value instead", ReplaceWith("entry(default, *path)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> ConfigProvider.entry(vararg path: String): Provider<T> =
    optionalEntry<T>(*path).requireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use entry with a default value instead", ReplaceWith("entry(default, *paths)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> ConfigProvider.entry(vararg paths: Array<String>): Provider<T> =
    optionalEntry<T>(*paths.toLists()).requireNotNull { missingEntryMessage(*paths.toLists()) }

@Deprecated("Use entry instead", ReplaceWith("entry(default, *path)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> ConfigProvider.entryOrElse(default: T?, vararg path: String): Provider<T> =
    if (default != null) entry(default, *path) else optionalEntry<T>(*path).requireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use entry instead", ReplaceWith("entry(default, *path)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> ConfigProvider.entryOrElse(default: Provider<T>?, vararg path: String): Provider<T> =
    if (default != null) entry(default, *path) else optionalEntry<T>(*path).requireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use entry instead", ReplaceWith("entry(type, default, *path)", "xyz.xenondevs.nova.config.entry"))
fun <T : Any> ConfigProvider.entryOrElse(type: KType, default: Provider<T>?, vararg path: String): Provider<T> =
    if (default != null) entry(type, default, path.asList()) else optionalEntry<T>(type, path.asList()).requireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use entry instead", ReplaceWith("entry(default, *paths)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> ConfigProvider.entryOrElse(default: T?, vararg paths: Array<String>): Provider<T> =
    if (default != null) entry(default, *paths.toLists()) else optionalEntry<T>(*paths.toLists()).requireNotNull { missingEntryMessage(*paths.toLists()) }

@Deprecated("Use entry instead", ReplaceWith("entry(default, *paths)", "xyz.xenondevs.nova.config.entry"))
inline fun <reified T : Any> ConfigProvider.entryOrElse(default: Provider<T>?, vararg paths: Array<String>): Provider<T> =
    if (default != null) entry(default, *paths.toLists()) else optionalEntry<T>(*paths.toLists()).requireNotNull { missingEntryMessage(*paths.toLists()) }

@Deprecated("Use entry instead", ReplaceWith("entry(type, default, *paths)", "xyz.xenondevs.nova.config.entry"))
fun <T : Any> ConfigProvider.entryOrElse(type: KType, default: Provider<T>?, vararg paths: Array<String>): Provider<T> =
    if (default != null) entry(type, default, *paths.toLists()) else optionalEntry<T>(type, *paths.toLists()).requireNotNull { missingEntryMessage(*paths.toLists()) }

@Deprecated("Use strongEntry with a default value instead", ReplaceWith("strongEntry(default, *path)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> ConfigProvider.strongEntry(vararg path: String): Provider<T> =
    strongOptionalEntry<T>(*path).strongRequireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use strongEntry with a default value instead", ReplaceWith("strongEntry(default, *paths)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> ConfigProvider.strongEntry(vararg paths: Array<String>): Provider<T> =
    strongOptionalEntry<T>(*paths.toLists()).strongRequireNotNull { missingEntryMessage(*paths.toLists()) }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *path)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> ConfigProvider.strongEntryOrElse(default: T?, vararg path: String): Provider<T> =
    if (default != null) strongEntry(default, *path) else strongOptionalEntry<T>(*path).strongRequireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *path)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> ConfigProvider.strongEntryOrElse(default: Provider<T>?, vararg path: String): Provider<T> =
    if (default != null) strongEntry(default, *path) else strongOptionalEntry<T>(*path).strongRequireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(type, default, *path)", "xyz.xenondevs.nova.config.strongEntry"))
fun <T : Any> ConfigProvider.strongEntryOrElse(type: KType, default: Provider<T>?, vararg path: String): Provider<T> =
    if (default != null) strongEntry(type, default, path.asList()) else strongOptionalEntry<T>(type, path.asList()).strongRequireNotNull { missingEntryMessage(path.asList()) }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *paths)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> ConfigProvider.strongEntryOrElse(default: T?, vararg paths: Array<String>): Provider<T> =
    if (default != null) strongEntry(default, *paths.toLists()) else strongOptionalEntry<T>(*paths.toLists()).strongRequireNotNull { missingEntryMessage(*paths.toLists()) }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(default, *paths)", "xyz.xenondevs.nova.config.strongEntry"))
inline fun <reified T : Any> ConfigProvider.strongEntryOrElse(default: Provider<T>?, vararg paths: Array<String>): Provider<T> =
    if (default != null) strongEntry(default, *paths.toLists()) else strongOptionalEntry<T>(*paths.toLists()).strongRequireNotNull { missingEntryMessage(*paths.toLists()) }

@Deprecated("Use strongEntry instead", ReplaceWith("strongEntry(type, default, *paths)", "xyz.xenondevs.nova.config.strongEntry"))
fun <T : Any> ConfigProvider.strongEntryOrElse(type: KType, default: Provider<T>?, vararg paths: Array<String>): Provider<T> =
    if (default != null) strongEntry(type, default, *paths.toLists()) else strongOptionalEntry<T>(type, *paths.toLists()).strongRequireNotNull { missingEntryMessage(*paths.toLists()) }

@PublishedApi
internal fun ConfigProvider.missingEntryMessage(vararg paths: List<String>): String =
    "Missing config entry ${paths.joinToString(" or ") { "'${it.joinToString(" > ")}'" }} in '$configId'"

@PublishedApi
internal fun Array<out Array<String>>.toLists(): Array<List<String>> =
    map { it.asList() }.toTypedArray()
//</editor-fold>

@SubclassOptInRequired(UnstableProviderApi::class)
interface ConfigProvider : Provider<JsonElement> {
    
    /**
     * The id of this config.
     */
    val configId: Key
    
    /**
     * Gets a [ConfigProvider] scoped to the sub-node at [path].
     *
     * The returned provider will only be stored in a [WeakReference] in the parent provider.
     */
    fun node(path: List<String>): ConfigProvider
    
    /**
     * Gets an entry [Provider] for a value of [type] under the first existing path from [paths].
     * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
     *
     * The returned provider will only be stored in a [WeakReference] in the parent provider.
     */
    fun <T : Any> entry(type: KType, default: Provider<T>, vararg paths: List<String>): Provider<T>
    
    /**
     * Gets an optional entry [Provider] for a value of [type] under the first existing path from [paths],
     * whose value will be null if no entry exists or could not be deserialized due to [SerializationException].
     *
     * The returned provider will only be stored in a [WeakReference] in the parent provider.
     */
    fun <T : Any> optionalEntry(type: KType, vararg paths: List<String>): Provider<T?>
    
    /**
     * Gets a [ConfigProvider] scoped to the sub-node at [path].
     */
    fun strongNode(path: List<String>): ConfigProvider
    
    /**
     * Gets an entry [Provider] for a value of [type] under the first existing path from [paths].
     * Falls back to [default] if no entry exists or could not be deserialized due to [SerializationException].
     */
    fun <T : Any> strongEntry(type: KType, default: Provider<T>, vararg paths: List<String>): Provider<T>
    
    /**
     * Gets an optional entry [Provider] for a value of [type] under the first existing path from [paths],
     * whose value will be null if no entry exists or could not be deserialized due to [SerializationException].
     */
    fun <T : Any> strongOptionalEntry(type: KType, vararg paths: List<String>): Provider<T?>
    
}