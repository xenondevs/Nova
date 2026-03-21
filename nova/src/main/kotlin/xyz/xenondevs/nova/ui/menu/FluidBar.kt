package xyz.xenondevs.nova.ui.menu

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.commons.provider.combinedProvider
import xyz.xenondevs.commons.provider.flatten
import xyz.xenondevs.commons.provider.provider
import xyz.xenondevs.invui.dsl.ClickDsl
import xyz.xenondevs.invui.dsl.by
import xyz.xenondevs.invui.dsl.item
import xyz.xenondevs.invui.dsl.by
import xyz.xenondevs.invui.gui.SlotElementSupplier
import xyz.xenondevs.invui.internal.util.InventoryUtils
import xyz.xenondevs.invui.item.Item
import xyz.xenondevs.invui.item.ItemBuilder
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkConnectionType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.FluidType
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.FluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.container.NetworkedFluidContainer
import xyz.xenondevs.nova.world.block.tileentity.network.type.fluid.holder.FluidHolder
import xyz.xenondevs.nova.world.item.DefaultGuiItems
import xyz.xenondevs.nova.world.item.NovaItem

private val DEFAULT_FLUID_BAR_ITEMS = mapOf(
    null to DefaultGuiItems.BAR_BLUE,
    FluidType.WATER to DefaultGuiItems.BAR_BLUE,
    FluidType.LAVA to DefaultGuiItems.BAR_ORANGE
)

private val DEFAULT_TP_FLUID_BAR_ITEMS = mapOf(
    null to DefaultGuiItems.TP_BAR_BLUE,
    FluidType.WATER to DefaultGuiItems.TP_BAR_BLUE,
    FluidType.LAVA to DefaultGuiItems.TP_BAR_ORANGE
)

/**
 * A [verticalBar] for displaying a fluid level.
 * Ranges from [FluidContainer.amountProvider] to [FluidContainer.capacityProvider],
 * with the bar item determined by [FluidContainer.typeProvider] and [barTypes].
 * Clicking the bar item with a bucket inserts or extracts fluid to and from the [container].
 */
fun fluidBar(
    fluidHolder: FluidHolder,
    container: FluidContainer,
    barTypes: Map<FluidType?, Provider<NovaItem>> = DEFAULT_TP_FLUID_BAR_ITEMS,
    onClick: ClickDsl.() -> Unit = {}
): SlotElementSupplier = fluidBar(
    fluidHolder.containers[container]!!,
    container,
    container.typeProvider,
    container.amountProvider,
    container.capacityProvider,
    barTypes,
    onClick,
)

/**
 * A [verticalBar] for displaying a fluid level.
 * Ranges from [amount] to [capacity], with the bar item determined by [type] and [barTypes].
 * Clicking the bar item with a bucket inserts or extracts fluid to and from the [container].
 */
fun fluidBar(
    allowedConnectionType: NetworkConnectionType,
    container: NetworkedFluidContainer,
    type: Provider<FluidType?>,
    amount: Provider<Long>,
    capacity: Provider<Long>,
    barTypes: Map<FluidType?, Provider<NovaItem>> = DEFAULT_TP_FLUID_BAR_ITEMS,
    onClick: ClickDsl.() -> Unit = {}
): SlotElementSupplier = fluidBar(type, amount, capacity, barTypes) {
    val cursor = player.itemOnCursor.takeUnlessEmpty()
    when (cursor?.type) {
        Material.BUCKET -> if (allowedConnectionType.extract && container.amount >= 1000) {
            val bucket = container.type!!.bucket
            if (cursor.amount > 1) {
                cursor.amount -= 1
                if (player.inventory.addItemCorrectly(bucket) != 0)
                    InventoryUtils.dropItemLikePlayer(player, bucket)
            } else player.setItemOnCursor(bucket)
            container.takeFluid(1000)
        }
        
        Material.WATER_BUCKET -> if (allowedConnectionType.insert && container.accepts(FluidType.WATER, 1000)) {
            player.setItemOnCursor(ItemStack(Material.BUCKET))
            container.addFluid(FluidType.WATER, 1000)
        }
        
        Material.LAVA_BUCKET -> if (allowedConnectionType.insert && container.accepts(FluidType.LAVA, 1000)) {
            player.setItemOnCursor(ItemStack(Material.BUCKET))
            container.addFluid(FluidType.LAVA, 1000)
        }
        
        else -> Unit
    }
    onClick()
}


/**
 * A [verticalBar] for displaying a fluid level.
 * Ranges from [amount] to [capacity], with the bar item determined by [type] and [barTypes].
 */
