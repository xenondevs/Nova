@file:Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.world.item.logic

import com.mojang.serialization.Dynamic
import net.minecraft.ChatFormatting
import net.minecraft.core.component.DataComponentExactPredicate
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponentType
import net.minecraft.core.component.DataComponents
import net.minecraft.core.component.TypedDataComponent
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.nbt.Tag
import net.minecraft.network.HashedPatchMap
import net.minecraft.network.HashedStack
import net.minecraft.network.chat.Component
import net.minecraft.network.protocol.game.ClientboundRecipeBookAddPacket
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.resources.RegistryOps
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.TooltipFlag
import net.minecraft.world.item.component.BundleContents
import net.minecraft.world.item.component.CustomData
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.component.ItemLore
import net.minecraft.world.item.component.TooltipDisplay
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
import org.apache.commons.lang3.math.Fraction
import org.bukkit.Material
import org.bukkit.craftbukkit.util.CraftMagicNumbers
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.network.event.PacketHandler
import xyz.xenondevs.nova.network.event.PacketListener
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetContentPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundContainerSetSlotPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundMerchantOffersPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundPlaceGhostRecipePacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundRecipeBookAddPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetCursorItemPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetEntityDataPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundSetEquipmentPacketEvent
import xyz.xenondevs.nova.network.event.clientbound.ClientboundUpdateRecipesPacketEvent
import xyz.xenondevs.nova.network.event.registerPacketListener
import xyz.xenondevs.nova.network.event.serverbound.ServerboundContainerClickPacketEvent
import xyz.xenondevs.nova.network.event.serverbound.ServerboundSetCreativeModeSlotPacketEvent
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.resources.ResourceGeneration
import xyz.xenondevs.nova.util.REGISTRY_ACCESS
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.data.getCompoundOrNull
import xyz.xenondevs.nova.util.data.getFirstOrThrow
import xyz.xenondevs.nova.util.data.getStringOrNull
import xyz.xenondevs.nova.util.getValue
import xyz.xenondevs.nova.util.item.novaCompound
import xyz.xenondevs.nova.util.item.unsafeCustomData
import xyz.xenondevs.nova.util.item.unsafeNovaTag
import xyz.xenondevs.nova.util.item.update
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.serverPlayer
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import java.util.*
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [ResourceGeneration.PreWorld::class]
)
internal object PacketItems : Listener, PacketListener {
    
    val SERVER_SIDE_MATERIAL = Material.SHULKER_SHELL
    val SERVER_SIDE_ITEM = CraftMagicNumbers.getItem(SERVER_SIDE_MATERIAL)!!
    val SERVER_SIDE_ITEM_HOLDER = BuiltInRegistries.ITEM.wrapAsHolder(SERVER_SIDE_ITEM)
    const val SKIP_SERVER_SIDE_TOOLTIP = "NovaSkipPacketItems"
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    //<editor-fold desc="packet events", defaultstate="collapsed">
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
        event.item = getClientSideStack(event.player, event.item.copy())
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
        
