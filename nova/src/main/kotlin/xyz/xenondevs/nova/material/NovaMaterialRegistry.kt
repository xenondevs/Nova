@file:Suppress("unused", "MemberVisibilityCanBePrivate", "DEPRECATION")

package xyz.xenondevs.nova.material

import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.data.NamespacedId
import xyz.xenondevs.nova.data.world.block.property.BlockPropertyType
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.behavior.Consumable
import xyz.xenondevs.nova.item.behavior.ItemBehaviorHolder
import xyz.xenondevs.nova.item.impl.TileEntityItemBehavior
import xyz.xenondevs.nova.material.builder.BlockNovaMaterialBuilder
import xyz.xenondevs.nova.material.builder.ItemNovaMaterialBuilder
import xyz.xenondevs.nova.material.builder.TileEntityNovaMaterialBuilder
import xyz.xenondevs.nova.material.options.BlockOptions
import xyz.xenondevs.nova.material.options.FoodOptions
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.world.block.NovaBlock
import xyz.xenondevs.nova.world.block.TileEntityBlock
import xyz.xenondevs.nova.api.data.NamespacedId as INamespacedId
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

object NovaMaterialRegistry : INovaMaterialRegistry {
    
    private val materialsById = HashMap<String, ItemNovaMaterial>()
    private val materialsByName = HashMap<String, ArrayList<ItemNovaMaterial>>()
    
    val values: Collection<ItemNovaMaterial>
        get() = materialsById.values
    
    override fun getOrNull(id: String): ItemNovaMaterial? = materialsById[id.lowercase()]
    override fun getOrNull(id: INamespacedId): ItemNovaMaterial? = getOrNull(id.toString())
    override fun getOrNull(item: ItemStack): ItemNovaMaterial? = item.novaMaterial
    override fun get(id: String): ItemNovaMaterial = getOrNull(id)!!
    override fun get(id: INamespacedId): ItemNovaMaterial = getOrNull(id)!!
    override fun get(item: ItemStack): ItemNovaMaterial = getOrNull(item)!!
    override fun getNonNamespaced(name: String): List<ItemNovaMaterial> = materialsByName[name.lowercase()] ?: emptyList()
    
    fun tileEntity(addon: Addon, name: String, tileEntity: TileEntityConstructor) =
        TileEntityNovaMaterialBuilder(addon, name, tileEntity)
    
    fun block(addon: Addon, name: String): BlockNovaMaterialBuilder =
        BlockNovaMaterialBuilder(addon, name)
    
    fun item(addon: Addon, name: String): ItemNovaMaterialBuilder =
        ItemNovaMaterialBuilder(addon, name)
    
    fun registerItem(
        addon: Addon,
        name: String,
        vararg behaviors: ItemBehaviorHolder<*>,
        localizedName: String = "item.${addon.description.id}.$name",
        isHidden: Boolean = false
    ): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(namespace, name),
            localizedName,
            NovaItem(*behaviors),
            isHidden = isHidden
        ))
    }
    
    fun registerUnnamedItem(
        addon: Addon,
        name: String,
        isHidden: Boolean = false
    ): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(namespace, name),
            "",
            NovaItem(),
            isHidden = isHidden
        ))
    }
    
    fun registerUnnamedHiddenItem(
        addon: Addon,
        name: String
    ): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(namespace, name),
            "",
            NovaItem(),
            isHidden = true
        ))
    }
    
    //<editor-fold desc="deprecated", defaultstate="collapsed">
    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Builder functions should be used")
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
            addon, name, options, tileEntityConstructor, NovaItem(TileEntityItemBehavior()), 64,
            if (isInteractive) TileEntityBlock.INTERACTIVE else TileEntityBlock.NON_INTERACTIVE,
            properties, placeCheck, multiBlockLoader
        )
    }
    
    @Deprecated("Builder functions should be used")
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
            NamespacedId(namespace, name), "block.$namespace.$name", item, maxStackSize, null, false,
            block, options, tileEntityConstructor, properties, placeCheck, multiBlockLoader
        )
        return register(material)
    }
    
    @Deprecated("Builder functions should be used")
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
            NamespacedId(namespace, name), "block.$namespace.$name", item, maxStackSize, null, false,
            block, options, properties, placeCheck, multiBlockLoader
        )
        return register(material)
    }
    
    @Deprecated("Misleading name", ReplaceWith("registerItem(addon, name, *itemBehaviors)", "xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem"))
    fun registerDefaultItem(addon: Addon, name: String, vararg itemBehaviors: ItemBehaviorHolder<*>): ItemNovaMaterial =
        registerItem(addon, name, *itemBehaviors)
    
    @Deprecated("Misleading name, Item behavior holders should be specified directly", ReplaceWith("registerItem(addon, name)", "xyz.xenondevs.nova.material.NovaMaterialRegistry.registerItem"))
    fun registerDefaultItem(addon: Addon, name: String, novaItem: NovaItem = NovaItem(), maxStackSize: Int = 64): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(addon.description.id, name),
            "item.$namespace.$name",
            novaItem,
            maxStackSize
        ))
    }
    
    @Deprecated("Item behavior holders should be specified directly", ReplaceWith("registerDefaultItem(addon, name, Consumable(options))", "xyz.xenondevs.nova.item.behavior.Consumable"))
    fun registerFood(addon: Addon, name: String, options: FoodOptions): ItemNovaMaterial {
        val namespace = addon.description.id
        return register(ItemNovaMaterial(
            NamespacedId(namespace, name),
            "item.$namespace.$name",
            NovaItem(Consumable(options))
        ))
    }
    //</editor-fold>
    
    //<editor-fold desc="internal", defaultstate="collapsed">
    internal fun registerCoreItem(
        name: String,
        vararg itemBehaviors: ItemBehaviorHolder<*>,
        localizedName: String = "item.nova.$name",
        isHidden: Boolean = false
    ): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId("nova", name),
            localizedName,
            NovaItem(*itemBehaviors),
            isHidden = isHidden
        ))
    }
    
    internal fun registerUnnamedHiddenCoreItem(name: String, localizedName: String = "", vararg itemBehaviors: ItemBehaviorHolder<*>): ItemNovaMaterial {
        return register(ItemNovaMaterial(
            NamespacedId("nova", name),
            localizedName,
            NovaItem(*itemBehaviors),
            isHidden = true
        ))
    }
    //</editor-fold>
    
    internal fun <T : ItemNovaMaterial> register(material: T): T {
        val id = material.id
        val idStr = material.id.toString()
        require(idStr !in materialsById) { "Duplicate NovaMaterial id: $id" }
        
        materialsById[idStr] = material
        materialsByName.getOrPut(id.name) { ArrayList() } += material
        
        return material
    }
    
}