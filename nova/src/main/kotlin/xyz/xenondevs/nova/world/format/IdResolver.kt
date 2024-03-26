package xyz.xenondevs.nova.world.format

interface IdResolver<T> {
    
    /**
     * The amount of different ids that can be resolved.
     */
    val size: Int
    
    /**
     * Gets the id of the given value.
     */
    fun toId(value: T?): Int
    
    /**
     * Gets the value of the given id.
     */
    fun fromId(id: Int): T?
    
}