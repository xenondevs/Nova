@file:Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.item.logic

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.google.common.hash.HashCode
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.serialization.Dynamic
import io.papermc.paper.command.brigadier.PaperCommands
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import it.unimi.dsi.fastutil.objects.ReferenceSet
import net.minecraft.advancements.Advancement
import net.minecraft.advancements.AdvancementHolder
import net.minecraft.advancements.DisplayInfo
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.ResourceArgument
import net.minecraft.commands.arguments.ResourceKeyArgument
import net.minecraft.commands.arguments.ResourceOrTagArgument
import net.minecraft.commands.arguments.ResourceOrTagKeyArgument
import net.minecraft.commands.arguments.ResourceSelectorArgument
import net.minecraft.commands.synchronization.SuggestionProviders
import net.minecraft.core.Holder
import net.minecraft.core.HolderSet
import net.minecraft.core.component.DataComponentExactPredicate
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.particles.ItemParticleOption
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.HashedPatchMap
import net.minecraft.network.HashedStack
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.ComponentContents
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.network.chat.contents.NbtContents
import net.minecraft.network.chat.contents.ObjectContents
import net.minecraft.network.chat.contents.SelectorContents
import net.minecraft.network.chat.contents.TranslatableContents
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket
import net.minecraft.network.protocol.game.ClientboundCommandsPacket
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.resources.Identifier
import net.minecraft.resources.RegistryOps
import net.minecraft.server.dialog.ActionButton
import net.minecraft.server.dialog.CommonButtonData
import net.minecraft.server.dialog.CommonDialogData
import net.minecraft.server.dialog.ConfirmationDialog
import net.minecraft.server.dialog.Dialog
import net.minecraft.server.dialog.DialogListDialog
import net.minecraft.server.dialog.Input
import net.minecraft.server.dialog.MultiActionDialog
import net.minecraft.server.dialog.NoticeDialog
import net.minecraft.server.dialog.ServerLinksDialog
import net.minecraft.server.dialog.body.DialogBody
import net.minecraft.server.dialog.body.ItemBody
import net.minecraft.server.dialog.body.PlainMessage
import net.minecraft.server.dialog.input.BooleanInput
import net.minecraft.server.dialog.input.InputControl
import net.minecraft.server.dialog.input.NumberRangeInput
import net.minecraft.server.dialog.input.SingleOptionInput
import net.minecraft.server.dialog.input.TextInput
import net.minecraft.tags.ItemTags
import net.minecraft.tags.TagNetworkSerialization
import net.minecraft.util.HashOps
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.ItemStackTemplate
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.BundleContents
import net.minecraft.world.item.component.ChargedProjectiles
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.ItemContainerContents
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.TooltipDisplay
import net.minecraft.world.item.component.UseRemainder
import net.minecraft.world.item.crafting.Ingredient
import net.minecraft.world.item.crafting.SelectableRecipe
import net.minecraft.world.item.crafting.display.FurnaceRecipeDisplay
import net.minecraft.world.item.crafting.display.RecipeDisplay
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay
import net.minecraft.world.item.crafting.display.SlotDisplay
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay
import net.minecraft.world.item.crafting.display.StonecutterRecipeDisplay
import net.minecraft.world.item.trading.ItemCost
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemType
import org.bukkit.persistence.PersistentDataType
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundBlockEntityDataPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundCommandSuggestionsPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundCommandsPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetContentPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetSlotPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundDisconnectPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundDisguisedChatPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLevelParticlesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundLoginDisconnectPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMerchantOffersPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundOpenScreenPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlaceGhostRecipePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlayerChatPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlayerCombatKillPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundRecipeBookAddPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundResourcePackPushPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundServerDataPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetActionBarTextPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetCursorItemPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetEntityDataPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetEquipmentPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetPlayerInventoryPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetScorePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetSubtitleTextPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetTitleTextPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundShowDialogPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSystemChatPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundTabListPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundTestInstanceBlockStatusEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateAdvancementsPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateRecipesPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateTagsPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundContainerClickPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSetCreativeModeSlotPacketEvent
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.asBukkitCopy
import xyz.xenondevs.nova.util.asBukkitMirror
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import xyz.xenondevs.nova.util.data.getStringOrNull
import xyz.xenondevs.nova.util.data.resultFirstOrNull
import xyz.xenondevs.nova.util.getOrNull
import xyz.xenondevs.nova.util.item.unsafeCustomData
import xyz.xenondevs.nova.util.nmsItem
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.toTemplate
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.novaItem
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import kotlin.jvm.optionals.getOrNull
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    runAfter = [ResourceGeneration.PreWorld::class]
)
internal object PacketItems : PacketListener {
    
    val SCROLLABLE_ITEM_HOLDER = BuiltInRegistries.ITEM.wrapAsHolder(Items.STRUCTURE_VOID)
    val ADVANCED_TOOLTIP_OVERRIDE = NamespacedKey("nova", "no_advanced_tooltip")
    val SCROLL_SUPPORT_MARKER = NamespacedKey("nova", "use_scrollable_item")
    const val SERVER_SIDE_COMPONENTS_TAG = "NovaServerSideComponents"
    const val SERVER_SIDE_ITEM_TYPE_TAG = "NovaServerSideType"
    
