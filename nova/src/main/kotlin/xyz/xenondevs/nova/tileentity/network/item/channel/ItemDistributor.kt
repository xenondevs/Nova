package xyz.xenondevs.nova.tileentity.network.item.channel

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.util.RoundRobinCounter
import kotlin.math.min

internal class ItemDistributor(
    private val consumers: List<List<FilteredNetworkedInventory>>,
    private val providers: List<List<FilteredNetworkedInventory>>
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
        
        // The content arrays in this map are only updated when items are removed, not when items are added.
        // This is intended to prevent (endless) loops of giving items back and forth by limiting items to be only
        // transferred once per "distributeBetween"-call.
        val providerContentMap = providersInScope.associateWithTo(HashMap()) { it.items }
        
        var transfersLeft = transferAmount
        
        val consumerFailCache = Array<ArrayList<ItemStack>>(consumersInScope.size, ::ArrayList) // caches which items cannot be added to this consumer
        val ignoredConsumers = BooleanArray(consumersInScope.size) // represents which consumers should be skipped
        while (ignoredConsumers.any { !it } && transfersLeft > 0) {
            
            // check that this consumer is not ignored, else skip
            if (!ignoredConsumers[consumerRRCounter.get()]) {
                val consumer = consumersInScope[consumerRRCounter.get()]
                val consumerFailList = consumerFailCache[consumerRRCounter.get()]
                var ignoreConsumer = true
                
                if (!consumer.isFull()) {
                    val providerRRCounter = providerRRCounters.getOrPut(consumer) { RoundRobinCounter(providersInScope.size) }
                    val ignoredProviders = BooleanArray(providersInScope.size) // represents which provider should be skipped
                    while (ignoredProviders.any { !it } && transfersLeft > 0) {
                        
                        // check that this provider is not ignored, else skip
                        if (!ignoredProviders[providerRRCounter.get()]) {
                            var ignoreProvider = true
                            val provider = providersInScope[providerRRCounter.get()]
                            
                            // prevent providers from providing to themselves
                            if (provider.canExchangeItemsWith(consumer)) {
                                val providerContent = providerContentMap[provider]!!
                                
                                // find the first item that can be extracted into the current consumer and perform the extraction
                                for ((slot, itemStack) in providerContent.withIndex()) {
                                    if (itemStack == null
                                        || provider.deniesItem(itemStack)
                                        || consumerFailList.any { it.isSimilar(itemStack) }
                                        || !provider.canDecrementByOne(slot)
                                    ) continue
                                    
                                    if (!consumer.deniesItem(itemStack)) {
                                        val singleStack = itemStack.clone().apply { amount = 1 }
                                        if (consumer.addItem(singleStack) == 0) {
                                            // decrease amount by one for the provider
                                            provider.decrementByOne(slot)
                                            // decrease amount by one for the providerContent array
                                            if (itemStack.amount <= 1) providerContent[slot] = null
                                            else itemStack.amount -= 1
                                            
                                            ignoreProvider = false
                                            ignoreConsumer = false
                                            transfersLeft -= 1
                                            
                                            break
                                        } else consumerFailList += singleStack
                                    } else consumerFailList += itemStack.clone()
                                }
                            }
                            
                            // this provider can be ignored in the future if it can't supply this consumer
                            if (ignoreProvider) ignoredProviders[providerRRCounter.get()] = true
                        }
                        
                        providerRRCounter.increment()
                    }
                }
                
                // this consumer can be ignored in the future if it cannot be supplied by any providers
                if (ignoreConsumer) ignoredConsumers[consumerRRCounter.get()] = true
            }
            
            consumerRRCounter.increment()
        }
        
        return transfersLeft
    }
    
    private fun distributeDirectlyBetween(transferAmount: Int, consumer: FilteredNetworkedInventory, provider: FilteredNetworkedInventory): Int {
        if (!consumer.canExchangeItemsWith(provider)) return transferAmount
        
        var transfersLeft = transferAmount
        
        for ((slot, itemStack) in provider.items.withIndex()) {
            if (itemStack == null || provider.deniesItem(itemStack) || consumer.deniesItem(itemStack)) continue
            
            val transferableStack = itemStack.clone().apply { amount = min(amount, transfersLeft) }
            val amountTransferred = transferableStack.amount - consumer.addItem(transferableStack)
            
            if (amountTransferred != 0) {
                val success = provider.setItem(slot, itemStack.also { it.amount -= amountTransferred }.takeUnless { it.amount <= 0 })
                if (success) {
                    transfersLeft -= amountTransferred
                }
            }
            
            if (transfersLeft == 0) break
        }
        
        return transfersLeft
    }
    
}