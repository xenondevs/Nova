package xyz.xenondevs.nova.item.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.TranslatableComponent
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.PacketItemData
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.NumberFormatUtils

internal class TileEntityItemBehavior : ItemBehavior() {
    
    override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
        val tileEntityData: Compound? = data[TileEntity.TILE_ENTITY_DATA_KEY]
        
        if (tileEntityData != null) {
            val lore = ArrayList<Array<BaseComponent>>()
            
            val energy = tileEntityData.get<Long>("energy")
            if (energy != null) {
                lore += TextComponent.fromLegacyText("§7" + NumberFormatUtils.getEnergyString(energy))
            }
            
            tileEntityData.keys.forEach { key ->
                if (key.startsWith("fluidContainer.")) {
                    val fluidData = tileEntityData.get<Compound>(key)!!
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
            
            itemData.addLore(lore)
        }
    }
    
}