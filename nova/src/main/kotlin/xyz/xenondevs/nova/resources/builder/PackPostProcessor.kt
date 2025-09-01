package xyz.xenondevs.nova.resources.builder

/**
 * A post-processing action that is applied to the zipped resource pack.
 */
interface PackPostProcessor {
    
    /**
     * Processes the in-memory zipped resource pack [pack] and returns the modified version.
     */
    fun process(pack: ByteArray): ByteArray
    
}