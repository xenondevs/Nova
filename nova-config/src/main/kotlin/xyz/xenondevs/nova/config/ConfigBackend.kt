package xyz.xenondevs.nova.config

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import net.kyori.adventure.key.Key

/**
 * Backend for [ConfigStorage] that handles loading config data, change detection, and error reporting.
 */
interface ConfigBackend {
    
    /**
     * Loads the config for [id] and returns the parsed [kotlinx.serialization.json.JsonElement],
     * or an empty [kotlinx.serialization.json.JsonObject] if no config exists for the given [id],
     * or `null` if the config could not be loaded for any other reason.
     */
    fun load(id: Key): JsonElement?
    
    /**
     * Returns the last modified timestamp (epoch millis) for the config with [id].
     * Used to determine whether a config needs to be reloaded.
     */
    fun getLastModified(id: Key): Long
    
    /**
     * Called when a deserialization failure on [path] in the config with [id] occurs.
     */
    fun onError(id: Key, path: List<String>, exception: SerializationException)
    
    /**
     * Called after configs have been reloaded.
     */
    fun postReload()
    
}