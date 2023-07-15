@file:Suppress("unused", "MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.item

import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.invui.item.ItemProvider
import xyz.xenondevs.invui.item.builder.ItemBuilder
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.data.resources.model.data.ItemModelData
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.logic.ItemLogic
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.data.LazyArray
import xyz.xenondevs.nova.world.block.NovaBlock
import kotlin.math.min
import kotlin.reflect.KClass

/**
 * Represents an item type in Nova.
 */
class NovaItem internal constructor(
    val id: ResourceLocation,
    val localizedName: String,
    internal val logic: ItemLogic,
    private val _maxStackSize: Int = 64,
    val craftingRemainingItem: ItemBuilder? = null,
    val isHidden: Boolean = false,
    val block: NovaBlock? = null
) {
    
    /**
     * The maximum stack size of this [NovaItem].
     */
    val maxStackSize: Int
        get() = min(_maxStackSize, logic.vanillaMaterial.maxStackSize)
    
    /**
     * The [ItemModelData] containing all the vanilla material and custom model data to be used for this [NovaItem].
     */
    val model: ItemModelData by lazy {
        val itemModelData = ResourceLookups.MODEL_DATA_LOOKUP.getOrThrow(id).item!!
        if (itemModelData.size == 1)
            return@lazy itemModelData.values.first()
        
        return@lazy itemModelData[logic.vanillaMaterial]!!
    }
    
    /**
     * An array of [ItemProviders][ItemProvider] for each subId of this [NovaItem].
     *
     * The items are in client-side format and do not have any other special data except their display name (hence "basic").
     */
    val basicClientsideProviders: LazyArray<ItemProvider> =
        LazyArray({ model.dataArray.size }) { model.createClientsideItemProvider(logic, true, it) }
    
    /**
     * An array of [ItemProviders][ItemProvider] for each subId of this [NovaItem].
     *
     * The items are in client-side format and have all special data (lore, other nbt tags, etc.) applied.
     */
    val clientsideProviders: LazyArray<ItemProvider> =
        LazyArray({ model.dataArray.size }) { model.createClientsideItemProvider(logic, false, it) }
    
    /**
     * The basic client-side provider for the first subId of this [NovaItem].
     * @see [basicClientsideProviders]
     */
    val basicClientsideProvider: ItemProvider by lazy { basicClientsideProviders[0] }
    
    /**
     * The client-side provider for the first subId of this [NovaItem].
     * @see [clientsideProviders]
     */
    val clientsideProvider: ItemProvider by lazy { clientsideProviders[0] }
    
    init {
        logic.setMaterial(this)
    }
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem], in server-side format.
     */
    fun createItemBuilder(): ItemBuilder =
        logic.modifyItemBuilder(model.createItemBuilder())
    
    /**
     * Creates an [ItemStack] of this [NovaItem] in server-side format.
     *
     * Functionally equivalent to: `createItemBuilder().setAmount(amount).get()`
     */
    fun createItemStack(amount: Int = 1): ItemStack =
        createItemBuilder().setAmount(amount).get()
    
    /**
     * Creates an [ItemBuilder] for an [ItemStack] of this [NovaItem] in client-side format.
     */
    fun createClientsideItemBuilder(): ItemBuilder =
        model.createClientsideItemBuilder()
    
    /**
     * Creates an [ItemStack] of this [NovaItem] in client-side format.
     *
     * Functionally equivalent to: `createClientsideItemBuilder().setAmount(amount).get()`
     */
    fun createClientsideItemStack(amount: Int): ItemStack =
        createClientsideItemBuilder().setAmount(amount).get()
    
    /**
     * Checks whether this [NovaItem] has an [ItemBehavior] of the reified type [T], or a subclass of it.
     */
    inline fun <reified T : Any> hasBehavior(): Boolean =
        hasBehavior(T::class)
    
    /**
     * Checks whether this [NovaItem] has an [ItemBehavior] of the specified class [behavior], or a subclass of it.
     */
    fun <T : Any> hasBehavior(behavior: KClass<T>): Boolean =
        logic.hasBehavior(behavior)
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [T], or null if there is none.
     */
    inline fun <reified T : Any> getBehaviorOrNull(): T? =
        getBehaviorOrNull(T::class)
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [behavior], or null if there is none.
     */
    fun <T : Any> getBehaviorOrNull(behavior: KClass<T>): T? =
        logic.getBehaviorOrNull(behavior)
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [T], or throws an [IllegalStateException] if there is none.
     */
    inline fun <reified T : Any> getBehavior(): T =
        getBehavior(T::class)
    
    /**
     * Gets the first [ItemBehavior] that is an instance of [behavior], or throws an [IllegalStateException] if there is none.
     */
    fun <T : Any> getBehavior(behavior: KClass<T>): T =
        getBehaviorOrNull(behavior) ?: throw IllegalStateException("Item $id does not have a behavior of type ${behavior.simpleName}")
    
    override fun toString() = id.toString()
    
    companion object {
        
        val CODEC = NovaRegistries.ITEM.byNameCodec()
        
    }
    
}