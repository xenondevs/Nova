package xyz.xenondevs.nova.world.format.chunk.container

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver

/**
 * A container for values of type [T] in a 16x16x16 space.
 */
internal sealed class SectionDataContainer<T>(protected val idResolver: IdResolver<T>) {
    
    /**
     * The amount of non-empty blocks in the section.
     */
    abstract val nonEmptyBlockCount: Int
    
    /**
     * Writes the section data to the [writer].
     */
    abstract fun write(writer: ByteWriter)
    
    /**
     * Gets the value at the specified [x], [y] and [z] section coordinates.
     */
    abstract operator fun get(x: Int, y: Int, z: Int): T?
    
    /**
     * Sets the value at the specified [x], [y] and [z] section coordinates to the specified [value]
     * and returns the previous [value].
     */
    abstract operator fun set(x: Int, y: Int, z: Int, value: T?): T?
    
    /**
     * Fills the entire section with the specified [value].
     */
    abstract fun fill(value: T?)
    
    /**
     * Iterates over all non-empty values in the section and calls the specified [action]
     * for each of them.
     */
    abstract fun forEachNonEmpty(action: (x: Int, y: Int, z: Int, value: T) -> Unit)
    
    /**
     * Checks whether all values in the section are the same.
     */
    abstract fun isMonotone(): Boolean
    
    /**
     * Packs the specified [x], [y] and [z] coordinates into a 12 bit integer.
     *
     * @see unpackX
     * @see unpackY
     * @see unpackZ
     */
    protected fun pack(x: Int, y: Int, z: Int): Int =
        (y shl 8) or (x shl 4) or z
    
    /**
     * Unpacks the x coordinate from the specified [packedPos].
     *
     * @see pack
     */
    protected fun unpackX(packedPos: Int): Int =
        packedPos shr 4 and 0xF
    
    /**
     * Unpacks the y coordinate from the specified [packedPos].
     *
     * @see pack
     */
    protected fun unpackY(packedPos: Int): Int =
        packedPos shr 8
    
    /**
     * Unpacks the z coordinate from the specified [packedPos].
     *
     * @see pack
     */
    protected fun unpackZ(packedPos: Int): Int =
        packedPos and 0xF
    
    companion object {
        
        private const val SINGLE_VALUE_SECTION_DATA_CONTAINER_ID = 0.toByte()
        private const val MAP_SECTION_DATA_CONTAINER_ID = 1.toByte()
        private const val ARRAY_SECTION_DATA_CONTAINER_ID = 2.toByte()
        
        /**
         * The amount of values in a section.
         */
        const val SECTION_SIZE = 4096
        
        /**
         * Reads a [SectionDataContainer] from the specified [reader].
         */
        fun <T> read(idResolver: IdResolver<T>, reader: ByteReader): SectionDataContainer<T> {
            val container = when (val containerType = reader.readByte()) {
                SINGLE_VALUE_SECTION_DATA_CONTAINER_ID -> SingleValueSectionDataContainer(idResolver, reader)
                MAP_SECTION_DATA_CONTAINER_ID -> MapSectionDataContainer(idResolver, reader)
                ARRAY_SECTION_DATA_CONTAINER_ID -> ArraySectionDataContainer(idResolver, reader)
                else -> throw IllegalArgumentException("Unknown SectionDataContainer type: $containerType")
            }
            return container
        }
        
        /**
         * Writes a [SectionDataContainer] to the specified [writer], including the container id byte.
         */
        fun <T> write(container: SectionDataContainer<T>, writer: ByteWriter) {
            when (container) {
                is SingleValueSectionDataContainer -> writer.writeByte(SINGLE_VALUE_SECTION_DATA_CONTAINER_ID)
                is MapSectionDataContainer -> writer.writeByte(MAP_SECTION_DATA_CONTAINER_ID)
                is ArraySectionDataContainer -> writer.writeByte(ARRAY_SECTION_DATA_CONTAINER_ID)
            }
            
            container.write(writer)
        }
        
    }
    
}