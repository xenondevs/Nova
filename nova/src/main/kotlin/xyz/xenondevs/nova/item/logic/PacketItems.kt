@file:Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item.logic

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.Tag
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.MobType
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers
import org.bukkit.entity.Player
import org.bukkit.event.Listener
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nmsutils.network.event.PacketHandler
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundContainerSetContentPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundContainerSetSlotPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundMerchantOffersPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSetEntityDataPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundSetEquipmentPacketEvent
import xyz.xenondevs.nmsutils.network.event.clientbound.ClientboundUpdateRecipesPacketEvent
import xyz.xenondevs.nmsutils.network.event.serverbound.ServerboundSetCreativeModeSlotPacketEvent
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.resources.ResourceGeneration
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InternalInit
import xyz.xenondevs.nova.initialize.InternalInitStage
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.vanilla.AttributeModifier
import xyz.xenondevs.nova.item.vanilla.HideableFlag
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.component.adventure.toJson
import xyz.xenondevs.nova.util.component.adventure.toNBT
import xyz.xenondevs.nova.util.component.adventure.withoutPreFormatting
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.data.getOrNull
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.get
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import xyz.xenondevs.nova.util.serverPlayer
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

private val SHULKER_BOX_ITEMS = setOf(
    Items.SHULKER_BOX,
    Items.BLUE_SHULKER_BOX,
    Items.BLACK_SHULKER_BOX,
    Items.CYAN_SHULKER_BOX,
    Items.BROWN_SHULKER_BOX,
    Items.GREEN_SHULKER_BOX,
    Items.GRAY_SHULKER_BOX,
    Items.LIGHT_BLUE_SHULKER_BOX,
    Items.LIGHT_GRAY_SHULKER_BOX,
    Items.LIME_SHULKER_BOX,
    Items.MAGENTA_SHULKER_BOX,
    Items.ORANGE_SHULKER_BOX,
    Items.PINK_SHULKER_BOX,
    Items.PURPLE_SHULKER_BOX,
    Items.RED_SHULKER_BOX,
    Items.WHITE_SHULKER_BOX,
    Items.YELLOW_SHULKER_BOX
)

private val ATTRIBUTE_DECIMAL_FORMAT = DecimalFormat("#.##")
    .apply { decimalFormatSymbols = DecimalFormatSymbols.getInstance(Locale.ROOT) }

private val GLOW_ENCHANTMENT_TAG = ListTag().apply {
    val entry = CompoundTag()
    entry.putString("id", "glow")
    entry.putInt("level", 1)
    add(entry)
}

@InternalInit(
    stage = InternalInitStage.POST_WORLD,
    dependsOn = [ResourceGeneration.PreWorld::class]
)
internal object PacketItems : Listener {
    
    val SERVER_SIDE_MATERIAL = Material.SHULKER_SHELL
    val SERVER_SIDE_ITEM = CraftMagicNumbers.getItem(SERVER_SIDE_MATERIAL)!!
    val SERVER_SIDE_ITEM_ID = BuiltInRegistries.ITEM.getKey(SERVER_SIDE_ITEM).toString()
    
    @InitFun
    private fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    //<editor-fold desc="packet events", defaultstate="collapsed">
    @PacketHandler
    private fun handleSetContentPacket(event: ClientboundContainerSetContentPacketEvent) {
        val player = event.player
        val items = event.items
        
        items.forEachIndexed { i, item ->
            items[i] = getClientSideStack(player, item)
        }
        
        event.carriedItem = getClientSideStack(player, event.carriedItem)
    }
    
    @PacketHandler
    private fun handleSetSlotPacket(event: ClientboundContainerSetSlotPacketEvent) {
        event.item = getClientSideStack(event.player, event.item)
    }
    
    @PacketHandler
    private fun handleEntityData(event: ClientboundSetEntityDataPacketEvent) {
        val player = event.player
        val packet = event.packet
        val data = packet.packedItems ?: return
        data.forEachIndexed { idx, dataValue ->
            val value = dataValue.value
            if (value is MojangStack) {
                data[idx] = DataValue(dataValue.id, EntityDataSerializers.ITEM_STACK, getClientSideStack(player, value, false))
            }
        }
    }
    