    private val TOOLTIP_HIDDEN_DATA_COMPONENTS = BuiltInRegistries.DATA_COMPONENT_TYPE.filterTo(LinkedHashSet()) { 
        it != DataComponents.BUNDLE_CONTENTS && it != DataComponents.LORE
    }
    
    @InitFun
    private fun init() {
        registerPacketListener()
    }
    
    //<editor-fold desc="command packet", defaultstate="collapsed">
    private val commandNodeBuilder = object : ClientboundCommandsPacket.NodeBuilder<CommandSourceStack> {
        
        private val AFFECTED_REGISTRY_KEYS = setOf(Registries.ITEM, Registries.BLOCK)
        private val ASK_SERVER_ID = Identifier.withDefaultNamespace("ask_server")
        private val EXECUTABLE_COMMAND = Command<CommandSourceStack> { 0 }
        private val RESTRICTED_REQUIREMENT = object : Predicate<CommandSourceStack>, Commands.RestrictedMarker {
            override fun test(source: CommandSourceStack) = true
        }
        
        override fun createLiteral(id: String): ArgumentBuilder<CommandSourceStack, *> =
            LiteralArgumentBuilder.literal(id)
        
        override fun createArgument(
            id: String,
            argumentType: ArgumentType<*>,
            suggestionId: Identifier?
        ): ArgumentBuilder<CommandSourceStack, *> {
            val builder: RequiredArgumentBuilder<CommandSourceStack, *> =
                RequiredArgumentBuilder.argument(id, argumentType.toClientSideArgumentType())
            val clientSuggestionId = if (argumentType.requiresServerSuggestions()) ASK_SERVER_ID else suggestionId
            if (clientSuggestionId != null)
                builder.suggests(SuggestionProviders.getProvider(clientSuggestionId))
            return builder
        }
        
        override fun configure(
            input: ArgumentBuilder<CommandSourceStack, *>,
            executable: Boolean,
            restricted: Boolean
        ): ArgumentBuilder<CommandSourceStack, *> {
            if (executable)
                input.executes(EXECUTABLE_COMMAND)
            if (restricted)
                input.requires(RESTRICTED_REQUIREMENT)
            return input
        }
        
        private fun ArgumentType<*>.requiresServerSuggestions(): Boolean = when (this) {
            is ResourceArgument<*> -> registryKey
            is ResourceKeyArgument<*> -> registryKey
            is ResourceOrTagArgument<*> -> registryKey
            is ResourceOrTagKeyArgument<*> -> registryKey
            is ResourceSelectorArgument<*> -> registryKey
            else -> return false
        } in AFFECTED_REGISTRY_KEYS
        
        private fun ArgumentType<*>.toClientSideArgumentType(): ArgumentType<*> = when (this) {
            is ResourceArgument<*> if registryKey in AFFECTED_REGISTRY_KEYS -> ResourceKeyArgument.key(registryKey)
            is ResourceOrTagArgument<*> if registryKey in AFFECTED_REGISTRY_KEYS -> ResourceOrTagKeyArgument.resourceOrTagKey(registryKey)
            else -> this
        }
    }
    
    @PacketHandler
    private fun handleCommands(event: ClientboundCommandsPacketEvent) {
        val root = event.packet.getRoot(PaperCommands.INSTANCE.buildContext, commandNodeBuilder)
        event.packet = ClientboundCommandsPacket(root, Commands.COMMAND_NODE_INSPECTOR)
    }
    //</editor-fold>
    
    //<editor-fold desc="chat component packets", defaultstate="collapsed">
    @PacketHandler
    private fun handleCommandSuggestions(event: ClientboundCommandSuggestionsPacketEvent) {
        event.suggestions = event.suggestions.map { entry ->
            ClientboundCommandSuggestionsPacket.Entry(
                entry.text,
                entry.tooltip.map { getClientSideComponent(event.player, it) }
            )
        }
    }
    
    @PacketHandler
    private fun handleSystemChat(event: ClientboundSystemChatPacketEvent) {
        event.content = getClientSideComponent(event.player, event.content)
    }
    
    @PacketHandler
    private fun handleDisguisedChat(event: ClientboundDisguisedChatPacketEvent) {
        event.message = getClientSideComponent(event.player, event.message)
        event.chatType = event.chatType.mapComponents(event.player)
    }
    
    @PacketHandler
    private fun handlePlayerChat(event: ClientboundPlayerChatPacketEvent) {
        event.unsignedContent = event.unsignedContent.map { getClientSideComponent(event.player, it) }
        event.chatType = event.chatType.mapComponents(event.player)
    }
    
    @PacketHandler
    private fun handleOpenScreen(event: ClientboundOpenScreenPacketEvent) {
        event.title = getClientSideComponent(event.player, event.title)
    }
    
    @PacketHandler
    private fun handlePlayerCombatKill(event: ClientboundPlayerCombatKillPacketEvent) {
        event.message = getClientSideComponent(event.player, event.message)
    }
    
