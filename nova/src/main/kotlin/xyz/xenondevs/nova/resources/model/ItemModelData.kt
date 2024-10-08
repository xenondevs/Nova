package xyz.xenondevs.nova.resources.model

import net.kyori.adventure.text.Component
import net.minecraft.core.component.DataComponents
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.component.CustomData
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.invui.item.builder.setDisplayName
import xyz.xenondevs.invui.item.builder.setLore
import xyz.xenondevs.nova.util.unwrap
import xyz.xenondevs.nova.world.item.NovaItem
import xyz.xenondevs.nova.world.item.logic.PacketItems

class ItemModelData internal constructor(
    private val item: NovaItem,
    private val models: Map<String, Int>,
) {
    
    private val modelNames = models.keys.toTypedArray()
    val size = models.size
    val unnamedClientsideProviders: Array<ItemProvider>
        by lazy { Array(size) { ItemWrapper(createClientsideItemStack(it)) } }
    val clientsideProviders: Map<String, ItemProvider>
        by lazy { models.mapValues { (modelId, _) -> ItemWrapper(createClientsideItemStack(modelId)) } }
    val clientsideProvider: ItemProvider
        get() = unnamedClientsideProviders[0]
    
    /**
     * Gets the custom model data for [modelId] or null if it does not exist.
     */
    fun getCustomModelData(modelId: String): Int? =
        models[modelId]
    
    /**
     * Gets the custom model data for [modelId] or null if it does not exist.
     */
    fun getCustomModelData(modelId: Int): Int? =
        models[modelNames[modelId]]
    
    /**
     * Creates a new [ItemBuilder] in client-side format, using the given [name], [lore], and [modelId].
     */
    fun createClientsideItemBuilder(name: Component? = null, lore: List<Component>? = null, modelId: String = "default"): ItemBuilder =
        ItemBuilder(clientsideProviders[modelId]!!.get().clone()).apply(name, lore)
    
    /**
     * Creates a new [ItemBuilder] in client-side format using the given [name], [lore] and [modelId].
     */
    fun createClientsideItemBuilder(name: Component? = null, lore: List<Component>? = null, modelId: Int): ItemBuilder =
        ItemBuilder(unnamedClientsideProviders[modelId].get().clone()).apply(name, lore)
    
    private fun ItemBuilder.apply(name: Component? = null, lore: List<Component>? = null): ItemBuilder {
        if (name != null)
            setDisplayName(name)
        if (lore != null)
            setLore(lore)
        return this
    }
    
    /**
     * Creates an [ItemProvider] for the given [modelId] in client-side format by running it through [PacketItems].
     * This provider is intended for usage in GUIs or similar.
     */
    fun createClientsideItemStack(modelId: String = "default"): ItemStack =
        createClientsideItemStackInternal { item.createItemStack(modelId = modelId) }
    
    /**
     * Creates an [ItemProvider] for the given [modelId] in client-side format by running it through [PacketItems].
     * This provider is intended for usage in GUIs or similar.
     */
    fun createClientsideItemStack(modelId: Int): ItemStack =
        createClientsideItemStackInternal { item.createItemStack(modelId = modelNames[modelId]) }
    
    private fun createClientsideItemStackInternal(createItemStack: () -> ItemStack): ItemStack {
        val clientStack = PacketItems.getClientSideStack(
            player = null,
            itemStack = createItemStack().unwrap(),
            storeServerSideTag = false
        )
        
        // remove existing custom data and tag item to not receive server-side tooltip (again)
        clientStack.set(DataComponents.CUSTOM_DATA, CustomData.of(CompoundTag().apply {
            putBoolean(PacketItems.SKIP_SERVER_SIDE_TOOLTIP, true)
        }))
        
        return clientStack.asBukkitMirror()
    }
    
}