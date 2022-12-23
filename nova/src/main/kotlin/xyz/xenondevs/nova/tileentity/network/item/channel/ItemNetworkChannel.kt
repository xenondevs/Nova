package xyz.xenondevs.nova.tileentity.network.item.channel

import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemFilter
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import java.util.*
import kotlin.math.max

internal typealias ItemFilterList = ArrayList<ItemFilter>

internal class AttachedInventoryConfiguration(itemHolder: ItemHolder, face: BlockFace, type: NetworkConnectionType) {
    
    val inventory: NetworkedInventory = itemHolder.containerConfig[face]!!
    
    val priority = when (type) {
        NetworkConnectionType.INSERT -> itemHolder.insertPriorities[face]!!
        NetworkConnectionType.EXTRACT -> itemHolder.extractPriorities[face]!!
        else -> throw IllegalArgumentException()
    }
    
    val filter = when (type) {
        NetworkConnectionType.INSERT -> itemHolder.insertFilters[face]
        NetworkConnectionType.EXTRACT -> itemHolder.extractFilters[face]
        else -> throw IllegalArgumentException()
    }
    
    operator fun component1() = inventory
    operator fun component2() = filter
    operator fun component3() = priority
    
}

internal class FilteredNetworkedInventory(
    private val inventory: NetworkedInventory,
    private val filters: ItemFilterList
) : NetworkedInventory by inventory {
    
    fun allowsItem(itemStack: ItemStack): Boolean = filters.isEmpty() || filters.all { it.allowsItem(itemStack) }
    
    fun deniesItem(itemStack: ItemStack): Boolean = filters.isNotEmpty() && filters.any { !it.allowsItem(itemStack) }
    
    fun canExchangeItemsWith(other: FilteredNetworkedInventory): Boolean = inventory.canExchangeItemsWith(other.inventory)
    
}

internal class ItemNetworkChannel {
    
    private val consumers = HashMap<ItemHolder, ArrayList<AttachedInventoryConfiguration>>()
    private val providers = HashMap<ItemHolder, ArrayList<AttachedInventoryConfiguration>>()
    private val consumerConfigurations = ArrayList<AttachedInventoryConfiguration>()
    private val providerConfigurations = ArrayList<AttachedInventoryConfiguration>()
    
    private var itemDistributor: ItemDistributor? = null
    
    private fun getConsumerConfigurations(holder: ItemHolder) = consumers.getOrPut(holder) { ArrayList() }
    private fun getProviderConfigurations(holder: ItemHolder) = providers.getOrPut(holder) { ArrayList() }
    
    fun addAll(otherChannel: ItemNetworkChannel) {
        require(this !== otherChannel) { "Can't add to self" }
        
        consumers += otherChannel.consumers
        providers += otherChannel.providers
        
        consumerConfigurations += otherChannel.consumerConfigurations
        providerConfigurations += otherChannel.providerConfigurations
        
        createDistributor()
    }
    
    fun addHolder(holder: ItemHolder, face: BlockFace, connectionType: NetworkConnectionType, createDistributor: Boolean) {
        when (connectionType) {
            NetworkConnectionType.INSERT -> addConsumer(holder, face)
            NetworkConnectionType.EXTRACT -> addProvider(holder, face)
            NetworkConnectionType.BUFFER -> {
                addConsumer(holder, face)
                addProvider(holder, face)
            }
            
            else -> throw UnsupportedOperationException()
        }
        
        if (createDistributor) createDistributor()
    }
    
    fun removeHolder(holder: ItemHolder, createDistributor: Boolean) {
        consumers[holder]?.forEach { consumerConfigurations -= it }
        providers[holder]?.forEach { providerConfigurations -= it }
        
        consumers -= holder
        providers -= holder
        
        if (createDistributor) createDistributor()
    }
    
    fun isEmpty() = consumers.isEmpty() && providers.isEmpty()
    
    private fun addConsumer(holder: ItemHolder, face: BlockFace) {
        val configuration = AttachedInventoryConfiguration(holder, face, NetworkConnectionType.INSERT)
        getConsumerConfigurations(holder) += configuration
        consumerConfigurations += configuration
    }
    
    private fun addProvider(holder: ItemHolder, face: BlockFace) {
        val configuration = AttachedInventoryConfiguration(holder, face, NetworkConnectionType.EXTRACT)
        getProviderConfigurations(holder) += configuration
        providerConfigurations += configuration
    }
    
    fun createDistributor() {
        itemDistributor = if (consumerConfigurations.isNotEmpty() && providerConfigurations.isNotEmpty()) {
            val (consumers, providers) = computeAvailableInventories()
            ItemDistributor(consumers, providers)
        } else null
    }
    
    private fun convertConfigurations(configurations: List<AttachedInventoryConfiguration>): Map<Int, List<FilteredNetworkedInventory>> {
        val tempMap = HashMap<NetworkedInventory, Pair<Int, ItemFilterList>>()
        
        configurations.forEach { (inventory, filter, priority) ->
            if (inventory in tempMap) {
                val pair = tempMap[inventory]!!
                if (filter != null) pair.second += filter
                tempMap[inventory] = max(pair.first, priority) to pair.second
            } else {
                val filterList = ItemFilterList()
                if (filter != null) filterList += filter
                tempMap[inventory] = priority to filterList
            }
        }
        
        val result = HashMap<Int, ArrayList<FilteredNetworkedInventory>>()
        tempMap.forEach { (inventory, pair) ->
            val (priority, filterList) = pair
            result.getOrPut(priority) { ArrayList() } += FilteredNetworkedInventory(inventory, filterList)
        }
        
        return result
    }
    
    private fun computeAvailableInventories(): Pair<List<List<FilteredNetworkedInventory>>, List<List<FilteredNetworkedInventory>>> {
        val consumers = convertConfigurations(consumerConfigurations)
        val providers = convertConfigurations(providerConfigurations)
        
        val consumerInventories = ArrayList<ArrayList<FilteredNetworkedInventory>>()
        val providerInventories = ArrayList<ArrayList<FilteredNetworkedInventory>>()
        
        TreeSet<Int>(Comparator.reverseOrder())
            .apply {
                addAll(consumers.keys)
                addAll(providers.keys)
            }.forEach {
                val consumerInventoriesForPriority = ArrayList<FilteredNetworkedInventory>().also(consumerInventories::add)
                val providerInventoriesForPriority = ArrayList<FilteredNetworkedInventory>().also(providerInventories::add)
                
                consumers[it]?.also(consumerInventoriesForPriority::addAll)
                providers[it]?.also(providerInventoriesForPriority::addAll)
            }
        
        return consumerInventories to providerInventories
    }
    
    fun distributeItems(transferAmount: Int): Int {
        return itemDistributor?.distribute(transferAmount) ?: transferAmount
    }
    
}