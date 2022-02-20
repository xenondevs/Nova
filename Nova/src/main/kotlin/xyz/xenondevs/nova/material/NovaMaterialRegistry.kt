package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

object NovaMaterialRegistry : INovaMaterialRegistry {
    
    private val materialsById = HashMap<String, NovaMaterial>()
    private val materialsByName = HashMap<String, ArrayList<NovaMaterial>>()
    
    val values: Collection<NovaMaterial>
        get() = materialsById.values
    
    val sortedValues: Set<NovaMaterial> by lazy { materialsById.values.toSortedSet() }
    
    override fun getOrNull(id: String): NovaMaterial? = materialsById[id.lowercase()]
    override fun getOrNull(item: ItemStack): NovaMaterial? = item.novaMaterial
    override fun get(id: String): NovaMaterial = getOrNull(id)!!
    override fun get(item: ItemStack): NovaMaterial = getOrNull(item)!!
    override fun getNonNamespaced(name: String): List<NovaMaterial> = materialsByName[name.lowercase()] ?: emptyList()
    
    fun registerEnergyTileEntity(
        addon: Addon,
        name: String,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true
    ): NovaMaterial {
        return registerTileEntity(
            addon,
            name,
            tileEntityConstructor,
            hitboxType,
            listOf(EnergyHolder::modifyItemBuilder),
            placeCheck,
            isDirectional,
        )
    }
    
    fun registerTileEntity(
        addon: Addon,
        name: String,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
    ): NovaMaterial {
        val namespace = addon.description.id
        val id = name.addNamespace(namespace)
        val localizedName = "block.$namespace.$name"
        val material = NovaMaterial(id, localizedName, null, itemBuilderModifiers, hitboxType,
            tileEntityConstructor, placeCheck, isDirectional)
        
        return register(material)
    }
    
    internal fun registerTileEntity(
        id: String,
        name: String,
        itemBuilderModifiers: List<ItemBuilderModifierFun>?,
        hitboxType: Material,
        tileEntityConstructor: TileEntityConstructor?,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
    ): NovaMaterial {
        val material = NovaMaterial(id, name, null, itemBuilderModifiers,
            hitboxType, tileEntityConstructor, placeCheck, isDirectional)
        
        return register(material)
    }
    
    fun registerItem(addon: Addon, name: String, localizedName: String = "", novaItem: NovaItem? = null): NovaMaterial {
        val namespace = addon.description.id
        val id = name.addNamespace(namespace)
        return register(NovaMaterial(id, localizedName, novaItem))
    }
    
    fun registerDefaultItem(addon: Addon, name: String, novaItem: NovaItem? = null): NovaMaterial {
        val namespace = addon.description.id
        val id = name.addNamespace(namespace)
        val localizedName = "item.$namespace.$name"
        return register(NovaMaterial(id, localizedName, novaItem))
    }
    
    internal fun registerDefaultItem(name: String, novaItem: NovaItem? = null) =
        registerItem("nova:$name", "item.nova.$name", novaItem)
    
    internal fun registerItem(id: String, name: String, novaItem: NovaItem? = null) =
        register(NovaMaterial(id, name, novaItem))
    
    internal fun registerItem(id: String) =
        register(NovaMaterial(id, ""))
    
    private fun register(material: NovaMaterial): NovaMaterial {
        val id = material.id
        require(id !in materialsById) { "Duplicate NovaMaterial id: $id" }
        
        materialsById[id] = material
        materialsByName.getOrPut(id.substringAfter(':')) { ArrayList() } += material
        
        return material
    }
    
}