    @PacketHandler
    private fun handleSetEquipment(event: ClientboundSetEquipmentPacketEvent) {
        val player = event.player
        val slots = ArrayList(event.slots).also { event.slots = it }
        
        slots.forEachIndexed { i, pair ->
            slots[i] = MojangPair(
                pair.first,
                getClientSideStack(player, pair.second)
            )
        }
    }
    
    @PacketHandler
    private fun handleCreativeSetItem(event: ServerboundSetCreativeModeSlotPacketEvent) {
        event.item = getServerSideStack(event.item)
    }
    
    @PacketHandler
    private fun handleRecipes(event: ClientboundUpdateRecipesPacketEvent) {
        val packet = event.packet
        packet.recipes.forEachIndexed { i, recipe ->
            val id = recipe.id
            if (id in RecipeManager.clientsideRecipes)
                packet.recipes[i] = RecipeManager.clientsideRecipes[id]!!
        }
    }
    
    @PacketHandler
    private fun handleMerchantOffers(event: ClientboundMerchantOffersPacketEvent) {
        val newOffers = MerchantOffers()
        
        event.offers.forEach { offer ->
            newOffers += MerchantOffer(
                getClientSideStack(event.player, offer.baseCostA),
                getClientSideStack(event.player, offer.costB),
                getClientSideStack(event.player, offer.result),
                offer.uses,
                offer.maxUses,
                offer.xp,
                offer.priceMultiplier,
                offer.demand
            
            )
        }
        
        event.offers = newOffers
    }
    //</editor-fold>
    
    //<editor-fold desc="server-side stack -> client-side stack", defaultstate="collapsed">
    fun getClientSideStack(player: Player?, itemStack: MojangStack, useName: Boolean = true, storeServerSideTag: Boolean = true): MojangStack {
        return if (itemStack.tag?.contains("nova", NBTUtils.TAG_COMPOUND) == true) {
            getClientSideNovaStack(player, itemStack, useName, storeServerSideTag)
        } else getClientsideVanillaStack(player, itemStack, storeServerSideTag)
    }
    
    fun getClientSideStack(player: Player?, itemStackCompound: CompoundTag, useName: Boolean = true, storeServerSideTag: Boolean = true): CompoundTag {
        val itemTag = itemStackCompound.getOrNull<CompoundTag>("tag")
            ?: return itemStackCompound
        
        val itemStack = MojangStack.of(itemStackCompound)
        val newItemStack = if (itemTag.contains("nova", NBTUtils.TAG_COMPOUND)) {
            getClientSideNovaStack(player, itemStack, useName, storeServerSideTag)
        } else getClientsideVanillaStack(player, itemStack, storeServerSideTag)
        
        return if (newItemStack == itemStack)
            itemStackCompound
        else newItemStack.save(CompoundTag())
    }
    
