package xyz.xenondevs.nova.world.format.chunk.palette

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver

internal interface Palette<T> {
    
    /**
     * The number of entries in this palette.
     */
    val size: Int
    
    /**
     * Writes the palette to the given [writer].
     */
    fun write(writer: ByteWriter)
    
    /**
     * Retrieves the corresponding palletized id for the given [value], or 0 if the value is null or not present.
     */
    fun getId(value: T?): Int
    
    /**
     * Retrieves the corresponding value for the given [id], or null if the id is 0 or unused.
     */
    fun getValue(id: Int): T?
    
    /**
     * Adds the given [value] to the palette and returns its palletized id.
     */
    fun putValue(value: T): Int
    
    companion object {
        
        private const val IDENTITY_PALETTE_ID = 0.toByte()
        private const val LINEAR_PALETTE_ID = 1.toByte()
        private const val HASH_PALETTE_ID = 2.toByte()
        
        /**
         * Reads a [Palette] from the given [reader].
         */
        fun <T> read(idResolver: IdResolver<T>, reader: ByteReader): Palette<T> {
            val palette = when (val paletteType = reader.readByte()) {
                IDENTITY_PALETTE_ID -> IdentityPalette(idResolver)
                LINEAR_PALETTE_ID -> LinearPalette(idResolver, reader)
                HASH_PALETTE_ID -> HashPalette(idResolver, reader)
                else -> throw IllegalStateException("Unknown palette id: $paletteType")
            }
            return palette
        }
        
        /**
         * Writes a [Palette] to the given writer, including the palette id byte.
         */
        fun <T> write(palette: Palette<T>, writer: ByteWriter) {
            when (palette) {
                is IdentityPalette -> writer.writeByte(IDENTITY_PALETTE_ID)
                is LinearPalette -> writer.writeByte(LINEAR_PALETTE_ID)
                is HashPalette -> writer.writeByte(HASH_PALETTE_ID)
                else -> throw IllegalStateException("Unknown palette type: ${palette::class.simpleName}")
            }
            palette.write(writer)
        }
        
    }
    
}