package xyz.xenondevs.nova.command.impl

import com.mojang.brigadier.arguments.IntegerArgumentType
import com.mojang.brigadier.context.CommandContext
import com.mojang.math.Transformation
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.commands.arguments.coordinates.BlockPosArgument
import net.minecraft.commands.arguments.coordinates.Coordinates
import net.minecraft.commands.arguments.selector.EntitySelector
import net.minecraft.world.InteractionHand
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.block.data.BlockData
import org.joml.Matrix4f
import org.joml.Vector3f
import xyz.xenondevs.commons.guava.component1
import xyz.xenondevs.commons.guava.component2
import xyz.xenondevs.commons.guava.component3
import xyz.xenondevs.commons.guava.iterator
import xyz.xenondevs.nova.LOGGER
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
import xyz.xenondevs.nova.data.context.Context
import xyz.xenondevs.nova.data.context.intention.DefaultContextIntentions
import xyz.xenondevs.nova.data.context.param.DefaultContextParamTypes
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.builder.ResourcePackBuilder
import xyz.xenondevs.nova.data.resources.upload.AutoUploadManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.logic.AdvancedTooltips
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.registry.NovaRegistries.NETWORK_TYPE
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.network.NetworkDebugger
import xyz.xenondevs.nova.tileentity.network.NetworkManager
import xyz.xenondevs.nova.tileentity.network.node.NetworkBridge
import xyz.xenondevs.nova.tileentity.network.node.NetworkEndPoint
import xyz.xenondevs.nova.tileentity.network.node.NetworkNode
import xyz.xenondevs.nova.tileentity.network.type.NetworkType
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntity
import xyz.xenondevs.nova.tileentity.vanilla.VanillaTileEntityManager
import xyz.xenondevs.nova.ui.menu.explorer.creative.ItemsWindow
import xyz.xenondevs.nova.ui.waila.WailaManager
import xyz.xenondevs.nova.util.BlockUtils
import xyz.xenondevs.nova.util.CUBE_FACES
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.component.adventure.indent
import xyz.xenondevs.nova.util.getSurroundingChunks
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.takeUnlessEmpty
import xyz.xenondevs.nova.util.runAsyncTask
import xyz.xenondevs.nova.util.toNovaPos
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.hitbox.HitboxManager
import xyz.xenondevs.nova.world.block.state.model.BackingStateBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.BackingStateConfig
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelData
import xyz.xenondevs.nova.world.block.state.model.DisplayEntityBlockModelProvider
import xyz.xenondevs.nova.world.block.state.model.ModelLessBlockModelProvider
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MAX_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.FakeEntityManager.MIN_RENDER_DISTANCE
import xyz.xenondevs.nova.world.fakeentity.fakeEntityRenderDistance
import xyz.xenondevs.nova.world.format.WorldDataManager
import xyz.xenondevs.nova.world.pos
import java.text.DecimalFormat
import java.util.*
import java.util.logging.Level
import kotlin.math.max
import kotlin.math.min

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
                .then(literal("removeTileEntities")
                    .requiresPlayer()
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching(::removeTileEntities)))
                .then(literal("removeInvalidVTEs")
                    .then(argument("range", IntegerArgumentType.integer(0))
                        .executesCatching(::removeInvalidVTEs)))
                .then(literal("getBlockData")
                    .requiresPlayer()
                    .executesCatching(::showBlockData))
                .then(literal("getBlockModelData")
                    .requiresPlayer()
                    .executesCatching(::showBlockModelData))
                .then(literal("getItemData")
                    .requiresPlayer()
                    .executesCatching(::showItemData))
                .then(literal("getNetworkNodeInfo")
                    .requiresPlayer()
                    .executesCatching(::showNetworkNodeInfoLookingAt)
                    .then(argument("pos", BlockPosArgument.blockPos())
                        .executesCatching(::showNetworkNodeInfoAt)))
                .then(literal("showNetwork")
                    .requiresPlayer()
                    .apply {
                        NETWORK_TYPE.forEach { type ->
                            then(literal(type.id.toString())
                                .executesCatching { toggleNetworkDebugging(it, type) })
                        }
                    }
                )
                .then(literal("showNetworkClusters")
                    .requiresPlayer()
                    .executesCatching(::toggleNetworkClusterDebugging))
                .then(literal("reregisterNetworkNodes")
                    .executesCatching(::reregisterNetworkNodes)
                )
                .then(literal("showHitboxes")
                    .requiresPlayer()
                    .executesCatching(::toggleHitboxDebugging))
                .then(literal("fill")
                    .requiresPlayer()
                    .then(argument("from", BlockPosArgument.blockPos())
                        .then(argument("to", BlockPosArgument.blockPos())
                            .apply {
                                for (block in NovaRegistries.BLOCK) {
                                    then(literal(block.id.toString()).executesCatching { fillArea(block, it) })
                                }
                            }
                        ))))
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
    
    private fun reloadConfigs(ctx: CommandContext<CommandSourceStack>) {
        try {
            ctx.source.sendSuccess(Component.translatable("command.nova.reload_configs.start", NamedTextColor.GRAY))
            val reloadedConfigs = Configs.reload()
            if (reloadedConfigs.isNotEmpty()) {
                ctx.source.sendSuccess(Component.translatable(
                    "command.nova.reload_configs.success", NamedTextColor.GRAY,
                    Component.text(reloadedConfigs.size),
                    Component.join(
                        JoinConfiguration.commas(true),
                        reloadedConfigs.map { cfgName -> Component.text(cfgName, NamedTextColor.AQUA) }
                    )
                ))
            } else {
                ctx.source.sendFailure(Component.translatable("command.nova.reload_configs.none", NamedTextColor.RED))
            }
        } catch (e: Exception) {
            if (ctx.source.isPlayer)
                ctx.source.sendFailure(Component.translatable("command.nova.reload_configs.failure", NamedTextColor.RED))
            
            LOGGER.log(Level.SEVERE, "Failed to reload configs", e)
        }
    }
    
    private fun reloadRecipes(ctx: CommandContext<CommandSourceStack>) {
        try {
            ctx.source.sendSuccess(Component.translatable("command.nova.reload_recipes.start", NamedTextColor.GRAY))
            RecipeManager.reload()
            ctx.source.sendSuccess(Component.translatable("command.nova.reload_recipes.success", NamedTextColor.GRAY))
        } catch (e: Exception) {
            if (ctx.source.isPlayer)
                ctx.source.sendFailure(Component.translatable("command.nova.reload_recipes.failure", NamedTextColor.RED))
            
            LOGGER.log(Level.SEVERE, "Failed to reload recipes", e)
        }
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
        val targetPlayers = ctx.getArgument("player", EntitySelector::class.java).findPlayers(ctx.source)
        
        if (targetPlayers.isNotEmpty()) {
            targetPlayers.forEach {
                val player = it.bukkitEntity
                player.inventory.addItemCorrectly(item.createItemStack(amount))
                
                ctx.source.sendSuccess(Component.translatable(
                    "command.nova.give.success",
                    NamedTextColor.GRAY,
                    Component.text(amount).color(NamedTextColor.AQUA),
                    item.name.color(NamedTextColor.AQUA),
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
                        ItemUtils.getName(itemStack).color(NamedTextColor.AQUA)
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
                            ItemUtils.getName(itemStack).color(NamedTextColor.AQUA)
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
                    ItemUtils.getName(itemStack).color(NamedTextColor.AQUA))
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
                        ItemUtils.getName(itemStack).color(NamedTextColor.AQUA)
                    ))
                }
                
                fun sendSuccess() {
                    ctx.source.sendSuccess(Component.translatable(
                        "command.nova.unenchant_all.success",
                        NamedTextColor.GRAY,
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        ItemUtils.getName(itemStack).color(NamedTextColor.AQUA)
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
                
                fun sendFailure() {
                    ctx.source.sendFailure(Component.translatable(
                        "command.nova.unenchant_single.failure",
                        NamedTextColor.RED,
                        Component.translatable(enchantment.localizedName, NamedTextColor.AQUA),
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        ItemUtils.getName(itemStack).color(NamedTextColor.AQUA)
                    ))
                }
                
                fun sendSuccess() {
                    ctx.source.sendSuccess(Component.translatable(
                        "command.nova.unenchant_single.success",
                        NamedTextColor.GRAY,
                        Component.translatable(enchantment.localizedName, NamedTextColor.AQUA),
                        Component.text(player.displayName, NamedTextColor.AQUA),
                        ItemUtils.getName(itemStack).color(NamedTextColor.AQUA)
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
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.remove_tile_entities.success",
            NamedTextColor.GRAY,
            Component.text(count).color(NamedTextColor.AQUA)
        ))
    }
    
    private fun removeInvalidVTEs(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val chunks = player.location.chunk.getSurroundingChunks(ctx["range"], true)
        val count = chunks.sumOf { VanillaTileEntityManager.removeInvalidVTEs(it.pos) }
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
    
    private fun showBlockData(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.player.getTargetBlockExact(8)?.location?.pos
        if (pos != null) {
            val novaBlockState = WorldDataManager.getBlockState(pos)
            if (novaBlockState != null) {
                val tileEntity = WorldDataManager.getTileEntity(pos)
                if (tileEntity != null) {
                    tileEntity.saveData()
                    ctx.source.sendSuccess(Component.translatable(
                        "command.nova.show_block_data.nova_tile_entity",
                        NamedTextColor.GRAY,
                        Component.text(novaBlockState.toString(), NamedTextColor.AQUA),
                        Component.text(tileEntity.data.toString(), NamedTextColor.WHITE)
                    ))
                } else {
                    ctx.source.sendSuccess(Component.translatable(
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
                    ctx.source.sendSuccess(Component.translatable(
                        "command.nova.show_block_data.vanilla_tile_entity",
                        NamedTextColor.GRAY,
                        Component.text(vanillaBlockState.toString(), NamedTextColor.AQUA),
                        Component.text(vanillaTileEntity.data.toString(), NamedTextColor.WHITE)
                    ))
                } else {
                    ctx.source.sendSuccess(Component.translatable(
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
                            Component.text(info.type.material.blockTranslationKey ?: "", NamedTextColor.AQUA),
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
                ctx.source.sendSuccess(message)
            } else ctx.source.sendFailure(Component.translatable("command.nova.show_block_model_data.failure", NamedTextColor.RED))
        }
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
                    ItemUtils.getName(item).color(NamedTextColor.AQUA),
                    Component.text(novaCompound.toString(), NamedTextColor.WHITE)
                ))
            } else ctx.source.sendFailure(Component.translatable("command.nova.show_item.no_data", NamedTextColor.RED))
        } else ctx.source.sendFailure(Component.translatable("command.nova.show_item_data.no_item", NamedTextColor.RED))
    }
    
    private fun toggleNetworkDebugging(ctx: CommandContext<CommandSourceStack>, type: NetworkType<*>) {
        val player = ctx.player
        val enabled = NetworkDebugger.toggleDebugger(type, player)
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.network_debug.${type.id.toLanguageKey()}.${if (enabled) "on" else "off"}",
            NamedTextColor.GRAY
        ))
    }
    
    private fun toggleNetworkClusterDebugging(ctx: CommandContext<CommandSourceStack>) {
        val player = ctx.player
        val enabled = NetworkDebugger.toggleClusterDebugger(player)
        
        ctx.source.sendSuccess(Component.translatable(
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
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.reregister_network_nodes.success",
            NamedTextColor.GRAY,
            Component.text(nodes.size)
        ))
    }
    
    private fun showNetworkNodeInfoLookingAt(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.player.getTargetBlockExact(8)?.location?.pos
        if (pos != null) {
            showNetworkNodeInfo(pos, ctx)
        } else ctx.source.sendFailure(Component.translatable("command.nova.show_network_node_info.failure", NamedTextColor.RED))
    }
    
    private fun showNetworkNodeInfoAt(ctx: CommandContext<CommandSourceStack>) {
        val pos = ctx.get<Coordinates>("pos")
            .getBlockPos(ctx.source)
            .toNovaPos(ctx.source.bukkitWorld!!)
        
        showNetworkNodeInfo(pos, ctx)
    }
    
    private fun showNetworkNodeInfo(pos: BlockPos, ctx: CommandContext<CommandSourceStack>) {
        val node = runBlocking { NetworkManager.getNode(pos) }
        if (node != null) {
            NetworkManager.queueRead(pos.world) { state ->
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
                
                ctx.source.sendSuccess(builder.build())
            }
        } else {
            ctx.source.sendFailure(Component.translatable(
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
        
        ctx.source.sendSuccess(Component.translatable(
            "command.nova.hitbox_debug",
            NamedTextColor.GRAY
        ))
    }
    
    private fun fillArea(block: NovaBlock, ctx: CommandContext<CommandSourceStack>) {
        val world = ctx.source.bukkitWorld!!
        val from = ctx.get<Coordinates>("from").getBlockPos(ctx.source)
        val to = ctx.get<Coordinates>("to").getBlockPos(ctx.source)
        val minX = min(from.x, to.x)
        val maxX = max(from.x, to.x)
        val minY = min(from.y, to.y)
        val maxY = max(from.y, to.y)
        val minZ = min(from.z, to.z)
        val maxZ = max(from.z, to.z)
        
        val placeCtxBuilder = Context.intention(DefaultContextIntentions.BlockPlace)
            .param(DefaultContextParamTypes.BLOCK_TYPE_NOVA, block)
        
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                for (z in minZ..maxZ) {
                    BlockUtils.placeBlock(
                        placeCtxBuilder
                            .param(DefaultContextParamTypes.BLOCK_POS, BlockPos(world, x, y, z))
                            .build()
                    )
                }
            }
        }
        
        ctx.source.sendSuccess(Component.translatable(
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

