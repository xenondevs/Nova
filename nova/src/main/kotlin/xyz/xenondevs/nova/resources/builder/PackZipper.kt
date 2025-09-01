package xyz.xenondevs.nova.resources.builder

/**
 * Responsible for creating a zip file from a resource pack directory.
 */
interface PackZipper {
    
    /**
     * Creates a zip of the resource pack and returns it as a [ByteArray].
     */
    fun createZip(): ByteArray
    
}