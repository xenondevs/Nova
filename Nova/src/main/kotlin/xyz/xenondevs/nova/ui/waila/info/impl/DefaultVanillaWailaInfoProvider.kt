package xyz.xenondevs.nova.ui.waila.info.impl

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.TranslatableComponent
import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.block.data.Powerable
import org.bukkit.block.data.type.PistonHead
import org.bukkit.block.data.type.TechnicalPiston
import org.bukkit.entity.Player
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.RedstonePowerLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine
import xyz.xenondevs.nova.util.data.ComponentWidthBuilder
import xyz.xenondevs.nova.util.item.localizedName

internal object DefaultVanillaWailaInfoProvider : VanillaWailaInfoProvider(null) {
    
    override fun getInfo(player: Player, block: Block): WailaInfo {
        val material = block.type
        val mainMaterial = getMainMaterial(block)
        
        val translate = TranslatableComponent(mainMaterial.localizedName ?: "block.minecraft.${mainMaterial.name.lowercase()}")
        translate.color = ChatColor.WHITE
        
        val lines = ArrayList<WailaLine>()
        lines += WailaLine(ComponentBuilder().append(translate).color(ChatColor.WHITE).create(), player, WailaLine.Alignment.CENTERED)
        lines += WailaLine(ComponentBuilder("minecraft:${material.name.lowercase()}").color(ChatColor.DARK_GRAY).create(), player, WailaLine.Alignment.CENTERED)
        lines += ToolLine.getToolLine(player, block)
        if (block.blockData is Powerable) {
            lines += WailaLine(ComponentWidthBuilder(player.locale).append("").create(), WailaLine.Alignment.CENTERED) // empty line
            lines += RedstonePowerLine.getRedstonePowerLine(player, block)
        }
        
        return WailaInfo(NamespacedId("minecraft", mainMaterial.name.lowercase()), lines)
    }
    
    private fun getMainMaterial(block: Block): Material {
        return when (val material = block.type) {
            // signs
            Material.OAK_WALL_SIGN -> Material.OAK_SIGN
            Material.SPRUCE_WALL_SIGN -> Material.SPRUCE_SIGN
            Material.BIRCH_WALL_SIGN -> Material.BIRCH_SIGN
            Material.JUNGLE_WALL_SIGN -> Material.JUNGLE_SIGN
            Material.ACACIA_WALL_SIGN -> Material.ACACIA_SIGN
            Material.DARK_OAK_WALL_SIGN -> Material.DARK_OAK_SIGN
            Material.MANGROVE_WALL_SIGN -> Material.MANGROVE_SIGN
            Material.CRIMSON_WALL_SIGN -> Material.CRIMSON_SIGN
            Material.WARPED_WALL_SIGN -> Material.WARPED_SIGN
            
            // plant
            Material.WEEPING_VINES_PLANT -> Material.WEEPING_VINES
            Material.TWISTING_VINES_PLANT -> Material.TWISTING_VINES
            
            // torch
            Material.WALL_TORCH -> Material.TORCH
            Material.REDSTONE_WALL_TORCH -> Material.TORCH
            Material.SOUL_WALL_TORCH -> Material.TORCH
            
            // misc
            Material.BIG_DRIPLEAF_STEM -> Material.BIG_DRIPLEAF
            Material.TRIPWIRE -> Material.STRING
            Material.PISTON_HEAD -> {
                val data = block.blockData as PistonHead
                if (data.type == TechnicalPiston.Type.STICKY)
                    Material.STICKY_PISTON
                else Material.PISTON
            }
            
            else -> material
        }
    }
    
}