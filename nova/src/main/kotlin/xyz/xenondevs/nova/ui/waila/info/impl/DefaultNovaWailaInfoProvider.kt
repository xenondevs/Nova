package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.xenondevs.commons.collections.firstInstanceOfOrNull
import xyz.xenondevs.nova.ui.waila.info.NovaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.EnergyHolderLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.DefaultBlocks
import xyz.xenondevs.nova.world.block.NovaTileEntityBlock
import xyz.xenondevs.nova.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.type.energy.holder.DefaultEnergyHolder
import xyz.xenondevs.nova.world.format.WorldDataManager

private val DELEGATES = setOf(
    DefaultBlocks.NOTE_BLOCK, DefaultBlocks.OAK_LEAVES, DefaultBlocks.SPRUCE_LEAVES,
    DefaultBlocks.BIRCH_LEAVES, DefaultBlocks.JUNGLE_LEAVES, DefaultBlocks.ACACIA_LEAVES,
    DefaultBlocks.DARK_OAK_LEAVES, DefaultBlocks.MANGROVE_LEAVES, DefaultBlocks.CHERRY_LEAVES,
    DefaultBlocks.AZALEA_LEAVES, DefaultBlocks.FLOWERING_AZALEA_LEAVES, DefaultBlocks.PALE_OAK_LEAVES,
    DefaultBlocks.TRIPWIRE
)

object DefaultNovaWailaInfoProvider : NovaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, pos: BlockPos, blockState: NovaBlockState): WailaInfo {
        if (blockState.block in DELEGATES)
            return DefaultVanillaWailaInfoProvider.getInfo(player, pos, pos.block.blockData)
        
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