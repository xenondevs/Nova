@file:Suppress("DEPRECATION", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item.logic

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.IntTag
import net.minecraft.nbt.ListTag
import net.minecraft.network.syncher.EntityDataSerializers
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.trading.MerchantOffer
import net.minecraft.world.item.trading.MerchantOffers
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_20_R1.util.CraftMagicNumbers
import org.bukkit.entity.Player
import org.bukkit.event.Listener
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
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.initialize.InitFun
import xyz.xenondevs.nova.initialize.InitializationStage
import xyz.xenondevs.nova.initialize.InternalInit
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
import xyz.xenondevs.nova.util.item.novaCompoundOrNull
import xyz.xenondevs.nova.util.namespacedKey
import xyz.xenondevs.nova.util.registerEvents
import xyz.xenondevs.nova.util.registerPacketListener
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

@InternalInit(
    stage = InitializationStage.POST_WORLD,
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
            val id = recipe.id.namespacedKey
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
        
        val itemModelDataMap = Resources.getModelDataOrNull(id)?.item
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
        
        val packetItemData = itemLogic.getPacketItemData(player, newItemStack)
        
        //<editor-fold desc="Display", defaultstate="collapsed">
        val displayTag = newItemTag.getOrPut("display", ::CompoundTag)
        
        // name
        var itemDisplayName = packetItemData.name
        if (useName && !displayTag.contains("Name") && itemDisplayName != null) {
            if (itemTag.contains("Enchantments", NBTUtils.TAG_LIST) && itemDisplayName.children().isEmpty()) {
                itemDisplayName = itemDisplayName.color(NamedTextColor.AQUA)
            }
            
            displayTag.putString("Name", itemDisplayName.withoutPreFormatting().toJson())
        }
        
        // lore
        val loreTag = displayTag.getOrPut("Lore", ::ListTag)
        val itemDisplayLore = packetItemData.lore
        itemDisplayLore?.forEach { loreTag += it.withoutPreFormatting().toNBT() }
        if (player != null && AdvancedTooltips.hasNovaTooltips(player)) {
            packetItemData.advancedTooltipsLore?.forEach { loreTag += it.withoutPreFormatting().toNBT() }
            loreTag += Component.text(id, NamedTextColor.DARK_GRAY).withoutPreFormatting().toNBT()
            
            var cbfTagCount = 0
            var nbtTagCount = itemTag.allKeys.size - 1 // don't count 'nova' tag
            
            val novaCompound = itemStack.novaCompoundOrNull
            if (novaCompound != null) {
                cbfTagCount = novaCompound.keys.size
                nbtTagCount -= 1 // don't count 'nova_cbf' tag
            }
            
            if (cbfTagCount > 0)
                loreTag += Component.translatable(
                    "item.cbf_tags",
                    NamedTextColor.DARK_GRAY,
                    Component.text(cbfTagCount)
                ).withoutPreFormatting().toNBT()
            
            if (nbtTagCount > 0)
                loreTag += Component.translatable(
                    "item.nbt_tags",
                    NamedTextColor.DARK_GRAY,
                    Component.text(nbtTagCount)
                ).withoutPreFormatting().toNBT()
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
        displayTag.putString(
            "Name",
            Component.text("Unknown item: $id", NamedTextColor.RED).withoutPreFormatting().toJson()
        )
        
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
        
        //<editor-fold desc="advanced tool tips">
        if (player != null && (itemStack.tag?.getInt("CustomModelData") ?: 0) == 0 && AdvancedTooltips.hasVanillaTooltips(player)) {
            if (newItemStack === itemStack)
                newItemStack = itemStack.copy()
            
            val tag = newItemStack.orCreateTag
            val novaCompound = newItemStack.novaCompoundOrNull
            
            val nbtTagCount = tag.allKeys.size
            val cbfTagCount = novaCompound?.keys?.size ?: 0
            
            val displayTag = tag.getOrPut("display", ::CompoundTag)
            val loreTag = displayTag.getOrPut("Lore", ::ListTag)
            
            val maxDamage = newItemStack.item.maxDamage
            if (maxDamage > 0) {
                loreTag += Component.translatable(
                    "item.durability",
                    NamedTextColor.WHITE,
                    Component.text(maxDamage - newItemStack.damageValue),
                    Component.text(maxDamage)
                ).withoutPreFormatting().toNBT()
            }
            
            loreTag += Component.translatable(
                BuiltInRegistries.ITEM.getKey(newItemStack.item).toString(),
                NamedTextColor.DARK_GRAY
            ).withoutPreFormatting().toNBT()
            
            if (cbfTagCount > 0)
                loreTag += Component.translatable(
                    "item.cbf_tags",
                    NamedTextColor.DARK_GRAY,
                    Component.text(cbfTagCount)
                ).withoutPreFormatting().toNBT()
            
            if (nbtTagCount > 0)
                loreTag += Component.translatable(
                    "item.nbt_tags",
                    NamedTextColor.DARK_GRAY,
                    Component.text(nbtTagCount)
                ).withoutPreFormatting().toNBT()
        }
        //</editor-fold>
        
        // save server-side nbt data if the item has been modified
        if (storeServerSideTag && newItemStack != itemStack) {
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