package xyz.xenondevs.nova.tileentity.network.type.item.channel

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.tileentity.network.type.item.inventory.NetworkedInventory
import xyz.xenondevs.nova.util.RoundRobinCounter
import java.util.*
import kotlin.math.min

internal class ItemDistributor(
    val providers: Collection<NetworkedInventory>,
    val filteredProviders: Collection<FilteredNetworkedInventory>,
    val providerLevels: List<List<FilteredNetworkedInventory>>,
    val consumerLevels: List<List<FilteredNetworkedInventory>>
) {
    
    val levels = providerLevels.size // == consumerLevels.size
    
    private val consumerRRCounters: Array<RoundRobinCounter> // level, rr counter
    private val providerRRCounters: Array<Array<RoundRobinCounter>> // level, consumer index, rr counter
    private val ignoredProviders: Array<Array<BitSet>> // level, consumer index, ignored provider mask
    lateinit var providerSnapshots: Array<Array<Array<ItemStack>>> // level, provider, snapshot
    
    init {
        require(providerLevels.size == consumerLevels.size)
        
        // RR counter to select consumer, per level
        consumerRRCounters = Array(levels) {
            val consumerCount = consumerLevels[it].size
            RoundRobinCounter(consumerCount)
        }
        
        // RR counter to select provider for each consumer, per level
        providerRRCounters = Array(levels) { level ->
            val consumerCount = consumerLevels[level].size
            val providerCount = providerLevels[level].size
            Array(consumerCount) { RoundRobinCounter(providerCount) }
        }
        
        // mask of ignored providers per consumer, per level
        ignoredProviders = Array(levels) { level ->
            val consumersInLevel = consumerLevels[level]
            val providersInLevel = providerLevels[level]
            
            Array(consumersInLevel.size) { consumerIdx ->
                val consumer = consumersInLevel[consumerIdx]
                val providerCount = providersInLevel.size
                val ignoredProviders = BitSet(providerCount)
                for (providerIdx in 0..<providerCount) {
                    val provider = providersInLevel[providerIdx]
                    if (!provider.canExchangeItemsWith(consumer))
                        ignoredProviders.set(providerIdx)
                }
                
                ignoredProviders
            }
        }
    }
    
    /**
     * Distributes [transferAmount] between the consumers and providers,
     * using [providerSnapshots] to get the current state of the providers.
     *
     * @return the remaining transfer capacity
     */
    fun distribute(transferAmount: Int): Int {
        var transfersLeft = transferAmount
        for (level in 0..<levels) {
            val consumersInScope = consumerLevels[level]
            val providersInScope = providerLevels[level]
            
            when {
                consumersInScope.size == 1 && providersInScope.size == 1 -> {
                    transfersLeft = distributeDirectlyBetween(
                        transfersLeft,
                        consumersInScope[0], providersInScope[0],
                        providerSnapshots[level][0]
                    )
                }
                
                consumersInScope.isNotEmpty() && providersInScope.isNotEmpty() -> {
                    transfersLeft = distributeBetween(
                        transfersLeft,
                        consumersInScope, providersInScope,
                        ignoredProviders[level].mapToArray { it.clone() as BitSet },
                        providerSnapshots[level],
                        consumerRRCounters[level], providerRRCounters[level]
                    )
                }
                
                else -> Unit
            }
            
            if (transfersLeft <= 0)
                break
        }
        
        return transfersLeft
    }
    
    /**
     * Distributes [transferAmount] items from [providers] to [consumers], with the order
     * being determined by [consumerRR] and [providersRR].
     *
     * This function assumes, that `consumer.size > 1 && providers.size > 1`.
     */
    private fun distributeBetween(
        transferAmount: Int,
        consumers: List<FilteredNetworkedInventory>,
        providers: List<FilteredNetworkedInventory>,
        ignoredProvidersPerConsumer: Array<BitSet>,
        providerSnapshots: Array<Array<ItemStack>>,
        consumerRR: RoundRobinCounter,
        providersRR: Array<RoundRobinCounter>
    ): Int {
        var transfersLeft = transferAmount
        
        val ignoredConsumers = createIgnoredConsumers(consumers)
        while (transfersLeft > 0 && ignoredConsumers.nextClearBit(0) < consumers.size) {
            val consumerIdx = consumerRR.next()
            
            // skip consumer if ignored
            if (ignoredConsumers[consumerIdx])
                continue
            
            val consumer = consumers[consumerIdx]
            var hasConsumed = false
            
            // take 1 item from first applicable provider
            val ignoredProviders = ignoredProvidersPerConsumer[consumerIdx]
            val providerRR = providersRR[consumerIdx]
            while (ignoredProviders.nextClearBit(0) < providers.size) {
                val providerIdx = providerRR.next()
                
                // skip provider if ignored
                if (ignoredProviders[providerIdx])
                    continue
                
                
                val provider = providers[providerIdx]
                val providerContent = providerSnapshots[providerIdx]
                var hasProvided = false
                
                // find the first item that can be extracted into the current consumer and perform the extraction
                for ((slot, itemStack) in providerContent.withIndex()) {
                    if (itemStack.isEmpty || consumer.denies(itemStack) || !provider.inventory.canTake(slot, 1))
                        continue
                    
                    if (consumer.inventory.add(itemStack, 1) == 0) {
                        provider.inventory.take(slot, 1)
                        providerContent[slot].amount--
                        transfersLeft--
                        
                        hasConsumed = true
                        hasProvided = true
                        
                        break
                    }
                }
                
                if (!hasProvided)
                    ignoredProviders.set(providerIdx)
                
                if (hasConsumed)
                    break
            }
            
            if (!hasConsumed)
                ignoredConsumers.set(consumerIdx)
        }
        
        return transfersLeft
    }
    
    /**
     * Creates a bit set where each bit specifies whether the corresponding consumer should be ignored.
     * (`true` = ignored`)
     *
     * This set is initialized with [NetworkedInventory.isFull].
     */
    private fun createIgnoredConsumers(consumers: List<FilteredNetworkedInventory>): BitSet {
        val bitSet = BitSet(consumers.size)
        for ((idx, consumer) in consumers.withIndex()) {
            if (consumer.inventory.isFull())
                bitSet.set(idx)
        }
        return bitSet
    }
    
    /**
     * Distributes [transferAmount] items directly between [consumer] and [provider],
     * using the [providerSnapshot] to get the current state of the provider.
     *
     * @return the remaining transfer capacity
     */
    private fun distributeDirectlyBetween(
        transferAmount: Int,
        consumer: FilteredNetworkedInventory,
        provider: FilteredNetworkedInventory,
        providerSnapshot: Array<ItemStack>
    ): Int {
        if (!consumer.canExchangeItemsWith(provider) || consumer.inventory.isFull() || provider.inventory.isEmpty())
            return transferAmount
        
        var transfersLeft = transferAmount
        for ((slot, itemStack) in providerSnapshot.withIndex()) {
            if (itemStack.isEmpty || consumer.denies(itemStack))
                continue
            
            val idealAmount = min(itemStack.amount, transfersLeft)
            if (!provider.inventory.canTake(slot, idealAmount))
                continue
            
            val transferred = idealAmount - consumer.inventory.add(itemStack, idealAmount)
            provider.inventory.take(slot, transferred)
            transfersLeft -= transferred
            
            if (transfersLeft <= 0)
                break
        }
        
        return transfersLeft
    }
    
}