    @PacketHandler
    private fun handleServerData(event: ClientboundServerDataPacketEvent) {
        event.motd = getClientSideComponent(event.player, event.motd)
    }
    
    @PacketHandler
    private fun handleActionBar(event: ClientboundSetActionBarTextPacketEvent) {
        event.text = getClientSideComponent(event.player, event.text)
    }
    
    @PacketHandler
    private fun handleTitle(event: ClientboundSetTitleTextPacketEvent) {
        event.text = getClientSideComponent(event.player, event.text)
    }
    
    @PacketHandler
    private fun handleSubtitle(event: ClientboundSetSubtitleTextPacketEvent) {
        event.text = getClientSideComponent(event.player, event.text)
    }
    
    @PacketHandler
    private fun handleScore(event: ClientboundSetScorePacketEvent) {
        event.display = event.display.map { getClientSideComponent(event.player, it) }
    }
    
    @PacketHandler
    private fun handleTabList(event: ClientboundTabListPacketEvent) {
        event.header = getClientSideComponent(event.player, event.header)
        event.footer = getClientSideComponent(event.player, event.footer)
    }
    
    @PacketHandler
    private fun handleTestInstanceStatus(event: ClientboundTestInstanceBlockStatusEvent) {
        event.status = getClientSideComponent(event.player, event.status)
    }
    
    @PacketHandler
    private fun handleResourcePackPrompt(event: ClientboundResourcePackPushPacketEvent) {
        event.prompt = event.prompt.map { getClientSideComponent(null, it) }
    }
    
    @PacketHandler
    private fun handleDisconnect(event: ClientboundDisconnectPacketEvent) {
        event.reason = getClientSideComponent(null, event.reason)
    }
    
    @PacketHandler
    private fun handleLoginDisconnect(event: ClientboundLoginDisconnectPacketEvent) {
        event.reason = getClientSideComponent(null, event.reason)
    }
    
    private fun ChatType.Bound.mapComponents(player: Player) =
        ChatType.Bound(
            chatType,
            getClientSideComponent(player, name),
            targetName.map { getClientSideComponent(player, it) }
        )
    
    private fun getClientSideComponent(player: Player?, component: Component): Component {
        val result = getClientSideContents(player, component.contents)
            .setStyle(getClientSideStyle(player, component.style))
        component.siblings.forEach { result.append(getClientSideComponent(player, it)) }
        return result
    }
    
    private fun getClientSideContents(player: Player?, contents: ComponentContents): MutableComponent = when (contents) {
        is TranslatableContents -> Component.translatableWithFallback(
            contents.key,
            contents.fallback,
            *contents.args.map { if (it is Component) getClientSideComponent(player, it) else it }.toTypedArray()
        )
        
        is SelectorContents -> MutableComponent.create(SelectorContents(
            contents.selector,
            contents.separator.map { getClientSideComponent(player, it) }
        ))
        
        is NbtContents -> MutableComponent.create(NbtContents(
            contents.nbtPath,
            contents.interpreting,
            contents.plain,
            contents.separator.map { getClientSideComponent(player, it) },
            contents.dataSource
        ))
        
        is ObjectContents -> MutableComponent.create(ObjectContents(
            contents.contents,
            contents.fallback.map { getClientSideComponent(player, it) }
        ))
        
        else -> MutableComponent.create(contents)
    }
    
    private fun getClientSideStyle(player: Player?, style: Style): Style {
        val hoverEvent = when (val hover = style.hoverEvent) {
            is HoverEvent.ShowText -> HoverEvent.ShowText(getClientSideComponent(player, hover.value))
            is HoverEvent.ShowItem -> HoverEvent.ShowItem(getClientSideItem(player, hover.item))
            else -> hover
        }
        return style.withHoverEvent(hoverEvent)
    }
    
    private fun getClientSideItem(player: Player?, template: ItemStackTemplate): ItemStackTemplate {
        val stack = getClientSideStack(player, template.create(), false)
        return ItemStackTemplate.fromNonEmptyStack(stack)
    }
    
    @PacketHandler
    private fun handleLevelParticles(event: ClientboundLevelParticlesPacketEvent) {
        val particle = event.particle
        if (particle is ItemParticleOption)
            event.particle = ItemParticleOption(particle.type, getClientSideItem(event.player, particle.item))
    }
    
    @PacketHandler
    private fun handleShowDialog(event: ClientboundShowDialogPacketEvent) {
        event.dialog = getClientSideDialogHolder(null, event.dialog, Collections.newSetFromMap(IdentityHashMap()))
    }
    
    private fun getClientSideDialogHolder(player: Player?, holder: Holder<Dialog>, visiting: MutableSet<Dialog>): Holder<Dialog> {
        val dialog = holder.value()
        if (!visiting.add(dialog))
            return holder
        
        val rewritten = getClientSideDialog(player, dialog, visiting)
        visiting.remove(dialog)
        return Holder.direct(rewritten)
    }
    
