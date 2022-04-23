package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial

open class ItemNovaMaterial internal constructor(
    final override val id: NamespacedId,
    val localizedName: String,
    val novaItem: NovaItem? = null,
) : INovaMaterial {
    
    val item: ModelData by lazy { Resources.getModelData(id).first!! }
    
    val basicItemProvider: ItemProvider by lazy { ItemWrapper(createBasicItemBuilder().get()) }
    val itemProvider: ItemProvider by lazy { ItemWrapper(createItemStack()) }
    
    val basicClientsideProvider: ItemProvider by lazy { ItemWrapper(item.createClientsideItemStack(localizedName)) }
    val clientsideProvider: ItemProvider by lazy { ItemWrapper(modifyItemBuilder(item.createClientsideItemBuilder(localizedName)).get()) }
    
    /**
     * Creates a basic [ItemBuilder][ItemBuilder] without any additional information
     * like an energy bar added to the [ItemStack].
     *
     * Can be used for just previewing the item type or as a base in
     * a `createItemBuilder` function for a [TileEntity].
     */
    fun createBasicItemBuilder(): ItemBuilder =
        item.createItemBuilder(localizedName)
    
    /**
     * Creates an [ItemBuilder][ItemBuilder] for this [ItemNovaMaterial].
     */
    fun createItemBuilder(): ItemBuilder =
        modifyItemBuilder(createBasicItemBuilder())
    
    /**
     * Creates an [ItemStack] for this [ItemNovaMaterial].
     */
    fun createItemStack(amount: Int = 1): ItemStack =
        createItemBuilder().setAmount(amount).get()
    
    override fun getLocalizedName(locale: String): String =
        LocaleManager.getTranslatedName(locale, this)
    
    override fun toString() = id.toString()
    
    protected fun modifyItemBuilder(itemBuilder: ItemBuilder): ItemBuilder {
        var builder = itemBuilder
        if (novaItem != null) builder = novaItem.modifyItemBuilder(itemBuilder)
        
        return builder
    }
    
}