        for ((i, pair) in slots.withIndex()) {
            slots[i] = MojangPair(
                pair.first,
                getClientSideStack(player, pair.second)
            )
        }
    }
    
    @PacketHandler
    private fun handleClick(event: ServerboundContainerClickPacketEvent) {
        // fixme: HashedStack breaks the approach of saving server-side data in the client-side stack via custom data
        //        This could be solved by caching a mapping from client-side stack to server-side stack during getClientSideStack
        
        // carried item needs to always be set to an item that triggers an update to prevent desync on single-item drag
        event.carriedItem = HashedStack.ActualItem(Items.DIRT.builtInRegistryHolder(), -1, HashedPatchMap(emptyMap(), emptySet()))
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
                    getClientSideRecipeDisplay(contents.display),
                    contents.group,
                    contents.category,
                    getClientSideIngredientList(contents.craftingRequirements)
                ),
                entry.notification(),
                entry.highlight()
            )
        }
    }
    
    @PacketHandler
    private fun handlePlaceGhostRecipe(event: ClientboundPlaceGhostRecipePacketEvent) {
        event.recipeDisplay = getClientSideRecipeDisplay(event.recipeDisplay)
    }
    
    @PacketHandler
    private fun handleMerchantOffers(event: ClientboundMerchantOffersPacketEvent) {
        val newOffers = MerchantOffers()
        event.offers.forEach { offer ->
            newOffers += MerchantOffer(
                offer.baseCostA.itemStack.let {
                    val stackA = getClientSideStack(event.player, it)
                    ItemCost(stackA.itemHolder, stackA.count, DataComponentExactPredicate.EMPTY, stackA)
                },
                offer.costB.map {
                    val stackB = getClientSideStack(event.player, it.itemStack)
                    ItemCost(stackB.itemHolder, stackB.count, DataComponentExactPredicate.EMPTY, stackB)
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
                    getClientSideIngredient(entry.input()),
                    SelectableRecipe(
                        getClientSideSlotDisplay(entry.recipe().optionDisplay()),
                        entry.recipe().recipe()
                    )
                )
            }
        )
    }
    
    //</editor-fold>
    
    //<editor-fold desc="server-side recipe -> client-side recipe">
    private fun getClientSideIngredientList(optList: Optional<List<Ingredient>>): Optional<List<Ingredient>> =
        optList.map { ingredientList -> ingredientList.map(::getClientSideIngredient) }
    
    private fun getClientSideIngredient(ingredient: Ingredient): Ingredient {
        val itemStacks = ingredient.itemStacks()
        if (itemStacks != null) {
            return Ingredient.ofStacks(itemStacks.map { getClientSideStack(null, it, false) })
        } else {
            return ingredient
        }
    }
    
    private fun getClientSideRecipeDisplay(display: RecipeDisplay): RecipeDisplay = when (display) {
        is FurnaceRecipeDisplay -> FurnaceRecipeDisplay(
            getClientSideSlotDisplay(display.ingredient),
            getClientSideSlotDisplay(display.fuel),
            getClientSideSlotDisplay(display.result),
            getClientSideSlotDisplay(display.craftingStation),
            display.duration,
            display.experience
        )
        
        is ShapedCraftingRecipeDisplay -> ShapedCraftingRecipeDisplay(
            display.width, display.height,
            display.ingredients.map(::getClientSideSlotDisplay),
            getClientSideSlotDisplay(display.result),
            getClientSideSlotDisplay(display.craftingStation)
        )
        
        is ShapelessCraftingRecipeDisplay -> ShapelessCraftingRecipeDisplay(
            display.ingredients.map(::getClientSideSlotDisplay),
            getClientSideSlotDisplay(display.result),
            getClientSideSlotDisplay(display.craftingStation)
        )
        
        is SmithingRecipeDisplay -> SmithingRecipeDisplay(
            getClientSideSlotDisplay(display.template),
            getClientSideSlotDisplay(display.base),
            getClientSideSlotDisplay(display.addition),
            getClientSideSlotDisplay(display.result),
            getClientSideSlotDisplay(display.craftingStation)
        )
        
        is StonecutterRecipeDisplay -> StonecutterRecipeDisplay(
            getClientSideSlotDisplay(display.input),
            getClientSideSlotDisplay(display.result),
            getClientSideSlotDisplay(display.craftingStation)
        )
        
        else -> {
            LOGGER.warn("Unknown recipe display type: ${display.javaClass}")
            display
        }
    }
    
    private fun getClientSideSlotDisplay(display: SlotDisplay): SlotDisplay = when (display) {
        is SlotDisplay.Composite -> SlotDisplay.Composite(
            display.contents.map(::getClientSideSlotDisplay)
        )
        
        is SlotDisplay.ItemStackSlotDisplay -> SlotDisplay.ItemStackSlotDisplay(
            getClientSideNovaStack(null, display.stack, false)
        )
        
        is SlotDisplay.SmithingTrimDemoSlotDisplay -> SlotDisplay.SmithingTrimDemoSlotDisplay(
            getClientSideSlotDisplay(display.base),
            getClientSideSlotDisplay(display.material),
            display.pattern
        )
        
        is SlotDisplay.WithRemainder -> SlotDisplay.WithRemainder(
            getClientSideSlotDisplay(display.input),
            getClientSideSlotDisplay(display.remainder)
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
    fun getClientSideStack(player: Player?, itemStack: MojangStack, storeServerSideTag: Boolean = true): MojangStack {
        if (itemStack.isEmpty)
            return itemStack
        
        return if (itemStack.unsafeNovaTag != null) {
            getClientSideNovaStack(player, itemStack, storeServerSideTag)
        } else getClientSideVanillaStack(player, itemStack, storeServerSideTag)
    }
    
    //<editor-fold desc="Nova", defaultstate="collapsed">
    private fun getClientSideNovaStack(player: Player?, itemStack: MojangStack, storeServerSideTag: Boolean): MojangStack {
        val novaTag = itemStack.unsafeNovaTag // read-only!
            ?: return itemStack
        val id = novaTag.getStringOrNull("id")
            ?: return getUnknownItem(itemStack, null)
        val novaItem = NovaRegistries.ITEM.getValue(id)
            ?: return getUnknownItem(itemStack, id)
        
        // client-side item stack copy
        val newItemType = CraftMagicNumbers.getItem(novaItem.modifyClientSideItemType(player, itemStack.asBukkitCopy(), novaItem.vanillaMaterial))
        val newItemHolder = BuiltInRegistries.ITEM.wrapAsHolder(newItemType)
        var newItemStack = MojangStack(
            newItemHolder, itemStack.count,
            buildClientSideDataComponentsPatch(newItemType, novaItem, itemStack.componentsPatch)
        )
        
        // customization through item behaviors
        newItemStack = novaItem.modifyClientSideStack(player, itemStack.asBukkitCopy(), newItemStack.asBukkitMirror()).unwrap()
        
        // generate tooltip server-side and apply as lore
        // we do not want data component modifications done by item behaviors in modifyClientSideStack
        // to be reflected in the tooltip, except for the item lore itself
        val itemStackToGenerateTooltipOf = itemStack.copy()
        itemStackToGenerateTooltipOf.set(DataComponents.LORE, newItemStack.get(DataComponents.LORE))
        applyServerSideTooltip(newItemStack, generateNovaTooltipLore(player, novaItem, itemStack.novaCompound?.keys?.size ?: 0, itemStackToGenerateTooltipOf))
        
        // save server-side nbt data (for creative mode)
        // this also drops existing custom data, which is ignored by the client anyway
        if (storeServerSideTag)
            storeServerSideTag(newItemStack, itemStack)
        
        return newItemStack
    }
    
    private fun buildClientSideDataComponentsPatch(vanilla: Item, nova: NovaItem, patch: DataComponentPatch): DataComponentPatch {
        val builder = DataComponentPatch.builder()
        
        // remove vanilla default base components
        for (vanillaBase in vanilla.components()) {
            builder.remove(vanillaBase.type)
        }
        
        // add nova default base components
        mergeIntoClientSidePatch(builder, nova.baseDataComponents.handle)
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
        for ((type, valueOpt) in patch.entrySet()) {
            if (isIrrelevantClientSideComponent(type))
                continue
            
            if (valueOpt.isPresent) {
                builder.set(TypedDataComponent.createUnchecked(type, valueOpt.get()))
            } else {
                builder.remove(type)
            }
        }
    }
    
    private fun isIrrelevantClientSideComponent(type: DataComponentType<*>): Boolean {
        return type == DataComponents.CUSTOM_DATA
    }
    
    private fun getUnknownItem(itemStack: MojangStack, id: String?): MojangStack {
        return MojangStack(Items.BARRIER).apply {
            set(
                DataComponents.ITEM_NAME,
                Component.literal("Unknown item: $id").withStyle(ChatFormatting.RED)
            )
            storeServerSideTag(this, itemStack)
        }
    }
    
    private fun generateNovaTooltipLore(player: Player?, novaItem: NovaItem, cbfTagCount: Int, itemStack: MojangStack): List<Component> {
        val isAdvanced = player?.let(AdvancedTooltips::hasNovaTooltips) == true
        val lore = generateTooltipLore(player, isAdvanced, itemStack).toMutableList()
        
        // entire tooltip is hidden
        if (lore.isEmpty())
            return emptyList()
        
        if (isAdvanced) {
            // nova item id
            lore[lore.size - 2] = Component.literal(novaItem.id.toString()).withStyle(ChatFormatting.DARK_GRAY)
            
            // cbf tag count
            if (cbfTagCount > 0) {
                lore.add(
                    Component.translatable(
                        "item.cbf_tags",
                        Component.literal(cbfTagCount.toString())
                    ).withStyle(ChatFormatting.DARK_GRAY)
                )
            }
        }
        
        return lore
    }
    //</editor-fold>
    
    //<editor-fold desc="Vanilla", defaultstate="collapsed">
    private fun getClientSideVanillaStack(player: Player?, itemStack: MojangStack, storeServerSideTag: Boolean): MojangStack {
        val newItemStack = itemStack.copy()
        var modified = false
        
        if (fixArmorColor(newItemStack))
            modified = true
        if (fixBundleContents(player, newItemStack))
            modified = true
        
        if (itemStack.unsafeCustomData?.contains(SKIP_SERVER_SIDE_TOOLTIP) != true) {
            val isAdvanced = player?.let(AdvancedTooltips::hasVanillaTooltips) == true
            if (isAdvanced || modified) { // server-side tooltip is only required if the server-side stack differs from the client-side stack
                applyServerSideTooltip(newItemStack, generateTooltipLore(player, isAdvanced, itemStack))
                modified = true
            }
        } else {
            disableClientSideTooltip(newItemStack)
            modified = true
        }
        
        // save server-side nbt data (for creative mode)
        // this also drops existing custom data, which is ignored by the client anyway
        if (modified && storeServerSideTag)
            storeServerSideTag(newItemStack, itemStack)
        
        return newItemStack
    }
    
    /**
     * Fixes the [DataComponents.DYED_COLOR] rgb value of vanilla armor items to prevent accidental use of
     * custom textures and returns whether the [itemStack] was modified.
     */
    private fun fixArmorColor(itemStack: MojangStack): Boolean {
        val color = itemStack.get(DataComponents.DYED_COLOR)
            ?: return false
        val rgb = color.rgb
        
        if (rgb % 2 == 0)
            return false
        
        if (CustomItemServiceManager.getId(itemStack.asBukkitMirror()) != null)
            return false
        
        itemStack.set(DataComponents.DYED_COLOR, DyedItemColor(rgb - 1))
        
        return true
    }
    
    /**
     * Updates the [BundleContents.items] to use client-side item stacks
     * and returns whether the [itemStack] was modified.
     */
    private fun fixBundleContents(player: Player?, itemStack: MojangStack): Boolean {
        if (!itemStack.has(DataComponents.BUNDLE_CONTENTS))
            return false
        
        itemStack.update(DataComponents.BUNDLE_CONTENTS) { bundleContents ->
            BundleContents(
                bundleContents.items().map { getClientSideStack(player, it, false) },
                Fraction.getFraction(0.0),
                bundleContents.selectedItem
            )
        }
        
        return true
    }
    //</editor-fold>
    
    //<editor-fold desc="tooltip", defaultstate="collapsed">
    private fun applyServerSideTooltip(itemStack: ItemStack, tooltip: List<Component>) {
        val lore = tooltip.fold(ItemLore.EMPTY) { l, line ->
            l.withLineAdded(line.withoutPreFormatting())
        }
        itemStack.set(DataComponents.LORE, lore)
        disableClientSideTooltip(itemStack)
    }
    
    private fun generateTooltipLore(player: Player?, advancedTooltips: Boolean, itemStack: MojangStack): List<Component> {
        val lore = itemStack.getTooltipLines(
            Item.TooltipContext.of(REGISTRY_ACCESS),
            player?.serverPlayer,
            if (advancedTooltips) TooltipFlag.ADVANCED else TooltipFlag.NORMAL
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
            BuiltInRegistries.DATA_COMPONENT_TYPE.filterTo(LinkedHashSet()) { it != DataComponents.BUNDLE_CONTENTS && it != DataComponents.LORE }
        ))
    }
    //</editor-fold>
    //</editor-fold>
    
    //<editor-fold desc="client-side stack -> server-side stack", defaultstate="collapsed">
    fun getServerSideStack(itemStack: MojangStack): MojangStack {
        val serversideTag = itemStack.get(DataComponents.CUSTOM_DATA)?.unsafe
            ?.getCompoundOrNull("NovaServerSideTag")
            ?: return itemStack
        val serversideComponents = decodeComponents(serversideTag)
        
        // use server-side item for all Nova items, otherwise keep current item
        val serversideCustomData = serversideComponents.get(DataComponents.CUSTOM_DATA)?.orElse(null)
        val item = if (serversideCustomData?.unsafe?.getCompoundOrNull("nova") != null)
            SERVER_SIDE_ITEM_HOLDER
        else itemStack.itemHolder
        
        return MojangStack(item, itemStack.count, serversideComponents)
    }
    //</editor-fold>
    
    private fun storeServerSideTag(clientSide: MojangStack, serverSide: MojangStack) {
        clientSide.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply {
            put("NovaServerSideTag", encodeComponents(serverSide))
        }))
    }
    
    private fun encodeComponents(itemStack: MojangStack): Tag {
        val patch = itemStack.componentsPatch
        return DataComponentPatch.CODEC.encodeStart(
            RegistryOps.create(NbtOps.INSTANCE, REGISTRY_ACCESS),
            patch
        ).getOrThrow()
    }
    
    private fun decodeComponents(components: Tag): DataComponentPatch {
        return DataComponentPatch.CODEC.decode(Dynamic(
            RegistryOps.create(NbtOps.INSTANCE, REGISTRY_ACCESS),
            components
        )).getFirstOrThrow()
    }
    
}