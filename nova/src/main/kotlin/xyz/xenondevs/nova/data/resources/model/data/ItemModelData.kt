package xyz.xenondevs.nova.data.resources.model.data

import net.kyori.adventure.text.Component
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.builder.setLore
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.logic.ItemLogic
import xyz.xenondevs.nova.item.logic.PacketItems
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.item.unhandledTags
import xyz.xenondevs.nova.util.nmsCopy
import net.minecraft.world.item.ItemStack as MojangStack

open class ItemModelData(val id: ResourceLocation, val material: Material, val dataArray: IntArray) {
    
    val data: Int
        get() = dataArray[0]
    
    /**
     * Creates a new [ItemBuilder] in server-side format.
     */
    fun createItemBuilder(modelId: Int = 0): ItemBuilder =
        ItemBuilder(PacketItems.SERVER_SIDE_MATERIAL)
            .addModifier { itemStack ->
                val novaCompound = CompoundTag()
                novaCompound.putString("id", id.toString())
                novaCompound.putInt("subId", modelId)
                
                val meta = itemStack.itemMeta!!
                meta.unhandledTags["nova"] = novaCompound
                itemStack.itemMeta = meta
                
                itemStack
            }
    
    /**
     * Creates a new [ItemBuilder] in client-side format.
     * 
     * Unlike the client-side provider in [NovaItem], this [ItemBuilder] does not have any additional data
     * (no display name, lore, other nbt data) applied. It will also not remove the item tooltip for empty name and lore.
     */
    fun createClientsideItemBuilder(name: Component? = null, lore: List<Component>? = null, modelId: Int = 0): ItemBuilder =
        ItemBuilder(material)
            .setDisplayName(name ?: Component.empty())
            .setCustomModelData(dataArray[modelId])
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .apply { if (lore != null) setLore(lore) }
    
    /**
     * Creates an [ItemProvider] for the given [modelId] in client-side format for usage in GUIs and similar.
     *
     * The [basic] parameter defines whether the returned [ItemProvider] should be in basic format (i.e. only display name)
     * or full format (i.e. display name, lore, other nbt data).
     */
    internal fun createClientsideItemProvider(logic: ItemLogic, basic: Boolean, modelId: Int): ItemProvider {
        val itemStack = createItemBuilder(modelId).get()
        
        val clientStack: MojangStack
        if (basic) {
            val basicName = logic.getPacketItemData(null).name
            clientStack = createClientsideItemBuilder(name = basicName, modelId = modelId).get().nmsCopy
        } else {
            clientStack = PacketItems.getClientSideStack(
                player = null,
                itemStack = itemStack.nmsCopy,
                useName = true,
                storeServerSideTag = false
            )
            clientStack.tag?.remove("nova") // prevents the item stack from being recognized as a nova item by PacketItems
        }
        
        return ItemWrapper(clientStack.bukkitMirror)
    }
    
    /**
     * Creates an [ItemProvider] of the given [modelId] for usage in display entities.
     */
    protected fun createBlockDisplayItemProvider(modelId: Int): ItemProvider {
        val itemStack = ItemBuilder(material).setCustomModelData(dataArray[modelId]).get()
        return ItemWrapper(itemStack)
    }
    
}