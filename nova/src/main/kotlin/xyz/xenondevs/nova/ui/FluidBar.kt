package xyz.xenondevs.nova.ui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.util.InventoryUtils
import xyz.xenondevs.nova.material.CoreGuiMaterial
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.container.FluidContainer
import xyz.xenondevs.nova.tileentity.network.fluid.holder.FluidHolder
import xyz.xenondevs.nova.util.NumberFormatUtils
import xyz.xenondevs.nova.util.addItemCorrectly

class FluidBar(
    height: Int,
    fluidHolder: FluidHolder,
    private val fluidContainer: FluidContainer
) : VerticalBar(height) {
    
    private val allowedConnectionType = fluidHolder.allowedConnectionTypes[fluidContainer]!!
    
    override val barMaterial: ItemNovaMaterial
        get() = when (fluidContainer.type) {
            FluidType.WATER -> CoreGuiMaterial.BAR_BLUE
            else -> CoreGuiMaterial.BAR_ORANGE
        }
    
    private var amount = 0L
    private var capacity = 0L
    
    init {
        fluidContainer.updateHandlers += ::update
        update()
    }
    
    fun update() {
        amount = fluidContainer.amount
        capacity = fluidContainer.capacity
        percentage = (amount.toDouble() / capacity.toDouble()).coerceIn(0.0, 1.0)
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        if (amount == Long.MAX_VALUE) {
            itemBuilder.setDisplayName("∞ mB / ∞ mB")
        } else {
            if (capacity == Long.MAX_VALUE) itemBuilder.setDisplayName(NumberFormatUtils.getFluidString(amount) + " / ∞ mB")
            else itemBuilder.setDisplayName(NumberFormatUtils.getFluidString(amount, capacity))
        }
        return itemBuilder
    }
    
    override fun createBarItem(section: Int, totalSections: Int) = FluidBarItem(section, totalSections) as VerticalBarItem
    
    private inner class FluidBarItem(section: Int, totalSections: Int) : VerticalBarItem(section, totalSections) {
        
        @Suppress("DEPRECATION")
        override fun handleClick(clickType: ClickType, player: Player, event: InventoryClickEvent) {
            val cursor = event.cursor?.takeUnless { it.type.isAir }
            when (cursor?.type) {
                Material.BUCKET -> if (allowedConnectionType.extract && fluidContainer.amount >= 1000) {
                    val bucket = fluidContainer.type!!.bucket!!
                    if (cursor.amount > 1) {
                        event.cursor!!.amount -= 1
                        if (player.inventory.addItemCorrectly(bucket) != 0)
                            InventoryUtils.dropItemLikePlayer(player, bucket)
                    } else event.cursor = bucket
                    fluidContainer.takeFluid(1000)
                }
                
                Material.WATER_BUCKET -> if (allowedConnectionType.insert && fluidContainer.accepts(FluidType.WATER, 1000)) {
                    event.cursor = ItemStack(Material.BUCKET)
                    fluidContainer.addFluid(FluidType.WATER, 1000)
                }
                
                Material.LAVA_BUCKET -> if (allowedConnectionType.insert && fluidContainer.accepts(FluidType.LAVA, 1000)) {
                    event.cursor = ItemStack(Material.BUCKET)
                    fluidContainer.addFluid(FluidType.LAVA, 1000)
                }
                
                else -> Unit
            }
        }
        
    }
    
}

class StaticFluidBar(
    private val type: FluidType,
    private val amount: Long,
    private val capacity: Long,
    height: Int
) : VerticalBar(height) {
    
    override val barMaterial: ItemNovaMaterial
        get() = when (type) {
            FluidType.WATER -> CoreGuiMaterial.TP_BAR_BLUE
            else -> CoreGuiMaterial.TP_BAR_ORANGE
        }
    
    init {
        percentage = amount / capacity.toDouble()
    }
    
    override fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        if (amount == Long.MAX_VALUE) {
            itemBuilder.setDisplayName("∞ mB / ∞ mB")
        } else {
            if (capacity == Long.MAX_VALUE) itemBuilder.setDisplayName(NumberFormatUtils.getFluidString(amount) + " / ∞ mB")
            else itemBuilder.setDisplayName(NumberFormatUtils.getFluidString(amount, capacity))
        }
        return itemBuilder
    }
    
}