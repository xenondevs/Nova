package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.runBlocking
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.ComponentBuilder
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.nova.addon.AddonManager
import xyz.xenondevs.nova.command.Command
import xyz.xenondevs.nova.command.executesCatching
import xyz.xenondevs.nova.command.get
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.command.requiresPermission
import xyz.xenondevs.nova.command.requiresPlayer
import xyz.xenondevs.nova.command.requiresPlayerPermission
import xyz.xenondevs.nova.command.sendFailure
import xyz.xenondevs.nova.command.sendSuccess
import xyz.xenondevs.nova.data.config.NovaConfig
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.material.AdvancedTooltips
import xyz.xenondevs.nova.material.ItemNovaMaterial
import xyz.xenondevs.nova.material.NovaMaterialRegistry
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkDebugger
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.network.NetworkTypeRegistry
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.menu.item.creative.ItemsWindow
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.data.ComponentUtils
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.localized
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.item.localizedName
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.behavior.BlockBehaviorManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MAX_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MIN_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.fakeEntityRenderDistance
import xyz.xenondevs.nova.world.pos

internal object NovaCommand : Command("nova") {
    
    init {
        builder = builder
            .then(literal("give")
                .requiresPermission("nova.command.give")
                .then(argument("player", EntityArgument.players())
                    .apply {
                        NovaMaterialRegistry.values.asSequence()
                            .filterNot { it.isHidden }
                            .forEach { material ->
                                then(literal(material.id.toString())
                                    .executesCatching { giveTo(it, material, 1) }
                                    .then(argument("amount", IntegerArgumentType.integer())
                                        .executesCatching { giveTo(it, material) }))
                            }
                    }))
            .then(literal("debug")
                .requiresPermission("nova.command.debug")
                .then(literal("removeNovaBlocks")
                    .requiresPlayer()
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching(::removeNovaBlocks)))
                .then(literal("removeInvalidVTEs")
                    .executesCatching(::removeInvalidVTEs))
                .then(literal("getTileEntityData")
                    .requiresPlayer()
                    .executesCatching(::showTileEntityData))
                .then(literal("listBlocks")
                    .requiresPlayer()
                    .executesCatching(::listBlocks))
                .then(literal("getItemData")
                    .requiresPlayer()
                    .executesCatching(::showItemData))
                .then(literal("reloadNetworks")
                    .executesCatching(::reloadNetworks))
                .then(literal("updateChunkSearchId")
                    .executesCatching(::updateChunkSearchId))
                .then(literal("showNetwork")
                    .requiresPlayer()
                    .apply {
                        NetworkTypeRegistry.types.forEach { type ->
                            then(literal(type.id.toString())
                                .executesCatching { toggleNetworkDebugging(it, type) })
                        }
                    }
                ))
            .then(literal("items")
                .requiresPlayerPermission("nova.command.items")
                .executesCatching(::openItemInventory))
            .then(literal("advancedTooltips")
                .requiresPlayerPermission("nova.command.advancedTooltips")
                .then(literal("off")
                    .executesCatching { toggleAdvancedTooltips(it, AdvancedTooltips.Type.OFF) })
                .then(literal("nova")
                    .executesCatching { toggleAdvancedTooltips(it, AdvancedTooltips.Type.NOVA) })
                .then(literal("all")
                    .executesCatching { toggleAdvancedTooltips(it, AdvancedTooltips.Type.ALL) }))
            .then(literal("waila")
                .requiresPlayerPermission("nova.command.waila")
                .then(literal("on")
                    .executesCatching { toggleWaila(it, true) })
                .then(literal("off")
                    .executesCatching { toggleWaila(it, false) }))
            .then(literal("renderDistance")
                .requiresPlayerPermission("nova.command.renderDistance")
                .then(argument("distance", IntegerArgumentType.integer(MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE))
                    .executesCatching(::setRenderDistance)))
            .then(literal("addons")
                .requiresPermission("nova.command.addons")
                .executesCatching(::sendAddons))
            .then(literal("resourcePack")
                .requiresPermission("nova.command.resourcePack")
                .then(literal("create")
                    .executesCatching(::createResourcePack))
                .then(literal("reupload")
                    .executesCatching(::reuploadResourcePack)))
            .then(literal("reload")
                .requiresPermission("nova.command.reload")
                .then(literal("configs")
                    .executesCatching(::reloadConfigs))
                .then(literal("recipes")
                    .executesCatching(::reloadRecipes)))
    }
    
    private fun updateChunkSearchId(ctx: CommandContext<CommandSourceStack>) {
        BlockBehaviorManager.updateChunkSearchId()
        ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.update_chunk_search_id.success"))
    }
    
    private fun reloadConfigs(ctx: CommandContext<CommandSourceStack>) {
        ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.reload_configs.start"))
        NovaConfig.reload()
        ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.reload_configs.success"))
    }
    
    private fun reloadRecipes(ctx: CommandContext<CommandSourceStack>) {
        ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.reload_recipes.start"))
        RecipeManager.reload()
        ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.reload_recipes.success"))
    }
    
    private fun createResourcePack(ctx: CommandContext<CommandSourceStack>) {
        runAsyncTask {
            ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.resource_pack.create.start"))
            ResourceGeneration.createResourcePack()
            ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.resource_pack.create.success"))
        }
    }
    
    private fun toggleAdvancedTooltips(ctx: CommandContext<CommandSourceStack>, type: AdvancedTooltips.Type) {
        val player = ctx.player
        val changed = AdvancedTooltips.setType(player, type)
        
        val typeName = type.name.lowercase()
        if (changed) {
            ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.advanced_tooltips.$typeName.success"))
            player.updateInventory()
        } else {
            ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.advanced_tooltips.$typeName.failure"))
        }
    }
    
    private fun toggleWaila(ctx: CommandContext<CommandSourceStack>, state: Boolean) {
        val player = ctx.player
        val changed = WailaManager.toggle(player, state)
        
        val onOff = if (state) "on" else "off"
        if (changed) {
            ctx.source.sendSuccess(localized(
                ChatColor.GRAY,
                "command.nova.waila.$onOff"
            ))
        } else {
            ctx.source.sendFailure(localized(
                ChatColor.RED,
                "command.nova.waila.already_$onOff"
            ))
        }
    }
    
    private fun reuploadResourcePack(ctx: CommandContext<CommandSourceStack>) {
        runAsyncTask {
            runBlocking {
                ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.resource_pack.reupload.start"))
                val url = AutoUploadManager.uploadPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                
                if (url != null)
                    ctx.source.sendSuccess(localized(ChatColor.GRAY, "command.nova.resource_pack.reupload.success", ComponentUtils.createLinkComponent(url)))
                else ctx.source.sendFailure(localized(ChatColor.RED, "command.nova.resource_pack.reupload.fail"))
            }
        }
    }
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>, material: ItemNovaMaterial) =
        giveTo(ctx, material, ctx["amount"])
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>, material: ItemNovaMaterial, amount: Int) {
        val itemName = material.localizedName.ifBlank { material.id.toString() }
        
        val targetPlayers = ctx.getArgument("player", EntitySelector::class.java).findPlayers(ctx.source)
        
        if (targetPlayers.isNotEmpty()) {
            targetPlayers.forEach {
                val player = it.bukkitEntity
                player.inventory.addItemCorrectly(material.createItemStack(amount))
                
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
    
    private fun removeInvalidVTEs(ctx: CommandContext<CommandSourceStack>) {
        val count = VanillaTileEntityManager.removeInvalidVTEs()
        if (count > 0) {
            ctx.source.sendSuccess(localized(
                ChatColor.GRAY,
                "command.nova.remove_invalid_vtes.success",
                coloredText(ChatColor.AQUA, count)
            ))
        } else {
            ctx.source.sendFailure(localized(
                ChatColor.RED,
                "command.nova.remove_invalid_vtes.failure"
            ))
        }
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
        
        fun sendSuccess(name: String, data: Compound) = ctx.source.sendSuccess(localized(
            ChatColor.GRAY,
            "command.nova.show_tile_entity_data.success",
            localized(ChatColor.AQUA, name),
            coloredText(ChatColor.WHITE, data.toString())
        ))
        
        val location = player.getTargetBlockExact(8)?.location
        if (location != null) {
            val tileEntity = TileEntityManager.getTileEntity(location, true)
            if (tileEntity != null) {
                sendSuccess(tileEntity.material.localizedName, tileEntity.data)
            } else {
                val vanillaTileEntity = VanillaTileEntityManager.getTileEntityAt(location)
                if (vanillaTileEntity != null) sendSuccess(vanillaTileEntity.block.type.name, vanillaTileEntity.data)
                else sendFailure()
            }
        } else sendFailure()
        
    }
    
    private fun listBlocks(ctx: CommandContext<CommandSourceStack>) {
        val chunk = ctx.player.location.chunkPos
        val states = WorldDataManager.getBlockStates(chunk)
        ctx.source.sendSuccess(TextComponent("Total: ${states.size}"))
        states.forEach {
            ctx.source.sendSuccess(TextComponent(it.key.toString()))
        }
    }
    
    private fun showItemData(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        
        val item = player.inventory.itemInMainHand.takeUnlessEmpty()
        
        if (item != null) {
            val novaCompound = item.novaCompoundOrNull
            if (novaCompound != null) {
                ctx.source.sendSuccess(localized(
                    ChatColor.GRAY,
                    "command.nova.show_item_data.success",
                    localized(ChatColor.AQUA, item.localizedName ?: item.type.name.lowercase()),
                    coloredText(ChatColor.WHITE, novaCompound.toString())
                ))
            } else {
                ctx.source.sendFailure(localized(
                    ChatColor.RED,
                    "command.nova.show_item.no_data"
                ))
            }
        } else {
            ctx.source.sendFailure(localized(
                ChatColor.RED,
                "command.nova.show_item_data.no_item"
            ))
        }
    }
    
    private fun toggleNetworkDebugging(ctx: CommandContext<CommandSourceStack>, type: NetworkType) {
        val player = ctx.player
        NetworkDebugger.toggleDebugger(type, player)
        
        ctx.source.sendSuccess(localized(
            ChatColor.GRAY,
            "command.nova.network_debug." + type.id.toString(".")
        ))
    }
    
    private fun openItemInventory(ctx: CommandContext<CommandSourceStack>) {
        ItemsWindow(ctx.player).show()
    }
    
    private fun setRenderDistance(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val distance: Int = ctx["distance"]
        player.fakeEntityRenderDistance = distance
        
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

