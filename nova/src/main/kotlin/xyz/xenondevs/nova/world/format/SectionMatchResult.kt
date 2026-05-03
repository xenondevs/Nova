@file:Suppress("NOTHING_TO_INLINE")

package xyz.xenondevs.nova.world.format

import xyz.xenondevs.nova.world.format.SectionMatchResult.Companion.NONE

// packs to bit index
private fun pack(x: Int, y: Int, z: Int): Int =
    (y shl 8) or (x shl 4) or z

/**
 * A result of a section match action where a chunk section was matched against a set of block states.
 * 
 * @see WorldDataManager.matchSection
 */
@JvmInline
value class SectionMatchResult(
    /**
     * The internal data of the [SectionMatchResult].
     * Never mutate this, as it could be the [NONE] set.
     */
    val data: LongArray = LongArray(64)
) {
    
    /**
     * Gets the value at the given [x], [y], [z] coordinates.
     * [x], [y], and [z] must be in the range of 0..15.
     */
    operator fun get(x: Int, y: Int, z: Int): Boolean {
        return get(pack(x, y, z))
    }
    
    /**
     * Gets the value at the given bit index,
     * where y are bits 8-15, x are bits 4-7, and z are bits 0-3.
     * The index must be in the range of 0..4095.
     */
    operator fun get(i: Int): Boolean {
        return data[i shr 6] and (1L shl (i and 0x3F)) != 0L
    }
    
    companion object {
        
        /**
         * An empty [SectionMatchResult]. Do not mutate.
         */
        @JvmStatic
        val NONE = SectionMatchResult()
        
        /**
         * A [SectionMatchResult] that matched on every block.
         */
        @JvmStatic
        val ALL = SectionMatchResult(LongArray(64).apply { fill(-1L) })
        
        /**
         * Builds a [SectionMatchResult] from the given [check] function,
         * where `packed` are the packed coordinates with y being bits 8-15, x being bits 4-7, and z being bits 0-3.
         */
        inline fun build(check: (packed: Int) -> Boolean): SectionMatchResult {
            val data = LongArray(64)
            for (i in 0..<4096) {
                set(data, i, check(i))
            }
            return SectionMatchResult(data)
        }
        
        /**
         * Sets the bit at the given [bit index][i] in the [array] to `1` if [value] is true or does nothing if it is false.
         */
        inline fun set(array: LongArray, i: Int, value: Boolean) {
            val v = if (value) 1L else 0L
            array[i shr 6] = array[i shr 6] or (v shl (i and 0x3F))
        }
        
    }
    
}