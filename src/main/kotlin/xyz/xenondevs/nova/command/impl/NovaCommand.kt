package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.minecraft.commands.CommandSourceStack
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.command.PlayerCommand
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.get
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.debug.NetworkDebugger
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.NetworkType
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.getMultiModelParent
import xyz.xenondevs.nova.tileentity.isMultiModel
import xyz.xenondevs.nova.ui.menu.CreativeMenu
import xyz.xenondevs.nova.ui.menu.RecipesMenu
import xyz.xenondevs.nova.util.capitalize
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.hasNovaData

class NovaCommand(name: String, permission: String) : PlayerCommand(name, permission) {
    
    init {
        builder = builder
            .then(literal("give")
                .apply {
                    NovaMaterial.values().forEach { material ->
                        then(literal(material.name)
                            .executesCatching { context -> handleGive(material, context) }
                        )
                    }
                })
            .then(literal("debug")
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
                .executesCatching { openCreativeInventory(it) })
            .then(literal("recipes")
                .executesCatching { openRecipesMenu(it) })
    }
    
    private fun handleGive(material: NovaMaterial, context: CommandContext<CommandSourceStack>) {
        val player = context.player
        player.inventory.addItem(material.createItemStack())
        val itemName = material.itemName.ifBlank { material.name }
        player.sendMessage("§7The item §b$itemName§7 has been added to your inventory.")
    }
    
    private fun listNearby(context: CommandContext<CommandSourceStack>) {
        val player = context.player
        val chunk = player.location.chunk
        val armorStands = chunk.entities.filterIsInstance<ArmorStand>()
        val tileEntityArmorStands = armorStands.filter { it.persistentDataContainer.hasNovaData() }
        
        player.sendMessage("§7Out of the §b${armorStands.count()}§7 ArmorStands in your chunk, §b${tileEntityArmorStands.count()}§7 are part of a TileEntity.")
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
        else player.sendMessage("§cCould not find a TileEntity nearby.")
    }
    
    private fun removeObsoleteModels(context: CommandContext<CommandSourceStack>) {
        val player = context.player
        val chunks = player.location.chunk.getSurroundingChunks(context["range"], true)
        val obsoleteModels = chunks
            .flatMap { it.entities.toList() }
            .filterIsInstance<ArmorStand>()
            .filter { it.isMultiModel() && it.getMultiModelParent() == null }
        obsoleteModels.forEach(ArmorStand::remove)
        player.sendMessage("§7Removed §b${obsoleteModels.count()} §7Armor Stands.")
    }
    
    private fun removeTileEntities(context: CommandContext<CommandSourceStack>) {
        val player = context.player
        val chunks = player.location.chunk.getSurroundingChunks(context["range"], true)
        val tileEntities = chunks.flatMap { TileEntityManager.getTileEntitiesInChunk(it) }
        tileEntities.forEach { TileEntityManager.destroyTileEntity(it, false) }
        player.sendMessage("§7Removed §b${tileEntities.count()} §7Tile Entities.")
    }
    
    private fun toggleNetworkDebugging(type: NetworkType, context: CommandContext<CommandSourceStack>) {
        val player = context.player
        NetworkDebugger.toggleDebugger(type, player)
        player.sendMessage("§7Toggled debug-view for §b${type.name.lowercase().capitalize()}-Networks")
    }
    
    private fun openCreativeInventory(context: CommandContext<CommandSourceStack>) {
        CreativeMenu.getWindow(context.player).show()
    }
    
    private fun openRecipesMenu(context: CommandContext<CommandSourceStack>) {
        RecipesMenu.open(context.player)
    }
    
}

