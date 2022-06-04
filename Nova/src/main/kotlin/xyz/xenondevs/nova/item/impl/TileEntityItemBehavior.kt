package xyz.xenondevs.nova.item.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.item.LoreContext
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.util.NumberFormatUtils

internal class TileEntityContext(val tileEntity: TileEntity) : LoreContext

internal object TileEntityItemBehavior : ItemBehavior() {
    
    override fun getLore(itemStack: ItemStack, context: LoreContext?): List<Array<BaseComponent>> {
        if (context is TileEntityContext) {
            val tileEntity = context.tileEntity
            if (tileEntity is NetworkedTileEntity) {
                val lore = ArrayList<Array<BaseComponent>>()
                
                val energyHolder = tileEntity.holders[NetworkType.ENERGY] as? EnergyHolder
                val fluidHolder = tileEntity.holders[NetworkType.FLUID] as? NovaFluidHolder
                
                if (energyHolder != null) {
                    val energy = energyHolder.energy
                    lore += TextComponent.fromLegacyText("§7" + NumberFormatUtils.getEnergyString(energy))
                }
                
                fluidHolder?.availableContainers?.values?.forEach { container ->
                    if (container.hasFluid()) {
                        val amount = container.amount
                        val capacity = container.capacity
                        
                        val amountStr = if (amount != Long.MAX_VALUE) {
                            if (capacity == Long.MAX_VALUE) NumberFormatUtils.getFluidString(amount) + " / ∞ mB"
                            else NumberFormatUtils.getFluidString(amount, capacity)
                        } else "∞ mB / ∞ mB"
                        
                        lore += ComponentBuilder()
                            .color(ChatColor.GRAY)
                            .append(TranslatableComponent(container.type!!.localizedName))
                            .append(": $amountStr")
                            .create()
                    }
                }
                
                return lore
            }
        }
        
        return emptyList()
    }
    
}