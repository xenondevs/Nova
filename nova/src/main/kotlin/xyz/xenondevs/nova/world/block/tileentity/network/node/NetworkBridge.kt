package xyz.xenondevs.nova.world.block.tileentity.network.node

import net.minecraft.resources.ResourceLocation

/**
 * A type of [NetworkNode] that can connect multiple [NetworkEndPoints][NetworkEndPoint],
 * without having to place them next to each other.
 * [NetworkBridges][NetworkBridge] with the same [NetworkBridge.typeId] can also connect to each other.
 *
 * May be commonly known as "cable" or "pipe".
 *
 * Types that inherit from both [NetworkBridge] and [NetworkEndPoint] are not allowed.
 *
 * @see NetworkEndPoint
 */
interface NetworkBridge : NetworkNode {
    
    /**
     * An identifier that defines which [NetworkBridges][NetworkBridge] can connect to each other.
     */
    val typeId: ResourceLocation
    
}