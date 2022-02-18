package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.resources.Resources
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.world.armorstand.FakeArmorStand
import java.util.*
import xyz.xenondevs.nova.api.material.NovaMaterial as INovaMaterial

typealias ItemBuilderModifierFun = (ItemBuilder, TileEntity?) -> ItemBuilder
typealias TileEntityConstructor = ((UUID, CompoundElement, NovaMaterial, UUID, FakeArmorStand) -> TileEntity)
typealias PlaceCheckFun = ((Player, ItemStack, Location) -> Boolean)

class NovaMaterial internal constructor(
    override val id: String,
    val localizedName: String,
    val novaItem: NovaItem? = null,
    private val itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
    val hitboxType: Material? = null,
    val tileEntityConstructor: TileEntityConstructor? = null,
    val placeCheck: PlaceCheckFun? = null,
    val isDirectional: Boolean = true
) : INovaMaterial, Comparable<NovaMaterial> {
    
    val item: ModelData by lazy { Resources.getModelData(id).first!! }
    val block: ModelData? by lazy { Resources.getModelData(id).second }
    
    val basicItemProvider: ItemProvider by lazy { ItemWrapper(createBasicItemBuilder().get()) }
    val itemProvider: ItemProvider by lazy { ItemWrapper(createItemStack()) }
    
    val basicClientsideProvider: ItemProvider by lazy { ItemWrapper(item.createClientsideItemStack(localizedName)) }
    val clientsideProvider: ItemProvider by lazy { ItemWrapper(modifyItemBuilder(item.createClientsideItemBuilder(localizedName)).get()) }
    
    val isTileEntity = tileEntityConstructor != null
    
    /**
     * Creates a basic [ItemBuilder][ItemBuilder] without any additional information
     * like an energy bar added to the [ItemStack].
     *
     * Can be used for just previewing the item type or as a base in
     * a `createItemBuilder` function for a [TileEntity].
     */
    fun createBasicItemBuilder(): ItemBuilder = item.createItemBuilder(localizedName)
    
    /**
     * Creates an [ItemBuilder][ItemBuilder] for this [NovaMaterial].
     *
     * The [TileEntity] provided must be of the same type as the [TileEntity]
     * returned by the [tileEntityConstructor] function.
     *
     */
    fun createItemBuilder(tileEntity: TileEntity? = null): ItemBuilder {
        return modifyItemBuilder(createBasicItemBuilder(), tileEntity)
    }
    
    /**
     * Creates an [ItemStack] for this [NovaMaterial].
     */
    fun createItemStack(amount: Int = 1, tileEntity: TileEntity? = null): ItemStack =
        createItemBuilder(tileEntity).setAmount(amount).get()
    
    override fun compareTo(other: NovaMaterial): Int {
        return item.data.compareTo(other.item.data)
    }
    
    override fun getLocalizedName(locale: String): String {
        return LocaleManager.getTranslatedName(locale, this)
    }
    
    override fun toString() = id
    
    private fun modifyItemBuilder(itemBuilder: ItemBuilder, tileEntity: TileEntity? = null): ItemBuilder {
        var builder = itemBuilder
        itemBuilderModifiers?.forEach { builder = it.invoke(builder, tileEntity) }
        if (novaItem != null) builder = novaItem.modifyItemBuilder(itemBuilder)
        
        return builder
    }
    
}