    //<editor-fold desc="Nova", defaultstate="collapsed">
    private fun getClientSideNovaStack(player: Player?, itemStack: MojangStack, useName: Boolean, storeServerSideTag: Boolean): MojangStack {
        val itemTag = itemStack.tag!!
        val novaTag = itemTag.getOrNull<CompoundTag>("nova")
            ?: throw IllegalArgumentException("The provided ItemStack is not a Nova item.")
        
        val id = novaTag.getString("id") ?: return getUnknownItem(itemStack, null)
        val material = NovaRegistries.ITEM[id] ?: return getUnknownItem(itemStack, id)
        val subId = novaTag.getInt("subId")
        val itemLogic = material.logic
        
        val itemModelDataMap = ResourceLookups.MODEL_DATA_LOOKUP[id]?.item
        val data = itemModelDataMap?.get(itemLogic.vanillaMaterial)
            ?: itemModelDataMap?.values?.first()
            ?: return getUnknownItem(itemStack, id)
        
        val newItemStack = itemStack.copy()
        val newItemTag = newItemStack.tag!!
        
        // save server-side nbt data
        if (storeServerSideTag) {
            newItemTag.put("NovaServerSideTag", itemTag)
        }
        
        // set item type and model data
        newItemStack.item = CraftMagicNumbers.getItem(data.material)
        newItemTag.putInt("CustomModelData", data.dataArray[subId])
        
        val packetItemData = itemLogic.getPacketItemData(newItemStack)
        
        //<editor-fold desc="Display", defaultstate="collapsed">
        val displayTag = newItemTag.getOrPut("display", ::CompoundTag)
        
        // name
        var itemDisplayName = packetItemData.name
        if (useName && !displayTag.contains("Name") && itemDisplayName != null) {
            if (Enchantable.isEnchanted(newItemStack) && itemDisplayName.children().isEmpty()) {
                itemDisplayName = itemDisplayName.color(NamedTextColor.AQUA)
            }
            
            displayTag.putString("Name", itemDisplayName.withoutPreFormatting().toJson())
        }
        
        val loreTag = displayTag.getOrPut("Lore", ::ListTag)
        // enchantments
        val enchantmentsTooltip = buildEnchantmentsTooltip(newItemStack)
        enchantmentsTooltip.forEach { loreTag += it.withoutPreFormatting().toNBT() }
        if (packetItemData.glow ?: enchantmentsTooltip.isNotEmpty()) {
            newItemTag.put("Enchantments", GLOW_ENCHANTMENT_TAG)
        }
        // actual lore
        val itemDisplayLore = packetItemData.lore
        itemDisplayLore?.forEach { loreTag += it.withoutPreFormatting().toNBT() }
        // attribute modifier tooltip
        buildAttributeModifiersTooltip(player?.serverPlayer, newItemStack).forEach { loreTag += it.withoutPreFormatting().toNBT() }
        // advanced tooltips
        if (player != null && AdvancedTooltips.hasNovaTooltips(player)) {
            packetItemData.advancedTooltipsLore?.forEach { loreTag += it.withoutPreFormatting().toNBT() }
            buildNovaAdvancedTooltip(itemStack, material).forEach { loreTag += it.withoutPreFormatting().toNBT() }
        }
        //</editor-fold>
        
        //<editor-fold desc="Damage", defaultstate="collapsed">
        val itemDisplayDurabilityBar = packetItemData.durabilityBar
        if (itemDisplayDurabilityBar != 1.0) {
            val maxDurability = newItemStack.item.maxDamage
            newItemTag.putInt("Damage", maxDurability - (maxDurability * itemDisplayDurabilityBar).toInt())
        }
        //</editor-fold>
        
        //<editor-fold desc="HideFlags">
        val hiddenFlags = packetItemData.hiddenFlags
        if (hiddenFlags.isNotEmpty()) {
            newItemTag.putInt("HideFlags", newItemTag.getInt("HideFlags") or HideableFlag.toInt(hiddenFlags))
        }
        //</editor-fold>
        
        return newItemStack
    }
    
    private fun getUnknownItem(itemStack: MojangStack, id: String?): MojangStack {
        val newItemStack = itemStack.copy()
        newItemStack.item = Items.BARRIER
        val tag = newItemStack.tag!!
        
        // remove custom model data
        tag.putInt("CustomModelData", 0)
        
        // save server-side nbt data
        tag.put("NovaServerSideTag", itemStack.tag!!)
        
        // overwrite display tag with "missing model" name
        val displayTag = CompoundTag().also { tag.put("display", it) }
        displayTag.putString("Name", Component.text("Unknown item: $id", NamedTextColor.RED).withoutPreFormatting().toJson())
        
        return newItemStack
    }
    //</editor-fold>
    
