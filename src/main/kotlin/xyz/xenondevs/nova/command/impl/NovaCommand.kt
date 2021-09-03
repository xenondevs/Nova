package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkDebugger
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.menu.item.creative.ItemsWindow
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager.MAX_RENDER_DISTANCE
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager.MIN_RENDER_DISTANCE
import xyz.xenondevs.nova.world.armorstand.armorStandRenderDistance


object NovaCommand : PlayerCommand("nova") {
    
    init {
        builder = builder
            .then(literal("give")
                .requiresPermission("nova.creative")
                .apply {
                    NovaMaterialRegistry.sortedObtainables.forEach { material ->
                        then(literal(material.typeName)
                            .executesCatching { handleGive(material, it) }
                        )
                    }
                })
            .then(literal("debug")
                .requiresPermission("nova.debug")
                .then(literal("removeTileEntities")
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching { removeTileEntities(it) }))
                .then(literal("getTileEntityData")
                    .executesCatching { showTileEntityData(it) })
                .then(literal("energyNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ENERGY, it) })
                .then(literal("itemNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ITEMS, it) }))
            .then(literal("items")
                .requiresPermission("nova.items")
                .executesCatching { openItemInventory(it) })
            .then(literal("renderDistance")
                .requiresPermission("nova.armor_stand_render_distance")
                .then(argument("distance", IntegerArgumentType.integer(MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE))
                    .executesCatching { setRenderDistance(it) }))
    }
    
    private fun handleGive(material: NovaMaterial, ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        player.inventory.addItem(material.createItemStack())
        val itemName = material.localizedName.ifBlank { material.typeName }
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.give.success",
            localized(ChatColor.AQUA, itemName)
        ))
    }
    
    private fun removeTileEntities(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val chunks = player.location.chunk.getSurroundingChunks(ctx["range"], true)
        val tileEntities = chunks.flatMap { TileEntityManager.getTileEntitiesInChunk(it) }
        tileEntities.forEach { TileEntityManager.destroyTileEntity(it, false) }
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.remove_tile_entities.success",
            coloredText(ChatColor.AQUA, tileEntities.count())
        ))
    }
    
    private fun showTileEntityData(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        
        fun sendFailure() = player.spigot().sendMessage(localized(
            ChatColor.RED,
            "command.nova.show_tile_entity_data.failure"
        ))
        
        fun sendSuccess(name: String, data: CompoundElement) = player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.show_tile_entity_data.success",
            localized(ChatColor.AQUA, name),
            coloredText(ChatColor.WHITE, data.toString())
        ))
        
        val location = player.getTargetBlockExact(8)?.location
        if (location != null) {
            val tileEntity = TileEntityManager.getTileEntityAt(location, true)
            if (tileEntity != null) {
                sendSuccess(tileEntity.material.localizedName, tileEntity.data)
            } else {
                val vanillaTileEntity = VanillaTileEntityManager.getTileEntityAt(location)
                if (vanillaTileEntity != null) sendSuccess(vanillaTileEntity.type.name, vanillaTileEntity.data)
                else sendFailure()
            }
        } else sendFailure()
        
    }
    
    private fun toggleNetworkDebugging(type: NetworkType, ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        NetworkDebugger.toggleDebugger(type, player)
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.network_debug." + type.name.lowercase()
        ))
    }
    
    private fun openItemInventory(ctx: CommandContext<CommandSourceStack>) {
        ItemsWindow(ctx.player).show()
    }
    
    private fun setRenderDistance(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val distance: Int = ctx["distance"]
        player.armorStandRenderDistance = distance
        
        player.spigot().sendMessage(localized(
            ChatColor.GRAY,
            "command.nova.render_distance",
            coloredText(ChatColor.AQUA, distance)
        ))
    }
    
}

