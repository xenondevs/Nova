@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.math.Transformation
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import org.bukkit.entity.Player
import org.joml.Matrix4f
import org.joml.Vector3f
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.command.Command
import xyz.xenondevs.nova.command.argument.NetworkTypeArgumentType
import xyz.xenondevs.nova.command.argument.NovaBlockArgumentType
import xyz.xenondevs.nova.command.argument.NovaItemArgumentType
import xyz.xenondevs.nova.command.executes0
import xyz.xenondevs.nova.command.get
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.command.requiresPermission
import xyz.xenondevs.nova.command.requiresPlayer
import xyz.xenondevs.nova.config.Configs
import xyz.xenondevs.nova.context.Context
import xyz.xenondevs.nova.context.intention.DefaultContextIntentions
import xyz.xenondevs.nova.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.registry.NovaRegistries.NETWORK_TYPE
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.ui.menu.explorer.creative.ItemsWindow
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.component.adventure.indent
import xyz.xenondevs.nova.util.data.getStringOrNull
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.customModelData
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.item.unsafeNovaTag
import xyz.xenondevs.nova.util.novaBlock
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.ChunkPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.hitbox.HitboxManager
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import xyz.xenondevs.nova.world.block.tileentity.TileEntity
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkDebugger
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.block.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MAX_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MIN_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.fakeEntityRenderDistance
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.logic.AdvancedTooltips
import xyz.xenondevs.nova.world.item.logic.PacketItems
import xyz.xenondevs.nova.world.item.recipe.RecipeManager
import xyz.xenondevs.nova.world.pos
import xyz.xenondevs.nova.world.toNovaPos
import java.text.DecimalFormat
import java.util.*
import java.util.logging.Level
import kotlin.math.max
import kotlin.math.min
import org.bukkit.inventory.ItemStack as BukkitStack

internal object NovaCommand : Command() {
    