    private fun getClientSideDialog(player: Player?, dialog: Dialog, visiting: MutableSet<Dialog>): Dialog = when (dialog) {
        is ConfirmationDialog -> ConfirmationDialog(
            getClientSideCommonDialogData(player, dialog.common),
            getClientSideActionButton(player, dialog.yesButton),
            getClientSideActionButton(player, dialog.noButton)
        )
        
        is DialogListDialog -> DialogListDialog(
            getClientSideCommonDialogData(player, dialog.common),
            HolderSet.direct(dialog.dialogs.map { getClientSideDialogHolder(player, it, visiting) }),
            dialog.exitAction.map { getClientSideActionButton(player, it) },
            dialog.columns,
            dialog.buttonWidth
        )
        
        is MultiActionDialog -> MultiActionDialog(
            getClientSideCommonDialogData(player, dialog.common),
            dialog.actions.map { getClientSideActionButton(player, it) },
            dialog.exitAction.map { getClientSideActionButton(player, it) },
            dialog.columns
        )
        
        is NoticeDialog -> NoticeDialog(
            getClientSideCommonDialogData(player, dialog.common),
            getClientSideActionButton(player, dialog.action)
        )
        
        is ServerLinksDialog -> ServerLinksDialog(
            getClientSideCommonDialogData(player, dialog.common),
            dialog.exitAction.map { getClientSideActionButton(player, it) },
            dialog.columns,
            dialog.buttonWidth
        )
        
        else -> dialog
    }
    
    private fun getClientSideCommonDialogData(player: Player?, data: CommonDialogData) = CommonDialogData(
        getClientSideComponent(player, data.title),
        data.externalTitle.map { getClientSideComponent(player, it) },
        data.canCloseWithEscape,
        data.pause,
        data.afterAction,
        data.body.map { getClientSideDialogBody(player, it) },
        data.inputs.map { getClientSideInput(player, it) }
    )
    
    private fun getClientSideDialogBody(player: Player?, body: DialogBody): DialogBody = when (body) {
        is ItemBody -> ItemBody(
            getClientSideItem(player, body.item),
            body.description.map { getClientSidePlainMessage(player, it) },
            body.showDecorations,
            body.showTooltip,
            body.width,
            body.height
        )
        
        is PlainMessage -> getClientSidePlainMessage(player, body)
        else -> body
    }
    
    private fun getClientSidePlainMessage(player: Player?, message: PlainMessage) = PlainMessage(
        getClientSideComponent(player, message.contents),
        message.width
    )
    
    private fun getClientSideInput(player: Player?, input: Input) = Input(input.key, getClientSideInputControl(player, input.control))
    
    private fun getClientSideInputControl(player: Player?, control: InputControl): InputControl = when (control) {
        is BooleanInput -> BooleanInput(
            getClientSideComponent(player, control.label),
            control.initial,
            control.onTrue,
            control.onFalse
        )
        
        is NumberRangeInput -> NumberRangeInput(
            control.width,
            getClientSideComponent(player, control.label),
            control.labelFormat,
            control.rangeInfo
        )
        
        is SingleOptionInput -> SingleOptionInput(
            control.width,
            control.entries.map { entry ->
                SingleOptionInput.Entry(
                    entry.id,
                    entry.display.map { getClientSideComponent(player, it) },
                    entry.initial
                )
            },
            getClientSideComponent(player, control.label),
            control.labelVisible
        )
        
        is TextInput -> TextInput(
            control.width,
            getClientSideComponent(player, control.label),
            control.labelVisible,
            control.initial,
            control.maxLength,
            control.multiline
        )
        
        else -> control
    }
    
    private fun getClientSideActionButton(player: Player?, button: ActionButton) = ActionButton(
        CommonButtonData(
            getClientSideComponent(player, button.button.label),
            button.button.tooltip.map { getClientSideComponent(player, it) },
            button.button.width
        ),
        button.action
    )
    //</editor-fold>
    
    //<editor-fold desc="block packets", defaultstate="collapsed">
    @Suppress("DEPRECATION")
    @PacketHandler
    private fun handleBlockEntityData(event: ClientboundBlockEntityDataPacketEvent) {
        if (event.type.builtInRegistryHolder().key().identifier().namespace != "minecraft")
            event.isCancelled = true
    }
    //</editor-fold>
    
    //<editor-fold desc="item packets", defaultstate="collapsed">
    @PacketHandler
    private fun handleSetContentPacket(event: ClientboundContainerSetContentPacketEvent) {
        val player = event.player
        event.items = event.items.map { getClientSideStack(player, it) }
        event.carriedItem = getClientSideStack(player, event.carriedItem)
    }
    
    @PacketHandler
    private fun handleSetSlotPacket(event: ClientboundContainerSetSlotPacketEvent) {
        event.item = getClientSideStack(event.player, event.item)
    }
    
    @PacketHandler
    private fun handleSetCursorPacket(event: ClientboundSetCursorItemPacketEvent) {
        event.contents = getClientSideStack(event.player, event.contents.copy())
    }
    
    @PacketHandler
    private fun handleSetPlayerInventory(event: ClientboundSetPlayerInventoryPacketEvent) {
        event.contents = getClientSideStack(event.player, event.contents)
    }
    
