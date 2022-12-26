package xyz.xenondevs.nova.material

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_19_R2.util.CraftMagicNumbers
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.ItemMergeEvent
import org.bukkit.event.inventory.InventoryDragEvent
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
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.item.vanilla.HideableFlag
import xyz.xenondevs.nova.util.bukkitStack
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.duplicate
import xyz.xenondevs.nova.util.data.getOrPut
import xyz.xenondevs.nova.util.data.serialize
import xyz.xenondevs.nova.util.data.withoutPreFormatting
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.item.novaMaxStackSize
import xyz.xenondevs.nova.util.namespacedKey
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
import java.util.*
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("DEPRECATION")
internal object PacketItems : Initializable(), Listener {
    
    val SERVER_SIDE_MATERIAL = Material.SHULKER_SHELL
    val SERVER_SIDE_ITEM = CraftMagicNumbers.getItem(SERVER_SIDE_MATERIAL)!!
    
    private val PLAYER_HOTBAR_BLOCKED_SLOTS = BooleanArray(36) { it < 9 }
    private val PLAYER_NON_HOTBAR_BLOCKED_SLOTS = BooleanArray(36) { it > 8 }
    
    override val initializationStage = InitializationStage.POST_WORLD
    override val dependsOn = setOf(ResourceGeneration.PreWorld)
    
    override fun init() {
        registerEvents()
        registerPacketListener()
    }
    
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    private fun handleMerge(event: ItemMergeEvent) {
        val first = event.entity.itemStack
        val second = event.target.itemStack
        
        val novaMaterial = first.novaMaterial ?: return
        if (first.amount + second.amount > novaMaterial.maxStackSize)
            event.isCancelled = true
    }
    
    @EventHandler(priority = EventPriority.HIGHEST)
    private fun handleDrag(event: InventoryDragEvent) {
        if (event.newItems.values.any { it.amount > it.novaMaxStackSize })
            event.isCancelled = true
    }
    
    @PacketHandler
    private fun handleSetContentPacket(event: ClientboundContainerSetContentPacketEvent) {
        val player = event.player
        val packet = event.packet
        val items = packet.items
        val carriedItem = packet.carriedItem
        
        items.forEachIndexed { i, item ->
            val newItem = getClientsideItemOrNull(player, item, fromCreative = false)
            if (newItem != null)
                items[i] = newItem
        }
        
        if (isNovaItem(carriedItem))
            event.carriedItem = getFakeItem(player, carriedItem)
    }
    
    @PacketHandler
    private fun handleSetSlotPacket(event: ClientboundContainerSetSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        val newItem = getClientsideItemOrNull(event.player, item, fromCreative = false)
        if (newItem != null)
            event.item = newItem
    }
    
    @PacketHandler
    private fun handleEntityData(event: ClientboundSetEntityDataPacketEvent) {
        val player = event.player
        val packet = event.packet
        val data = packet.packedItems ?: return
        data.forEachIndexed { idx, dataValue ->
            val value = dataValue.value
            if (value is MojangStack && isNovaItem(value)) {
                data[idx] = DataValue(dataValue.id, EntityDataSerializers.ITEM_STACK, getFakeItem(player, value, false))
            }
        }
    }
    
    @PacketHandler
    private fun handleSetEquipment(event: ClientboundSetEquipmentPacketEvent) {
        val player = event.player
        val packet = event.packet
        val slots = packet.slots
        
        slots.forEachIndexed { i, slot ->
            if (isNovaItem(slot.second))
                slots[i] = MojangPair(slot.first, getFakeItem(player, slot.second))
        }
    }
    
    @PacketHandler
    private fun handleCreativeSetItem(event: ServerboundSetCreativeModeSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        if (isContainerItem(item))
            event.item = filterContainerItems(item, fromCreative = true)
        else if (isFakeItem(item))
            event.item = getNovaItem(item)
    }
    
