package xyz.xenondevs.nova.tileentity.network.fluid.channel

import xyz.xenondevs.nova.tileentity.network.NetworkException
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.util.sumOfNoOverflow
import kotlin.math.min

class FluidDistributor(
    private val priorityLevels: List<Triple<List<FluidContainer>, List<FluidContainer>, List<FluidContainer>>>
) {
    
    private val fluidContainers: Set<FluidContainer> = HashSet<FluidContainer>().also {
        priorityLevels.forEach { (consumers, providers, buffers) ->
            it += consumers
            it += providers
            it += buffers
        }
    }
    
    /**
     * Distributes fluids in this [FluidNetworkChannel]
     * @param transferAmount The amount of fluid to transfer
     * @return The transfer capacity left over (transferAmount - fluidTransferred)
     */
    fun distribute(transferAmount: Long): Long {
        val type = findFluidType() ?: return transferAmount
        var priority = 0 // 0 represents the highest priority level
        var transfersLeft = transferAmount
        
        while (true) {
            val (consumersInScope, providersInScope, buffersInScope) = priorityLevels[priority]
            
            transfersLeft = distributeInScope(
                transfersLeft,
                type,
                consumersInScope, providersInScope, buffersInScope
            )
            if (transfersLeft == 0L) break
            
            if (priority < priorityLevels.size - 1) {
                priority++
            } else break
        }
        
        return transfersLeft
    }
    
    private fun findFluidType(): FluidType? {
        var fluidType: FluidType? = null
        fluidContainers.forEach {
            if (it.hasFluid()) {
                if (fluidType != null) {
                    if (fluidType != it.type) return null
                } else fluidType = it.type
            }
        }
        
        return fluidType?.takeUnless { fluidContainers.any { fluidType !in it.allowedTypes } }
    }
    
    private fun distributeInScope(
        transferAmount: Long,
        type: FluidType,
        consumersInScope: List<FluidContainer>,
        providersInScope: List<FluidContainer>,
        buffersInScope: List<FluidContainer>
    ): Long {
        val consumers = consumersInScope.filterNotTo(ArrayList()) { it.isFull() }
        val providers = providersInScope.filterTo(ArrayList()) { it.hasFluid() }
        
        var availableTransfers = transferAmount
        availableTransfers -= distributeBetween(availableTransfers, type, consumers, providers)
        
        if (availableTransfers != 0L) {
            if (consumers.isEmpty() xor providers.isEmpty()) {
                if (consumers.isEmpty()) consumers += buffersInScope
                else providers += buffersInScope
                
                availableTransfers -= distributeBetween(availableTransfers, type, consumers, providers)
                
                // TODO: balance buffers?
            }
        }
        
        return availableTransfers
    }
    
    private fun distributeBetween(
        maxTransfers: Long,
        type: FluidType,
        consumers: MutableList<FluidContainer>,
        providers: MutableList<FluidContainer>
    ): Long {
        val availableFluid = min(maxTransfers, providers.sumOfNoOverflow { it.amount })
        if (availableFluid == 0L) return 0L
        
        val distributed = giveEqually(availableFluid, type, consumers)
        takeEqually(distributed, providers)
        
        return distributed
    }
    
    private fun giveEqually(amount: Long, type: FluidType, consumers: MutableList<FluidContainer>): Long {
        if (amount == 0L) return amount
        
        var remaining = amount
        while (remaining != 0L && consumers.isNotEmpty()) {
            val cut = remaining / consumers.size
            if (cut == 0L) {
                remaining = giveFirst(remaining, type, consumers)
                break
            }
            
            consumers.removeIf {
                val added = it.tryAddFluid(type, cut)
                remaining -= added
                
                it.isFull()
            }
        }
        
        return amount - remaining
    }
    
    private fun giveFirst(amount: Long, type: FluidType, consumers: MutableList<FluidContainer>): Long {
        if (amount == 0L) return amount
        
        var remaining = amount
        consumers.forEach {
            remaining -= it.tryAddFluid(type, remaining)
        }
        
        return remaining
    }
    
    private fun takeEqually(amount: Long, providers: MutableList<FluidContainer>) {
        var remaining = amount
        while (remaining != 0L && providers.isNotEmpty()) {
            val cut = remaining / providers.size
            if (cut == 0L) {
                remaining = takeFirst(remaining, providers)
                break
            }
            
            providers.removeIf {
                val take = it.tryTakeFluid(cut)
                remaining -= take
                
                take == 0L
            }
        }
        
        if (remaining != 0L) throw NetworkException("Could not provide the fluid distributed")
    }
    
    private fun takeFirst(amount: Long, providers: MutableList<FluidContainer>): Long {
        var remaining = amount
        providers.forEach {
            remaining -= it.tryTakeFluid(remaining)
        }
        return remaining
    }
    
}