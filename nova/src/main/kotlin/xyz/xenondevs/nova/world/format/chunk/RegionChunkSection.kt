package xyz.xenondevs.nova.world.format.chunk

import xyz.xenondevs.cbf.io.ByteReader
import xyz.xenondevs.cbf.io.ByteWriter
import xyz.xenondevs.nova.world.format.IdResolver
import xyz.xenondevs.nova.world.format.chunk.container.ArraySectionDataContainer
import xyz.xenondevs.nova.world.format.chunk.container.MapSectionDataContainer
import xyz.xenondevs.nova.world.format.chunk.container.SectionDataContainer
import xyz.xenondevs.nova.world.format.chunk.container.SingleValueSectionDataContainer

/**
 * A 16x16x16 section of a [RegionChunk].
 *
 * Each section has its own [SectionDataContainer] which stores the block states.
 * The type of that container is chosen based on the amount of non-empty blocks in the section.
 */
internal class RegionChunkSection<T>(
    private val idResolver: IdResolver<T>,
    container: SectionDataContainer<T>
) {
    
    var container: SectionDataContainer<T> = container
        private set
    
    constructor(idResolver: IdResolver<T>) : this(idResolver, MapSectionDataContainer(idResolver))
    
    /**
     * Returns true if this section is empty.
     */
    fun isEmpty(): Boolean =
        container.nonEmptyBlockCount == 0
    
    /**
     * Counts all non-empty blocks matching [predicate].
     */
    inline fun countNonEmpty(crossinline predicate: (T) -> Boolean): Int {
        var i = 0
        container.forEachNonEmpty { _, _, _, value -> if (predicate(value)) i++ }
        return i
    }
    
    /**
     * Retrieves the value at the given [x], [y] and [z] section coordinates.
     */
    operator fun get(x: Int, y: Int, z: Int): T? =
        container[x, y, z]
    
    /**
     * Sets the value at the given [x], [y] and [z] section coordinates and returns the previous value.
     */
    operator fun set(x: Int, y: Int, z: Int, state: T?): T? {
        val current = get(x, y, z)
        if (current == state)
            return current
        
        checkMigrateContainer(state)
        return container.set(x, y, z, state)
    }
    
    /**
     * Migrates the container to another type if necessary.
     */
    private fun checkMigrateContainer(state: T?) {
        val container = container
        when {
            // convert from single value container if a state has changed
            container is SingleValueSectionDataContainer && state != container.value -> {
                this.container = if (container.value != null)
                    ArraySectionDataContainer(idResolver).apply { fill(container.value) }
                else MapSectionDataContainer(idResolver)
            }
            
            // convert from map to array container if block count is higher than the threshold
            container is MapSectionDataContainer && container.nonEmptyBlockCount > MAP_TO_ARRAY_CONTAINER_THRESHOLD -> {
                val arrayContainer = ArraySectionDataContainer(idResolver)
                container.forEachNonEmpty { x, y, z, value -> arrayContainer[x, y, z] = value }
                this.container = arrayContainer
            }
        }
    }
    
    /**
     * Writes this section to the given [writer].
     * Returns true if the section was written, false if it was empty.
     */
    fun write(writer: ByteWriter): Boolean {
        optimizeContainer()
        
        if (container.nonEmptyBlockCount == 0)
            return false
        
        SectionDataContainer.write(container, writer)
        return true
    }
    
    /**
     * Converts the container to a more efficient type if possible.
     */
    private fun optimizeContainer() {
        val container = container
        
        // single value container cannot be optimized further
        if (container is SingleValueSectionDataContainer)
            return
        
        when {
            // convert to empty single value container if there are no blocks
            container.nonEmptyBlockCount == 0 -> this.container = SingleValueSectionDataContainer(idResolver, null)
            
            // convert from array to map container if block count is lower than the threshold
            container is ArraySectionDataContainer && container.nonEmptyBlockCount < MAP_TO_ARRAY_CONTAINER_THRESHOLD -> {
                val mapContainer = MapSectionDataContainer(idResolver)
                container.forEachNonEmpty { x, y, z, value -> mapContainer[x, y, z] = value }
                this.container = mapContainer
            }
            
            // convert to single value container if all states are the same
            container.isMonotone() -> this.container = SingleValueSectionDataContainer(idResolver, container[0, 0, 0])
        }
    }
    
    companion object {
        
        private const val MAP_TO_ARRAY_CONTAINER_THRESHOLD = 16
        
        fun <T> read(idResolver: IdResolver<T>, reader: ByteReader): RegionChunkSection<T> {
            val dataContainer = SectionDataContainer.read(idResolver, reader)
            return RegionChunkSection(idResolver, dataContainer)
        }
        
    }
    
}