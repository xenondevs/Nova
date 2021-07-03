package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.debug.NetworkDebugger
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.getMultiModelParent
import xyz.xenondevs.nova.tileentity.isMultiModel
import xyz.xenondevs.nova.ui.menu.CreativeMenu
import xyz.xenondevs.nova.ui.menu.RecipesMenu
import xyz.xenondevs.nova.util.*

object NovaCommand : PlayerCommand("nova") {
    
    init {
        builder = builder
            .then(literal("give")
                .requiresPermission("nova.creative")
                .apply {
                    NovaMaterial.values().forEach { material ->
                        then(literal(material.name)
                            .executesCatching { context -> handleGive(material, context) }
                        )
                    }
                })
            .then(literal("debug")
                .requiresPermission("nova.debug")
                .then(literal("listNearby")
                    .executesCatching { listNearby(it) })
                .then(literal("getNearestData")
                    .executesCatching { getNearestData(it) })
                .then(literal("removeObsoleteModels")
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching { removeObsoleteModels(it) }))
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
            .then(literal("recipes")
                .executesCatching { openRecipesMenu(it) })
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
    
    private fun listNearby(context: CommandContext<CommandSourceStack>) {
        val player = context.player
        val chunk = player.location.chunk
        val armorStands = chunk.entities.filterIsInstance<ArmorStand>()
        val tileEntityArmorStands = armorStands.filter { it.persistentDataContainer.hasNovaData() }
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.list_nearby.success",
            coloredText(ChatColor.AQUA, armorStands.count()),
            coloredText(ChatColor.AQUA, tileEntityArmorStands.count())
        ))
    }
    
    private fun getNearestData(context: CommandContext<CommandSourceStack>) {
        val player = context.player
        val chunks = player.location.chunk.getSurroundingChunks(1, true)
        val armorStands = chunks
            .flatMap { it.entities.toList() }
            .filterIsInstance<ArmorStand>()
            .filter { it.persistentDataContainer.hasNovaData() }
        val armorStand = armorStands.minByOrNull { it.location.distance(player.location) }
        if (armorStand != null) player.chat("/data get entity ${armorStand.uniqueId}")
        else player.spigot().sendMessage(localized(ChatColor.GRAY, "command.nova.get_nearest_data.failed"))
    }
    
    private fun removeObsoleteModels(context: CommandContext<CommandSourceStack>) {
        val player = context.player
        val chunks = player.location.chunk.getSurroundingChunks(context["range"], true)
        val obsoleteModels = chunks
            .flatMap { it.entities.toList() }
            .filterIsInstance<ArmorStand>()
            .filter {
                (it.isMultiModel() && it.getMultiModelParent() == null) ||
                    (!it.isMultiModel()
                        && TileEntityManager.getTileEntityAt(it.location.blockLocation) == null
                        && it.equipment?.helmet?.novaMaterial != null)
            }
        obsoleteModels.forEach(ArmorStand::remove)
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.remove_obsolete_models.success",
            coloredText(ChatColor.AQUA, obsoleteModels.count())
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
    
    private fun openRecipesMenu(context: CommandContext<CommandSourceStack>) {
        RecipesMenu.open(context.player)
    }
    
}

