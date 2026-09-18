@file:Suppress("UnstableApiUsage")

package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.brigadier.tree.LiteralCommandNode
import com.mojang.math.Transformation
import io.papermc.paper.command.brigadier.CommandSourceStack
import io.papermc.paper.command.brigadier.Commands.argument
import io.papermc.paper.command.brigadier.Commands.literal
import io.papermc.paper.command.brigadier.argument.ArgumentTypes
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver
import io.papermc.paper.registry.RegistryKey
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.nbt.NbtUtils
import net.minecraft.world.level.block.state.BlockState
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.block.Block
import org.bukkit.block.BlockType
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.IS_DEV_SERVER
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.addon.AddonBootstrapper
import xyz.xenondevs.nova.command.Command
import xyz.xenondevs.nova.command.argument.KeyArgumentType
import xyz.xenondevs.nova.command.argument.NovaRegistryArgumentType
import xyz.xenondevs.nova.command.argument.ResourcePackIdArgumentType
import xyz.xenondevs.nova.command.argument.UpdatableFileSuggestionProvider
import xyz.xenondevs.nova.command.executes0
import xyz.xenondevs.nova.command.get
import xyz.xenondevs.nova.command.player
import xyz.xenondevs.nova.command.requiresPermission
import xyz.xenondevs.nova.command.requiresPlayer
import xyz.xenondevs.nova.config.CONFIGS
import xyz.xenondevs.nova.config.NovaConfigBackend
import xyz.xenondevs.nova.packetentity.MAX_PACKET_ENTITY_RENDER_DISTANCE
import xyz.xenondevs.nova.packetentity.MIN_PACKET_ENTITY_RENDER_DISTANCE
import xyz.xenondevs.nova.packetentity.packetEntityRenderDistance
import xyz.xenondevs.nova.registry.MutableNovaRegistry
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.NovaRegistries.NETWORK_TYPE
import xyz.xenondevs.nova.registry.RegistryLoader
import xyz.xenondevs.nova.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.ui.menu.explorer.ItemsMenu
import xyz.xenondevs.nova.ui.menu.explorer.blockTagExplorer
import xyz.xenondevs.nova.ui.menu.explorer.itemTagExplorer
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.CubeFaceSet
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.asBukkitMirror
import xyz.xenondevs.nova.util.component.adventure.indent
import xyz.xenondevs.nova.util.component.adventure.toAdventureComponent
import xyz.xenondevs.nova.util.data.UpdatableFile
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.nmsBlock
import xyz.xenondevs.nova.util.nmsBlockEntity
import xyz.xenondevs.nova.util.nmsBlockState
import xyz.xenondevs.nova.util.toBlock
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.util.world.BlockStateSearcher
import xyz.xenondevs.nova.world.block.NovaBlockState
import xyz.xenondevs.nova.world.block.blockType
import xyz.xenondevs.nova.world.block.hitbox.HitboxManager
import xyz.xenondevs.nova.world.block.name
import xyz.xenondevs.nova.world.block.novaBlock
import xyz.xenondevs.nova.world.block.novaTileEntities
import xyz.xenondevs.nova.world.block.novaTileEntity
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityModelProviderManager
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkDebugger
import xyz.xenondevs.nova.world.block.tileentity.network.NetworkManager
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.world.block.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.world.block.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.world.chunkPos
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager
import xyz.xenondevs.nova.world.item.DefaultBlockOverlays
import xyz.xenondevs.nova.world.item.itemType
import xyz.xenondevs.nova.world.item.logic.AdvancedTooltips
import xyz.xenondevs.nova.world.item.logic.PacketItems
import xyz.xenondevs.nova.world.item.name
import xyz.xenondevs.nova.world.item.novaItem
import xyz.xenondevs.nova.world.item.recipe.RecipeManager
import java.text.DecimalFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

internal object NovaCommand : Command() {
    
