package xyz.xenondevs.nova.world.block

/**
 * Defines how vanilla block states should be set.
 */
enum class BlockUpdateMethod {
    
    /**
     * The default way of setting a block, with block updates.
     */
    WITH_BLOCK_UPDATES,
    
    /**
     * Places the block without block updates.
     */
    WITHOUT_BLOCK_UPDATES,
    
    /**
     * Places the block without block updates and without notifying the client.
     */
    WITHOUT_BOCK_UPDATES_WITHOUT_PACKETS
    
}