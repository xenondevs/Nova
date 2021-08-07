package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.debug.NetworkDebugger
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.ui.menu.CreativeMenu
import xyz.xenondevs.nova.util.coloredText
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.localized


object NovaCommand : PlayerCommand("nova") {
    
    init {
        builder = builder
            .then(literal("give")
                .requiresPermission("nova.creative")
                .apply {
                    NovaMaterial.values().forEach { material ->
                        then(literal(material.name)
                            .executesCatching { handleGive(material, it) }
                        )
                    }
                })
            .then(literal("debug")
                .requiresPermission("nova.debug")
                .then(literal("removeTileEntities")
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching { removeTileEntities(it) }))
                .then(literal("energyNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ENERGY, it) })
                .then(literal("itemNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ITEMS, it) }))
            .then(literal("inventory")
                .requiresPermission("nova.creative")
                .executesCatching { openCreativeInventory(it) })
    }
    
    private fun handleGive(material: NovaMaterial, context: CommandContext<CommandSourceStack>) {
        val player = context.player
        player.inventory.addItem(material.createItemStack())
        val itemName = material.itemName.ifBlank { material.name }
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.give.success",
            localized(ChatColor.AQUA, itemName)
        ))
    }
    
    private fun removeTileEntities(context: CommandContext<CommandSourceStack>) {
        val player = context.player
        val chunks = player.location.chunk.getSurroundingChunks(context["range"], true)
        val tileEntities = chunks.flatMap { TileEntityManager.getTileEntitiesInChunk(it) }
        tileEntities.forEach { TileEntityManager.destroyTileEntity(it, false) }
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.remove_tile_entities.success",
            coloredText(ChatColor.AQUA, tileEntities.count())
        ))
    }
    
    private fun toggleNetworkDebugging(type: NetworkType, context: CommandContext<CommandSourceStack>) {
        val player = context.player
        NetworkDebugger.toggleDebugger(type, player)
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.network_debug." + type.name.lowercase()
        ))
    }
    
    private fun openCreativeInventory(context: CommandContext<CommandSourceStack>) {
        CreativeMenu.getWindow(context.player).show()
    }
    
}