    @PacketHandler
    private fun handleEntityData(event: ClientboundSetEntityDataPacketEvent) {
        val oldItems = event.packedItems
        val newItems = ArrayList<DataValue<*>>()
        for (dataValue in oldItems) {
            val value = dataValue.value
            if (value is MojangStack) {
                newItems += DataValue(
                    dataValue.id,
                    EntityDataSerializers.ITEM_STACK,
                    getClientSideStack(event.player, value, false)
                )
            } else {
                newItems += dataValue
            }
        }
        event.packedItems = newItems
    }
    
    @PacketHandler
    private fun handleSetEquipment(event: ClientboundSetEquipmentPacketEvent) {
        val player = event.player
        val slots = ArrayList(event.slots).also { event.slots = it }
        
        for ([i, pair] in slots.withIndex()) {
            slots[i] = MojangPair(
                pair.first,
                getClientSideStack(player, pair.second)
            )
        }
    }
    
    @PacketHandler
    private fun handleClick(event: ServerboundContainerClickPacketEvent) {
        val playerId = event.player.uniqueId
        event.changedSlots = event.changedSlots.mapValuesTo(Int2ObjectOpenHashMap()) { [_, stack] ->
            stackHashMappings.getIfPresent(stack.toCacheKey(playerId)) ?: stack
        }
        event.carriedItem = stackHashMappings.getIfPresent(event.carriedItem.toCacheKey(playerId)) ?: event.carriedItem
    }
    
    @PacketHandler
    private fun handleCreativeSetItem(event: ServerboundSetCreativeModeSlotPacketEvent) {
        event.itemStack = getServerSideStack(event.itemStack)
    }
    
    @PacketHandler
    private fun handleRecipeBookAdd(event: ClientboundRecipeBookAddPacketEvent) {
        event.entries = event.entries.map { entry ->
            val contents = entry.contents
            ClientboundRecipeBookAddPacket.Entry(
                RecipeDisplayEntry(
                    contents.id,
                    getClientSideRecipeDisplay(event.player, contents.display),
                    contents.group,
                    contents.category,
                    getClientSideIngredientList(event.player, contents.craftingRequirements)
                ),
                entry.notification(),
                entry.highlight()
            )
        }
    }
    
    @PacketHandler
    private fun handlePlaceGhostRecipe(event: ClientboundPlaceGhostRecipePacketEvent) {
        event.recipeDisplay = getClientSideRecipeDisplay(event.player, event.recipeDisplay)
    }
    
    @PacketHandler
    private fun handleMerchantOffers(event: ClientboundMerchantOffersPacketEvent) {
        val newOffers = MerchantOffers()
        event.offers.forEach { offer ->
            newOffers += MerchantOffer(
                offer.baseCostA.itemStack.let {
                    val stackA = getClientSideStack(event.player, it)
                    ItemCost(stackA.typeHolder(), stackA.count, DataComponentExactPredicate.EMPTY, stackA)
                },
                offer.costB.map {
                    val stackB = getClientSideStack(event.player, it.itemStack)
                    ItemCost(stackB.typeHolder(), stackB.count, DataComponentExactPredicate.EMPTY, stackB)
                },
                getClientSideStack(event.player, offer.result),
                offer.uses,
                offer.maxUses,
                offer.rewardExp,
                offer.specialPriceDiff,
                offer.demand,
                offer.priceMultiplier,
                offer.xp,
                offer.ignoreDiscounts
            )
        }
        
        event.offers = newOffers
    }
    
    @PacketHandler
    private fun handleRecipes(event: ClientboundUpdateRecipesPacketEvent) {
        event.stonecutterRecipes = SelectableRecipe.SingleInputSet(
            event.stonecutterRecipes.entries().map { entry ->
                SelectableRecipe.SingleInputEntry(
                    getClientSideIngredient(event.player, entry.input()),
                    SelectableRecipe(
                        getClientSideSlotDisplay(event.player, entry.recipe().optionDisplay()),
                        entry.recipe().recipe()
                    )
                )
            }
        )
    }
    
    @PacketHandler
    private fun handleAdvancements(event: ClientboundUpdateAdvancementsPacketEvent) {
        event.added = event.added.map { (advancement, x, y) ->
            ClientboundUpdateAdvancementsPacket.PositionedAdvancement(AdvancementHolder(
                advancement.id,
                Advancement(
                    advancement.value.parent,
                    advancement.value.display.map { display ->
                        DisplayInfo(
                            getClientSideStack(event.player, display.icon.create(), false).toTemplate()!!,
                            getClientSideComponent(event.player, display.title),
                            getClientSideComponent(event.player, display.description),
                            display.background,
                            display.type,
                            display.showToast,
                            display.announceToChat,
                            display.hidden
                        )
                    },
                    advancement.value.rewards,
                    advancement.value.criteria,
                    advancement.value.requirements,
                    advancement.value.sendsTelemetryEvent,
                    advancement.value.name
                )
            ), x, y)
        }
    }
    
