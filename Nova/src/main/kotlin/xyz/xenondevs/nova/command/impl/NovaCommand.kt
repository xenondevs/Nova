package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.command.*
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.material.ItemCategories
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkDebugger
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.menu.item.creative.ItemsWindow
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager.MAX_RENDER_DISTANCE
import xyz.xenondevs.nova.world.armorstand.FakeArmorStandManager.MIN_RENDER_DISTANCE
import xyz.xenondevs.nova.world.armorstand.armorStandRenderDistance
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.pos

object NovaCommand : Command("nova") {
    
    init {
        builder = builder
            .then(literal("give")
                .requiresPermission("nova.command.give")
                .then(argument("player", EntityArgument.players())
                    .apply {
                        ItemCategories.OBTAINABLE_MATERIALS.forEach { material ->
                            then(literal(material.id.toString())
                                .executesCatching { handleGiveTo(it, material, 1) }
                                .then(argument("amount", IntegerArgumentType.integer())
                                    .executesCatching { handleGiveTo(it, material) }))
                        }
                    }))
            .then(literal("debug")
                .requiresPlayerPermission("nova.command.debug")
                .then(literal("removeNovaBlocks")
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching(::removeNovaBlocks)))
                .then(literal("getTileEntityData")
                    .executesCatching(::showTileEntityData))
                .then(literal("reloadNetworks")
                    .executesCatching(::reloadNetworks))
                .then(literal("energyNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ENERGY, it) })
                .then(literal("itemNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.ITEMS, it) })
                .then(literal("fluidNet")
                    .executesCatching { toggleNetworkDebugging(NetworkType.FLUID, it) }))
            .then(literal("items")
                .requiresPlayerPermission("nova.command.items")
                .executesCatching(::openItemInventory))
            .then(literal("renderDistance")
                .requiresPlayerPermission("nova.command.renderDistance")
                .then(argument("distance", IntegerArgumentType.integer(MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE))
                    .executesCatching(::setRenderDistance)))
            .then(literal("addons")
                .requiresPermission("nova.command.addons")
                .executesCatching(::sendAddons))
            .then(literal("createResourcePack")
                .requiresPermission("nova.command.zip")
                .executesCatching(::handleCreateResourcePack)
            )
    }
    
    private fun handleCreateResourcePack(ctx: CommandContext<CommandSourceStack>) {
        runAsyncTask {
            Resources.createResourcePack()
        }
    }
    
    private fun handleGiveTo(ctx: CommandContext<CommandSourceStack>, material: ItemNovaMaterial) =
        handleGiveTo(ctx, material, ctx["amount"])
    
    private fun handleGiveTo(ctx: CommandContext<CommandSourceStack>, material: ItemNovaMaterial, amount: Int) {
        val itemName = material.localizedName.ifBlank { material.id.toString() }
        
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
    
    private fun removeNovaBlocks(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val chunks = player.location.chunk.getSurroundingChunks(ctx["range"], true)
        val novaBlocks = chunks.flatMap { WorldDataManager.getBlockStates(it.pos).values.filterIsInstance<NovaBlockState>() }
        novaBlocks.forEach { BlockManager.removeBlock(BlockBreakContext(it.pos)) }
        
        ctx.source.sendSuccess(localized(
            ChatColor.GRAY,
            "command.nova.remove_tile_entities.success",
            coloredText(ChatColor.AQUA, novaBlocks.count())
        ))
    }
    
    private fun reloadNetworks(ctx: CommandContext<CommandSourceStack>) {
        NetworkManager.queueAsync {
            it.reloadNetworks()
            ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.network_reload.success"))
        }
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
                if (vanillaTileEntity != null) sendSuccess(vanillaTileEntity.block.type.name, vanillaTileEntity.data)
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
    
    private fun sendAddons(ctx: CommandContext<CommandSourceStack>) {
        val addons = AddonManager.addons.values.toList()
        val builder = ComponentBuilder()
        
        builder.append(localized(
            ChatColor.WHITE,
            "command.nova.addons.header",
            addons.size
        ))
        
        for (i in addons.indices) {
            val addon = addons[i]
            val desc = addon.description
            
            val hoverText = TextComponent("§a${desc.name} v${desc.version} by ${desc.authors.joinToString("§f,§a ")}")
            val component = coloredText(ChatColor.GREEN, desc.name)
            component.hoverEvent = HoverEvent(Action.SHOW_TEXT, Text(arrayOf(hoverText)))
            
            builder.append(component)
            if (i < addons.size - 1) builder.append("§f, ")
        }
        
        ctx.source.sendSuccess(builder.create())
    }
    
}