    //<editor-fold desc="Vanilla", defaultstate="collapsed">
    private fun getClientsideVanillaStack(player: Player?, itemStack: MojangStack, storeServerSideTag: Boolean): MojangStack {
        var newItemStack = itemStack
        
        if (storeServerSideTag && isContainerItem(itemStack)) {
            newItemStack = getClientSideContainerItem(player, itemStack)
        } else if (isIllegallyColoredArmor(itemStack)) {
            newItemStack = getColorCorrectedArmor(itemStack)
        }
        
        if (newItemStack === itemStack)
            newItemStack = itemStack.copy()
        
        val tag = newItemStack.orCreateTag
        val loreTag = tag
            .getOrPut("display", ::CompoundTag)
            .getOrPut("Lore", ::ListTag)
        
        // enchantments tooltip (above normal lore text)
        val enchantmentsTooltip = buildEnchantmentsTooltip(itemStack)
        enchantmentsTooltip.forEachIndexed { idx, line -> loreTag.add(idx, line.withoutPreFormatting().toNBT()) }
        if (enchantmentsTooltip.isNotEmpty()) tag.put("Enchantments", GLOW_ENCHANTMENT_TAG) // ensures glow effect
        // attributes tooltip
        buildAttributeModifiersTooltip(player?.serverPlayer, newItemStack).forEach { loreTag += it.withoutPreFormatting().toNBT() }
        // advanced tooltips
        if (player != null && tag.getInt("CustomModelData") == 0 && AdvancedTooltips.hasVanillaTooltips(player)) {
            buildVanillaAdvancedTooltip(itemStack, itemStack.item).forEach { loreTag += it.withoutPreFormatting().toNBT() }
        }
        
        // hide flags
        var hideFlags = HideableFlag.modifyInt(tag.getInt("HideFlags"), HideableFlag.ENCHANTMENTS, HideableFlag.MODIFIERS)
        if (itemStack.item == Items.ENCHANTED_BOOK)
            hideFlags = hideFlags or HideableFlag.ADDITIONAL.mask
        tag.putInt("HideFlags", hideFlags)
        
        // save server-side nbt data
        if (storeServerSideTag) {
            newItemStack.orCreateTag.put("NovaServerSideTag", itemStack.orCreateTag)
        }
        
        return newItemStack
    }
    
    private fun isContainerItem(item: MojangStack): Boolean {
        return item.item == Items.BUNDLE || item.item in SHULKER_BOX_ITEMS
    }
    
    private fun getClientSideContainerItem(player: Player?, itemStack: MojangStack): MojangStack {
        when (itemStack.item) {
            Items.BUNDLE -> {
                val items = itemStack.tag
                    ?.getOrNull<ListTag>("Items")
                    ?: return itemStack
                
                val newItems = convertItemList(player, items)
                return if (newItems != items) {
                    itemStack.copy().apply { tag!!.put("Items", newItems) }
                } else itemStack
            }
            
            in SHULKER_BOX_ITEMS -> {
                val items = itemStack.tag
                    ?.getOrNull<CompoundTag>("BlockEntityTag")
                    ?.getOrNull<ListTag>("Items")
                    ?: return itemStack
                
                val newItems = convertItemList(player, items)
                return if (newItems != items) {
                    itemStack.copy().apply { tag!!.getCompound("BlockEntityTag").put("Items", newItems) }
                } else itemStack
            }
        }
        
        return itemStack
    }
    
    private fun convertItemList(player: Player?, list: ListTag): ListTag {
        val newList = list.copy()
        var changed = false
        newList.forEachIndexed { idx, itemStack ->
            itemStack as CompoundTag
            val newItemStack = getClientSideStack(player, itemStack, useName = true, storeServerSideTag = false)
            if (newItemStack != itemStack) {
                newList[idx] = newItemStack
                changed = true
            }
        }
        
        return if (changed) newList else list
    }
    
    private fun isIllegallyColoredArmor(itemStack: MojangStack): Boolean {
        val item = itemStack.item
        if (item == Items.LEATHER_BOOTS
            || item == Items.LEATHER_LEGGINGS
            || item == Items.LEATHER_CHESTPLATE
            || item == Items.LEATHER_HELMET
        ) {
            val color = itemStack.tag?.getOrNull<CompoundTag>("display")?.getOrNull<IntTag>("color")?.asInt
            // custom armor only uses odd color codes
            if (color != null && color % 2 != 0) {
                // allow armor from custom item services to have any color
                return CustomItemServiceManager.getId(itemStack.bukkitMirror) == null
            }
        }
        
        return false
    }
    
    private fun getColorCorrectedArmor(itemStack: MojangStack): MojangStack {
        val newItem = itemStack.copy()
        val display = newItem.tag!!.getCompound("display")
        display.putInt("color", display.getInt("color") and 0xFFFFFF)
        return newItem
    }
    //</editor-fold>
    