    override val node: LiteralCommandNode<CommandSourceStack> = literal("nova")
        .then(literal("give")
            .requiresPermission("nova.command.give")
            .then(argument("player", ArgumentTypes.players())
                .then(argument("item", NovaItemArgumentType)
                    .executes0(::giveSingleTo)
                    .then(argument("amount", IntegerArgumentType.integer())
                        .executes0(::giveTo)))))
        .then(literal("debug")
            .requiresPermission("nova.command.debug")
            .then(literal("removeTileEntities")
                .requiresPlayer()
                .then(argument("range", IntegerArgumentType.integer(0))
                    .executes0(::removeTileEntities)))
            .then(literal("removeInvalidVTEs")
                .then(argument("range", IntegerArgumentType.integer(0))
                    .executes0(::removeInvalidVTEs)))
            .then(literal("getBlockData")
                .requiresPlayer()
                .executes0(::showBlockData))
            .then(literal("getBlockModelData")
                .requiresPlayer()
                .executes0(::showBlockModelData))
            .then(literal("getItemBehaviors")
                .requiresPlayer()
                .executes0(::showItemBehaviors))
            .then(literal("getItemModelData")
                .requiresPlayer()
                .executes0(::showItemModelData))
            .then(literal("getNetworkNodeInfo")
                .requiresPlayer()
                .executes0(::showNetworkNodeInfoLookingAt)
                .then(argument("pos", ArgumentTypes.blockPosition())
                    .executes0(::showNetworkNodeInfoAt)))
            .then(literal("showNetwork")
                .requiresPlayer()
                .then(argument("type", NetworkTypeArgumentType)
                    .executes0(::toggleNetworkDebugging)))
            .then(literal("showNetworkClusters")
                .requiresPlayer()
                .executes0(::toggleNetworkClusterDebugging))
            .then(literal("reregisterNetworkNodes")
                .executes0(::reregisterNetworkNodes))
            .then(literal("showHitboxes")
                .requiresPlayer()
                .executes0(::toggleHitboxDebugging))
            .then(literal("fill")
                .requiresPlayer()
                .then(argument("from", ArgumentTypes.blockPosition())
                    .then(argument("to", ArgumentTypes.blockPosition())
                        .then(argument("block", NovaBlockArgumentType)
                            .executes0(::fillArea)))))
            .then(literal("giveClientsideStack")
                .requiresPlayer()
                .executes0(::copyClientsideStack)
                .then(argument("item", NovaItemArgumentType)
                    .executes0(::giveClientsideStack)))
            .then(literal("searchBlock")
                .requiresPlayer()
                .then(argument("block", NovaBlockArgumentType)
                    .then(argument("range", IntegerArgumentType.integer(1, 10))
                        .executes0(::searchBlock)))))
        .then(literal("items")
            .requiresPlayer()
            .requiresPermission("nova.command.items")
            .executes0(::openItemInventory))
        .then(literal("advancedTooltips")
            .requiresPlayer()
            .requiresPermission("nova.command.advancedTooltips")
            .then(literal("off")
                .executes0 { toggleAdvancedTooltips(it, AdvancedTooltips.Type.OFF) })
            .then(literal("nova")
                .executes0 { toggleAdvancedTooltips(it, AdvancedTooltips.Type.NOVA) })
            .then(literal("all")
                .executes0 { toggleAdvancedTooltips(it, AdvancedTooltips.Type.ALL) }))
        .then(literal("waila")
            .requiresPlayer()
            .requiresPermission("nova.command.waila")
            .then(literal("on")
                .executes0 { toggleWaila(it, true) })
            .then(literal("off")
                .executes0 { toggleWaila(it, false) }))
        .then(literal("renderDistance")
            .requiresPlayer()
            .requiresPermission("nova.command.renderDistance")
            .then(argument("distance", IntegerArgumentType.integer(MIN_RENDER_DISTANCE, MAX_RENDER_DISTANCE))
                .executes0(::setRenderDistance)))
        .then(literal("addons")
            .requiresPermission("nova.command.addons")
            .executes0(::sendAddons))
        .then(literal("resourcePack")
            .requiresPermission("nova.command.resourcePack")
            .then(literal("create")
                .executes0(::createResourcePack))
            .then(literal("reupload")
                .executes0(::reuploadResourcePack)))
        .then(literal("reload")
            .requiresPermission("nova.command.reload")
            .then(literal("configs")
                .executes0(::reloadConfigs))
            .then(literal("recipes")
                .executes0(::reloadRecipes)))
        .build()
    
