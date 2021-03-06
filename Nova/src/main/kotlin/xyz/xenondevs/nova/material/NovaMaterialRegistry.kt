package xyz.xenondevs.nova.material

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.impl.TileEntityItemBehavior
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.TileEntityBlock
import xyz.xenondevs.nova.world.block.model.ArmorStandModelProvider
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

object NovaMaterialRegistry : INovaMaterialRegistry {
    
    private val materialsById = HashMap<String, ItemNovaMaterial>()
    private val materialsByName = HashMap<String, ArrayList<ItemNovaMaterial>>()
    
    val values: Collection<ItemNovaMaterial>
        get() = materialsById.values
    
    override fun getOrNull(id: String): ItemNovaMaterial? = materialsById[id.lowercase()]
    override fun getOrNull(item: ItemStack): ItemNovaMaterial? = item.novaMaterial
    override fun get(id: String): ItemNovaMaterial = getOrNull(id)!!
    override fun get(item: ItemStack): ItemNovaMaterial = getOrNull(item)!!
    override fun getNonNamespaced(name: String): List<ItemNovaMaterial> = materialsByName[name.lowercase()] ?: emptyList()
    
    fun registerTileEntity(
        addon: Addon,
        name: String,
        options: BlockOptions,
        tileEntityConstructor: TileEntityConstructor,
        placeCheck: PlaceCheckFun? = null,
        multiBlockLoader: MultiBlockLoader? = null,
        isInteractive: Boolean = true,
        properties: List<BlockPropertyType<*>> = listOf(Directional)
    ): TileEntityNovaMaterial {
        val namespace = addon.description.id
        val material = TileEntityNovaMaterial(
            NamespacedId(namespace, name), "block.$namespace.$name", NovaItem(TileEntityItemBehavior),
            if (isInteractive) TileEntityBlock.INTERACTIVE else TileEntityBlock.NON_INTERACTIVE,
            options, tileEntityConstructor, ArmorStandModelProvider, properties, placeCheck, multiBlockLoader
        )
        return register(material)
    }
    
    fun registerItem(addon: Addon, name: String, localizedName: String = "", novaItem: NovaItem?): ItemNovaMaterial {
        return register(ItemNovaMaterial(NamespacedId(addon.description.id, name), localizedName, novaItem))
    }
    
    fun registerItem(addon: Addon, name: String, localizedName: String = "", vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId(addon.description.id, name),
            localizedName,
            itemBehaviors.takeUnless(Array<*>::isEmpty)?.let(::NovaItem)
        ))
    }
    
    fun registerDefaultItem(addon: Addon, name: String, novaItem: NovaItem?): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(NamespacedId(addon.description.id, name), "item.$namespace.$name", novaItem))
    }
    
    fun registerDefaultItem(addon: Addon, name: String, vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(namespace, name),
            "item.$namespace.$name",
            itemBehaviors.takeUnless(Array<*>::isEmpty)?.let(::NovaItem)
        ))
    }
    
    fun registerFood(addon: Addon, name: String, options: FoodOptions): FoodNovaMaterial {
        val namespace = addon.description.id
        return register(FoodNovaMaterial(
            NamespacedId(namespace, name),
            "item.$namespace.$name",
            options
        ))
    }
    
    internal fun registerDefaultCoreItem(name: String, vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId("nova", name),
            "item.nova.$name",
            itemBehaviors.takeUnless(Array<*>::isEmpty)?.let(::NovaItem)
        ))
    }
    
    internal fun registerCoreItem(name: String, localizedName: String = "", vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId("nova", name),
            localizedName,
            itemBehaviors.takeUnless(Array<*>::isEmpty)?.let(::NovaItem)
        ))
    }
    
    private fun <T : ItemNovaMaterial> register(material: T): T {
        val id = material.id
        val idStr = material.id.toString()
        require(idStr !in materialsById) { "Duplicate NovaMaterial id: $id" }
        
        materialsById[idStr] = material
        materialsByName.getOrPut(id.name) { ArrayList() } += material
        
        return material
    }
    
}