@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.world.block.tileentity.network.type.item

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroup
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkGroupData
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.ItemNetwork.Companion.MAX_COMPLEXITY
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.channel.FilteredNetworkedInventory
import xyz.xenondevs.nova.world.block.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.RoundRobinCounter

internal class ItemNetworkGroup(data: NetworkGroupData<ItemNetwork>) : NetworkGroup<ItemNetwork>, NetworkGroupData<ItemNetwork> by data {
    
    private val providerSnapshots = HashMap<NetworkedInventory, Array<ItemStack>>()
    private val filteredProviderSnapshots = HashMap<FilteredNetworkedInventory, Array<ItemStack>>()
    private var hasSnapshot = false
    
    private val roundRobin = RoundRobinCounter(networks.size)
    
    init {
        for (network in networks) {
            for (distributor in network.channels) {
                if (distributor == null)
                    continue
                
                // init snapshot arrays
                for (provider in distributor.providers) {
                    if (provider in providerSnapshots)
                        continue
                    
                    val arr = Array(provider.size) { ItemStack.empty() }
                    providerSnapshots[provider] = arr
                }
                for (filteredProvider in distributor.filteredProviders) {
                    if (filteredProvider in filteredProviderSnapshots)
                        continue
                    
                    val arr = Array(filteredProvider.inventory.size) { ItemStack.empty() }
                    filteredProviderSnapshots[filteredProvider] = arr
                }
                
                // link snapshot arrays with distributor
                val snapshots = Array<Array<Array<ItemStack>?>?>(distributor.levels) { null }
                for (level in 0..<distributor.levels) {
                    val providersInLevel = distributor.providerLevels[level]
                    val levelSnapshots = Array<Array<ItemStack>?>(providersInLevel.size) { null }
                    snapshots[level] = levelSnapshots
                    for ((idx, provider) in providersInLevel.withIndex()) {
                        levelSnapshots[idx] = filteredProviderSnapshots[provider]!!
                    }
                }
                distributor.providerSnapshots = snapshots as Array<Array<Array<ItemStack>>>
            }
        }
    }
    
    override fun preTick() {
        if (MAX_COMPLEXITY != -1 && networks.all { it.complexity > MAX_COMPLEXITY }) {
            hasSnapshot = false
            return
        }
        
        takeSnapshot()
        hasSnapshot = true
    }
    
    override fun tick() {
        if (!hasSnapshot)
            return
        
        val startIdx = roundRobin.next()
        for (i in startIdx..<networks.size) {
            networks[i].tick()
        }
        for (i in 0..<startIdx) {
            networks[i].tick()
        }
    }
    
    private fun takeSnapshot() {
        for ((provider, snapshot) in providerSnapshots) {
            provider.copyContents(snapshot)
        }
        
        for ((provider, snapshot) in filteredProviderSnapshots) {
            val unfilteredSnapshot = providerSnapshots[provider.inventory]!!
            for ((slot, itemStack) in unfilteredSnapshot.withIndex()) {
                snapshot[slot] = if (provider.denies(itemStack)) ItemStack.empty() else itemStack
            }
        }
    }
    
}