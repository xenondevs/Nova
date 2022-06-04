package xyz.xenondevs.nova.material

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_18_R2.util.CraftMagicNumbers
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.network.event.clientbound.*
import xyz.xenondevs.nova.network.event.serverbound.SetCreativeModeSlotPacketEvent
import xyz.xenondevs.nova.util.data.*
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.unhandledTags
import xyz.xenondevs.nova.util.namespacedKey
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

/**
 * The fake durability displayed clientside, stored in the nova tag.
 * Ranges from 0.0 to 1.0
 */
var ItemStack.clientsideDurability: Double
    get() = (itemMeta?.unhandledTags?.get("nova") as CompoundTag?)?.getDouble("durability") ?: 1.0
    set(value) {
        require(value in 0.0..1.0)
        val itemMeta = itemMeta
        (itemMeta?.unhandledTags?.get("nova") as CompoundTag?)?.putDouble("durability", value)
        this.itemMeta = itemMeta
    }

@Suppress("DEPRECATION")
object PacketItems : Initializable(), Listener {
    
    val SERVER_SIDE_MATERIAL = Material.SHULKER_SHELL
    val SERVER_SIDE_ITEM = CraftMagicNumbers.getItem(SERVER_SIDE_MATERIAL)!!
    
    override val inMainThread = true
    override val dependsOn = setOf(Resources)
    
    override fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleSetContentPacket(event: ContainerSetContentPacketEvent) {
        val player = event.player
        val packet = event.packet
        val items = packet.items
        val carriedItem = packet.carriedItem
        
        items.forEachIndexed { i, item ->
            if (isContainerItem(item))
                items[i] = filterContainerItems(item, fromCreative = false)
            else if (isNovaItem(item))
                items[i] = getFakeItem(player, item)
        }
        
        if (isNovaItem(carriedItem))
            event.carriedItem = getFakeItem(player, carriedItem)
    }
    
    @EventHandler
    fun handleSetSlotPacket(event: ContainerSetSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        if (isContainerItem(item))
            event.item = filterContainerItems(item, fromCreative = false)
        else if (isNovaItem(item))
            event.item = getFakeItem(event.player, item)
    }
    
    @EventHandler
    fun handleEntityData(event: SetEntityDataPacketEvent) {
        val player = event.player
        val packet = event.packet
        val data = packet.unpackedData ?: return
        data.forEachIndexed { i, d ->
            val value = d.value
            if (value is MojangStack && isNovaItem(value)) {
                @Suppress("UNCHECKED_CAST") // Has to be <MojangStack> since the value is a MojangStack
                val newDataItem = DataItem(d.accessor as EntityDataAccessor<MojangStack>, getFakeItem(player, value, false))
                data[i] = newDataItem
            }
        }
    }
    
    @EventHandler
    fun handleSetEquipment(event: SetEquipmentPacketEvent) {
        val player = event.player
        val packet = event.packet
        val slots = packet.slots
        
        slots.forEachIndexed { i, slot ->
            if (isNovaItem(slot.second))
                slots[i] = MojangPair(slot.first, getFakeItem(player, slot.second))
        }
    }
    
    @EventHandler
    fun handleCreativeSetItem(event: SetCreativeModeSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        if (isContainerItem(item))
            event.item = filterContainerItems(item, fromCreative = true)
        else if (isFakeItem(item))
            event.item = getNovaItem(item)
    }
    
    @EventHandler
    fun handleRecipes(event: UpdateRecipesPacketEvent) {
        val packet = event.packet
        packet.recipes.forEachIndexed { i, recipe ->
            if (isNovaItem(recipe.resultItem)) {
                val id = recipe.id.namespacedKey
                if (id in RecipeManager.fakeRecipes)
                    packet.recipes[i] = RecipeManager.fakeRecipes[id]!!
            }
        }
    }
    
    internal fun isNovaItem(item: MojangStack): Boolean {
        return item.item == SERVER_SIDE_ITEM
            && item.tag != null
            && item.tag!!.contains("nova", NBTUtils.TAG_COMPOUND)
    }
    
    internal fun isFakeItem(item: MojangStack): Boolean {
        return item.tag != null
            && item.tag!!.contains("nova", NBTUtils.TAG_COMPOUND)
            && item.tag!!.contains("CustomModelData", NBTUtils.TAG_INT)
    }
    
    internal fun isContainerItem(item: MojangStack): Boolean {
        return item.item == Items.BUNDLE
            || item.item in ItemUtils.SHULKER_BOX_ITEMS
    }
    
    internal fun getNovaItem(item: MojangStack): MojangStack {
        return item.apply {
            this.item = SERVER_SIDE_ITEM
            
            val tag = tag!!
            tag.remove("CustomModelData")
            tag.remove("Damage")
            
            val nova = tag.getOrNull<CompoundTag>("nova")
            val display = tag.getOrNull<CompoundTag>("display")
            
            if (nova != null && display != null) {
                display.remove("Lore")
                
                val clientName = display.getOrNull<StringTag>("Name")?.asString
                val novaName = nova.getOrNull<StringTag>("name")?.asString
                
                if (clientName == novaName)
                    display.remove("Name")
            }
        }
    }
    
    internal fun getFakeItem(player: Player?, item: MojangStack, useName: Boolean = true): MojangStack {
        val itemTag = item.tag!!
        val novaTag = itemTag.getCompound("nova")
            ?: throw IllegalStateException("Item is not a Nova item!")
        
        val id = novaTag.getString("id") ?: return getMissingItem(item, null)
        val subId = novaTag.getInt("subId")
        val isBlock = novaTag.getBoolean("isBlock")
        val durabilityPercentage = if (novaTag.contains("durability")) novaTag.getDouble("durability") else null
        val name = novaTag.getString("name")
        val lore = if (novaTag.contains("lore")) novaTag.getList("lore", NBTUtils.TAG_STRING) else null
        
        val (itemData, blockData) = Resources.getModelDataOrNull(id) ?: return getMissingItem(item, id)
        val data = (if (isBlock) blockData else itemData) ?: return getMissingItem(item, id)
        
        val newItem = item.copy()
        val newItemTag = newItem.tag!!
        newItem.item = CraftMagicNumbers.getItem(data.material)
        newItemTag.putInt("CustomModelData", data.dataArray[subId])
        
        val displayTag: CompoundTag = if (newItemTag.contains("display")) {
            newItemTag.getCompound("display")
        } else CompoundTag().also { newItemTag.put("display", it) }
        
        if (useName && !displayTag.contains("Name")) {
            displayTag.putString("Name", name)
        }
        
        if (lore != null) {
            displayTag.put("Lore", lore)
        }
        
        if (player != null && player in AdvancedTooltips.players) {
            val newLore = displayTag.getOrPut("Lore", ::ListTag)
            newLore.add(StringTag.valueOf(ComponentSerializer.toString(TextComponent.fromLegacyText("§8$id").withoutPreFormatting())))
        }
        
        if (durabilityPercentage != null) {
            val maxDurability = newItem.item.maxDamage
            newItem.damageValue = maxDurability - (maxDurability * durabilityPercentage).toInt()
        }
        
        return newItem
    }
    
    internal fun filterContainerItems(item: MojangStack, fromCreative: Boolean): MojangStack {
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
        newItem.tag!!.putInt("CustomModelData", 0)
        newItem.tag!!.getCompound("display")!!.put("Lore", NBTUtils.createStringList(
            listOf(ComponentSerializer.toString(coloredText(ChatColor.RED, "Missing model for $id").withoutPreFormatting()))
        ))
        
        return newItem
    }
    
}