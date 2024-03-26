package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.WorldDataManager

object DefaultNovaWailaInfoProvider : NovaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, pos: BlockPos, block: NovaBlockState): WailaInfo {
        val blockType = block.block
        var id = blockType.id
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(blockType.name, WailaLine.Alignment.CENTERED)
        lines += WailaLine(Component.text(id.toString(), NamedTextColor.DARK_GRAY), WailaLine.Alignment.CENTERED)
        lines += ToolLine.getToolLine(player, pos.block)
        
        if (block.block is NovaTileEntityBlock) {
            val tileEntity = WorldDataManager.getTileEntity(pos)
            if (tileEntity is NetworkedTileEntity) {
                // TODO
//                val energyHolder = tileEntity.holders[DefaultNetworkTypes.ENERGY] as? NovaEnergyHolder
//                if (energyHolder != null && (energyHolder !is BufferEnergyHolder || !energyHolder.infiniteEnergy)) {
//                    lines += EnergyHolderLine.getEnergyBarLine(energyHolder)
//                    lines += EnergyHolderLine.getEnergyAmountLine(energyHolder)
//                    lines += EnergyHolderLine.getEnergyDeltaLine(energyHolder)
//                }
            }
        }
        
        // TODO
//        val subId = block.modelProvider.currentSubId
//        if (subId > 0) {
//            val subIdTexture = ResourceLocation(id.namespace, "${id.name}_$subId")
//            if (subIdTexture in ResourceLookups.WAILA_DATA_LOOKUP)
//                id = subIdTexture
//        }
        
        return WailaInfo(id, lines)
    }
    
}