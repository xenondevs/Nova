package xyz.xenondevs.nova.data.resources.model.data

import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.builder.setLore
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.logic.PacketItems
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.nmsCopy
import net.minecraft.world.item.ItemStack as MojangStack

open class ItemModelData(
    private val item: NovaItem,
    private val material: Material,
    private val namedCustomModelData: Map<String, Int>,
    private val unnamedCustomModelData: IntArray
) {
    
    val size = unnamedCustomModelData.size
    val unnamedClientsideProviders: Array<ItemProvider> =
        Array(size) { ItemWrapper(createClientsideItemStack(false, it)) }
    val unnamedBasicClientsideProviders: Array<ItemProvider> =
        Array(size) { ItemWrapper(createClientsideItemStack(true, it)) }
    val clientsideProviders: Map<String, ItemProvider> =
        namedCustomModelData.mapValues { (modelId, _) -> ItemWrapper(createClientsideItemStack(false, modelId)) }
    val basicClientsideProviders: Map<String, ItemProvider> =
        namedCustomModelData.mapValues { (modelId, _) -> ItemWrapper(createClientsideItemStack(true, modelId)) }
    val clientsideProvider: ItemProvider
        get() = unnamedClientsideProviders[0]
    val basicClientsideProvider: ItemProvider
        get() = unnamedBasicClientsideProviders[0]
    
    /**
     * Creates a new [ItemBuilder] in client-side format, using the given [name], [lore], and [modelId].
     */
    fun createClientsideItemBuilder(name: Component? = null, lore: List<Component>? = null, modelId: String = "default"): ItemBuilder =
        createClientsideItemBuilderInternal(name, lore, namedCustomModelData[modelId] ?: 0)
    
    /**
     * Creates a new [ItemBuilder] in client-side format using the given [name], [lore] and [modelId].
     */
    fun createClientsideItemBuilder(name: Component? = null, lore: List<Component>? = null, modelId: Int): ItemBuilder =
        createClientsideItemBuilderInternal(name, lore, unnamedCustomModelData[modelId])
    
    private fun createClientsideItemBuilderInternal(name: Component? = null, lore: List<Component>? = null, customModelData: Int): ItemBuilder =
        ItemBuilder(material)
            .setDisplayName(name ?: Component.empty())
            .setCustomModelData(customModelData)
            .addItemFlags(ItemFlag.HIDE_ATTRIBUTES)
            .apply { if (lore != null) setLore(lore) }
    
    /**
     * Creates an [ItemProvider] for the given [modelId] in client-side format by running it through [PacketItems].
     * This provider is intended for usage in GUIs or similar.
     *
     * The [basic] parameter defines whether the returned [ItemProvider] should be in basic format (i.e. only display name)
     * or full format (i.e. display name, lore, other nbt data).
     */
    fun createClientsideItemStack(basic: Boolean, modelId: String = "default"): ItemStack =
        createClientsideItemStackInternal(basic, { item.createItemBuilder(modelId) }, { createClientsideItemBuilder(it, null, modelId) })
    
    /**
     * Creates an [ItemProvider] for the given [modelId] in client-side format by running it through [PacketItems].
     * This provider is intended for usage in GUIs or similar.
     *
     * The [basic] parameter defines whether the returned [ItemProvider] should be in basic format (i.e. only display name)
     * or full format (i.e. display name, lore, other nbt data).
     */
    fun createClientsideItemStack(basic: Boolean, modelId: Int): ItemStack =
        createClientsideItemStackInternal(basic, { item.createItemBuilder(modelId) }, { createClientsideItemBuilder(it, null, modelId) })
    
    private fun createClientsideItemStackInternal(
        basic: Boolean,
        createItemBuilder: () -> ItemBuilder,
        createClientsideItemBuilder: (Component) -> ItemBuilder
    ): ItemStack {
        val clientStack: MojangStack
        if (basic) {
            val basicName = item.getPacketItemData(null).name
            clientStack = createClientsideItemBuilder(basicName).get().nmsCopy
        } else {
            clientStack = PacketItems.getClientSideStack(
                player = null,
                itemStack = createItemBuilder().get().nmsCopy,
                useName = true,
                storeServerSideTag = false
            )
            // prevent the item stack from being recognized as a server-side nova item by PacketItems
            clientStack.tag?.remove("nova")
        }
        
        return clientStack.bukkitMirror
    }
    
}