    private fun reloadConfigs(ctx: CommandContext<CommandSourceStack>) {
        try {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_configs.start", NamedTextColor.GRAY))
            val reloadedConfigs = Configs.reload()
            if (reloadedConfigs.isNotEmpty()) {
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.reload_configs.success", NamedTextColor.GRAY,
                    Component.text(reloadedConfigs.size),
                    Component.join(
                        JoinConfiguration.commas(true),
                        reloadedConfigs.map { cfgId -> Component.text(cfgId.toString(), NamedTextColor.AQUA) }
                    )
                ))
            } else {
                ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_configs.none", NamedTextColor.RED))
            }
        } catch (e: Exception) {
            if (ctx.source.sender is Player)
                ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_configs.failure", NamedTextColor.RED))
            
            LOGGER.log(Level.SEVERE, "Failed to reload configs", e)
        }
    }
    
    private fun reloadRecipes(ctx: CommandContext<CommandSourceStack>) {
        try {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_recipes.start", NamedTextColor.GRAY))
            RecipeManager.reload()
            ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_recipes.success", NamedTextColor.GRAY))
        } catch (e: Exception) {
            if (ctx.source.sender is Player)
                ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_recipes.failure", NamedTextColor.RED))
            
            LOGGER.log(Level.SEVERE, "Failed to reload recipes", e)
        }
    }
    
    private fun createResourcePack(ctx: CommandContext<CommandSourceStack>) {
        runAsyncTask {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.resource_pack.create.start", NamedTextColor.GRAY))
            ResourceGeneration.createResourcePack()
            ctx.source.sender.sendMessage(Component.translatable("command.nova.resource_pack.create.success", NamedTextColor.GRAY))
        }
    }
    
    private fun toggleAdvancedTooltips(ctx: CommandContext<CommandSourceStack>, type: AdvancedTooltips.Type) {
        val player = ctx.player
        val changed = AdvancedTooltips.setType(player, type)
        
        val typeName = type.name.lowercase()
        if (changed) {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.advanced_tooltips.$typeName.success", NamedTextColor.GRAY))
            player.updateInventory()
        } else {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.advanced_tooltips.$typeName.failure", NamedTextColor.RED))
        }
    }
    
    private fun toggleWaila(ctx: CommandContext<CommandSourceStack>, state: Boolean) {
        val player = ctx.player
        val changed = WailaManager.toggle(player, state)
        
        val onOff = if (state) "on" else "off"
        if (changed) {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.waila.$onOff", NamedTextColor.GRAY))
        } else {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.waila.already_$onOff", NamedTextColor.RED))
        }
    }
    
    private fun reuploadResourcePack(ctx: CommandContext<CommandSourceStack>) {
        runAsyncTask {
            runBlocking {
                ctx.source.sender.sendMessage(Component.translatable("command.nova.resource_pack.reupload.start", NamedTextColor.GRAY))
                val url = AutoUploadManager.uploadPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                
                if (url != null)
                    ctx.source.sender.sendMessage(Component.translatable(
                        "command.nova.resource_pack.reupload.success",
                        NamedTextColor.GRAY,
                        Component.text(url).clickEvent(ClickEvent.openUrl(url))
                    ))
                else ctx.source.sender.sendMessage(Component.translatable("command.nova.resource_pack.reupload.fail", NamedTextColor.RED))
            }
        }
    }
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>) =
        giveTo(ctx, ctx["item"], ctx["amount"])
    
    private fun giveSingleTo(ctx: CommandContext<CommandSourceStack>) =
        giveTo(ctx, ctx["item"], 1)
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>, item: NovaItem, amount: Int) {
        val targetPlayers = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
            .resolve(ctx.source)
        
        if (targetPlayers.isNotEmpty()) {
            targetPlayers.forEach { player ->
                player.inventory.addItemCorrectly(item.createItemStack(amount))
                
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.give.success",
                    NamedTextColor.GRAY,
                    Component.text(amount).color(NamedTextColor.AQUA),
                    item.name?.color(NamedTextColor.AQUA) ?: Component.empty(),
                    Component.text(player.name).color(NamedTextColor.AQUA)
                ))
            }
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.no-players", NamedTextColor.RED))
    }
    
    private fun removeTileEntities(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val chunks = player.location.chunk.getSurroundingChunks(ctx["range"], true)
        
        var count = 0
        chunks.asSequence()
            .flatMap { WorldDataManager.getTileEntities(it.pos) }
            .forEach { tileEntity ->
                BlockUtils.breakBlock(
                    Context.intention(DefaultContextIntentions.BlockBreak)
                        .param(DefaultContextParamTypes.BLOCK_POS, tileEntity.pos)
                        .build()
                )
                count++
            }
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.remove_tile_entities.success",
            NamedTextColor.GRAY,
            Component.text(count).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun removeInvalidVTEs(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val chunks = player.location.chunk.getSurroundingChunks(ctx["range"], true)
        
        var count = 0
        for (chunk in chunks) {
            for (vte in WorldDataManager.getVanillaTileEntities(chunk.pos)) {
                if (vte.pos.block.type !in vte.type.materials) {
                    WorldDataManager.setVanillaTileEntity(vte.pos, null)
                    vte.handleBreak()
                    count++
                }
            }
        }
        
        if (count > 0) {
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.remove_invalid_vtes.success",
                NamedTextColor.GRAY,
                Component.text(count).color(NamedTextColor.AQUA)
            ))
        } else {
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.remove_invalid_vtes.failure",
                NamedTextColor.RED
            ))
        }
    }
    
    private fun showBlockData(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.player.getTargetBlockExact(8)?.location?.pos
        if (pos != null) {
            val novaBlockState = WorldDataManager.getBlockState(pos)
            if (novaBlockState != null) {
                val tileEntity = WorldDataManager.getTileEntity(pos)
                if (tileEntity != null) {
                    tileEntity.saveData()
                    ctx.source.sender.sendMessage(Component.translatable(
                        "command.nova.show_block_data.nova_tile_entity",
                        NamedTextColor.GRAY,
                        Component.text(novaBlockState.toString(), NamedTextColor.AQUA),
                        Component.text(tileEntity.data.toString(), NamedTextColor.WHITE)
                    ))
                } else {
                    ctx.source.sender.sendMessage(Component.translatable(
                        "command.nova.show_block_data.nova_block",
                        NamedTextColor.GRAY,
                        Component.text(novaBlockState.toString(), NamedTextColor.AQUA)
                    ))
                }
            } else {
                val vanillaBlockState = pos.nmsBlockState
                val vanillaTileEntity = WorldDataManager.getVanillaTileEntity(pos)
                if (vanillaTileEntity != null) {
                    vanillaTileEntity.saveData()
                    ctx.source.sender.sendMessage(Component.translatable(
                        "command.nova.show_block_data.vanilla_tile_entity",
                        NamedTextColor.GRAY,
                        Component.text(vanillaBlockState.toString(), NamedTextColor.AQUA),
                        Component.text(vanillaTileEntity.data.toString(), NamedTextColor.WHITE)
                    ))
                } else {
                    ctx.source.sender.sendMessage(Component.translatable(
                        "command.nova.show_block_data.vanilla_block",
                        NamedTextColor.GRAY,
                        Component.text(vanillaBlockState.toString(), NamedTextColor.AQUA)
                    ))
                }
            }
        }
    }
    
    private fun showBlockModelData(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.player.getTargetBlockExact(8)?.location?.pos
        if (pos != null) {
            val novaBlockState = WorldDataManager.getBlockState(pos)
            if (novaBlockState != null) {
                val modelProvider = novaBlockState.modelProvider
                
                val message = when (modelProvider.provider) {
                    is ModelLessBlockModelProvider -> {
                        val info = modelProvider.info as BlockData
                        Component.translatable(
                            "command.nova.show_block_model_data.model_less",
                            NamedTextColor.GRAY,
                            Component.text(novaBlockState.toString(), NamedTextColor.AQUA),
                            Component.text(info.asString, NamedTextColor.AQUA)
                        )
                    }
                    
                    is BackingStateBlockModelProvider -> {
                        val info = modelProvider.info as BackingStateConfig
                        Component.translatable(
                            "command.nova.show_block_model_data.backing_state",
                            NamedTextColor.GRAY,
                            Component.text(novaBlockState.toString(), NamedTextColor.AQUA),
                            Component.text(info.vanillaBlockState.block.descriptionId, NamedTextColor.AQUA),
                            Component.text(info.variantString, NamedTextColor.AQUA)
                        )
                    }
                    
                    is DisplayEntityBlockModelProvider -> {
                        val info = modelProvider.info as DisplayEntityBlockModelData
                        val format = DecimalFormat("#.##")
                        
                        val modelComponents = info.models.map { model ->
                            val transform = Transformation(Matrix4f(model.transform))
                            val leftRotation = transform.leftRotation.getEulerAnglesXYZ(Vector3f())
                                .mul(1 / Math.PI.toFloat() * 180f).toString(format)
                            val rightRotation = transform.rightRotation.getEulerAnglesXYZ(Vector3f())
                                .mul(1 / Math.PI.toFloat() * 180f).toString(format)
                            
                            Component.translatable(
                                "command.nova.show_block_model_data.display_entity.model",
                                NamedTextColor.GRAY,
                                Component.translatable(model.material.itemTranslationKey ?: "", NamedTextColor.AQUA),
                                Component.text(model.customModelData, NamedTextColor.AQUA),
                                Component.text(transform.translation.toString(format), NamedTextColor.AQUA),
                                Component.text(leftRotation, NamedTextColor.AQUA),
                                Component.text(transform.scale.toString(format), NamedTextColor.AQUA),
                                Component.text(rightRotation, NamedTextColor.AQUA)
                            )
                        }
                        
                        Component.translatable(
                            "command.nova.show_block_model_data.display_entity",
                            NamedTextColor.GRAY,
                            Component.text(novaBlockState.toString(), NamedTextColor.AQUA),
                            Component.translatable(info.hitboxType.material.blockTranslationKey ?: "", NamedTextColor.AQUA),
                            Component.text(info.models.size),
                            Component.join(JoinConfiguration.newlines(), modelComponents)
                        )
                    }
                }
                ctx.source.sender.sendMessage(message)
            } else ctx.source.sender.sendMessage(Component.translatable("command.nova.show_block_model_data.failure", NamedTextColor.RED))
        }
    }
    
    private fun showItemBehaviors(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        
        val itemStack = player.inventory.itemInMainHand.takeUnlessEmpty()
        val novaItem = itemStack?.novaItem
        
        if (novaItem != null) {
            val behaviors = novaItem.behaviors
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.show_item_behaviors.success",
                NamedTextColor.GRAY,
                ItemUtils.getName(itemStack).color(NamedTextColor.AQUA),
                Component.text(behaviors.size).color(NamedTextColor.AQUA),
                Component.text(behaviors.joinToString("\n") { it.toString(itemStack) }, NamedTextColor.WHITE)
            ))
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.show_item_behaviors.no_item", NamedTextColor.RED))
    }
    
    private fun showItemModelData(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val itemStack = player.inventory.itemInMainHand
        val item = itemStack.novaItem
        
        if (item != null) {
            val novaTag = itemStack.unwrap().unsafeNovaTag
            val modelId = novaTag?.getStringOrNull("modelId")
            
            val modelName: String
            val clientSideStack: BukkitStack
            
            if (modelId != null) {
                modelName = modelId
                clientSideStack = item.model.clientsideProviders[modelId]!!.get()
            } else {
                modelName = "default"
                clientSideStack = item.model.clientsideProvider.get()
            }
            
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.show_item_model_data.success",
                NamedTextColor.GRAY,
                ItemUtils.getName(itemStack).color(NamedTextColor.AQUA),
                Component.text(modelName, NamedTextColor.AQUA),
                Component.translatable(clientSideStack.type.translationKey(), NamedTextColor.AQUA),
                Component.text(clientSideStack.customModelData, NamedTextColor.AQUA)
            ))
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.show_item_model_data.no_item", NamedTextColor.RED))
    }
    
    private fun toggleNetworkDebugging(ctx: CommandContext<CommandSourceStack>) {
        val type: NetworkType<*> = ctx["type"]
        val player = ctx.player
        val enabled = NetworkDebugger.toggleDebugger(type, player)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.network_debug.${type.id.toLanguageKey()}.${if (enabled) "on" else "off"}",
            NamedTextColor.GRAY
        ))
    }
    
    private fun toggleNetworkClusterDebugging(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val enabled = NetworkDebugger.toggleClusterDebugger(player)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.network_cluster_debug.${if (enabled) "on" else "off"}",
            NamedTextColor.GRAY
        ))
    }
    
    private fun reregisterNetworkNodes(ctx: CommandContext<CommandSourceStack>) {
        val nodes = Bukkit.getWorlds().asSequence()
            .flatMap { it.loadedChunks.asList() }
            .flatMap { runBlocking { NetworkManager.getNodes(it.pos) } }
            .toList()
        
        for (node in nodes) {
            when (node) {
                is NetworkBridge -> NetworkManager.queueRemoveBridge(node, true)
                is NetworkEndPoint -> NetworkManager.queueRemoveEndPoint(node, true)
            }
        }
        
        for (node in nodes) {
            when (node) {
                is NetworkBridge -> NetworkManager.queueAddBridge(node, NETWORK_TYPE.toSet(), CUBE_FACES, true)
                is NetworkEndPoint -> NetworkManager.queueAddEndPoint(node, true)
            }
        }
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.reregister_network_nodes.success",
            NamedTextColor.GRAY,
            Component.text(nodes.size)
        ))
    }
    
    private fun showNetworkNodeInfoLookingAt(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.player.getTargetBlockExact(8)?.location?.pos
        if (pos != null) {
            showNetworkNodeInfo(pos, ctx)
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.show_network_node_info.failure", NamedTextColor.RED))
    }
    
    private fun showNetworkNodeInfoAt(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.get<BlockPositionResolver>("pos")
            .resolve(ctx.source)
            .toNovaPos(ctx.source.location.world)
        
        showNetworkNodeInfo(pos, ctx)
    }
    
    private fun showNetworkNodeInfo(pos: BlockPos, ctx: CommandContext<CommandSourceStack>) {
        val node = runBlocking { NetworkManager.getNode(pos) }
        if (node != null) {
            NetworkManager.queueRead(pos.chunkPos) { state ->
                val connectedNodes = state.getConnectedNodes(node)
                
                suspend fun buildNetworkInfoComponent(id: UUID): Component {
                    val network = state.resolveNetwork(id)
                    return Component.translatable(
                        "command.nova.show_network_node_info.network", NamedTextColor.GRAY,
                        Component.text(network.type.id.toString(), NamedTextColor.AQUA),
                        Component.text(id.toString(), NamedTextColor.AQUA),
                        Component.text(network.nodes.size, NamedTextColor.AQUA),
                        Component.text(network.nodes.values.count { (node, _) -> node is NetworkBridge }, NamedTextColor.AQUA),
                        Component.text(network.nodes.values.count { (node, _) -> node is NetworkEndPoint }, NamedTextColor.AQUA)
                    )
                }
                
                fun buildNodeNameComponent(node: NetworkNode): Component =
                    Component.text()
                        .color(NamedTextColor.AQUA)
                        .append(
                            when (node) {
                                is TileEntity -> node.block.name
                                is VanillaTileEntity -> Component.translatable(node.pos.block.type.blockTranslationKey ?: "")
                                else -> Component.text(node::class.simpleName ?: "")
                            }
                        ).build()
                
                fun buildNodeComponent(node: NetworkNode): Component =
                    buildNodeNameComponent(node)
                        .hoverEvent(Component.translatable(
                            "command.nova.show_network_node_info.node", NamedTextColor.GRAY,
                            buildNodeNameComponent(node),
                            Component.text(node.pos.world.name, NamedTextColor.AQUA),
                            Component.text(node.pos.x, NamedTextColor.AQUA),
                            Component.text(node.pos.y, NamedTextColor.AQUA),
                            Component.text(node.pos.z, NamedTextColor.AQUA)
                        ))
                
                val builder = Component.text()
                    .color(NamedTextColor.GRAY)
                
                when (node) {
                    is NetworkBridge -> {
                        val networks = state.getNetworks(node)
                        
                        builder
                            .append(Component.translatable("command.nova.show_network_node_info.bridge.header", buildNodeComponent(node)))
                            .appendNewline().indent(2)
                            .append(Component.translatable(
                                "command.nova.show_network_node_info.bridge.supported_network_types",
                                Component.join(
                                    JoinConfiguration.commas(true),
                                    state.getSupportedNetworkTypes(node).map { Component.text(it.id.toString(), NamedTextColor.AQUA) }
                                )))
                            .appendNewline().indent(2)
                            .append(Component.translatable(
                                "command.nova.show_network_node_info.bridge.allowed_faces",
                                Component.join(
                                    JoinConfiguration.commas(true),
                                    state.getBridgeFaces(node).map { Component.text(it.name, NamedTextColor.AQUA) }
                                )
                            ))
                            .appendNewline().indent(2)
                            .append(Component.translatable(
                                "command.nova.show_network_node_info.networks.header",
                                Component.text(networks.size, NamedTextColor.AQUA)
                            ))
                            .appendNewline()
                        
                        for ((type, id) in networks) {
                            builder
                                .indent(4)
                                .append(Component.translatable(
                                    "command.nova.show_network_node_info.bridge.networks.entry",
                                    Component.text(type.id.toString(), NamedTextColor.AQUA),
                                    Component
                                        .text(id.toString().take(8) + "...", NamedTextColor.AQUA)
                                        .hoverEvent(buildNetworkInfoComponent(id))
                                ))
                                .appendNewline()
                        }
                    }
                    
                    is NetworkEndPoint -> {
                        val networks = state.getNetworks(node)
                        
                        builder
                            .append(Component.translatable("command.nova.show_network_node_info.end_point.header", buildNodeComponent(node)))
                            .appendNewline().indent(2)
                            .append(Component.translatable(
                                "command.nova.show_network_node_info.networks.header",
                                Component.text(networks.size(), NamedTextColor.AQUA)
                            ))
                            .appendNewline()
                        
                        for ((type, face, id) in networks) {
                            builder
                                .indent(4)
                                .append(Component.translatable(
                                    "command.nova.show_network_node_info.end_point.networks.entry",
                                    Component.text(type.id.toString(), NamedTextColor.AQUA),
                                    Component.text(face.name, NamedTextColor.AQUA),
                                    Component
                                        .text(id.toString().take(8) + "...", NamedTextColor.AQUA)
                                        .hoverEvent(buildNetworkInfoComponent(id))
                                ))
                                .appendNewline()
                        }
                    }
                }
                
                builder
                    .indent(2)
                    .append(
                        Component.translatable(
                            "command.nova.show_network_node_info.connected_nodes.header",
                            Component.text(connectedNodes.size(), NamedTextColor.AQUA)
                        )
                    )
                    .appendNewline()
                
                for ((type, face, connectedNode) in connectedNodes) {
                    builder
                        .indent(4)
                        .append(Component.translatable(
                            "command.nova.show_network_node_info.connected_nodes.entry",
                            Component.text(type.id.toString(), NamedTextColor.AQUA),
                            Component.text(face.name, NamedTextColor.AQUA),
                            buildNodeComponent(connectedNode)
                        ))
                        .appendNewline()
                }
                
                builder
                    .indent(2)
                    .append(Component.translatable(
                        "command.nova.show_network_node_info.linked_nodes.header",
                        Component.text(node.linkedNodes.size, NamedTextColor.AQUA)
                    ))
                    .appendNewline()
                
                for (relatedNode in node.linkedNodes) {
                    builder
                        .indent(4)
                        .append(Component.translatable(
                            "command.nova.show_network_node_info.linked_nodes.entry",
                            buildNodeComponent(relatedNode),
                        ))
                        .appendNewline()
                }
                
                builder
                    .indent(2)
                    .append(Component.translatable(
                        "command.nova.show_network_node_info.initialized",
                        if (node in state)
                            Component.text("true", NamedTextColor.GREEN)
                        else Component.text("false", NamedTextColor.RED)
                    ))
                
                ctx.source.sender.sendMessage(builder.build())
            }
        } else {
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.show_network_node_info.failure", NamedTextColor.RED,
                Component.text(pos.world.name, NamedTextColor.AQUA),
                Component.text(pos.x, NamedTextColor.AQUA),
                Component.text(pos.y, NamedTextColor.AQUA),
                Component.text(pos.z, NamedTextColor.AQUA)
            ))
        }
    }
    
    private fun toggleHitboxDebugging(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        HitboxManager.toggleVisualizer(player)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.hitbox_debug",
            NamedTextColor.GRAY
        ))
    }
    
    private fun fillArea(ctx: CommandContext<CommandSourceStack>) {
        val block: NovaBlock = ctx["block"]
        val world = ctx.source.location.world
        val from = ctx.get<BlockPositionResolver>("from").resolve(ctx.source)
        val to = ctx.get<BlockPositionResolver>("to").resolve(ctx.source)
        val minX = min(from.blockX(), to.blockX())
        val maxX = max(from.blockX(), to.blockX())
        val minY = min(from.blockY(), to.blockY())
        val maxY = max(from.blockY(), to.blockY())
        val minZ = min(from.blockZ(), to.blockZ())
        val maxZ = max(from.blockZ(), to.blockZ())
        
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    world.getBlockAt(x, y, z).novaBlock = block
                }
            }
        }
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.fill.success", NamedTextColor.GRAY,
            Component.text(minX, NamedTextColor.AQUA),
            Component.text(minY, NamedTextColor.AQUA),
            Component.text(minZ, NamedTextColor.AQUA),
            Component.text(maxX, NamedTextColor.AQUA),
            Component.text(maxY, NamedTextColor.AQUA),
            Component.text(maxZ, NamedTextColor.AQUA),
            block.name.color(NamedTextColor.AQUA)
        ))
    }
    
    private fun giveClientsideStack(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val item: NovaItem = ctx["item"]
        val clientSideStack = PacketItems.getClientSideStack(
            player,
            item.createItemStack().unwrap()
        ).asBukkitMirror()
        
        player.inventory.addItemCorrectly(clientSideStack)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.give_clientside_stack.success",
            NamedTextColor.GRAY,
            item.name?.color(NamedTextColor.AQUA) ?: Component.empty()
        ))
    }
    
    private fun copyClientsideStack(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val clientsideStack = PacketItems.getClientSideStack(
            player,
            player.inventory.itemInMainHand.unwrap()
        ).asBukkitMirror()
        
        player.inventory.addItemCorrectly(clientsideStack)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.copy_clientside_stack.success",
            NamedTextColor.GRAY,
            ItemUtils.getName(clientsideStack).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun searchBlock(ctx: CommandContext<CommandSourceStack>) = runBlocking {
        val player = ctx.player
        val block: NovaBlock = ctx["block"]
        val range: Int = ctx["range"]
        
        val center = player.location.chunkPos
        for (xOff in -range..range) {
            for (zOff in -range..range) {
                val chunkPos = ChunkPos(center.worldUUID, center.x + xOff, center.z + zOff)
                WorldDataManager.getOrLoadChunk(chunkPos).forEachNonEmpty { pos, blockState ->
                    if (blockState.block != block)
                        return@forEachNonEmpty
                    
                    ctx.source.sender.sendMessage(Component.translatable(
                        "command.nova.search_block.result",
                        NamedTextColor.GRAY,
                        block.name,
                        Component.text("x=${pos.x}, y=${pos.y}, z=${pos.z}", NamedTextColor.AQUA)
                            .clickEvent(ClickEvent.suggestCommand("/tp ${pos.x} ${pos.y} ${pos.z}"))
                    ))
                }
            }
        }
        
        ctx.source.sender.sendMessage(Component.translatable("command.nova.search_block.done", NamedTextColor.GRAY))
    }
    
    private fun openItemInventory(ctx: CommandContext<CommandSourceStack>) {
        ItemsWindow(ctx.player).show()
    }
    
    private fun setRenderDistance(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val distance: Int = ctx["distance"]
        player.fakeEntityRenderDistance = distance
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.render_distance",
            NamedTextColor.GRAY,
            Component.text(distance).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun sendAddons(ctx: CommandContext<CommandSourceStack>) {
        val addons = Bukkit.getPluginManager().plugins
            .filter { it is Addon }
        
        val builder = Component.text()
        builder.append(Component.translatable("command.nova.addons.header", Component.text(addons.size)))
        for ((i, addon) in addons.withIndex()) {
            val meta = addon.pluginMeta
            
            builder.append(
                Component.text(meta.name, NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(
                    Component.text("§a${meta.name} v${meta.version} by ${meta.authors.joinToString("§f,§a ")}")
                ))
            )
            
            if (i < addons.size - 1)
                builder.append(Component.text("§f, "))
        }
        
        ctx.source.sender.sendMessage(builder.build())
    }
    
}