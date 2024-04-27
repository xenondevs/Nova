package xyz.xenondevs.nova.tileentity.network.task

import org.bukkit.block.BlockFace

internal class ProtectionResult(private val result: BooleanArray) {
    
    /**
     * Gets the protection result for the given [face].
     */
    operator fun get(face: BlockFace): Boolean =
        result[face.ordinal]
    
    /**
     * Removes all faces from [collection] that are considered protected by this [ProtectionResult].
     */
    fun removeProtected(collection: MutableCollection<BlockFace>) {
        collection.removeIf { !result[it.ordinal] }
    }
    
    companion object {
        val ALL_ALLOWED = ProtectionResult(BooleanArray(6) { true })
    }
    
}