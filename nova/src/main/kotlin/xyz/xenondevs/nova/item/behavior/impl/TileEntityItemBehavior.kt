package xyz.xenondevs.nova.item.behavior.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.data.serialization.cbf.NamespacedCompound
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.logic.PacketItemData
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.util.NumberFormatUtils

internal class TileEntityItemBehavior : ItemBehavior() {
    
    override fun updatePacketItemData(data: NamespacedCompound, itemData: PacketItemData) {
        val tileEntityData: Compound? = data[TileEntity.TILE_ENTITY_DATA_KEY]
        
        if (tileEntityData != null) {
            val lore = ArrayList<Component>()
            
            val energy = tileEntityData.get<Long>("energy")
            if (energy != null) {
                lore += Component.text(NumberFormatUtils.getEnergyString(energy), NamedTextColor.GRAY)
            }
            
            tileEntityData.keys.forEach { key ->
                if (key.startsWith("fluidContainer.")) {
                    val fluidData = tileEntityData.get<Compound>(key)!!
                    val amount = fluidData.get<Long>("amount")!!
                    val type = fluidData.get<FluidType>("type")
                    
                    if (type != null) {
                        val amountStr = if (amount != Long.MAX_VALUE)
                            NumberFormatUtils.getFluidString(amount) + " mB"
                        else "∞ mB"
                        
                        lore += Component.text()
                            .color(NamedTextColor.GRAY)
                            .append(Component.translatable(type.localizedName))
                            .append(Component.text(": $amountStr"))
                            .build()
                    }
                }
            }
            
            itemData.addLore(lore)
        }
    }
    
}