    //<editor-fold desc="tooltip", defaultstate="collapsed">
    private fun buildEnchantmentsTooltip(itemStack: MojangStack): List<Component> {
        val enchantments = Enchantable.getEnchantments(itemStack).takeUnlessEmpty()
            ?: Enchantable.getStoredEnchantments(itemStack).takeUnlessEmpty()
            ?: return emptyList()
        
        val lore = ArrayList<Component>()
        for ((enchantment, level) in enchantments) {
            lore += Component.text().apply {
                it.color(if (enchantment.isCurse) NamedTextColor.RED else NamedTextColor.GRAY)
                it.append(Component.translatable(enchantment.localizedName))
                it.append(Component.text(" "))
                if (!enchantment.isCurse) it.append(Component.translatable("enchantment.level.$level"))
            }.build()
        }
        
        return lore
    }
    
    private fun buildAttributeModifiersTooltip(player: ServerPlayer?, itemStack: MojangStack): List<Component> {
        if (HideableFlag.MODIFIERS.isHidden(itemStack.tag?.getInt("HideFlags") ?: 0))
            return emptyList()
        
        // if the item has custom modifiers set, all default modifiers are ignored
        val hasCustomModifiers = itemStack.tag?.contains("AttributeModifiers", Tag.TAG_LIST.toInt()) == true
        
        val lore = ArrayList<Component>()
        EquipmentSlot.entries.forEach { slot ->
            val modifiers: List<AttributeModifier>
            if (hasCustomModifiers) {
                // use custom attribute modifiers from nbt data
                modifiers = ItemUtils.getCustomAttributeModifiers(itemStack, slot)
            } else {
                val novaItem = itemStack.novaItem
                if (novaItem != null) {
                    // use base attribute modifiers from nova item
                    modifiers = novaItem.logic.attributeModifiers[slot] ?: emptyList()
                } else {
                    // use base attribute modifiers from vanilla item
                    modifiers = itemStack.item.getDefaultAttributeModifiers(slot).entries().map { (attribute, modifier) ->
                        AttributeModifier(modifier.id, modifier.name, attribute, modifier.operation, modifier.amount, true, slot)
                    }
                }
            }
            
            if (modifiers.isEmpty() || modifiers.none { it.showInLore && it.value != 0.0 })
                return@forEach
            
            lore += Component.empty()
            lore += Component.translatable("item.modifiers.${slot.name.lowercase()}", NamedTextColor.GRAY)
            
            modifiers.asSequence()
                .filter { it.showInLore && it.value != 0.0 }
                .forEach { modifier ->
                    var value = modifier.value
                    var isBaseModifier = false
                    
                    when (modifier.uuid) {
                        Tool.BASE_ATTACK_DAMAGE_UUID -> {
                            value += player?.getAttributeBaseValue(Attributes.ATTACK_DAMAGE) ?: 1.0
                            value += EnchantmentHelper.getDamageBonus(itemStack, MobType.UNDEFINED)
                            isBaseModifier = true
                        }
                        
                        Tool.BASE_ATTACK_SPEED_UUID -> {
                            value += player?.getAttributeBaseValue(Attributes.ATTACK_SPEED) ?: 4.0
                            isBaseModifier = true
                        }
                    }
                    
                    var displayedValue = if (modifier.operation == Operation.ADDITION) {
                        if (modifier.attribute == Attributes.KNOCKBACK_RESISTANCE) {
                            value * 10.0 // vanilla behavior
                        } else value
                    } else value * 100.0
                    
                    fun appendModifier(type: String, color: TextColor) {
                        lore += Component.text()
                            .append(Component.text(if (isBaseModifier) " " else ""))
                            .append(Component.translatable(
                                "attribute.modifier.$type.${modifier.operation.ordinal}",
                                color,
                                Component.text(ATTRIBUTE_DECIMAL_FORMAT.format(displayedValue)),
                                Component.translatable(modifier.attribute.descriptionId)
                            ))
                            .build()
                    }
                    
                    if (isBaseModifier) {
                        appendModifier("equals", NamedTextColor.DARK_GREEN)
                    } else if (value > 0.0) {
                        appendModifier("plus", NamedTextColor.BLUE)
                    } else if (value < 0.0) {
                        displayedValue *= -1
                        appendModifier("take", NamedTextColor.RED)
                    }
                }
        }
        
        return lore
    }
    
