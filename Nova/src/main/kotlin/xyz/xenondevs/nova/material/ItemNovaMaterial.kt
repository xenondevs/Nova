package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial

@Suppress("MemberVisibilityCanBePrivate", "LeakingThis")
open class ItemNovaMaterial internal constructor(
    final override val id: NamespacedId,
    val localizedName: String,
    novaItem: NovaItem? = null,
) : INovaMaterial {
    
    val novaItem: NovaItem = novaItem ?: NovaItem()
    val item: ItemModelData by lazy { Resources.getModelData(id).first!! }
    val maxStackSize: Int
        get() = item.material.maxStackSize
    
    val itemProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(item.dataArray.size) { ItemWrapper(this.novaItem.modifyItemBuilder(item.createItemBuilder(it)).get()) }
    }
    
    val basicClientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(item.dataArray.size) { subId ->
            val itemStack = item.createItemBuilder(subId).get()
            ItemWrapper(item.createClientsideItemBuilder(
                this.novaItem.getName(itemStack),
                null,
                subId
            ).get())
        }
    }
    
    val clientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(item.dataArray.size) { subId ->
            val itemStack = itemProvider.get()
            ItemWrapper(this.novaItem.modifyItemBuilder(item.createClientsideItemBuilder(
                this.novaItem.getName(itemStack),
                this.novaItem.getLore(itemStack),
                subId
            )).get())
        }
    }
    
    val itemProvider: ItemProvider by lazy { itemProviders[0] }
    val basicClientsideProvider: ItemProvider by lazy { basicClientsideProviders[0] }
    val clientsideProvider: ItemProvider by lazy { clientsideProviders[0] }
    
    init {
        this.novaItem.setMaterial(this)
    }
    
    /**
     * Creates a basic [ItemBuilder][ItemBuilder] without any additional information
     * like an energy bar added to the [ItemStack].
     *
     * Can be used for just previewing the item type or as a base in
     * a `createItemBuilder` function for a [TileEntity].
     */
    fun createBasicItemBuilder(): ItemBuilder =
        item.createItemBuilder()
    
    /**
     * Creates an [ItemBuilder][ItemBuilder] for this [ItemNovaMaterial].
     */
    fun createItemBuilder(): ItemBuilder =
        novaItem.modifyItemBuilder(createBasicItemBuilder())
    
    fun createClientsideItemBuilder(): ItemBuilder =
        item.createClientsideItemBuilder()
    
    /**
     * Creates an [ItemStack] for this [ItemNovaMaterial].
     */
    fun createItemStack(amount: Int = 1): ItemStack =
        createItemBuilder().setAmount(amount).get()
    
    override fun getLocalizedName(locale: String): String =
        LocaleManager.getTranslatedName(locale, this)
    
    override fun toString() = id.toString()
    
}