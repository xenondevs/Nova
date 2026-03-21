package xyz.xenondevs.nova.registry

import net.kyori.adventure.key.Key

/**
 * Creates a new, possibly [reloadable], [MutableNovaRegistry] identified by [key].
 */
fun <T : NovaRegistryElement<T>> MutableNovaRegistry(key: Key, reloadable: Boolean): MutableNovaRegistry<T> =
    if (reloadable) ReloadableNovaRegistry(key) else StableNovaRegistry(key)

/**
 * A mutable [NovaRegistry] that allows registering entries and tags via [set].
 * After initialization, the registry can be [frozen][freeze] to prevent further modifications and bind values to entries.
 * 
 * Some registries may be [reloadable][isReloadable], allowing [reload] to reconfigure the registered
 * contents of the registry. Corresponding entries and entry sets will be updated accordingly.
 */
interface MutableNovaRegistry<T : NovaRegistryElement<T>> : NovaRegistry<T> {
    
    /**
     * An unmodifiable view of this registry.
     */
    val unmodifiableView: NovaRegistry<T>
    
    /**
     * Whether the registry is reloadable, i.e. whether it can be reset and modified after being frozen.
     */
    val isReloadable: Boolean
    
    /**
     * Registers [value] under [key].
     * 
     * @throws IllegalStateException If the registry is frozen.
     * @throws IllegalArgumentException If [key] is already registered.
     * @throws IllegalArgumentException If [value] is already registered under a different key.
     */
    operator fun set(key: Key, value: T)
    
    /**
     * Registers a tag of [entries] under [tagKey].
     * 
     * @throws IllegalStateException If the registry is frozen.
     * @throws IllegalArgumentException If [tagKey] matches [key], as [key] is reserved for [entrySet].
     * @throws IllegalArgumentException If a tag with key [tagKey] is already registered.
     * @throws IllegalArgumentException If any of the entries in [entries] does not belong to this registry.
     */
    operator fun set(tagKey: Key, entries: Set<NovaTagEntry<T>>)
    
    /**
     * Freezes the registry.
     * This binds [RegistryEntries][RegistryEntry] to their values and prevents
     * any further modifications to the registry.
     * Requires prior registration of all elements and tags referenced in entries and entry sets.
     * 
     * @throws IllegalStateException If the registry is already frozen.
     * @throws IllegalStateException If this method is called before an entry or tag referenced in
     * an entry or entry set is registered.
     */
    fun freeze()
    
    /**
     * Atomically reloads the registry by:
     * 1. Resetting the registry to an empty state, i.e. clearing all entries and tags.
     * 2. Applying [configure] to the registry.
     * 3. [Freezing][freeze] the registry.
     * 
     * Note that reloading needs to re-register all entries and tags that existed previously,
     * as they already have [registry entries][RegistryEntry] and [registry entry sets][RegistryEntrySet]
     * pointing to them.
     * 
     * @throws UnsupportedOperationException If the registry is [not reloadable][isReloadable].
     * @throws IllegalStateException If the registry is not frozen.
     * @throws IllegalStateException If the registry is already reloading.
     * @throws IllegalStateException If this method is called before an entry or tag referenced in
     * an entry or entry set is registered.
     */
    fun reload(configure: MutableNovaRegistry<T>.() -> Unit)
    
}