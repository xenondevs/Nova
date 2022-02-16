package xyz.xenondevs.nova.material

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.chat.ComponentSerializer
import net.minecraft.network.syncher.EntityDataAccessor
import net.minecraft.network.syncher.SynchedEntityData.DataItem
import net.minecraft.world.item.Items
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.craftbukkit.v1_18_R1.util.CraftMagicNumbers
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.initialize.Initializable
import xyz.xenondevs.nova.integration.customitems.CustomItemServiceManager
import xyz.xenondevs.nova.network.event.clientbound.*
import xyz.xenondevs.nova.network.event.serverbound.SetCreativeModeSlotPacketEvent
import xyz.xenondevs.nova.util.data.NBTUtils
import xyz.xenondevs.nova.util.data.coloredText
import xyz.xenondevs.nova.util.data.withoutPreFormatting
import xyz.xenondevs.nova.util.namespacedKey
import com.mojang.datafixers.util.Pair as MojangPair
import net.minecraft.world.item.ItemStack as MojangStack

object PacketItems : Initializable(), Listener {
    
    val SERVER_SIDE_MATERIAL = Material.SHULKER_SHELL
    val SERVER_SIDE_ITEM = CraftMagicNumbers.getItem(SERVER_SIDE_MATERIAL)!!
    
    override val inMainThread = true
    override val dependsOn = CustomItemServiceManager
    
    override fun init() {
        Bukkit.getServer().pluginManager.registerEvents(this, NOVA)
    }
    
    @EventHandler
    fun handleSetContentPacket(event: ContainerSetContentPacketEvent) {
        val packet = event.packet
        val items = packet.items
        val carriedItem = packet.carriedItem
        
        items.forEachIndexed { i, item ->
            if (isNovaItem(item))
                items[i] = getFakeItem(item)
        }
        
        if (isNovaItem(carriedItem))
            event.carriedItem = getFakeItem(carriedItem)
    }
    
    @EventHandler
    fun handleSetSlotPacket(event: ContainerSetSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        if (isNovaItem(item))
            event.item = getFakeItem(item)
    }
    
    @EventHandler
    fun handleEntityData(event: SetEntityDataPacketEvent) {
        val packet = event.packet
        val data = packet.unpackedData ?: return
        data.forEachIndexed { i, d ->
            val value = d.value
            if (value is MojangStack && isNovaItem(value)) {
                val newDataItem = DataItem(d.accessor as EntityDataAccessor<MojangStack>, getFakeItem(value))
                data[i] = newDataItem
            }
        }
    }
    
    @EventHandler
    fun handleSetEquipment(event: SetEquipmentPacketEvent) {
        val packet = event.packet
        val slots = packet.slots
        
        slots.forEachIndexed { i, slot ->
            if (isNovaItem(slot.second))
                slots[i] = MojangPair(slot.first, getFakeItem(slot.second))
        }
    }
    
    @EventHandler
    fun handleCreativeSetItem(event: SetCreativeModeSlotPacketEvent) {
        val packet = event.packet
        val item = packet.item
        if (isFakeItem(item))
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
    
    internal fun getNovaItem(item: MojangStack): MojangStack {
        return item.apply {
            this.item = SERVER_SIDE_ITEM
            tag?.remove("CustomModelData")
        }
    }
    
    internal fun getFakeItem(item: MojangStack): MojangStack {
        val novaTag = item.tag!!.getCompound("nova")
            ?: throw IllegalStateException("Item is not a Nova item!")
        val id = novaTag.getString("id") ?: return getMissingItem(item, null)
        val subId = novaTag.getInt("subId")
        val isBlock = novaTag.getBoolean("isBlock")
        
        val (itemData, blockData) = Resources.getModelDataOrNull(id) ?: return getMissingItem(item, id)
        val data = (if (isBlock) blockData else itemData) ?: return getMissingItem(item, id)
        
        val newItem = item.copy()
        newItem.item = CraftMagicNumbers.getItem(data.material)
        newItem.tag!!.putInt("CustomModelData", data.dataArray[subId])
        
        return newItem
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