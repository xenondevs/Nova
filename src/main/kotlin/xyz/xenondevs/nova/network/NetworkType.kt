package xyz.xenondevs.nova.network

import xyz.xenondevs.nova.network.energy.EnergyNetwork

enum class NetworkType(val networkConstructor: () -> Network) {
    
    /**
     * Transfers Energy
     */
    ENERGY(::EnergyNetwork),
    
    // TODO: ItemNetwork
    
}