    override val node: LiteralCommandNode<CommandSourceStack> = literal("nova")
        .then(literal("give")
            .requiresPermission("nova.command.give")
            .then(argument("player", ArgumentTypes.players())
                .then(argument("item", ArgumentTypes.resource(RegistryKey.ITEM))
                    .executes0(::giveSingleTo)
                    .then(argument("amount", IntegerArgumentType.integer())
                        .executes0(::giveTo)))))
        .then(literal("debug")
            .requiresPermission("nova.command.debug")
            .then(literal("removeTileEntities")
                .requiresPlayer()
                .then(argument("range", IntegerArgumentType.integer(0))
                    .executes0(::removeTileEntities)))
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
                .then(argument("type", NovaRegistryArgumentType(NETWORK_TYPE))
                    .executes0(::toggleNetworkDebugging)))
            .then(literal("showNetworkClusters")
                .requiresPlayer()
                .executes0(::toggleNetworkClusterDebugging))
            .then(literal("reregisterNetworkNodes")
                .executes0(::reregisterNetworkNodes))
            .then(literal("showHitboxes")
                .requiresPlayer()
                .executes0(::toggleHitboxDebugging))
            .then(literal("showEntityColliders")
                .executes0(::toggleEntityColliderDebugging))
            .then(literal("showItemTags")
                .requiresPlayer()
                .executes0(::showItemTagsMenu))
            .then(literal("showBlockTags")
                .requiresPlayer()
                .executes0(::showBlockTagsMenu))
            .then(literal("giveClientsideStack")
                .requiresPlayer()
                .executes0(::copyClientsideStack)
                .then(argument("item", ArgumentTypes.resource(RegistryKey.ITEM))
                    .executes0(::giveClientsideStack)))
            .then(literal("searchBlock")
                .then(argument("block", ArgumentTypes.resource(RegistryKey.BLOCK))
                    .requiresPlayer()
                    .then(argument("range", IntegerArgumentType.integer(0, 10))
                        .executes0(::searchBlockFromPlayer)))
                .then(argument("chunkX", IntegerArgumentType.integer())
                    .then(argument("chunkZ", IntegerArgumentType.integer())
                        .then(argument("block", ArgumentTypes.resource(RegistryKey.BLOCK))
                            .then(argument("range", IntegerArgumentType.integer(0, 10))
                                .executes0(::searchBlockFromChunk))))))
            .then(literal("fill")
                .requiresPlayer()
                .then(argument("from", ArgumentTypes.blockPosition())
                    .then(argument("block", ArgumentTypes.resource(RegistryKey.BLOCK))
                        .executes0 { ctx -> fillArea(ctx, "from", "from") })
                    .then(argument("to", ArgumentTypes.blockPosition())
                        .then(argument("block", ArgumentTypes.resource(RegistryKey.BLOCK))
                            .executes0 { ctx -> fillArea(ctx, "from", "to") })))))
        .then(literal("items")
            .requiresPlayer()
            .requiresPermission("nova.command.items")
            .executes0(::openItemInventory))
        .then(literal("advancedTooltips")
            .requiresPlayer()
            .requiresPermission("nova.command.advancedTooltips")
            .then(literal("off")
                .executes0 { toggleAdvancedTooltips(it, false) })
            .then(literal("on")
                .executes0 { toggleAdvancedTooltips(it, true) }))
        .then(literal("waila")
            .requiresPlayer()
            .requiresPermission("nova.command.waila")
            .then(literal("on")
                .executes0 { toggleWaila(it, true) })
            .then(literal("off")
                .executes0 { toggleWaila(it, false) })
            .then(literal("background")
                .then(literal("on")
                    .executes0 { toggleWailaBackground(it, true) })
                .then(literal("off")
                    .executes0 { toggleWailaBackground(it, false) })))
        .then(literal("renderDistance")
            .requiresPlayer()
            .requiresPermission("nova.command.renderDistance")
            .then(argument("distance", IntegerArgumentType.integer(MIN_PACKET_ENTITY_RENDER_DISTANCE, MAX_PACKET_ENTITY_RENDER_DISTANCE))
                .executes0(::setRenderDistance)))
        .then(literal("addons")
            .requiresPermission("nova.command.addons")
            .executes0(::sendAddons))
        .then(literal("resourcePack")
            .requiresPermission("nova.command.resourcePack")
            .then(literal("build")
                .then(argument("pack", ResourcePackIdArgumentType)
                    .executes0 { buildResourcePack(it, it["pack"]) })
                .executes0(::buildResourcePack)))
        .then(literal("reload")
            .requiresPermission("nova.command.reload")
            .then(literal("configs")
                .executes0(::reloadConfigs))
            .then(literal("recipes")
                .executes0(::reloadRecipes))
            .apply {
                val reloadableRegistries = NovaRegistries.registries.values.filter(MutableNovaRegistry<*>::isReloadable)
                val rerunnableRegistries = if (IS_DEV_SERVER) listOf(RegistryKey.ITEM, RegistryKey.BLOCK) else emptyList()
                val registryKeys = reloadableRegistries.map { it.key } + rerunnableRegistries.map { it.key() }
                
                if (registryKeys.isNotEmpty()) {
                    then(literal("registry")
                        .executes0 { reloadRegistries(it, reloadableRegistries, rerunnableRegistries) }
                        .then(argument("registry", KeyArgumentType(registryKeys)).executes0 { ctx ->
                            val key: Key = ctx["registry"]
                            val nova = reloadableRegistries.filter { it.key == key }
                            val paper = rerunnableRegistries.filter { it.key() == key }
                            reloadRegistries(ctx, nova, paper)
                        }))
                }
            }
        )
        .then(literal("reset")
            .requiresPermission("command.nova.reset")
            .then(literal("file")
                .then(argument("path", StringArgumentType.greedyString())
                    .suggests(UpdatableFileSuggestionProvider)
                    .executes0(::resetFiles)
                )
            )
        )
        .build()
    
    private fun reloadConfigs(ctx: CommandContext<CommandSourceStack>) {
        try {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_configs.start", NamedTextColor.GRAY))
            val reloadedConfigs = CONFIGS.reload()
            if (reloadedConfigs.isNotEmpty()) {
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.reload_configs.success", NamedTextColor.GRAY,
                    Component.text(reloadedConfigs.size),
                    Component.join(
                        JoinConfiguration.commas(true),
                        reloadedConfigs.map { cfgId -> Component.text(cfgId.asString(), NamedTextColor.AQUA) }
                    )
                ))
            } else {
                ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_configs.none", NamedTextColor.RED))
            }
        } catch (e: Exception) {
            if (ctx.source.sender is Player)
                ctx.source.sender.sendMessage(Component.translatable("command.nova.reload_configs.failure", NamedTextColor.RED))
            
            LOGGER.error("Failed to reload configs", e)
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
            
            LOGGER.error("Failed to reload recipes", e)
        }
    }
    
    private fun reloadRegistries(ctx: CommandContext<CommandSourceStack>, nova: Iterable<MutableNovaRegistry<*>>, paper: Iterable<RegistryKey<*>>) {
        for (registry in nova) {
            RegistryLoader.reload(registry)
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.reload_registry.success",
                NamedTextColor.GRAY,
                Component.text(registry.key.asString(), NamedTextColor.AQUA),
                Component.text(registry.entrySet.get().size, NamedTextColor.AQUA)
            ))
        }
        
        NovaConfigBackend.postReload()
    }
    
    @OptIn(DelicateCoroutinesApi::class)
    private fun buildResourcePack(ctx: CommandContext<CommandSourceStack>, id: Key? = null) {
        val ids = if (id == null) ResourcePackBuilder.configurations.keys else setOf(id)
        for (toBuild in ids) {
            GlobalScope.launch {
                ResourcePackBuilder.build(toBuild, extraListener = ctx.source.sender as? Player)
            }
        }
    }
    
    private fun toggleAdvancedTooltips(ctx: CommandContext<CommandSourceStack>, type: Boolean) {
        val player = ctx.player
        val typeName = if (type) "on" else "off"
        if (AdvancedTooltips.set(player, type)) {
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
    
    private fun toggleWailaBackground(ctx: CommandContext<CommandSourceStack>, state: Boolean) {
        val changed = WailaManager.toggleBackground(ctx.player, state)
        val onOff = if (state) "on" else "off"
        val result = if (changed) onOff else "already_$onOff"
        val color = if (changed) NamedTextColor.GRAY else NamedTextColor.RED
        ctx.source.sender.sendMessage(Component.translatable("command.nova.waila.background.$result", color))
    }
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>) =
        giveTo(ctx, ctx["item"], ctx["amount"])
    
    private fun giveSingleTo(ctx: CommandContext<CommandSourceStack>) =
        giveTo(ctx, ctx["item"], 1)
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>, item: ItemType, amount: Int) {
        val targetPlayers = ctx.getArgument("player", PlayerSelectorArgumentResolver::class.java)
            .resolve(ctx.source)
        
        if (targetPlayers.isNotEmpty()) {
            targetPlayers.forEach { player ->
                player.inventory.addItemCorrectly(item.createItemStack(amount))
                
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.give.success",
                    NamedTextColor.GRAY,
                    Component.text(amount).color(NamedTextColor.AQUA),
                    item.name.color(NamedTextColor.AQUA),
                    Component.text(player.name).color(NamedTextColor.AQUA)
                ))
            }
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.no-players", NamedTextColor.RED))
    }
    
    private fun removeTileEntities(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val range: Int = ctx["range"]
        
        var count = 0
        val (x, z) = player.location.chunkPos
        for (dx in -range..range) for (dz in -range..range) {
            for (te in player.world.getChunkAt(x + dx, z + dz).novaTileEntities) {
                te.block.blockType = BlockType.AIR
                count++
            }
        }
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.remove_tile_entities.success",
            NamedTextColor.GRAY,
            Component.text(count).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun showBlockData(ctx: CommandContext<CommandSourceStack>) {
        val block = ctx.player.getTargetBlockExact(8)?.location?.block
            ?: return
        val blockState = block.blockData
        if (blockState is NovaBlockState) {
            val tileEntity = block.novaTileEntity
            if (tileEntity != null) {
                tileEntity.saveData()
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.show_block_data.nova_tile_entity",
                    NamedTextColor.GRAY,
                    Component.text(blockState.asString, NamedTextColor.AQUA),
                    Component.text(tileEntity.data.toString(), NamedTextColor.WHITE)
                ))
            } else {
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.show_block_data.nova_block",
                    NamedTextColor.GRAY,
                    Component.text(blockState.asString, NamedTextColor.AQUA)
                ))
            }
        } else {
            val blockEntity = block.nmsBlockEntity
            if (blockEntity != null) {
                val data = blockEntity.persistentDataContainer.toTagCompound()
                data.keySet().removeIf { !it.startsWith("nova:") }
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.show_block_data.vanilla_tile_entity",
                    NamedTextColor.GRAY,
                    Component.text(blockState.asString, NamedTextColor.AQUA),
                    NbtUtils.toPrettyComponent(data).toAdventureComponent()
                ))
            } else {
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.show_block_data.vanilla_block",
                    NamedTextColor.GRAY,
                    Component.text(blockState.asString, NamedTextColor.AQUA)
                ))
            }
        }
    }
    
    private fun showBlockModelData(ctx: CommandContext<CommandSourceStack>) {
        val block = ctx.player.getTargetBlockExact(8)?.location?.block
            ?: return
        
        val blockState = block.blockData
        if (blockState !is NovaBlockState) {
            ctx.source.sender.sendMessage(Component.translatable("command.nova.show_block_model_data.failure", NamedTextColor.RED))
            return
        }
        
        val message = when (val modelProvider = blockState.novaBlock.modelProviders.get()[blockState.nmsBlockState]!!) {
            is ModelLessBlockModelProvider -> {
                val info = modelProvider.info
                Component.translatable(
                    "command.nova.show_block_model_data.model_less",
                    NamedTextColor.GRAY,
                    Component.text(blockState.asString, NamedTextColor.AQUA),
                    Component.text(info.toString(), NamedTextColor.AQUA)
                )
            }
            
            is BackingStateBlockModelProvider -> {
                val info = modelProvider.info
                Component.translatable(
                    "command.nova.show_block_model_data.backing_state",
                    NamedTextColor.GRAY,
                    Component.text(blockState.asString, NamedTextColor.AQUA),
                    Component.translatable(info.vanillaBlockState.block.descriptionId, NamedTextColor.AQUA),
                    Component.text(info.variantMap.toString(), NamedTextColor.AQUA)
                )
            }
            
            is DisplayEntityBlockModelProvider -> {
                val info = modelProvider.info
                val format = DecimalFormat("#.##")
                
                fun createModelComponent(model: Key, matrix: Matrix4f): Component {
                    val transform = Transformation(matrix)
                    val leftRotation = transform.leftRotation().getEulerAnglesXYZ(Vector3f())
                        .mul(1 / Math.PI.toFloat() * 180f).toString(format)
                    val rightRotation = transform.rightRotation().getEulerAnglesXYZ(Vector3f())
                        .mul(1 / Math.PI.toFloat() * 180f).toString(format)
                    
                    return Component.translatable(
                        "command.nova.show_block_model_data.display_entity.model",
                        NamedTextColor.GRAY,
                        Component.text(model.asString(), NamedTextColor.AQUA),
                        Component.text(Vector3f(transform.translation()).toString(format), NamedTextColor.AQUA),
                        Component.text(leftRotation, NamedTextColor.AQUA),
                        Component.text(Vector3f(transform.scale()).toString(format), NamedTextColor.AQUA),
                        Component.text(rightRotation, NamedTextColor.AQUA)
                    )
                }
                
                val modelComponents = info.models
                    .mapTo(ArrayList(info.models.size + 1)) { model ->
                        createModelComponent(model.model, Matrix4f(model.transform))
                    }
                if (info.waterlogged) {
                    modelComponents += createModelComponent(DefaultBlockOverlays.WATERLOGGED.key, Matrix4f())
                }
                val colliderComponents = info.extraColliders.map { cube ->
                    Component.translatable(
                        "command.nova.show_block_model_data.display_entity.extra_collider",
                        NamedTextColor.GRAY,
                        Component.text(Vector3d(cube.minX, cube.minY, cube.minZ).toString(format), NamedTextColor.AQUA),
                        Component.text(format.format(cube.size), NamedTextColor.AQUA)
                    )
                }
                
                Component.text()
                    .append(Component.translatable(
                        "command.nova.show_block_model_data.display_entity",
                        NamedTextColor.GRAY,
                        Component.text(blockState.asString, NamedTextColor.AQUA),
                        info.collider.blockType.name.color(NamedTextColor.AQUA),
                        Component.text(modelComponents.size),
                        Component.join(JoinConfiguration.newlines(), modelComponents)
                    ))
                    .appendNewline()
                    .append(Component.translatable(
                        "command.nova.show_block_model_data.display_entity.extra_colliders",
                        NamedTextColor.GRAY,
                        Component.text(colliderComponents.size, NamedTextColor.AQUA),
                        Component.join(JoinConfiguration.newlines(), colliderComponents)
                    ))
                    .build()
            }
        }
        ctx.source.sender.sendMessage(message)
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
            val clientSideType = item.modifyClientSideItemType(player, itemStack, itemStack.itemType)
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.show_item_model_data.success",
                NamedTextColor.GRAY,
                ItemUtils.getName(itemStack).color(NamedTextColor.AQUA),
                Component.translatable(clientSideType.translationKey(), NamedTextColor.AQUA),
                Component.text(item.key.asString(), NamedTextColor.AQUA)
            ))
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.show_item_model_data.no_item", NamedTextColor.RED))
    }
    
    private fun toggleNetworkDebugging(ctx: CommandContext<CommandSourceStack>) {
        val type: NetworkType<*> = ctx["type"]
        val player = ctx.player
        val enabled = NetworkDebugger.toggleDebugger(type, player)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.network_debug.${type.key.namespace()}.${type.key.value()}.${if (enabled) "on" else "off"}",
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
            .flatMap { NetworkManager.getNodes(it).nodes.values }
            .toList()
        
        for (node in nodes) {
            when (node) {
                is NetworkBridge -> NetworkManager.queueRemoveBridge(node, true)
                is NetworkEndPoint -> NetworkManager.queueRemoveEndPoint(node, true)
            }
        }
        
        for (node in nodes) {
            when (node) {
                is NetworkBridge -> NetworkManager.queueAddBridge(node, NETWORK_TYPE.entrySet.get(), CubeFaceSet.ALL, true)
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
        val pos = ctx.player.getTargetBlockExact(8)?.location?.block
        if (pos != null) {
            showNetworkNodeInfo(pos, ctx)
        } else ctx.source.sender.sendMessage(Component.translatable("command.nova.show_network_node_info.failure", NamedTextColor.RED))
    }
    
    private fun showNetworkNodeInfoAt(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.get<BlockPositionResolver>("pos")
            .resolve(ctx.source)
            .toBlock(ctx.source.location.world)
        
        showNetworkNodeInfo(pos, ctx)
    }
    
    private fun showNetworkNodeInfo(block: Block, ctx: CommandContext<CommandSourceStack>) {
        NetworkManager.queueRead(block.chunkPos) { state ->
            val node = state.getNode(block)
            if (node != null) {
                val connectedNodes = state.getConnectedNodes(node)
                
                fun buildNetworkInfoComponent(type: NetworkType<*>, id: UUID): Component {
                    val network = state.getNetworkOrThrow(type, id)
                    return Component.translatable(
                        "command.nova.show_network_node_info.network", NamedTextColor.GRAY,
                        Component.text(network.type.key.asString(), NamedTextColor.AQUA),
                        Component.text(id.toString(), NamedTextColor.AQUA),
                        Component.text(network.nodes.size, NamedTextColor.AQUA),
                        Component.text(network.nodes.values.count { [node, _] -> node is NetworkBridge }, NamedTextColor.AQUA),
                        Component.text(network.nodes.values.count { [node, _] -> node is NetworkEndPoint }, NamedTextColor.AQUA)
                    )
                }
                
                fun buildNodeNameComponent(node: NetworkNode): Component =
                    Component.text()
                        .color(NamedTextColor.AQUA)
                        .append(node.block.blockType.name)
                        .build()
                
                fun buildNodeComponent(node: NetworkNode): Component =
                    buildNodeNameComponent(node)
                        .hoverEvent(Component.translatable(
                            "command.nova.show_network_node_info.node", NamedTextColor.GRAY,
                            buildNodeNameComponent(node),
                            Component.text(node.block.world.name, NamedTextColor.AQUA),
                            Component.text(node.block.x, NamedTextColor.AQUA),
                            Component.text(node.block.y, NamedTextColor.AQUA),
                            Component.text(node.block.z, NamedTextColor.AQUA)
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
                                    state.getSupportedNetworkTypes(node).map { Component.text(it.key.asString(), NamedTextColor.AQUA) }
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
                        
                        for ([type, id] in networks) {
                            builder
                                .indent(4)
                                .append(Component.translatable(
                                    "command.nova.show_network_node_info.bridge.networks.entry",
                                    Component.text(type.key.asString(), NamedTextColor.AQUA),
                                    Component
                                        .text(id.toString().take(8) + "...", NamedTextColor.AQUA)
                                        .hoverEvent(buildNetworkInfoComponent(type, id))
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
                        
                        for ([type, face, id] in networks) {
                            builder
                                .indent(4)
                                .append(Component.translatable(
                                    "command.nova.show_network_node_info.end_point.networks.entry",
                                    Component.text(type.key.asString(), NamedTextColor.AQUA),
                                    Component.text(face.name, NamedTextColor.AQUA),
                                    Component
                                        .text(id.toString().take(8) + "...", NamedTextColor.AQUA)
                                        .hoverEvent(buildNetworkInfoComponent(type, id))
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
                
                for ([type, face, connectedNode] in connectedNodes) {
                    builder
                        .indent(4)
                        .append(Component.translatable(
                            "command.nova.show_network_node_info.connected_nodes.entry",
                            Component.text(type.key.asString(), NamedTextColor.AQUA),
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
            } else {
                ctx.source.sender.sendMessage(Component.translatable(
                    "command.nova.show_network_node_info.failure", NamedTextColor.RED,
                    Component.text(block.world.name, NamedTextColor.AQUA),
                    Component.text(block.x, NamedTextColor.AQUA),
                    Component.text(block.y, NamedTextColor.AQUA),
                    Component.text(block.z, NamedTextColor.AQUA)
                ))
            }
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
    
    private fun toggleEntityColliderDebugging(ctx: CommandContext<CommandSourceStack>) {
        val enabled = !DisplayEntityModelProviderManager.colliderOutlinesEnabled
        DisplayEntityModelProviderManager.colliderOutlinesEnabled = enabled
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.entity_collider_debug.${if (enabled) "on" else "off"}",
            NamedTextColor.GRAY
        ))
    }
    
    private fun showItemTagsMenu(ctx: CommandContext<CommandSourceStack>) {
        itemTagExplorer(ctx.player).open()
    }
    
    private fun showBlockTagsMenu(ctx: CommandContext<CommandSourceStack>) {
        blockTagExplorer(ctx.player).open()
    }
    
    private fun fillArea(ctx: CommandContext<CommandSourceStack>, from: String, to: String) {
        val blockType: BlockType = ctx["block"]
        val blockState = blockType.createBlockData()
        
        val from = ctx.get<BlockPositionResolver>(from).resolve(ctx.source)
        val to = ctx.get<BlockPositionResolver>(to).resolve(ctx.source)
        
        val world = ctx.source.location.world
        val minX = min(from.blockX(), to.blockX())
        val maxX = max(from.blockX(), to.blockX())
        val minY = min(from.blockY(), to.blockY())
        val maxY = max(from.blockY(), to.blockY())
        val minZ = min(from.blockZ(), to.blockZ())
        val maxZ = max(from.blockZ(), to.blockZ())
        
        for (x in minX..maxX) for (y in minY..maxY) for (z in minZ..maxZ) {
            world.setBlockData(x, y, z, blockState)
        }
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.fill.success", NamedTextColor.GRAY,
            Component.text(minX, NamedTextColor.AQUA),
            Component.text(minY, NamedTextColor.AQUA),
            Component.text(minZ, NamedTextColor.AQUA),
            Component.text(maxX, NamedTextColor.AQUA),
            Component.text(maxY, NamedTextColor.AQUA),
            Component.text(maxZ, NamedTextColor.AQUA),
            blockType.name.color(NamedTextColor.AQUA)
        ))
    }
    
    private fun giveClientsideStack(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val item: ItemType = ctx["item"]
        val clientSideStack = PacketItems.getClientSideStack(
            player,
            item.createItemStack().unwrap()
        ).asBukkitMirror()
        
        player.inventory.addItemCorrectly(clientSideStack)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.give_clientside_stack.success",
            NamedTextColor.GRAY,
            item.name.color(NamedTextColor.AQUA)
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
    
    private fun searchBlockFromPlayer(ctx: CommandContext<CommandSourceStack>) {
        searchBlock(ctx, ctx.player.location.chunk)
    }
    
    private fun searchBlockFromChunk(ctx: CommandContext<CommandSourceStack>) {
        val chunkX: Int = ctx["chunkX"]
        val chunkZ: Int = ctx["chunkZ"]
        val world = ctx.source.location.world
        searchBlock(ctx, world.getChunkAt(chunkX, chunkZ))
    }
    
    private fun searchBlock(ctx: CommandContext<CommandSourceStack>, center: Chunk) {
        val block: BlockType = ctx["block"]
        val range: Int = ctx["range"]
        
        val nmsBlock = block.nmsBlock
        val query: (BlockState) -> Boolean = { it.block == nmsBlock }
        for (xOff in -range..range) {
            for (zOff in -range..range) {
                val chunk = center.world.getChunkAt(center.x + xOff, center.z + zOff)
                BlockStateSearcher.searchChunk(chunk, query) { pos, _ ->
                    sendBlockSearchResult(ctx, block.name, pos.x, pos.y, pos.z)
                }
            }
        }
        
        ctx.source.sender.sendMessage(Component.translatable("command.nova.search_block.done", NamedTextColor.GRAY))
    }
    
    private fun sendBlockSearchResult(ctx: CommandContext<CommandSourceStack>, blockName: Component, x: Int, y: Int, z: Int) {
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.search_block.result",
            NamedTextColor.GRAY,
            blockName,
            Component.text("x=$x, y=$y, z=$z", NamedTextColor.AQUA).clickEvent(ClickEvent.suggestCommand("/tp $x $y $z"))
        ))
    }
    
    private fun openItemInventory(ctx: CommandContext<CommandSourceStack>) {
        ItemsMenu.open(ctx.player)
    }
    
    private fun setRenderDistance(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val distance: Int = ctx["distance"]
        player.packetEntityRenderDistance = distance
        FakeEntityManager.updateRenderDistance(player)
        
        ctx.source.sender.sendMessage(Component.translatable(
            "command.nova.render_distance",
            NamedTextColor.GRAY,
            Component.text(distance).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun sendAddons(ctx: CommandContext<CommandSourceStack>) {
        val builder = Component.text()
        val addons = AddonBootstrapper.addons
        builder.append(Component.translatable("command.nova.addons.header", Component.text(addons.size)))
        for ([i, addon] in addons.withIndex()) {
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
    
    private fun resetFiles(ctx: CommandContext<CommandSourceStack>) {
        val path: String = ctx["path"]
        val count = UpdatableFile.reset(path)
        if (count > 0) {
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.reset.files.success",
                NamedTextColor.GRAY,
                Component.text(count).color(NamedTextColor.AQUA),
                Component.text(path)
            ))
        } else {
            ctx.source.sender.sendMessage(Component.translatable(
                "command.nova.reset.files.no_files",
                NamedTextColor.RED,
                Component.text(path
                )))
        }
    }
    
}
