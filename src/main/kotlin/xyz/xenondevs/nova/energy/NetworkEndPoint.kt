package xyz.xenondevs.nova.energy

import org.bukkit.block.BlockFace

fun NetworkEndPoint.removeNetwork(face: BlockFace) {
    if (this is EnergyProvider && provideNetworks.containsKey(face))
        provideNetworks[face] = null
    if (this is EnergyConsumer && consumeNetworks.containsKey(face))
        consumeNetworks[face] = null
}

/**
 * [EnergyProvider]s and [EnergyConsumer]s
 */
interface NetworkEndPoint : NetworkNode