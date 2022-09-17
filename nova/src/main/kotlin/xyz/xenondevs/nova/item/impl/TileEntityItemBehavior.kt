package xyz.xenondevs.nova.item.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.serialization.persistentdata.get
import xyz.xenondevs.nova.item.ItemDisplayData
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.NumberFormatUtils

internal object TileEntityItemBehavior : ItemBehavior() {
    
    override fun updateItemDisplay(itemStack: ItemStack, display: ItemDisplayData) {
        val data: Compound? = itemStack.itemMeta?.persistentDataContainer?.get(TileEntity.TILE_ENTITY_KEY)
        
        if (data != null) {
            val lore = ArrayList<Array<BaseComponent>>()
            
            val energy = data.get<Long>("energy")
            if (energy != null) {
                lore += TextComponent.fromLegacyText("§7" + NumberFormatUtils.getEnergyString(energy))
            }
            
            data.keys.forEach { key ->
                if (key.startsWith("fluidContainer.")) {
                    val fluidData = data.get<Compound>(key)!!
                    val amount = fluidData.get<Long>("amount")!!
                    val type = fluidData.get<FluidType?>("type")
                    
                    if (type != null) {
                        val amountStr = if (amount != Long.MAX_VALUE)
                            NumberFormatUtils.getFluidString(amount) + " mB"
                        else "∞ mB"
                        
                        lore += ComponentBuilder()
                            .color(ChatColor.GRAY)
                            .append(TranslatableComponent(type.localizedName))
                            .append(": $amountStr")
                            .create()
                    }
                }
            }
            
            display.addLore(lore)
        }
    }
    
}