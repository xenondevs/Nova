package xyz.xenondevs.nova.ui.waila

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.GameMode
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.registry.NovaRegistrar.wailaToolIconProvider
import xyz.xenondevs.nova.registry.RegistryEntrySet
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.registry.tags.BlockTypeTags
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.world.block.blockType

@InternalInit(
    stage = BOOTSTRAP,
    runBefore = [RegistryLoader::class]
)
object DefaultWailaToolIconProviders {
    
    val DEFAULT = wailaToolIconProvider("default") {
        iconProvider { tags ->
            buildSet {
                if (BlockTypeTags.SHEARS_MINOR_BREAKING_SPEED in tags || BlockTypeTags.SHEARS_MAJOR_BREAKING_SPEED in tags || BlockTypeTags.SHEARS_EXTREME_BREAKING_SPEED in tags)
                    add(Key.key("item/shears"))
                if (BlockTypeTags.SWORD_EFFICIENT in tags || BlockTypeTags.SWORD_INSTANTLY_MINES in tags)
                    add(Key.key("item/wooden_sword"))
                
                fun toolType(name: String, typeTag: RegistryEntrySet.Paper.Tag<BlockType>) {
                    if (typeTag !in tags)
                        return
                    when {
                        BlockTypeTags.NEEDS_STONE_TOOL in tags -> add(Key.key("item/stone_$name"))
                        BlockTypeTags.NEEDS_IRON_TOOL in tags -> add(Key.key("item/iron_$name"))
                        BlockTypeTags.NEEDS_DIAMOND_TOOL in tags -> add(Key.key("item/diamond_$name"))
                        BlockTypeTags.INCORRECT_FOR_WOODEN_TOOL !in tags -> add(Key.key("item/wooden_$name"))
                        else -> Unit
                    }
                }
                
                toolType("shovel", BlockTypeTags.MINEABLE_SHOVEL)
                toolType("pickaxe", BlockTypeTags.MINEABLE_PICKAXE)
                toolType("axe", BlockTypeTags.MINEABLE_AXE)
                toolType("hoe", BlockTypeTags.MINEABLE_HOE)
            }
            
        }
    }
    
}

private val CHECK_MARK = Component.text("✔", NamedTextColor.GREEN)
private val CROSS = Component.text("❌", NamedTextColor.RED)

internal fun getToolText(player: Player, block: Block): Component {
    val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
    return getToolText(
        player,
        WailaToolIconProvider.getToolIcons(block.blockType),
        block.blockType.hardness.toDouble(),
        block.isPreferredTool(tool ?: ItemStack.empty())
    )
}

internal fun getCustomItemServiceToolText(player: Player, block: Block): Component {
    val tool = player.inventory.itemInMainHand.takeUnlessEmpty()
    return getToolText(
        player,
        emptyList(),
        1.0,
        CustomItemServiceManager.canBreakBlock(block, tool)
    )
}

private fun getToolText(
    player: Player,
    toolIcons: List<Component>,
    hardness: Double,
    correctToolForDrops: Boolean?
): Component {
    val builder = Component.text()
    
    if (hardness < 0)
        return builder.append(CROSS).build()
    
    if (player.gameMode == GameMode.CREATIVE || correctToolForDrops == true) {
        builder.append(CHECK_MARK)
    } else if (correctToolForDrops != null) {
        builder.append(CROSS)
    }
    
    toolIcons.forEach(builder::append)
    
    return builder.build()
}