    private fun buildVanillaAdvancedTooltip(itemStack: MojangStack, item: Item): List<Component> {
        val tag = itemStack.orCreateTag
        val novaCompound = itemStack.novaCompoundOrNull
        val nbtTagCount = tag.allKeys.size
        val cbfTagCount = novaCompound?.keys?.size ?: 0
        
        val lore = ArrayList<Component>()
        
        val maxDamage = item.maxDamage
        if (maxDamage > 0) {
            lore += Component.translatable(
                "item.durability",
                NamedTextColor.WHITE,
                Component.text(maxDamage - itemStack.damageValue),
                Component.text(maxDamage)
            )
        }
        
        lore += Component.translatable(
            BuiltInRegistries.ITEM.getKey(item).toString(),
            NamedTextColor.DARK_GRAY
        )
        
        if (cbfTagCount > 0)
            lore += Component.translatable(
                "item.cbf_tags",
                NamedTextColor.DARK_GRAY,
                Component.text(cbfTagCount)
            )
        
        if (nbtTagCount > 0)
            lore += Component.translatable(
                "item.nbt_tags",
                NamedTextColor.DARK_GRAY,
                Component.text(nbtTagCount)
            )
        
        return lore
    }
    
    private fun buildNovaAdvancedTooltip(itemStack: MojangStack, item: NovaItem): List<Component> {
        var cbfTagCount = 0
        var nbtTagCount = itemStack.orCreateTag.allKeys.size - 1 // don't count 'nova' tag
        
        val lore = ArrayList<Component>()
        lore += Component.text(item.id.toString(), NamedTextColor.DARK_GRAY)
        
        val novaCompound = itemStack.novaCompoundOrNull
        if (novaCompound != null) {
            cbfTagCount = novaCompound.keys.size
            nbtTagCount -= 1 // don't count 'nova_cbf' tag
        }
        
        if (cbfTagCount > 0)
            lore += Component.translatable(
                "item.cbf_tags",
                NamedTextColor.DARK_GRAY,
                Component.text(cbfTagCount)
            )
        
        if (nbtTagCount > 0)
            lore += Component.translatable(
                "item.nbt_tags",
                NamedTextColor.DARK_GRAY,
                Component.text(nbtTagCount)
            )
        
        return lore
    }
    //</editor-fold>
    //</editor-fold>
    
    //<editor-fold desc="client-side stack -> server-side stack", defaultstate="collapsed">
    fun getServerSideStack(itemStack: CompoundTag): CompoundTag {
        val tag = itemStack.getOrNull<CompoundTag>("tag")
        val serversideTag = tag
            ?.getOrNull<CompoundTag>("NovaServerSideTag")
            ?: return itemStack
        
        val serverSideStack = CompoundTag()
        
        // use server-side item for all Nova items, otherwise keep current item id
        if (tag.contains("nova", NBTUtils.TAG_COMPOUND)) {
            serverSideStack.putString("id", SERVER_SIDE_ITEM_ID)
        } else serverSideStack.putString("id", itemStack.getString("id"))
        // copy count
        serverSideStack.putInt("Count", serverSideStack.getInt("Count"))
        // apply server-side tag
        serverSideStack.put("tag", serversideTag)
        
        return serverSideStack
    }
    
    fun getServerSideStack(itemStack: MojangStack): MojangStack {
        val tag = itemStack.tag
        val serversideTag = tag
            ?.getOrNull<CompoundTag>("NovaServerSideTag")
            ?: return itemStack
        
        // use server-side item for all Nova items, otherwise keep current item
        val item = if (tag.contains("nova", NBTUtils.TAG_COMPOUND)) SERVER_SIDE_ITEM else itemStack.item
        val serversideStack = ItemStack(item)
        // copy count
        serversideStack.count = itemStack.count
        // apply server-side tag
        serversideStack.tag = serversideTag
        
        return serversideStack
    }
    //</editor-fold>
    
}