    @PacketHandler
    private fun handleRegistryData(event: ClientboundUpdateTagsPacketEvent) {
        // removes nova items from tags
        // inject STRUCTURE_VOID into the minecraft:bundles tag for scroll support
        event.tags = event.tags.mapValues { [key, payloads] ->
            if (key != Registries.ITEM)
                return@mapValues payloads
            
            val tags = payloads
                .resolve(REGISTRY_ACCESS.lookupOrThrow(Registries.ITEM))
                .tags()
                .toMutableMap()
            
            tags.compute(ItemTags.BUNDLES) { _, previousTagValues ->
                buildList {
                    addAll(previousTagValues ?: emptyList())
                    add(SCROLLABLE_ITEM_HOLDER)
                }
            }
            
            val serialized = tags.entries.associate { [tagKey, tagValues] ->
                val registry = REGISTRY_ACCESS.lookupOrThrow(tagKey.registry())
                tagKey.location() to IntArrayList(tagValues.size).apply {
                    for (holder in tagValues) {
                        val item = holder.value()
                        if (item !is NovaItem)
                            add(registry.getId(item))
                    }
                }
            }
            
            TagNetworkSerialization.NetworkPayload(serialized)
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="server-side recipe -> client-side recipe">
    private fun getClientSideIngredientList(player: Player, optList: Optional<List<Ingredient>>): Optional<List<Ingredient>> =
        optList.map { ingredientList -> ingredientList.map { getClientSideIngredient(player, it) } }
    
    private fun getClientSideIngredient(player: Player, ingredient: Ingredient): Ingredient {
        val itemStacks = ingredient.itemStacks()
        if (itemStacks != null) {
            return Ingredient.ofStacks(itemStacks.map { getClientSideStack(player, it, false) })
        } else {
            return ingredient
        }
    }
    
    private fun getClientSideRecipeDisplay(player: Player, display: RecipeDisplay): RecipeDisplay = when (display) {
        is FurnaceRecipeDisplay -> FurnaceRecipeDisplay(
            getClientSideSlotDisplay(player, display.ingredient),
            getClientSideSlotDisplay(player, display.fuel),
            getClientSideSlotDisplay(player, display.result),
            getClientSideSlotDisplay(player, display.craftingStation),
            display.duration,
            display.experience
        )
        
        is ShapedCraftingRecipeDisplay -> ShapedCraftingRecipeDisplay(
            display.width, display.height,
            display.ingredients.map { getClientSideSlotDisplay(player, it) },
            getClientSideSlotDisplay(player, display.result),
            getClientSideSlotDisplay(player, display.craftingStation)
        )
        
        is ShapelessCraftingRecipeDisplay -> ShapelessCraftingRecipeDisplay(
            display.ingredients.map { getClientSideSlotDisplay(player, it) },
            getClientSideSlotDisplay(player, display.result),
            getClientSideSlotDisplay(player, display.craftingStation)
        )
        
        is SmithingRecipeDisplay -> SmithingRecipeDisplay(
            getClientSideSlotDisplay(player, display.template),
            getClientSideSlotDisplay(player, display.base),
            getClientSideSlotDisplay(player, display.addition),
            getClientSideSlotDisplay(player, display.result),
            getClientSideSlotDisplay(player, display.craftingStation)
        )
        
        is StonecutterRecipeDisplay -> StonecutterRecipeDisplay(
            getClientSideSlotDisplay(player, display.input),
            getClientSideSlotDisplay(player, display.result),
            getClientSideSlotDisplay(player, display.craftingStation)
        )
        
        else -> {
            LOGGER.warn("Unknown recipe display type: ${display.javaClass}")
            display
        }
    }
    
    private fun getClientSideSlotDisplay(player: Player, display: SlotDisplay): SlotDisplay = when (display) {
        is SlotDisplay.Composite -> SlotDisplay.Composite(
            display.contents.map { getClientSideSlotDisplay(player, it) }
        )
        
        is SlotDisplay.ItemStackSlotDisplay -> SlotDisplay.ItemStackSlotDisplay(
            ItemStackTemplate.fromNonEmptyStack(getClientSideStack(player, display.stack.create(), false))
        )
        
        is SlotDisplay.SmithingTrimDemoSlotDisplay -> SlotDisplay.SmithingTrimDemoSlotDisplay(
            getClientSideSlotDisplay(player, display.base),
            getClientSideSlotDisplay(player, display.material),
            display.pattern
        )
        
        is SlotDisplay.WithRemainder -> SlotDisplay.WithRemainder(
            getClientSideSlotDisplay(player, display.input),
            getClientSideSlotDisplay(player, display.remainder)
        )
        
        is SlotDisplay.WithAnyPotion -> SlotDisplay.WithAnyPotion(
            getClientSideSlotDisplay(player, display.display)
        )
        
        is SlotDisplay.OnlyWithComponent -> SlotDisplay.OnlyWithComponent(
            getClientSideSlotDisplay(player, display.source),
            display.component
        )
        
        is SlotDisplay.DyedSlotDemo -> SlotDisplay.DyedSlotDemo(
            getClientSideSlotDisplay(player, display.dye),
            getClientSideSlotDisplay(player, display.target),
        )
        
        is SlotDisplay.AnyFuel,
        is SlotDisplay.Empty,
        is SlotDisplay.ItemSlotDisplay,
        is SlotDisplay.TagSlotDisplay -> display
        
        else -> {
            LOGGER.warn("Unknown slot display type: ${display.javaClass}")
            display
        }
    }
    //</editor-fold>
    
    //<editor-fold desc="server-side stack -> client-side stack", defaultstate="collapsed">
    fun getClientSideStack(player: Player?, serverSideStack: MojangStack, storeServerSideTag: Boolean = true): MojangStack {
        if (serverSideStack.isEmpty)
            return MojangStack.EMPTY
        
        val severSideType = serverSideStack.typeHolder()
        val novaItem = serverSideStack.novaItem
        
        // client-side item stack copy
        val clientSideType: Holder<Item>
        if (serverSideStack.asBukkitMirror().persistentDataContainer.get(SCROLL_SUPPORT_MARKER, PersistentDataType.BOOLEAN) == true) {
            clientSideType = SCROLLABLE_ITEM_HOLDER
        } else if (novaItem != null) {
            clientSideType = novaItem.modifyClientSideItemType(player, serverSideStack.asBukkitCopy(), ItemType.SHULKER_SHELL).nmsItem.builtInRegistryHolder()
        } else {
            clientSideType = severSideType
        }
        
        var clientSideStack = MojangStack(
            clientSideType, serverSideStack.count,
            buildClientSideDataComponentsPatch(severSideType, clientSideType, serverSideStack.componentsPatch)
        )
        
        // customization through item behaviors
        if (novaItem != null) {
            clientSideStack = novaItem.modifyClientSideStack(player, serverSideStack.asBukkitCopy(), clientSideStack.asBukkitMirror()).unwrap()
        }
        
        fixNestedStacks(player, clientSideStack)
        
        // generate tooltip server-side and apply as lore
        // we do not want data component modifications done by item behaviors in modifyClientSideStack
        // to be reflected in the tooltip, except for the item lore itself
        val itemStackToGenerateTooltipOf = serverSideStack.copy()
        itemStackToGenerateTooltipOf.set(DataComponents.LORE, clientSideStack.get(DataComponents.LORE))
        applyServerSideTooltip(clientSideStack, generateTooltipLore(player, itemStackToGenerateTooltipOf))
        
        // save server-side nbt data (for creative mode)
        // this also drops existing custom data, which is ignored by the client anyway
        if (storeServerSideTag)
            storeServerSideTag(clientSideStack, serverSideStack)
        
        if (player != null) {
            stackHashMappings.put(
                HashedStack.create(clientSideStack, HASH_GENERATOR).toCacheKey(player.uniqueId),
                HashedStack.create(serverSideStack, HASH_GENERATOR)
            )
        }
        
        return clientSideStack
    }
    
    private fun buildClientSideDataComponentsPatch(server: Holder<Item>, client: Holder<Item>, patch: DataComponentPatch): DataComponentPatch {
        val builder = DataComponentPatch.builder()
        
        // remove client-side default base components
        for (vanillaBase in client.components()) {
            builder.remove(vanillaBase.type)
        }
        
        // add server-side default base components
        mergeIntoClientSidePatch(builder, server.components())
        // add item stack patch components
        mergeIntoClientSidePatch(builder, patch)
        
        return builder.build()
    }
    
    private fun mergeIntoClientSidePatch(builder: DataComponentPatch.Builder, base: DataComponentMap) {
        for (customBase in base) {
            if (isIrrelevantClientSideComponent(customBase.type))
                continue
            
            builder.set(customBase)
        }
    }
    
    private fun mergeIntoClientSidePatch(builder: DataComponentPatch.Builder, patch: DataComponentPatch) {
        val (added, removed) = patch.split()
        for (component in added) {
            if (isIrrelevantClientSideComponent(component.type))
                continue
            builder.set(component)
        }
        for (type in removed) {
            if (!isIrrelevantClientSideComponent(type)) {
                builder.remove(type)
            }
        }
    }
    
    private fun isIrrelevantClientSideComponent(type: DataComponentType<*>): Boolean {
        return type == DataComponents.CUSTOM_DATA
    }
    
    private fun fixNestedStacks(player: Player?, itemStack: MojangStack): Boolean {
        var modified = false
        
        itemStack.get(DataComponents.BUNDLE_CONTENTS)?.let { contents ->
            itemStack.set(
                DataComponents.BUNDLE_CONTENTS,
                BundleContents(contents.items().map { getClientSideItem(player, it) })
            )
            modified = true
        }
        
        itemStack.get(DataComponents.CHARGED_PROJECTILES)?.let { projectiles ->
            itemStack.set(
                DataComponents.CHARGED_PROJECTILES,
                ChargedProjectiles(projectiles.items.map { getClientSideItem(player, it) })
            )
            modified = true
        }
        
        itemStack.get(DataComponents.CONTAINER)?.let { contents ->
            itemStack.set(
                DataComponents.CONTAINER,
                ItemContainerContents.fromItems(
                    contents.itemCopies()
                        .map { getClientSideStack(player, it, false) }
                        .toList()
                )
            )
            modified = true
        }
        
        itemStack.get(DataComponents.USE_REMAINDER)?.let { remainder ->
            itemStack.set(
                DataComponents.USE_REMAINDER,
                UseRemainder(getClientSideItem(player, remainder.convertInto))
            )
            modified = true
        }
        
        return modified
    }
    
    //<editor-fold desc="tooltip", defaultstate="collapsed">
    private fun applyServerSideTooltip(itemStack: ItemStack, tooltip: List<Component>) {
        val lore = tooltip.fold(ItemLore.EMPTY) { l, line ->
            l.withLineAdded(line.withoutPreFormatting())
        }
        itemStack.set(DataComponents.LORE, lore)
        disableClientSideTooltip(itemStack)
    }
    
    private fun generateTooltipLore(player: Player?, serverSideStack: MojangStack): List<Component> {
        val isAdvanced = (serverSideStack.asBukkitMirror().persistentDataContainer.get(ADVANCED_TOOLTIP_OVERRIDE, PersistentDataType.BOOLEAN)
            ?: (serverSideStack.novaItem?.isHidden != true && player?.let(AdvancedTooltips::get) == true))
        
        val lore = serverSideStack.getTooltipLines(
            Item.TooltipContext.of(REGISTRY_ACCESS),
            player?.serverPlayer,
            if (isAdvanced) TooltipFlag.ADVANCED else TooltipFlag.NORMAL
        )
        
        // entire tooltip is hidden
        if (lore.isEmpty())
            return emptyList()
        
        lore.removeAt(0) // item name
        
        return lore
    }
    
    private fun disableClientSideTooltip(itemStack: MojangStack) {
        itemStack.set(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay(
            itemStack.get(DataComponents.TOOLTIP_DISPLAY)?.hideTooltip == true,
            TOOLTIP_HIDDEN_DATA_COMPONENTS
        ))
    }
    //</editor-fold>
    //</editor-fold>
    
    //<editor-fold desc="client-side stack -> server-side stack", defaultstate="collapsed">
    fun getServerSideStack(itemStack: MojangStack): MojangStack {
        val nbt = itemStack.unsafeCustomData
        val serverSideType = nbt
            ?.getStringOrNull(SERVER_SIDE_ITEM_TYPE_TAG)
            ?.let { BuiltInRegistries.ITEM.getOrNull(it) }
            ?: return itemStack
        val serverSideComponents = nbt
            .getCompoundOrNull(SERVER_SIDE_COMPONENTS_TAG)
            ?.let(::decodeComponents)
            ?: return itemStack
        return MojangStack(serverSideType, itemStack.count, serverSideComponents)
    }
    //</editor-fold>
    
    //<editor-fold desc="hashed stack caching", defaultstate="collapsed">
    // HashedStack does not have proper equals/hashCode as internal collections are (sometimes) identity-based
    private sealed interface HashedStackKey {
        
        data object Empty : HashedStackKey
        
        data class ActualItem(
            val playerUuid: UUID,
            val item: Holder<Item>,
            val count: Int,
            val addedComponents: Reference2IntMap<DataComponentType<*>>,
            val removedComponents: ReferenceSet<DataComponentType<*>>
        ) : HashedStackKey
    }
    
    private val stackHashMappings = Caffeine.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .build<HashedStackKey, HashedStack>()
    
    private val HASH_GENERATOR: HashedPatchMap.HashGenerator = object : HashedPatchMap.HashGenerator {
        
        private val hashOps = RegistryOps.create<HashCode>(HashOps.CRC32C_INSTANCE, REGISTRY_ACCESS)
        private val cache: LoadingCache<TypedDataComponent<*>, Int> = Caffeine.newBuilder()
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build { it.encodeValue(hashOps).getOrThrow()!!.asInt() }
        
        override fun apply(tdc: TypedDataComponent<*>): Int {
            return cache.get(tdc)
        }
        
    }
    
    private fun HashedStack.toCacheKey(playerUuid: UUID): HashedStackKey =
        when (this) {
            is HashedStack.ActualItem -> HashedStackKey.ActualItem(
                playerUuid,
                item,
                count,
                Reference2IntOpenHashMap(components.addedComponents()),
                ReferenceOpenHashSet(components.removedComponents())
            )
            
            else -> HashedStackKey.Empty
        }
    //</editor-fold>
    
    private fun storeServerSideTag(clientSide: MojangStack, serverSide: MojangStack) {
        clientSide.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply {
            val type = serverSide.typeHolder().unwrapKey().getOrNull()?.identifier()?.toString()
            putString(SERVER_SIDE_ITEM_TYPE_TAG, type ?: "")
            put(SERVER_SIDE_COMPONENTS_TAG, encodeComponents(serverSide))
        }))
    }
    
    private fun encodeComponents(itemStack: MojangStack): Tag {
        val patch = itemStack.componentsPatch
        return DataComponentPatch.CODEC.encodeStart(
            RegistryOps.create(NbtOps.INSTANCE, REGISTRY_ACCESS),
            patch
        ).getOrThrow()
    }
    
    private fun decodeComponents(components: Tag): DataComponentPatch? {
        return DataComponentPatch.CODEC.decode(Dynamic(
            RegistryOps.create(NbtOps.INSTANCE, REGISTRY_ACCESS),
            components
        )).resultFirstOrNull()
    }
    
}
