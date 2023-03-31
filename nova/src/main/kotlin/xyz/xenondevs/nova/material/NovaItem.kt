package xyz.xenondevs.nova.material

import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.ItemWrapper
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.ItemLogic
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.util.nmsCopy
import kotlin.math.min
import kotlin.reflect.KClass

@Suppress("MemberVisibilityCanBePrivate", "LeakingThis")
open class NovaItem internal constructor(
    val id: ResourceLocation,
    val localizedName: String,
    internal val itemLogic: ItemLogic,
    private val _maxStackSize: Int = 64,
    val craftingRemainingItem: ItemBuilder? = null,
    val isHidden: Boolean = false,
    val block: NovaBlock? = null
) {
    
    val maxStackSize: Int
        get() = min(_maxStackSize, itemLogic.vanillaMaterial.maxStackSize)
    
    val model: ItemModelData by lazy {
        val itemModelData = Resources.getModelData(id).item!!
        if (itemModelData.size == 1)
            return@lazy itemModelData.values.first()
        
        return@lazy itemModelData[itemLogic.vanillaMaterial]!!
    }
    
    //<editor-fold desc="ItemProviders">
    val basicClientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(model.dataArray.size) { subId ->
            ItemWrapper(
                model.createClientsideItemBuilder(
                    itemLogic.getPacketItemData(null, null).name,
                    null,
                    subId
                ).get()
            )
        }
    }
    
    val clientsideProviders: LazyArray<ItemProvider> by lazy {
        LazyArray(model.dataArray.size) { subId ->
            val itemStack = model.createItemBuilder(subId).get()
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
    //</editor-fold>
    
    init {
        this.itemLogic.setMaterial(this)
    }
    
    /**
     * Creates an [ItemBuilder] for this [NovaItem].
     */
    fun createItemBuilder(): ItemBuilder =
        itemLogic.modifyItemBuilder(model.createItemBuilder())
    
    /**
     * Creates a clientside [ItemBuilder] for this [NovaItem].
     * It does not have a display name, lore, or any special nbt data.
     */
    fun createClientsideItemBuilder(): ItemBuilder =
        model.createClientsideItemBuilder()
    
    fun createItemStack(amount: Int = 1): ItemStack =
        createItemBuilder().setAmount(amount).get()
    
    fun createClientsideItemStack(amount: Int): ItemStack =
        createClientsideItemBuilder().setAmount(amount).get()
    
    fun getLocalizedName(locale: String): String =
        LocaleManager.getTranslatedName(locale, this)
    
    inline fun <reified T: ItemBehavior> hasBehavior(): Boolean =
        hasBehavior(T::class)
    
    fun <T: ItemBehavior> hasBehavior(behavior: KClass<T>): Boolean =
        itemLogic.hasBehavior(behavior)
    
    inline fun <reified T : ItemBehavior> getBehavior(): T? =
        getBehavior(T::class)
    
    fun <T : ItemBehavior> getBehavior(behavior: KClass<T>): T? =
        itemLogic.getBehavior(behavior)
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC = NovaRegistries.ITEM.byNameCodec()
        
        internal fun of(block: NovaBlock, item: ItemLogic, maxStackSize: Int, craftingRemainingItem: ItemBuilder? = null, isHidden: Boolean = false) =
            NovaItem(block.id, block.localizedName, item, maxStackSize, craftingRemainingItem, isHidden, block)
        
    }
    
}