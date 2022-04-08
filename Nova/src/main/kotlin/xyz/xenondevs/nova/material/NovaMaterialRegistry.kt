package xyz.xenondevs.nova.material

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.tileentity.network.energy.holder.NovaEnergyHolder
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.TileEntityBlock
import xyz.xenondevs.nova.world.block.model.ArmorStandModelProvider
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

object NovaMaterialRegistry : INovaMaterialRegistry {
    
    private val materialsById = HashMap<String, ItemNovaMaterial>()
    private val materialsByName = HashMap<String, ArrayList<ItemNovaMaterial>>()
    
    val values: Collection<ItemNovaMaterial>
        get() = materialsById.values
    
    val sortedValues: Set<ItemNovaMaterial> by lazy { materialsById.values.toSortedSet() }
    
    override fun getOrNull(id: String): ItemNovaMaterial? = materialsById[id.lowercase()]
    override fun getOrNull(item: ItemStack): ItemNovaMaterial? = item.novaMaterial
    override fun get(id: String): ItemNovaMaterial = getOrNull(id)!!
    override fun get(item: ItemStack): ItemNovaMaterial = getOrNull(item)!!
    override fun getNonNamespaced(name: String): List<ItemNovaMaterial> = materialsByName[name.lowercase()] ?: emptyList()
    
    fun registerEnergyTileEntity(
        addon: Addon,
        name: String,
        options: BlockOptions,
        tileEntityConstructor: TileEntityConstructor,
        placeCheck: PlaceCheckFun? = null,
        multiBlockLoader: MultiBlockLoader? = null,
        isInteractable: Boolean = true,
        properties: List<BlockPropertyType<*>> = listOf(Directional)
    ): TileEntityNovaMaterial {
        return registerTileEntity(addon, name, options, tileEntityConstructor, listOf(NovaEnergyHolder::modifyItemBuilder),
            placeCheck, multiBlockLoader, isInteractable, properties)
    }
    
    fun registerTileEntity(
        addon: Addon,
        name: String,
        options: BlockOptions,
        tileEntityConstructor: TileEntityConstructor,
        itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
        placeCheck: PlaceCheckFun? = null,
        multiBlockLoader: MultiBlockLoader? = null,
        isInteractable: Boolean = true,
        properties: List<BlockPropertyType<*>> = listOf(Directional)
    ): TileEntityNovaMaterial {
        val namespace = addon.description.id
        val id = name.addNamespace(namespace)
        val localizedName = "block.$namespace.$name"
        val material = TileEntityNovaMaterial(id, localizedName, null,
            if (isInteractable) TileEntityBlock.INTERACTIVE else TileEntityBlock.NON_INTERACTIVE,
            options, tileEntityConstructor, itemBuilderModifiers, ArmorStandModelProvider, properties, placeCheck, multiBlockLoader)
        
        return register(material)
    }
    
    fun registerItem(addon: Addon, name: String, localizedName: String = "", novaItem: NovaItem? = null): ItemNovaMaterial {
        val namespace = addon.description.id
        val id = name.addNamespace(namespace)
        return register(ItemNovaMaterial(id, localizedName, novaItem))
    }
    
    fun registerDefaultItem(addon: Addon, name: String, novaItem: NovaItem? = null): ItemNovaMaterial {
        val namespace = addon.description.id
        val id = name.addNamespace(namespace)
        val localizedName = "item.$namespace.$name"
        return register(ItemNovaMaterial(id, localizedName, novaItem))
    }
    
    internal fun registerDefaultItem(name: String, novaItem: NovaItem? = null) =
        registerItem("nova:$name", "item.nova.$name", novaItem)
    
    internal fun registerItem(id: String, name: String, novaItem: NovaItem? = null) =
        register(ItemNovaMaterial(id, name, novaItem))
    
    internal fun registerItem(id: String) =
        register(ItemNovaMaterial(id, ""))
    
    private fun <T : ItemNovaMaterial> register(material: T): T {
        val id = material.id
        require(id !in materialsById) { "Duplicate NovaMaterial id: $id" }
        
        materialsById[id] = material
        materialsByName.getOrPut(id.substringAfter(':')) { ArrayList() } += material
        
        return material
    }
    
}