package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkType
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
        
        val translate = TranslatableComponent(material.localizedName)
        translate.color = ChatColor.WHITE
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(ComponentBuilder().append(translate).color(ChatColor.WHITE).create(), player, WailaLine.Alignment.CENTERED)
        lines += WailaLine(ComponentBuilder(material.id.toString()).color(ChatColor.DARK_GRAY).create(), player, WailaLine.Alignment.CENTERED)
        lines += ToolLine.getToolLine(player, material)
        
        if (block is NovaTileEntityState && block.isInitialized) {
            val tileEntity = block.tileEntity
            if (tileEntity is NetworkedTileEntity) {
                val energyHolder = tileEntity.holders[NetworkType.ENERGY] as? NovaEnergyHolder      
                if (energyHolder != null && (energyHolder !is BufferEnergyHolder || !energyHolder.creative)) {
                    lines += EnergyHolderLine.getEnergyBarLine(player, energyHolder)
                    lines += EnergyHolderLine.getEnergyAmountLine(player, energyHolder)
                    lines += EnergyHolderLine.getEnergyDeltaLine(player, energyHolder)
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