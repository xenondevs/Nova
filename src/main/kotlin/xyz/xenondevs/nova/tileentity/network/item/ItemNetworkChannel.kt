package xyz.xenondevs.nova.tileentity.network.item

import org.bukkit.block.BlockFace
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType.*
import xyz.xenondevs.nova.tileentity.network.item.holder.ItemHolder
import xyz.xenondevs.nova.tileentity.network.item.inventory.NetworkedInventory
import java.util.*
import kotlin.math.max
import kotlin.math.min

typealias ItemFilterList = ArrayList<ItemFilter>

class AttachedInventoryConfiguration(itemHolder: ItemHolder, face: BlockFace, type: ItemConnectionType) {
    
    val inventory: NetworkedInventory = itemHolder.inventories[face]!!
    
    val priority = when (type) {
        INSERT -> itemHolder.insertPriorities[face]!!
        EXTRACT -> itemHolder.extractPriorities[face]!!
        else -> throw IllegalArgumentException()
    }
    
    val filter = when (type) {
        INSERT -> itemHolder.insertFilters[face]
        EXTRACT -> itemHolder.extractFilters[face]
        else -> throw IllegalArgumentException()
    }
    
    operator fun component1() = inventory
    operator fun component2() = filter
    operator fun component3() = priority
    
}

class FilteredNetworkedInventory(
    private val inventory: NetworkedInventory,
    private val filters: ItemFilterList
) : NetworkedInventory by inventory {
    
    fun allowsItem(itemStack: ItemStack) = filters.isEmpty() || filters.all { it.allowsItem(itemStack) }
    
    fun deniesItem(itemStack: ItemStack) = filters.isNotEmpty() && filters.any { !it.allowsItem(itemStack) }
    
    fun isSameInventory(other: FilteredNetworkedInventory) = inventory == other.inventory
    
}

class ItemNetworkChannel {
    
    private val consumers = HashMap<ItemHolder, ArrayList<AttachedInventoryConfiguration>>()
    private val providers = HashMap<ItemHolder, ArrayList<AttachedInventoryConfiguration>>()
    
    private val consumerConfigurations = ArrayList<AttachedInventoryConfiguration>()
    private val providerConfigurations = ArrayList<AttachedInventoryConfiguration>()
    
    private var itemDistributor: ItemDistributor? = null
    
    private fun getConsumerConfigurations(itemHolder: ItemHolder) = consumers.getOrPut(itemHolder) { ArrayList() }
    private fun getProviderConfigurations(itemHolder: ItemHolder) = providers.getOrPut(itemHolder) { ArrayList() }
    
    fun addItemHolder(itemHolder: ItemHolder, face: BlockFace) {
        when (itemHolder.itemConfig[face]!!) {
            EXTRACT -> addProvider(itemHolder, face)
            INSERT -> addConsumer(itemHolder, face)
            BUFFER -> {
                addProvider(itemHolder, face)
                addConsumer(itemHolder, face)
            }
            else -> throw UnsupportedOperationException()
        }
        
        createItemDistributor()
    }
    
    private fun addConsumer(itemHolder: ItemHolder, face: BlockFace) {
        val configuration = AttachedInventoryConfiguration(itemHolder, face, INSERT)
        getConsumerConfigurations(itemHolder) += configuration
        consumerConfigurations += configuration
    }
    
    private fun addProvider(itemHolder: ItemHolder, face: BlockFace) {
        val configuration = AttachedInventoryConfiguration(itemHolder, face, EXTRACT)
        getProviderConfigurations(itemHolder) += configuration
        providerConfigurations += configuration
    }
    
    fun addAll(otherChannel: ItemNetworkChannel) {
        require(this !== otherChannel) { "Can't add to self" }
        
        consumers += otherChannel.consumers
        providers += otherChannel.providers
        providerConfigurations += otherChannel.providerConfigurations
        consumerConfigurations += otherChannel.consumerConfigurations
        
        createItemDistributor()
    }
    
    fun removeItemHolder(itemHolder: ItemHolder) {
        providers[itemHolder]?.forEach(providerConfigurations::remove)
        consumers[itemHolder]?.forEach(consumerConfigurations::remove)
        
        providers -= itemHolder
        consumers -= itemHolder
        
        createItemDistributor()
    }
    
    fun isEmpty() = consumers.isEmpty() && providers.isEmpty()
    
