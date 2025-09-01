package xyz.xenondevs.nova.resources.builder

/**
 * Responsible for creating a zip file from a resource pack directory.
 */
interface PackZipper {
    
    /**
     * Creates a zip file from the given [root] directory and returns it as a [ByteArray].
     */
    fun createZip(): ByteArray
    
}