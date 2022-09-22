package xyz.xenondevs.nova.material

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.Consumable
import xyz.xenondevs.nova.item.behavior.ItemBehavior
import xyz.xenondevs.nova.item.impl.TileEntityItemBehavior
import xyz.xenondevs.nova.material.options.FoodOptions
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.TileEntityBlock
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
        properties: List<BlockPropertyType<*>> = emptyList()
    ): TileEntityNovaMaterial {
        return registerTileEntity(
            addon, name, options, tileEntityConstructor, NovaItem(TileEntityItemBehavior), 64,
            if (isInteractive) TileEntityBlock.INTERACTIVE else TileEntityBlock.NON_INTERACTIVE,
            properties, placeCheck, multiBlockLoader
        )
    }
    
    fun registerTileEntity(
        addon: Addon,
        name: String,
        options: BlockOptions,
        tileEntityConstructor: TileEntityConstructor,
        item: NovaItem = NovaItem(),
        maxStackSize: Int = 64,
        block: NovaBlock<NovaTileEntityState>,
        properties: List<BlockPropertyType<*>> = emptyList(),
        placeCheck: PlaceCheckFun? = null,
        multiBlockLoader: MultiBlockLoader? = null
    ): TileEntityNovaMaterial {
        val namespace = addon.description.id
        val material = TileEntityNovaMaterial(
            NamespacedId(namespace, name), "block.$namespace.$name", item, maxStackSize,
            block, options, tileEntityConstructor, properties, placeCheck, multiBlockLoader
        )
        return register(material)
    }
    
    fun registerBlock(
        addon: Addon,
        name: String,
        options: BlockOptions,
        item: NovaItem = NovaItem(),
        maxStackSize: Int = 64,
        block: NovaBlock<NovaBlockState> = NovaBlock.Default,
        properties: List<BlockPropertyType<*>> = emptyList(),
        placeCheck: PlaceCheckFun? = null,
        multiBlockLoader: MultiBlockLoader? = null
    ): BlockNovaMaterial {
        val namespace = addon.description.id
        val material = BlockNovaMaterial(
            NamespacedId(namespace, name), "block.$namespace.$name", item, maxStackSize,
            block, options, properties, placeCheck, multiBlockLoader
        )
        return register(material)
    }
    
    fun registerItem(addon: Addon, name: String, localizedName: String = "", novaItem: NovaItem = NovaItem(), maxStackSize: Int = 64): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId(addon.description.id, name),
            localizedName,
            novaItem,
            maxStackSize
        ))
    }
    
    fun registerItem(addon: Addon, name: String, localizedName: String = "", vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId(addon.description.id, name),
            localizedName,
            NovaItem(*itemBehaviors)
        ))
    }
    
    fun registerDefaultItem(addon: Addon, name: String, novaItem: NovaItem = NovaItem(), maxStackSize: Int = 64): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(addon.description.id, name),
            "item.$namespace.$name",
            novaItem,
            maxStackSize
        ))
    }
    
    fun registerDefaultItem(addon: Addon, name: String, vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(namespace, name),
            "item.$namespace.$name",
            NovaItem(*itemBehaviors)
        ))
    }
    
    fun registerFood(addon: Addon, name: String, options: FoodOptions): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(namespace, name),
            "item.$namespace.$name",
            NovaItem(Consumable(options))
        ))
    }
    
    internal fun registerDefaultCoreItem(name: String, vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId("nova", name),
            "item.nova.$name",
            NovaItem(*itemBehaviors)
        ))
    }
    
    internal fun registerCoreItem(name: String, localizedName: String = "", vararg itemBehaviors: ItemBehavior): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId("nova", name),
            localizedName,
            NovaItem(*itemBehaviors)
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