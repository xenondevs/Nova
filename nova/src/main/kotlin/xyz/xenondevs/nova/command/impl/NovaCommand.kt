package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
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
import xyz.xenondevs.nova.data.config.Configs
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.data.world.WorldDataManager
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.logic.AdvancedTooltips
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.NovaRegistries.NETWORK_TYPE
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.NetworkDebugger
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.NetworkType
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.menu.item.creative.ItemsWindow
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.item.localizedName
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.backingstate.BackingStateManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.hitbox.HitboxManager
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
                        NovaRegistries.ITEM.asSequence()
                            .filterNot { it.isHidden }
                            .forEach { material ->
                                then(literal(material.id.toString())
                                    .executesCatching { giveTo(it, material, 1) }
                                    .then(argument("amount", IntegerArgumentType.integer())
                                        .executesCatching { giveTo(it, material) }))
                            }
                    }))
            .then(literal("enchant")
                .requiresPermission("nova.command.enchant")
                .then(argument("player", EntityArgument.players())
                    .apply {
                        for (enchantment in NovaRegistries.ENCHANTMENT) {
                            then(literal(enchantment.id.toString())
                                .then(argument("level", IntegerArgumentType.integer(enchantment.minLevel, enchantment.maxLevel))
                                    .executesCatching { enchant(it, enchantment) }))
                        }
                    }))
            .then(literal("unenchant")
                .requiresPermission("nova.command.unenchant")
                .then(argument("player", EntityArgument.players())
                    .executesCatching(::unenchant)
                    .apply {
                        for (enchantment in NovaRegistries.ENCHANTMENT) {
                            then(literal(enchantment.id.toString()).executesCatching { unenchant(it, enchantment) })
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
                        NETWORK_TYPE.forEach { type ->
                            then(literal(type.id.toString())
                                .executesCatching { toggleNetworkDebugging(it, type) })
                        }
                    }
                )
                .then(literal("showHitboxes")
                    .requiresPlayer()
                    .executesCatching(::toggleHitboxDebugging)))
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
        BackingStateManager.updateChunkSearchId()
        ctx.source.sendSuccess(Component.translatable("command.nova.update_chunk_search_id.success", NamedTextColor.GRAY))
    }
    
    private fun reloadConfigs(ctx: CommandContext<CommandSourceStack>) {
        ctx.source.sendSuccess(Component.translatable("command.nova.reload_configs.start", NamedTextColor.GRAY))
        Configs.reload()
        ctx.source.sendSuccess(Component.translatable("command.nova.reload_configs.success", NamedTextColor.GRAY))
    }
    
    private fun reloadRecipes(ctx: CommandContext<CommandSourceStack>) {
        ctx.source.sendSuccess(Component.translatable("command.nova.reload_recipes.start", NamedTextColor.GRAY))
        RecipeManager.reload()
        ctx.source.sendSuccess(Component.translatable("command.nova.reload_recipes.success", NamedTextColor.GRAY))
    }
    
    private fun createResourcePack(ctx: CommandContext<CommandSourceStack>) {
        runAsyncTask {
            ctx.source.sendSuccess(Component.translatable("command.nova.resource_pack.create.start", NamedTextColor.GRAY))
            ResourceGeneration.createResourcePack()
            ctx.source.sendSuccess(Component.translatable("command.nova.resource_pack.create.success", NamedTextColor.GRAY))
        }
    }
    
    private fun toggleAdvancedTooltips(ctx: CommandContext<CommandSourceStack>, type: AdvancedTooltips.Type) {
        val player = ctx.player
        val changed = AdvancedTooltips.setType(player, type)
        
        val typeName = type.name.lowercase()
        if (changed) {
            ctx.source.sendSuccess(Component.translatable("command.nova.advanced_tooltips.$typeName.success", NamedTextColor.GRAY))
            player.updateInventory()
        } else {
            ctx.source.sendFailure(Component.translatable("command.nova.advanced_tooltips.$typeName.failure", NamedTextColor.RED))
        }
    }
    
    private fun toggleWaila(ctx: CommandContext<CommandSourceStack>, state: Boolean) {
        val player = ctx.player
        val changed = WailaManager.toggle(player, state)
        
        val onOff = if (state) "on" else "off"
        if (changed) {
            ctx.source.sendSuccess(Component.translatable("command.nova.waila.$onOff", NamedTextColor.GRAY))
        } else {
            ctx.source.sendFailure(Component.translatable("command.nova.waila.already_$onOff", NamedTextColor.RED))
        }
    }
    
    private fun reuploadResourcePack(ctx: CommandContext<CommandSourceStack>) {
        runAsyncTask {
            runBlocking {
                ctx.source.sendSuccess(Component.translatable("command.nova.resource_pack.reupload.start", NamedTextColor.GRAY))
                val url = AutoUploadManager.uploadPack(ResourcePackBuilder.RESOURCE_PACK_FILE)
                
                if (url != null)
                    ctx.source.sendSuccess(Component.translatable(
                        "command.nova.resource_pack.reupload.success",
                        NamedTextColor.GRAY,
                        Component.text(url).clickEvent(ClickEvent.openUrl(url))
                    ))
                else ctx.source.sendFailure(Component.translatable("command.nova.resource_pack.reupload.fail", NamedTextColor.RED))
            }
        }
    }
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>, item: NovaItem) =
        giveTo(ctx, item, ctx["amount"])
    
    private fun giveTo(ctx: CommandContext<CommandSourceStack>, item: NovaItem, amount: Int) {
        val itemName = item.localizedName.ifBlank { item.id.toString() }
        
        val targetPlayers = ctx.getArgument("player", EntitySelector::class.java).findPlayers(ctx.source)
        
        if (targetPlayers.isNotEmpty()) {
            targetPlayers.forEach {
                val player = it.bukkitEntity
                player.inventory.addItemCorrectly(item.createItemStack(amount))
                
                ctx.source.sendSuccess(Component.translatable(
                    "command.nova.give.success",
                    NamedTextColor.GRAY,
                    Component.text(amount).color(NamedTextColor.AQUA),
                    Component.translatable(itemName).color(NamedTextColor.AQUA),
                    Component.text(player.name).color(NamedTextColor.AQUA)
                ))
            }
        } else ctx.source.sendFailure(Component.translatable("command.nova.no-players", NamedTextColor.RED))
    }
    
    private fun enchant(ctx: CommandContext<CommandSourceStack>, enchantment: Enchantment) {
        val targetPlayers = ctx.getArgument("player", EntitySelector::class.java).findPlayers(ctx.source)
        val level: Int = ctx["level"]
        
        if (targetPlayers.isNotEmpty()) {
            for (player in targetPlayers) {
                val itemStack = player.mainHandItem
                val enchantments = Enchantable.getEnchantments(itemStack)
                
                if (enchantments.any { !it.key.isCompatibleWith(enchantment) }) {
                    ctx.source.sendFailure(Component.translatable(
                        "command.nova.enchant.incompatible",
                        NamedTextColor.RED,
                        Component.translatable(enchantment.localizedName, NamedTextColor.AQUA),
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        Component.translatable(itemStack.bukkitMirror.localizedName ?: "", NamedTextColor.AQUA)
                    ))
                    
                    continue
                }
                
                if (itemStack.item == Items.BOOK || itemStack.item == Items.ENCHANTED_BOOK) {
                    val enchantedBook = if (itemStack.item == Items.ENCHANTED_BOOK) itemStack else ItemStack(Items.ENCHANTED_BOOK)
                    Enchantable.addStoredEnchantment(enchantedBook, enchantment, level)
                    player.setItemInHand(InteractionHand.MAIN_HAND, enchantedBook)
                } else {
                    // verify categories for non-stored enchantments
                    val categories = NovaRegistries.ENCHANTMENT_CATEGORY.filter { enchantment in it.enchantments }
                    if (categories.none { it.canEnchant(itemStack) }) {
                        ctx.source.sendFailure(Component.translatable(
                            "command.nova.enchant.unsupported",
                            NamedTextColor.RED,
                            Component.translatable(enchantment.localizedName, NamedTextColor.AQUA),
                            Component.text(player.displayName, NamedTextColor.AQUA),
                            Component.translatable(itemStack.bukkitMirror.localizedName ?: "", NamedTextColor.AQUA)
                        ))
                        
                        continue
                    }
                    
                    Enchantable.addEnchantment(itemStack, enchantment, level)
                }
                
                ctx.source.sendSuccess(Component.translatable(
                    "command.nova.enchant.success",
                    NamedTextColor.GRAY,
                    Component.textOfChildren(
                        Component.translatable(enchantment.localizedName, NamedTextColor.AQUA),
                        Component.text(" "),
                        Component.translatable("enchantment.level.$level", NamedTextColor.AQUA),
                    ),
                    Component.text(player.displayName, NamedTextColor.AQUA),
                    Component.translatable(itemStack.bukkitMirror.localizedName ?: "", NamedTextColor.AQUA))
                )
            }
        } else ctx.source.sendFailure(Component.translatable("command.nova.no-players", NamedTextColor.RED))
    }
    
    private fun unenchant(ctx: CommandContext<CommandSourceStack>) {
        val targetPlayers = ctx.getArgument("player", EntitySelector::class.java).findPlayers(ctx.source)
        
        
        if (targetPlayers.isNotEmpty()) {
            for (player in targetPlayers) {
                val itemStack = player.mainHandItem
                
                fun sendFailure() {
                    ctx.source.sendFailure(Component.translatable(
                        "command.nova.unenchant_all.failure",
                        NamedTextColor.RED,
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        Component.translatable(itemStack.bukkitMirror.localizedName ?: "", NamedTextColor.AQUA)
                    ))
                }
                
                fun sendSuccess() {
                    ctx.source.sendSuccess(Component.translatable(
                        "command.nova.unenchant_all.success",
                        NamedTextColor.GRAY,
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        Component.translatable(itemStack.bukkitMirror.localizedName ?: "", NamedTextColor.AQUA)
                    ))
                }
                
                if (itemStack.item == Items.ENCHANTED_BOOK) {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.BOOK))
                    sendSuccess()
                } else if (Enchantable.isEnchanted(itemStack)) {
                    Enchantable.removeAllEnchantments(itemStack)
                    sendSuccess()
                } else {
                    sendFailure()
                }
            }
        } else ctx.source.sendFailure(Component.translatable("command.nova.no-players", NamedTextColor.RED))
    }
    
    private fun unenchant(ctx: CommandContext<CommandSourceStack>, enchantment: Enchantment) {
        val targetPlayers = ctx.getArgument("player", EntitySelector::class.java).findPlayers(ctx.source)
        
        if (targetPlayers.isNotEmpty()) {
            for (player in targetPlayers) {
                val itemStack = player.mainHandItem
                
                fun sendFailure(){
                    ctx.source.sendFailure(Component.translatable(
                        "command.nova.unenchant_single.failure",
                        NamedTextColor.RED,
                        Component.translatable(enchantment.localizedName, NamedTextColor.AQUA),
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        Component.translatable(itemStack.bukkitMirror.localizedName ?: "", NamedTextColor.AQUA)
                    ))
                }
                
                fun sendSuccess() {
                    ctx.source.sendSuccess(Component.translatable(
                        "command.nova.unenchant_single.success",
                        NamedTextColor.GRAY,
                        Component.translatable(enchantment.localizedName, NamedTextColor.AQUA),
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        Component.translatable(itemStack.bukkitMirror.localizedName ?: "", NamedTextColor.AQUA)
                    ))
                }
                
                if (itemStack.item == Items.ENCHANTED_BOOK) {
                    val storedEnchantments = Enchantable.getStoredEnchantments(itemStack)
                    if (enchantment in storedEnchantments) {
                        Enchantable.removeStoredEnchantment(itemStack, enchantment)
                        if (!Enchantable.hasStoredEnchantments(itemStack))
                            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack(Items.BOOK))
                        
                        sendSuccess()
                    } else sendFailure()
                } else {
                    val enchantments = Enchantable.getEnchantments(itemStack)
                    if (enchantment in enchantments) {
                        Enchantable.removeEnchantment(itemStack, enchantment)
                        sendSuccess()
                    } else sendFailure()
                }
            }
        } else ctx.source.sendFailure(Component.translatable("command.nova.no-players", NamedTextColor.RED))
    }
    
    private fun removeNovaBlocks(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val chunks = player.location.chunk.getSurroundingChunks(ctx["range"], true)
        val novaBlocks = chunks.flatMap { WorldDataManager.getBlockStates(it.pos).values.filterIsInstance<NovaBlockState>() }
        novaBlocks.forEach { BlockManager.removeBlockState(BlockBreakContext(it.pos)) }
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.remove_tile_entities.success",
            NamedTextColor.GRAY,
            Component.text(novaBlocks.count()).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun removeInvalidVTEs(ctx: CommandContext<CommandSourceStack>) {
        val count = VanillaTileEntityManager.removeInvalidVTEs()
        if (count > 0) {
            ctx.source.sendSuccess(Component.translatable(
                "command.nova.remove_invalid_vtes.success",
                NamedTextColor.GRAY,
                Component.text(count).color(NamedTextColor.AQUA)
            ))
        } else {
            ctx.source.sendSuccess(Component.translatable(
                "command.nova.remove_invalid_vtes.failure",
                NamedTextColor.RED
            ))
        }
    }
    
    private fun reloadNetworks(ctx: CommandContext<CommandSourceStack>) {
        NetworkManager.queueAsync {
            it.reloadNetworks()
            ctx.source.sendSuccess(Component.translatable("command.nova.network_reload.success", NamedTextColor.GRAY))
        }
    }
    
    private fun showTileEntityData(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        
        fun sendFailure() = ctx.source.sendFailure(Component.translatable(
            "command.nova.show_tile_entity_data.failure",
            NamedTextColor.RED
        ))
        
        fun sendSuccess(name: String, data: Compound) = ctx.source.sendSuccess(Component.translatable(
            "command.nova.show_tile_entity_data.success",
            NamedTextColor.GRAY,
            Component.text(name).color(NamedTextColor.AQUA),
            Component.text(data.toString(), NamedTextColor.WHITE)
        ))
        
        val location = player.getTargetBlockExact(8)?.location
        if (location != null) {
            val tileEntity = TileEntityManager.getTileEntity(location, true)
            if (tileEntity != null) {
                sendSuccess(tileEntity.block.localizedName, tileEntity.data)
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
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.list_blocks.success",
            NamedTextColor.GRAY,
            Component.text(states.size, NamedTextColor.AQUA)
        ))
        states.forEach { ctx.source.sendSuccess(Component.text(it.key.toString(), NamedTextColor.GRAY)) }
    }
    
    private fun showItemData(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        
        val item = player.inventory.itemInMainHand.takeUnlessEmpty()
        
        if (item != null) {
            val novaCompound = item.novaCompoundOrNull
            if (novaCompound != null) {
                ctx.source.sendSuccess(Component.translatable(
                    "command.nova.show_item_data.success",
                    NamedTextColor.GRAY,
                    Component.translatable(item.localizedName ?: item.type.name.lowercase(), NamedTextColor.AQUA),
                    Component.text(novaCompound.toString(), NamedTextColor.WHITE)
                ))
            } else ctx.source.sendFailure(Component.translatable("command.nova.show_item.no_data", NamedTextColor.RED))
        } else ctx.source.sendFailure(Component.translatable("command.nova.show_item_data.no_item", NamedTextColor.RED))
    }
    
    private fun toggleNetworkDebugging(ctx: CommandContext<CommandSourceStack>, type: NetworkType) {
        val player = ctx.player
        NetworkDebugger.toggleDebugger(type, player)
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.network_debug." + type.id.toLanguageKey(),
            NamedTextColor.GRAY
        ))
    }
    
    private fun toggleHitboxDebugging(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        HitboxManager.toggleVisualizer(player)
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.hitbox_debug",
            NamedTextColor.GRAY
        ))
    }
    
    private fun openItemInventory(ctx: CommandContext<CommandSourceStack>) {
        ItemsWindow(ctx.player).show()
    }
    
    private fun setRenderDistance(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val distance: Int = ctx["distance"]
        player.fakeEntityRenderDistance = distance
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.render_distance",
            NamedTextColor.GRAY,
            Component.text(distance).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun sendAddons(ctx: CommandContext<CommandSourceStack>) {
        val addons = AddonManager.addons.values.toList()
        val builder = Component.text()
        
        builder.append(Component.translatable("command.nova.addons.header", Component.text(addons.size)))
        
        for (i in addons.indices) {
            val addon = addons[i]
            val desc = addon.description
            
            builder.append(
                Component.text(desc.name, NamedTextColor.GREEN).hoverEvent(HoverEvent.showText(
                    Component.text("§a${desc.name} v${desc.version} by ${desc.authors.joinToString("§f,§a ")}")
                ))
            )
            
            if (i < addons.size - 1)
                builder.append(Component.text("§f, "))
        }
        
        ctx.source.sendSuccess(builder.build())
    }
    
}

