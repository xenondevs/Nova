package xyz.xenondevs.nova.tileentity.network

import org.bukkit.block.BlockFace
import xyz.xenondevs.nova.data.config.Reloadable

/**
 * Holds the [NetworkType]-specific data for [NetworkEndPoints][NetworkEndPoint].
 */
interface EndPointDataHolder : Reloadable {
    
    /**
     * The [NetworkEndPoint] of this [EndPointDataHolder].
     */
    val endPoint: NetworkEndPoint
    
    /**
     * A map of [block faces][BlockFace] and the configured [NetworkConnectionType].
     */
    val connectionConfig: MutableMap<BlockFace, NetworkConnectionType>
    
    /**
     * A set of [block faces][BlockFace] where connections are allowed.
     */
    val allowedFaces: Set<BlockFace>
        get() = connectionConfig.mapNotNullTo(HashSet()) { if (it.value == NetworkConnectionType.NONE) null else it.key }
    
    /**
     * Saves the data.
     */
    fun saveData()
    
}