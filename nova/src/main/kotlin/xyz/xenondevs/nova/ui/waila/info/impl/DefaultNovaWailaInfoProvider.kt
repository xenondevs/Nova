package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.resources.ResourceLocation
import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.EnergyHolderLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine
import xyz.xenondevs.nova.util.name
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.format.WorldDataManager

object DefaultNovaWailaInfoProvider : NovaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: NovaBlockState): WailaInfo {
        val blockType = blockState.block
        val id = blockType.id
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(blockType.name, WailaLine.Alignment.CENTERED)
        lines += WailaLine(Component.text(id.toString(), NamedTextColor.DARK_GRAY), WailaLine.Alignment.CENTERED)
        lines += ToolLine.getToolLine(player, pos.block)
        
        if (blockState.block is NovaTileEntityBlock) {
            val tileEntity = WorldDataManager.getTileEntity(pos)
            if (tileEntity is NetworkedTileEntity) {
                val energyHolder = tileEntity.holders.firstInstanceOfOrNull<DefaultEnergyHolder>()
                if (energyHolder != null) {
                    lines += EnergyHolderLine.getEnergyBarLine(energyHolder)
                    lines += EnergyHolderLine.getEnergyAmountLine(energyHolder)
                    lines += EnergyHolderLine.getEnergyDeltaLine(energyHolder)
                }
            }
        }
        
        // TODO waila icon by block state
        
        return WailaInfo(id, lines)
    }
    
}