    @PacketHandler
    private fun handleRecipes(event: ClientboundUpdateRecipesPacketEvent) {
        val packet = event.packet
        packet.recipes.forEachIndexed { i, recipe ->
            val id = recipe.id.namespacedKey
            if (id in RecipeManager.clientsideRecipes)
                packet.recipes[i] = RecipeManager.clientsideRecipes[id]!!
        }
    }
    
    @PacketHandler
    private fun handleMerchantOffers(event: ClientboundMerchantOffersPacketEvent) {
        val newOffers = MerchantOffers()
        var changed = false
        
        event.offers.forEach { offer ->
            val newSlotA = getClientsideItemOrNull(event.player, offer.baseCostA, fromCreative = false)
            val newSlotB = getClientsideItemOrNull(event.player, offer.costB, fromCreative = false)
            val newResult = getClientsideItemOrNull(event.player, offer.result, fromCreative = false)
            if (newSlotA != null || newSlotB != null || newResult != null) {
                newOffers.add(MerchantOffer(
                    newSlotA ?: offer.baseCostA,
                    newSlotB ?: offer.costB,
                    newResult ?: offer.result,
                    offer.uses,
                    offer.maxUses,
                    offer.xp,
                    offer.priceMultiplier,
                    offer.demand
                ))
                changed = true
            } else newOffers.add(offer)
        }
        if (changed) event.offers = newOffers
    }
    
    internal fun isNovaItem(item: MojangStack): Boolean {
        return item.item == SERVER_SIDE_ITEM
            && item.tag != null
            && item.tag!!.contains("nova", NBTUtils.TAG_COMPOUND)
    }
    
    private fun isFakeItem(item: MojangStack): Boolean {
        return item.tag != null
            && item.tag!!.contains("nova", NBTUtils.TAG_COMPOUND)
            && item.tag!!.contains("CustomModelData", NBTUtils.TAG_INT)
    }
    
    private fun isContainerItem(item: MojangStack): Boolean {
        return item.item == Items.BUNDLE
            || item.item in ItemUtils.SHULKER_BOX_ITEMS
    }
    
    private fun getNovaItem(item: MojangStack): MojangStack {
        val serversideStack = ItemStack(SERVER_SIDE_ITEM, item.count)
        serversideStack.tag = item.tag!!.getCompound("nova").getCompound("serversideTag")
        return serversideStack
    }
    
    private fun getClientsideItemOrNull(player: Player?, item: MojangStack, fromCreative: Boolean, useName: Boolean = true) =
        when {
            isContainerItem(item) -> filterContainerItems(item, fromCreative)
            isNovaItem(item) -> getFakeItem(player, item, useName)
            else -> null
        }
    
    internal fun getFakeItem(player: Player?, item: MojangStack, useName: Boolean = true): MojangStack {
        val itemTag = item.tag!!
        val novaTag = itemTag.getCompound("nova")
            ?: throw IllegalStateException("Item is not a Nova item!")
        
        val id = novaTag.getString("id") ?: return getMissingItem(item, null)
        val material = NovaMaterialRegistry.getOrNull(id) ?: return getMissingItem(item, id)
        val subId = novaTag.getInt("subId")
        val novaItem = material.novaItem
        
        val itemModelDataMap = Resources.getModelDataOrNull(id)?.first
        val data = itemModelDataMap?.get(novaItem.vanillaMaterial)
            ?: itemModelDataMap?.values?.first()
            ?: return getMissingItem(item, id)
        
        val newItem = item.copy()
        val newItemTag = newItem.tag!!
        newItemTag.getCompound("nova").put("serversideTag", itemTag)
        newItem.item = CraftMagicNumbers.getItem(data.material)
        newItemTag.putInt("CustomModelData", data.dataArray[subId])
        
        val displayTag = newItemTag.getOrPut("display", ::CompoundTag)
        
        val itemDisplayData = novaItem.getPacketItemData(item.bukkitStack)
        
        // name
        var itemDisplayName = itemDisplayData.name
        if (useName && !displayTag.contains("Name") && itemDisplayName != null) {
            if (item.isEnchanted && itemDisplayName.size == 1) {
                itemDisplayName = itemDisplayName.duplicate()
                itemDisplayName[0].color = ChatColor.AQUA
            }
            
            displayTag.putString("Name", itemDisplayName.serialize())
        }
        
        // lore
        val loreTag = displayTag.getOrPut("Lore", ::ListTag)
        val itemDisplayLore = itemDisplayData.lore
        itemDisplayLore?.forEach { loreTag += StringTag.valueOf(it.withoutPreFormatting().serialize()) }
        if (player != null && player in AdvancedTooltips.players) {
            itemDisplayData.advancedTooltipsLore?.forEach {
                loreTag += StringTag.valueOf(it.withoutPreFormatting().serialize())
            }
            loreTag += StringTag.valueOf(coloredText(ChatColor.DARK_GRAY, id).withoutPreFormatting().serialize())
        }
        
        // durability
        val itemDisplayDurabilityBar = itemDisplayData.durabilityBar
        if (itemDisplayDurabilityBar != 1.0) {
            val maxDurability = newItem.item.maxDamage
            newItem.damageValue = maxDurability - (maxDurability * itemDisplayDurabilityBar).toInt()
        }
        
        // hide flags
        val hiddenFlags = itemDisplayData.hiddenFlags
        if (hiddenFlags.isNotEmpty()) {
            newItemTag.putInt("HideFlags", newItemTag.getInt("HideFlags") or HideableFlag.toInt(hiddenFlags))
        }
        
        return newItem
    }
    
