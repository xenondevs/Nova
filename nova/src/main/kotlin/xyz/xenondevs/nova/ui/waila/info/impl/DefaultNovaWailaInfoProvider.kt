package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.DefaultNetworkTypes
import xyz.xenondevs.nova.tileentity.network.energy.holder.BufferEnergyHolder
import xyz.xenondevs.nova.tileentity.network.energy.holder.NovaEnergyHolder
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.EnergyHolderLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine

object DefaultNovaWailaInfoProvider : NovaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, block: NovaBlockState): WailaInfo {
        val material = block.material
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(Component.translatable(material.localizedName), WailaLine.Alignment.CENTERED)
        lines += WailaLine(Component.text(material.id.toString(), NamedTextColor.DARK_GRAY), WailaLine.Alignment.CENTERED)
        lines += ToolLine.getToolLine(player, block.pos.block)
        
        if (block is NovaTileEntityState) {
            val tileEntity = block.tileEntity
            if (tileEntity is NetworkedTileEntity) {
                val energyHolder = tileEntity.holders[DefaultNetworkTypes.ENERGY] as? NovaEnergyHolder
                if (energyHolder != null && (energyHolder !is BufferEnergyHolder || !energyHolder.infiniteEnergy)) {
                    lines += EnergyHolderLine.getEnergyBarLine(energyHolder)
                    lines += EnergyHolderLine.getEnergyAmountLine(energyHolder)
                    lines += EnergyHolderLine.getEnergyDeltaLine(energyHolder)
                }
            }
        }
        
        var id = material.id
        val subId = block.modelProvider.currentSubId
        if (subId > 0) {
            val subIdTexture = NamespacedId(id.namespace, "${id.name}_$subId")
            if (Resources.getWailaIconCharOrNull(subIdTexture) != null)
                id = subIdTexture
        }
        
        return WailaInfo(id, lines)
    }
    
}