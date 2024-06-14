package xyz.xenondevs.nova.ui.waila.info.impl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.block.data.BlockData
import org.bukkit.block.data.type.PistonHead
import org.bukkit.block.data.type.TechnicalPiston
import org.bukkit.entity.Player
import xyz.xenondevs.nova.ui.waila.info.VanillaWailaInfoProvider
import xyz.xenondevs.nova.ui.waila.info.WailaInfo
import xyz.xenondevs.nova.ui.waila.info.WailaLine
import xyz.xenondevs.nova.ui.waila.info.line.ToolLine
import xyz.xenondevs.nova.util.item.localizedName
import xyz.xenondevs.nova.world.BlockPos

object DefaultVanillaWailaInfoProvider : VanillaWailaInfoProvider<BlockData>(null) {

    override fun getInfo(player: Player, pos: BlockPos, blockState: BlockData): WailaInfo {
        val mainMaterial = getMainMaterial(blockState)

        val lines = buildList {
            this += WailaLine(
                Component.translatable(
                    mainMaterial.localizedName
                        ?: "block.minecraft.${mainMaterial.name.lowercase()}"
                ),
                WailaLine.Alignment.CENTERED
            )

            this += WailaLine(
                Component.text(
                    "minecraft:${blockState.material.name.lowercase()}",
                    NamedTextColor.DARK_GRAY
                ),
                WailaLine.Alignment.CENTERED
            )
            this += ToolLine.getToolLine(player, pos.block)
        }
        
        return WailaInfo(ResourceLocation("minecraft", mainMaterial.name.lowercase()), lines)
    }

    private fun getMainMaterial(blockState: BlockData): Material {
        return when (val material = blockState.material) {
            // infested blocks
            Material.INFESTED_CHISELED_STONE_BRICKS -> Material.CHISELED_STONE_BRICKS
            Material.INFESTED_COBBLESTONE -> Material.COBBLESTONE
            Material.INFESTED_CRACKED_STONE_BRICKS -> Material.STONE_BRICKS
            Material.INFESTED_DEEPSLATE -> Material.DEEPSLATE
            Material.INFESTED_MOSSY_STONE_BRICKS -> Material.MOSSY_STONE_BRICKS
            Material.INFESTED_STONE -> Material.STONE
            Material.INFESTED_STONE_BRICKS -> Material.STONE_BRICKS

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
            Material.BAMBOO_WALL_SIGN -> Material.BAMBOO_SIGN
            Material.CHERRY_WALL_SIGN -> Material.CHERRY_SIGN
            Material.OAK_WALL_HANGING_SIGN -> Material.OAK_HANGING_SIGN
            Material.SPRUCE_WALL_HANGING_SIGN -> Material.SPRUCE_HANGING_SIGN
            Material.BIRCH_WALL_HANGING_SIGN -> Material.BIRCH_HANGING_SIGN
            Material.JUNGLE_WALL_HANGING_SIGN -> Material.JUNGLE_HANGING_SIGN
            Material.ACACIA_WALL_HANGING_SIGN -> Material.ACACIA_HANGING_SIGN
            Material.DARK_OAK_WALL_HANGING_SIGN -> Material.DARK_OAK_HANGING_SIGN
            Material.MANGROVE_WALL_HANGING_SIGN -> Material.MANGROVE_HANGING_SIGN
            Material.CRIMSON_WALL_HANGING_SIGN -> Material.CRIMSON_HANGING_SIGN
            Material.WARPED_WALL_HANGING_SIGN -> Material.WARPED_HANGING_SIGN
            Material.BAMBOO_WALL_HANGING_SIGN -> Material.BAMBOO_HANGING_SIGN
            Material.CHERRY_WALL_HANGING_SIGN -> Material.CHERRY_HANGING_SIGN

            // plant
            Material.WEEPING_VINES_PLANT -> Material.WEEPING_VINES
            Material.TWISTING_VINES_PLANT -> Material.TWISTING_VINES
            Material.KELP_PLANT -> Material.KELP
            Material.ATTACHED_MELON_STEM -> Material.MELON_STEM
            Material.ATTACHED_PUMPKIN_STEM -> Material.PUMPKIN_STEM

            // torch
            Material.WALL_TORCH -> Material.TORCH
            Material.REDSTONE_WALL_TORCH -> Material.TORCH
            Material.SOUL_WALL_TORCH -> Material.TORCH

            // head / skull
            Material.ZOMBIE_WALL_HEAD -> Material.ZOMBIE_HEAD
            Material.CREEPER_WALL_HEAD -> Material.CREEPER_HEAD
            Material.PLAYER_WALL_HEAD -> Material.PLAYER_HEAD
            Material.SKELETON_WALL_SKULL -> Material.SKELETON_SKULL
            Material.WITHER_SKELETON_WALL_SKULL -> Material.WITHER_SKELETON_SKULL
            Material.DRAGON_WALL_HEAD -> Material.DRAGON_HEAD

            // misc
            Material.BIG_DRIPLEAF_STEM -> Material.BIG_DRIPLEAF
            Material.TRIPWIRE -> Material.STRING
            Material.PISTON_HEAD -> {
                blockState as PistonHead
                if (blockState.type == TechnicalPiston.Type.STICKY)
                    Material.STICKY_PISTON
                else Material.PISTON
            }

            else -> material
        }
    }

}