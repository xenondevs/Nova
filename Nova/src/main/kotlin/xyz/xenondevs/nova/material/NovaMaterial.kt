package xyz.xenondevs.nova.material

import de.studiocode.invui.item.ItemProvider
import de.studiocode.invui.item.ItemWrapper
import de.studiocode.invui.item.builder.ItemBuilder
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
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

class NovaMaterial(
    val typeName: String,
    val localizedName: String,
    val item: ModelData,
    val novaItem: NovaItem? = null,
    private val itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
    val block: ModelData? = null,
    val hitboxType: Material? = null,
    val tileEntityConstructor: TileEntityConstructor? = null,
    val placeCheck: PlaceCheckFun? = null,
    val isDirectional: Boolean = true,
    val legacyItemIds: IntArray? = null,
) : INovaMaterial, Comparable<NovaMaterial> {
    
    val isBlock = block != null && tileEntityConstructor != null
    
    val basicItemProvider: ItemProvider = ItemWrapper(createBasicItemBuilder().get())
    val itemProvider: ItemProvider = ItemWrapper(createItemStack())
    
    val maxStackSize = item.material.maxStackSize
    
    init {
        require(item.dataArray.isNotEmpty())
    }
    
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
        var builder = createBasicItemBuilder()
        itemBuilderModifiers?.forEach { builder = it.invoke(builder, tileEntity) }
        if (novaItem != null) builder = novaItem.modifyItemBuilder(builder)
        return builder
    }
    
    /**
     * Creates an [ItemStack] for this [NovaMaterial].
     */
    fun createItemStack(amount: Int = 1): ItemStack = createItemBuilder().setAmount(amount).get()
    
    override fun compareTo(other: NovaMaterial): Int {
        return item.data.compareTo(other.item.data)
    }
    
    override val id: String
        get() = "nova:${typeName.lowercase()}"
    
    override fun getLocalizedName(locale: String): String {
        return LocaleManager.getTranslatedName(locale, this)
    }
    
    override fun toString() = typeName
    
}