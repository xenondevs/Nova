package xyz.xenondevs.nova.material

import com.mojang.datafixers.util.Either
import com.mojang.serialization.Codec
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.immutable.lazyProvider
import xyz.xenondevs.commons.provider.immutable.map
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.util.data.asDataResult
import xyz.xenondevs.nova.util.nmsCopy
import kotlin.math.min
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial

@Suppress("MemberVisibilityCanBePrivate", "LeakingThis")
open class ItemNovaMaterial internal constructor(
    final override val id: NamespacedId,
    val localizedName: String,
    val novaItem: NovaItem,
    maxStackSize: Int = 64,
    val craftingRemainingItem: ItemBuilder? = null,
    val isHidden: Boolean = false
) : INovaMaterial {
    
    override val maxStackSize: Int by lazyProvider { novaItem.vanillaMaterialProvider.map { min(maxStackSize, it.maxStackSize) } }
    
    val item: ItemModelData by lazy {
        val itemModelData = Resources.getModelData(id).item!!
        if (itemModelData.size == 1)
            return@lazy itemModelData.values.first()
        
        return@lazy itemModelData[novaItem.vanillaMaterial]!!
    }
    
    val basicClientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(item.dataArray.size) { subId ->
            ItemWrapper(
                item.createClientsideItemBuilder(
                    novaItem.getPacketItemData(null, null).name,
                    null,
                    subId
                ).get()
            )
        }
    }
    
    val clientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(item.dataArray.size) { subId ->
            val itemStack = item.createItemBuilder(subId).get()
            val clientsideItemStack = PacketItems.getClientSideStack(
                player = null,
                itemStack = itemStack.nmsCopy,
                useName = true,
                storeServerSideTag = false
            )
            clientsideItemStack.tag?.remove("nova")
            ItemWrapper(clientsideItemStack.bukkitMirror)
        }
    }
    
    val basicClientsideProvider: ItemProvider by lazy { basicClientsideProviders[0] }
    val clientsideProvider: ItemProvider by lazy { clientsideProviders[0] }
    
    init {
        this.novaItem.setMaterial(this)
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
    
    override fun createItemStack(amount: Int): ItemStack =
        createItemBuilder().setAmount(amount).get()
    
    override fun createClientsideItemStack(amount: Int): ItemStack =
        createClientsideItemBuilder().setAmount(amount).get()
    
    override fun getLocalizedName(locale: String): String =
        LocaleManager.getTranslatedName(locale, this)
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC: Codec<ItemNovaMaterial> = Codec.either(NamespacedId.CODEC, Codec.STRING).comapFlatMap(
            { either ->
                either.map(
                    { NovaMaterialRegistry.getOrNull(it).asDataResult("Could not find material with id $it") },
                    { NovaMaterialRegistry.getNonNamespaced(it).firstOrNull().asDataResult("Could not find material with id $it") }
                )
            },
            { material -> Either.left(material.id) }
        ).stable()
        
    }
    
}