fun fluidBar(
    type: Provider<FluidType?>,
    amount: Provider<Long>,
    capacity: Provider<Long>,
    barTypes: Map<FluidType?, Provider<NovaItem>> = DEFAULT_TP_FLUID_BAR_ITEMS,
    onClick: ClickDsl.() -> Unit = {}
): SlotElementSupplier = verticalBar(
    percentage = combinedProvider(
        amount, capacity
    ) { amount, capacity -> amount.toDouble() / capacity.toDouble() },
    barType = type.flatMap {
        barTypes[it] ?: throw NoSuchElementException("No gui item for $it")
    },
    modifyItemProvider = {
        name by combinedProvider(amount, capacity) { amount, capacity ->
            when {
                amount == Long.MAX_VALUE -> "∞ mB / ∞ mB"
                capacity == Long.MAX_VALUE -> NumberFormatUtils.getFluidString(amount) + " / ∞ mB"
                else -> NumberFormatUtils.getFluidString(amount, capacity)
            }
        }
    },
    onClick = onClick
) 

private fun ItemBuilder.setFluidDisplayName(amount: Long, capacity: Long): ItemBuilder {
    if (amount == Long.MAX_VALUE) {
        setName("∞ mB / ∞ mB")
    } else if (capacity == Long.MAX_VALUE) {
        setName(NumberFormatUtils.getFluidString(amount) + " / ∞ mB")
    } else {
        setName(NumberFormatUtils.getFluidString(amount, capacity))
    }
    
    return this
}

/**
 * A multi-item gui component for displaying fluid levels.
 */
class FluidBar(
    height: Int,
    fluidHolder: FluidHolder,
    private val fluidContainer: NetworkedFluidContainer,
    private val capacity: Provider<Long>,
    private val type: Provider<FluidType?>,
    private val amount: Provider<Long>,
    private val items: Map<FluidType?, Provider<NovaItem>> = DEFAULT_FLUID_BAR_ITEMS
) : VerticalBar(height) {
    
    private val allowedConnectionType = fluidHolder.containers[fluidContainer]!!
    
    constructor(
        height: Int,
        fluidHolder: FluidHolder,
        container: FluidContainer,
        items: Map<FluidType?, Provider<NovaItem>> = DEFAULT_FLUID_BAR_ITEMS
    ) : this(
        height,
        fluidHolder,
        container,
        container.capacityProvider,
        container.typeProvider,
        container.amountProvider,
        items
    )
    
    @Suppress("DEPRECATION")
    override fun createBarItem(section: Int): Item = item {
        itemProvider by combinedProvider(type, amount, capacity) { type, amount, capacity ->
            items[type]?.map { item ->
                createItemBuilder(item, section, amount.toDouble() / capacity.toDouble())
                    .setFluidDisplayName(amount, capacity)
            } ?: provider(ItemProvider.EMPTY)
        }.flatten()
        
        onClick {
            val cursor = player.itemOnCursor.takeUnlessEmpty()
            when (cursor?.type) {
                Material.BUCKET -> if (allowedConnectionType.extract && fluidContainer.amount >= 1000) {
                    val bucket = fluidContainer.type!!.bucket
                    if (cursor.amount > 1) {
                        cursor.amount -= 1
                        if (player.inventory.addItemCorrectly(bucket) != 0)
                            InventoryUtils.dropItemLikePlayer(player, bucket)
                    } else player.setItemOnCursor(bucket)
                    fluidContainer.takeFluid(1000)
                }
                
                Material.WATER_BUCKET -> if (allowedConnectionType.insert && fluidContainer.accepts(FluidType.WATER, 1000)) {
                    player.setItemOnCursor(ItemStack(Material.BUCKET))
                    fluidContainer.addFluid(FluidType.WATER, 1000)
                }
                
                Material.LAVA_BUCKET -> if (allowedConnectionType.insert && fluidContainer.accepts(FluidType.LAVA, 1000)) {
                    player.setItemOnCursor(ItemStack(Material.BUCKET))
                    fluidContainer.addFluid(FluidType.LAVA, 1000)
                }
                
                else -> Unit
            }
        }
    }
    
}

/**
 * A multi-item gui component for displaying a constant fluid level.
 */
class StaticFluidBar(
    height: Int,
    private val capacity: Long,
    private val type: FluidType,
    private val amount: Long,
    private val items: Map<FluidType?, Provider<NovaItem>> = DEFAULT_FLUID_BAR_ITEMS
) : VerticalBar(height) {
    
    override fun createBarItem(section: Int): Item = item {
        val item = items[type] ?: return@item
        itemProvider by item.map { item ->
            createItemBuilder(item, section, amount.toDouble() / capacity.toDouble())
                .setFluidDisplayName(amount, capacity)
        }
    }
    
}