    private fun createItemDistributor() {
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

private class RoundRobinCounter(val maxExclusive: Int) {
    
    private var i = 0
    
    fun get() = i
    
    fun increment() {
        i++
        if (i == maxExclusive) i = 0
    }
    
}

private class ItemDistributor(
    val consumers: List<List<FilteredNetworkedInventory>>,
    val providers: List<List<FilteredNetworkedInventory>>
) {
    
    private val consumerRRCounters: Array<RoundRobinCounter>
    private val providerRRCounters = Array(providers.size) { HashMap<FilteredNetworkedInventory, RoundRobinCounter>() }
    
    init {
        var size = 0
        consumerRRCounters = Array(consumers.size) {
            size += consumers[it].size
            RoundRobinCounter(size)
        }
    }
    
    /**
     * Distributes items in this [ItemNetworkChannel]
     * @param transferAmount The amount of items to transfer
     * @return The transfer capacity left over (transferAmount - itemsTransferred)
     */
    fun distribute(transferAmount: Int): Int {
        // starts at 0, representing the highest priority
        var priority = 0
        
        var transfersLeft = transferAmount
        
        val consumersInScope = ArrayList(consumers[0])
        val providersInScope = ArrayList(providers[0])
        
        while (true) {
            if (consumersInScope.isNotEmpty() && providersInScope.isNotEmpty()) {
                transfersLeft = distributeBetween(
                    transfersLeft,
                    consumersInScope, providersInScope,
                    consumerRRCounters[priority], providerRRCounters[priority]
                )
                if (transfersLeft == 0) break
            }
            
            if (priority < providers.size - 1) {
                priority++
                providersInScope += providers[priority]
                consumersInScope += consumers[priority]
            } else break
        }
        
        return transfersLeft
    }
    
    /**
     * Distributes items between the specified [providers][providersInScope] and [consumers][consumersInScope].
     */
    private fun distributeBetween(
        transferAmount: Int,
        consumersInScope: List<FilteredNetworkedInventory>,
        providersInScope: List<FilteredNetworkedInventory>,
        consumerRRCounter: RoundRobinCounter,
        providerRRCounters: HashMap<FilteredNetworkedInventory, RoundRobinCounter>,
    ): Int {
        if (consumersInScope.size == 1 && providersInScope.size == 1)
            return distributeDirectlyBetween(transferAmount, consumersInScope[0], providersInScope[0])
        
        var transfersLeft = transferAmount
        
        val ignoredConsumers = BooleanArray(consumersInScope.size) // represents which consumers should be skipped
        while (ignoredConsumers.any { !it } && transfersLeft > 0) {
            
            // check that this consumer is not ignored, else skip
            if (!ignoredConsumers[consumerRRCounter.get()]) {
                val consumer = consumersInScope[consumerRRCounter.get()]
                var didConsume = false
                
                val providerRRCounter = providerRRCounters.getOrPut(consumer) { RoundRobinCounter(providersInScope.size) }
                val ignoredProviders = BooleanArray(providersInScope.size) // represents which provider should be skipped
                while (ignoredProviders.any { !it } && transfersLeft > 0) {
                    
                    // check that this provider is not ignored, else skip
                    if (!ignoredProviders[providerRRCounter.get()]) {
                        var didProvide = false
                        val provider = providersInScope[providerRRCounter.get()]
                        
                        // prevent providers from providing to themselves
                        if (!provider.isSameInventory(consumer)) {
                            // find the first item that can be extracted into the current consumer and perform the extraction
                            for ((slot, itemStack) in provider.items.withIndex()) {
                                if (itemStack == null || provider.deniesItem(itemStack) || consumer.deniesItem(itemStack)) continue
                                
                                val singleStack = itemStack.clone().apply { amount = 1 }
                                if (consumer.addItem(singleStack) == 0) {
                                    provider.decrementByOne(slot)
                                    didProvide = true
                                    didConsume = true
                                    transfersLeft -= 1
                                    
                                    break
                                }
                            }
                        }
                        
                        // this provider can be ignored in the future if it can't supply this consumer
                        if (!didProvide) ignoredProviders[providerRRCounter.get()] = true
                    }
                    
                    providerRRCounter.increment()
                }
                
                // this consumer can be ignored in the future if it cannot be supplied by any providers
                if (!didConsume) ignoredConsumers[consumerRRCounter.get()] = true
            }
            
            consumerRRCounter.increment()
        }
        
        return transfersLeft
    }
    
    private fun distributeDirectlyBetween(transferAmount: Int, consumer: FilteredNetworkedInventory, provider: FilteredNetworkedInventory): Int {
        if (consumer.isSameInventory(provider)) return transferAmount
        
        var transfersLeft = transferAmount
        
        for ((slot, itemStack) in provider.items.withIndex()) {
            if (itemStack == null || provider.deniesItem(itemStack) || consumer.deniesItem(itemStack)) continue
            
            val transferableStack = itemStack.clone().apply { amount = min(amount, transfersLeft) }
            val amountTransferred = transferableStack.amount - consumer.addItem(transferableStack)
            
            if (amountTransferred != 0) {
                provider.setItem(slot, itemStack.also { it.amount -= amountTransferred }.takeUnless { it.amount <= 0 })
                transfersLeft -= amountTransferred
            }
            
            if (transfersLeft == 0) break
        }
        
        return transfersLeft
    }
    
}