    private fun filterContainerItems(item: MojangStack, fromCreative: Boolean): MojangStack {
        if (item.tag == null) return item
        when (item.item) {
            
            Items.BUNDLE -> {
                val tag = item.tag!!
                if (!tag.contains("Items", NBTUtils.TAG_LIST))
                    return item
                
                val copy = item.copy()
                val copyTag = copy.tag!!
                
                val newItems = filterItemList(copyTag.getList("Items", NBTUtils.TAG_COMPOUND), fromCreative)
                copyTag.put("Items", newItems)
                return copy
            }
            
            in ItemUtils.SHULKER_BOX_ITEMS -> {
                val tag = item.tag!!
                if (!tag.contains("BlockEntityTag", NBTUtils.TAG_COMPOUND))
                    return item
                
                val copy = item.copy()
                val copyTag = copy.tag!!
                val blockEntityTag = copyTag.getCompound("BlockEntityTag")
                if (!blockEntityTag.contains("Items", NBTUtils.TAG_LIST))
                    return item
                
                val newItems = filterItemList(blockEntityTag.getList("Items", NBTUtils.TAG_COMPOUND), fromCreative)
                blockEntityTag.put("Items", newItems)
                return item
            }
            
        }
        return item
    }
    
    private fun filterItemList(list: ListTag, fromCreative: Boolean): ListTag {
        val items = ListTag()
        val stream = NBTUtils.convertListToStream(list)
        stream.forEach { contentItem ->
            val compound = CompoundTag()
            
            if (isContainerItem(contentItem)) {
                filterContainerItems(contentItem, fromCreative).save(compound)
            } else if (fromCreative) {
                if (isFakeItem(contentItem)) // Don't collapse if statement to prevent isNovaItem call
                    getNovaItem(contentItem).save(compound)
            } else if (isNovaItem(contentItem)) {
                getFakeItem(null, contentItem).save(compound)
            } else {
                contentItem.save(compound)
            }
            
            items.add(compound)
        }
        return items
    }
    
    private fun getMissingItem(item: MojangStack, id: String?): MojangStack {
        val newItem = item.copy()
        newItem.item = Items.BARRIER
        val tag = newItem.tag!!
        tag.putInt("CustomModelData", 0)
        
        val displayTag = tag.getCompound("display").also { tag.put("display", it) }
        displayTag.put("Lore", NBTUtils.createStringList(
            listOf(ComponentSerializer.toString(coloredText(ChatColor.RED, "Missing model for $id").withoutPreFormatting()))
        ))
        
        return newItem
    }
    
}