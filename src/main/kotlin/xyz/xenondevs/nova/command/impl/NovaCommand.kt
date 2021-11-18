package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
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
import xyz.xenondevs.nova.world.pos


object NovaCommand : Command("nova") {
    
    init {
        builder = builder
            .then(literal("give")
                .requiresPermission("nova.command.give")
                .then(argument("player", EntityArgument.players())
                    .apply {
                        NovaMaterialRegistry.sortedObtainables.forEach { material ->
                            then(literal(material.typeName.lowercase())
                                .executesCatching { handleGiveTo(it, material, 1) }
                                .then(argument("amount", IntegerArgumentType.integer())
                                    .executesCatching { handleGiveTo(it, material) }))
                        }
                    }))
            .then(literal("debug")
                .requiresPlayerPermission("nova.command.debug")
                .then(literal("removeTileEntities")
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching { removeTileEntities(it) }))
                .then(literal("getTileEntityData")
                    .executesCatching { showTileEntityData(it) })
                .then(literal("energyNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ENERGY, it) })
                .then(literal("itemNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ITEMS, it) })
                .then(literal("fluidNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.FLUID, it) }))
            .then(literal("items")
                .requiresPlayerPermission("nova.command.items")
                .executesCatching { openItemInventory(it) })
            .then(literal("renderDistance")
                .requiresPlayerPermission("nova.command.renderDistance")
                .then(argument("distance", IntegerArgumentType.integer(MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE))
                    .executesCatching { setRenderDistance(it) }))
    }
    
    private fun handleGiveTo(ctx: CommandContext<CommandSourceStack>, material: NovaMaterial) =
        handleGiveTo(ctx, material, ctx["amount"])
    
    private fun handleGiveTo(ctx: CommandContext<CommandSourceStack>, material: NovaMaterial, amount: Int) {
        val itemName = material.localizedName.ifBlank { material.typeName }
        
        val targetPlayers = ctx.getArgument("player", EntitySelector::class.java).findPlayers(ctx.source)
        
        if (targetPlayers.isNotEmpty()) {
            targetPlayers.forEach {
                val player = it.bukkitEntity
                player.inventory.addItem(material.createItemStack(amount))
                
                ctx.source.sendSuccess(localized(
                    ChatColor.GRAY,
                    "command.nova.give.success",
                    amount,
                    localized(ChatColor.AQUA, itemName),
                    player.name
                ))
            }
        } else ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.no-players"))
    }
    
    private fun removeTileEntities(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val chunks = player.location.chunk.getSurroundingChunks(ctx["range"], true)
        val tileEntities = chunks.flatMap { TileEntityManager.getTileEntitiesInChunk(it.pos) }
        tileEntities.forEach { TileEntityManager.destroyTileEntity(it, false) }
        
        ctx.source.sendSuccess(localized(
            ChatColor.GRAY,
            "command.nova.remove_tile_entities.success",
            coloredText(ChatColor.AQUA, tileEntities.count())
        ))
    }
    
    private fun showTileEntityData(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        
        fun sendFailure() = ctx.source.sendFailure(localized(
            ChatColor.RED,
            "command.nova.show_tile_entity_data.failure"
        ))
        
        fun sendSuccess(name: String, data: CompoundElement) = ctx.source.sendSuccess(localized(
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
        
        ctx.source.sendSuccess(localized(
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
        
        ctx.source.sendSuccess(localized(
            ChatColor.GRAY,
            "command.nova.render_distance",
            coloredText(ChatColor.AQUA, distance)
        ))
    }
    
}

