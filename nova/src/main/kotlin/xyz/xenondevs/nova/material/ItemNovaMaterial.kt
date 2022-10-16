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
import xyz.xenondevs.nova.util.data.LazyArray
import kotlin.math.min
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial

@Suppress("MemberVisibilityCanBePrivate", "LeakingThis")
open class ItemNovaMaterial internal constructor(
    final override val id: NamespacedId,
    val localizedName: String,
    val novaItem: NovaItem,
    maxStackSize: Int = 64
) : INovaMaterial {
    
    val item: ItemModelData by lazy {
        val itemModelData = Resources.getModelData(id).first!!
        if (itemModelData.size == 1)
            return@lazy itemModelData.values.first()
        
        return@lazy itemModelData[novaItem.vanillaMaterial]!!
    }
    
    val maxStackSize: Int
    
    val basicClientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(item.dataArray.size) { subId ->
            val itemStack = item.createItemBuilder(subId).get()
            val itemDisplayData = novaItem.getPacketItemData(itemStack)
            ItemWrapper(
                item.createClientsideItemBuilder(
                    itemDisplayData.name,
                    null,
                    subId
                ).get()
            )
        }
    }
    
    val clientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(item.dataArray.size) { subId ->
            val itemStack = item.createItemBuilder(subId).get()
            val itemDisplayData = novaItem.getPacketItemData(itemStack)
            ItemWrapper(
                novaItem.modifyItemBuilder(
                    item.createClientsideItemBuilder(
                        itemDisplayData.name,
                        itemDisplayData.lore,
                        subId
                    )
                ).get()
            )
        }
    }
    
    val basicClientsideProvider: ItemProvider by lazy { basicClientsideProviders[0] }
    val clientsideProvider: ItemProvider by lazy { clientsideProviders[0] }
    
    init {
        this.novaItem.setMaterial(this)
        this.maxStackSize = min(maxStackSize, novaItem.vanillaMaterial.maxStackSize)
    }
    
    /**
     * Creates an [ItemBuilder] for this [ItemNovaMaterial].
     */
    fun createItemBuilder(): ItemBuilder =
        novaItem.modifyItemBuilder(item.createItemBuilder())
    
    /**
     * Creates a clientside [ItemBuilder] for this [ItemNovaMaterial].
     * It does not have a display name, lore, or any special nbt data.
     */
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