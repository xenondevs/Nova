package xyz.xenondevs.nova.tileentity.network.type.item.channel

import net.minecraft.world.item.ItemStack
import org.bukkit.block.BlockFace
import xyz.xenondevs.commons.collections.getOrSet
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.type.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.type.item.ItemNetwork
import xyz.xenondevs.nova.tileentity.network.type.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import java.util.*

internal data class FilteredNetworkedInventory(
    val inventory: NetworkedInventory,
    private val filter: ItemFilter?
) {
    
    fun allowsItem(itemStack: ItemStack): Boolean = filter == null //|| filter.allowsItem(itemStack)
    
    fun deniesItem(itemStack: ItemStack): Boolean = filter != null //&& !filter.allowsItem(itemStack)
    
    fun canExchangeItemsWith(other: FilteredNetworkedInventory): Boolean = inventory.canExchangeItemsWith(other.inventory)
    
}

internal class ItemChannelsBuilder {
    
    private val channels = arrayOfNulls<ItemDistributorBuilder?>(ItemNetwork.CHANNEL_AMOUNT)
    
    fun addHolder(holder: ItemHolder, faces: Set<BlockFace>) {
        for (face in faces) {
            val channel = holder.channels[face]
                ?: throw IllegalStateException("$holder is missing channel for $face")
            channels.getOrSet(channel, ::ItemDistributorBuilder).addHolder(holder, face)
        }
    }
    
    fun build(): Array<ItemDistributor?> =
        channels.mapToArray { it?.build() }
    
}

internal class ItemDistributorBuilder {
    
    private val providers = TreeMap<Int, MutableList<FilteredNetworkedInventory>>(Comparator.reverseOrder())
    private val consumers = TreeMap<Int, MutableList<FilteredNetworkedInventory>>(Comparator.reverseOrder())
    
    fun addHolder(holder: ItemHolder, face: BlockFace) {
        val conType = holder.connectionConfig[face] ?: return
        
        when (conType) {
            NetworkConnectionType.INSERT -> addConsumer(holder, face)
            NetworkConnectionType.EXTRACT -> addProvider(holder, face)
            NetworkConnectionType.BUFFER -> {
                addConsumer(holder, face)
                addProvider(holder, face)
            }
            
            else -> throw UnsupportedOperationException()
        }
    }
    
    private fun addProvider(holder: ItemHolder, face: BlockFace) {
        val inventory = holder.containerConfig[face]
            ?: throw IllegalStateException("$holder is missing container config for $face")
        val priority = holder.extractPriorities[face]
            ?: throw IllegalStateException("$holder is missing extract priority for $face")
        val filter = holder.extractFilters[face]
        
        val filteredInventory = FilteredNetworkedInventory(inventory, filter)
        providers.getOrPut(priority, ::ArrayList) += filteredInventory
        // add priority entry in consumers to ensure that indices in both maps correspond to the same priority
        consumers.getOrPut(priority, ::ArrayList)
    }
    
    private fun addConsumer(holder: ItemHolder, face: BlockFace) {
        val inventory = holder.containerConfig[face]
            ?: throw IllegalStateException("$holder is missing consumer container config for $face")
        val priority = holder.insertPriorities[face]
            ?: throw IllegalStateException("$holder is missing insert priority for $face")
        val filter = holder.insertFilters[face]
        
        val filteredInventory = FilteredNetworkedInventory(inventory, filter)
        consumers.getOrPut(priority, ::ArrayList) += filteredInventory
        // add priority entry in providers to ensure that indices in both maps correspond to the same priority
        providers.getOrPut(priority, ::ArrayList)
    }
    
    fun build(): ItemDistributor {
        val providers = providers.entries.map { it.value }
        val consumers = consumers.entries.map { it.value }
        check(providers.size == consumers.size)
        
        val flatProviders = HashSet<NetworkedInventory>()
        val flatFilteredProviders = HashSet<FilteredNetworkedInventory>()
        
        val levels = providers.size // == consumers.size
        val providerLevels = ArrayList<ArrayList<FilteredNetworkedInventory>>(levels)
        val consumerLevels = ArrayList<ArrayList<FilteredNetworkedInventory>>(levels)
        for (i in providers.indices) {
            val providersInLevel = ArrayList<FilteredNetworkedInventory>()
            val consumersInLevel = ArrayList<FilteredNetworkedInventory>()
            
            if (i > 0) {
                providersInLevel += providerLevels[i - 1]
                consumersInLevel += consumerLevels[i - 1]
            }
            
            providersInLevel += providers[i]
            consumersInLevel += consumers[i]
            
            providerLevels += providersInLevel
            consumerLevels += consumersInLevel
            
            flatFilteredProviders += providers[i]
            flatProviders += providers[i].map { it.inventory }
        }
        
        return ItemDistributor(flatProviders, flatFilteredProviders, providerLevels, consumerLevels)
    }
    
}