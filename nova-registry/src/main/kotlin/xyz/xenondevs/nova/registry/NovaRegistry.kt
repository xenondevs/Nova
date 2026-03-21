package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key
import xyz.xenondevs.commons.provider.Provider

/**
 * A thread-safe storage for [values][NovaRegistryElement] identified by [keys][Key].
 * 
 * During initialization (bootstrap), elements of registries cannot be accessed.
 * Instead, use [registry entries][RegistryEntry] and [entry sets][RegistryEntrySet],
 * which will be bound to the corresponding values upon registry freeze.
 * 
 * Elements of registries can be grouped together using [tags][RegistryEntrySet.Nova.Tag],
 * which are also identifiable by a [Key].
 */
interface NovaRegistry<out T : NovaRegistryElement<T>> {
    
    /**
     * The key of this registry.
     */
    val key: Key
    
    /**
     * A set containing all entries in this registry.
     * 
     * The entry set is a special tag in the registry that uses [key] as tag key.
     */
    val entrySet: RegistryEntrySet.Nova.Tag<T>
        get() = getTag(key)
    
    /**
     * Gets an entry for the given [key].
     * This function may be used before registries are frozen (i.e. during bootstrap)
     * and before the actual value is available.
     * Attempting to resolve the entry's value before registry freeze will throw an exception.
     * If no entry was registered under [key], registry freezing will fail.
     * 
     * @throws IllegalArgumentException If this method is called after registries are frozen and the entry does not exist.
     */
    operator fun get(key: Key): RegistryEntry.Nova<T>
    
    /**
     * Gets a provider that contains an entry as resolved from [get] 
     * or `null` if no entry exists for the given [key].
     * This function may be used before registries are frozen (i.e. during bootstrap)
     * and before the actual value is available.
     */
    fun getOptional(key: Key): Provider<RegistryEntry.Nova<T>?>
    
    /**
     * Gets an entry set containing all entries of the tag with the given [key].
     * This function may be used before registries are frozen (i.e. during bootstrap)
     * and before the actual tag is available.
     * Attempting to resolve the entry set's contents before registry freeze will throw an exception.
     * If no tag was registered under [key], registry freezing will fail.
     * 
     * @throws IllegalArgumentException If this method is called after registries are frozen and the tag does not exist.
     */
    fun getTag(key: Key): RegistryEntrySet.Nova.Tag<T>
    
    /**
     * Gets a provider that contains an entry set like the one resolved from [getTag]
     * or `null` if no tag exists for the given [key].
     * This function may be used before registries are frozen (i.e. during bootstrap)
     * and before the actual tag is available.
     */
    fun getOptionalTag(key: Key): Provider<RegistryEntrySet.Nova.Tag<T>?>
    
    /**
     * Gets the value registered under [key], or `null` if no value exists.
     * 
     * @throws IllegalStateException If this method is called before registries are frozen (i.e. during bootstrap).
     */
    fun getValue(key: Key): T?
    
    /**
     * Gets the value registered under [key], or throws an exception if no value exists.
     * 
     * @throws NoSuchElementException If no value is registered under [key].
     * @throws IllegalStateException If this method is called before registries are frozen (i.e. during bootstrap).
     */
    fun getValueOrThrow(key: Key): T = getValue(key)
        ?: throw NoSuchElementException("No element found for key $key")
    
    /**
     * Gets all entries with the given [name], ignoring their namespace.
     * 
     * @throws IllegalStateException If this method is called before registries are frozen (i.e. during bootstrap).
     */
    fun getValuesByName(name: String): List<T>
    
    /**
     * Checks whether a value is registered under the given [key].
     * 
     * @throws IllegalStateException If this method is called before registries are frozen (i.e. during bootstrap).
     */
    operator fun contains(key: Key): Boolean
    
    /**
     * Checks whether the given [value] is registered in this registry.
     * 
     * @throws IllegalStateException If this method is called before registries are frozen (i.e. during bootstrap).
     */
    operator fun contains(value: @UnsafeVariance T): Boolean
    
}