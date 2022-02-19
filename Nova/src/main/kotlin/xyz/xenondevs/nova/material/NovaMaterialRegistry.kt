package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.Material.*
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.impl.JetpackItem
import xyz.xenondevs.nova.tileentity.impl.energy.*
import xyz.xenondevs.nova.tileentity.impl.storage.InfiniteWaterSource
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.util.addNamespace
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.api.material.NovaMaterialRegistry as INovaMaterialRegistry

@Suppress("unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
object NovaMaterialRegistry : INovaMaterialRegistry {
    
    private val materialsById = HashMap<String, NovaMaterial>()
    private val materialsByName = HashMap<String, ArrayList<NovaMaterial>>()
    
    val values: Collection<NovaMaterial>
        get() = materialsById.values
    
    val sortedValues: Set<NovaMaterial> by lazy { materialsById.values.toSortedSet() }
    
    // Blocks
    val SOLAR_PANEL = registerEnergyTileEntity("SOLAR_PANEL", ::SolarPanel, BARRIER)
    val LIGHTNING_EXCHANGER = registerEnergyTileEntity("LIGHTNING_EXCHANGER", ::LightningExchanger, BARRIER)
    val INFINITE_WATER_SOURCE = registerDefaultTileEntity("INFINITE_WATER_SOURCE", ::InfiniteWaterSource, SANDSTONE)
    
    // Upgrades and similar
    val WRENCH = registerDefaultItem("WRENCH")
    
    // Equipment, Attachments
    val JETPACK = registerItem("JETPACK", "item.nova.jetpack", JetpackItem)
    
    // MultiModel Blocks
    // Reserved for legacy cables
    val WIND_TURBINE = registerTileEntity("WIND_TURBINE", "block.nova.wind_turbine", listOf(EnergyHolder::modifyItemBuilder), BARRIER, ::WindTurbine, WindTurbine::canPlace)
    val FURNACE_GENERATOR = registerTileEntity("FURNACE_GENERATOR", "block.nova.furnace_generator", listOf(EnergyHolder::modifyItemBuilder), COBBLESTONE, ::FurnaceGenerator)
    val LAVA_GENERATOR = registerTileEntity("LAVA_GENERATOR", "block.nova.lava_generator", listOf(EnergyHolder::modifyItemBuilder, NovaFluidHolder::modifyItemBuilder), COBBLESTONE, ::LavaGenerator)
    
    // UI Elements
    val GRAY_BUTTON = registerItem("GRAY_BUTTON", "")
    val ORANGE_BUTTON = registerItem("ORANGE_BUTTON", "")
    val BLUE_BUTTON = registerItem("BLUE_BUTTON", "")
    val GREEN_BUTTON = registerItem("GREEN_BUTTON", "")
    val WHITE_BUTTON = registerItem("WHITE_BUTTON", "")
    val RED_BUTTON = registerItem("RED_BUTTON", "")
    val YELLOW_BUTTON = registerItem("YELLOW_BUTTON", "")
    val PINK_BUTTON = registerItem("PINK_BUTTON", "")
    val SIDE_CONFIG_BUTTON = registerItem("SIDE_CONFIG_BUTTON", "")
    val PLATE_ON_BUTTON = registerItem("PLATE_ON_BUTTON", "")
    val PLATE_OFF_BUTTON = registerItem("PLATE_OFF_BUTTON", "")
    val GEAR_ON_BUTTON = registerItem("GEAR_ON_BUTTON", "")
    val GEAR_OFF_BUTTON = registerItem("GEAR_OFF_BUTTON", "")
    val WHITELIST_BUTTON = registerItem("WHITELIST_BUTTON", "")
    val BLACKLIST_BUTTON = registerItem("BLACKLIST_BUTTON", "")
    val PLUS_ON_BUTTON = registerItem("PLUS_ON_BUTTON", "")
    val PLUS_OFF_BUTTON = registerItem("PLUS_OFF_BUTTON", "")
    val MINUS_ON_BUTTON = registerItem("MINUS_ON_BUTTON", "")
    val MINUS_OFF_BUTTON = registerItem("MINUS_OFF_BUTTON", "")
    val AREA_ON_BUTTON = registerItem("AREA_ON_BUTTON", "")
    val AREA_OFF_BUTTON = registerItem("AREA_OFF_BUTTON", "")
    val NBT_ON_BUTTON = registerItem("NBT_ON_BUTTON", "")
    val NBT_OFF_BUTTON = registerItem("NBT_OFF_BUTTON", "")
    val HOE_ON_BUTTON = registerItem("HOE_ON_BUTTON", "")
    val HOE_OFF_BUTTON = registerItem("HOE_OFF_BUTTON", "")
    val ARROW_LEFT_ON_BUTTON = registerItem("ARROW_LEFT_ON_BUTTON", "")
    val ARROW_LEFT_OFF_BUTTON = registerItem("ARROW_LEFT_OFF_BUTTON", "")
    val ARROW_RIGHT_ON_BUTTON = registerItem("ARROW_RIGHT_ON_BUTTON", "")
    val ARROW_RIGHT_OFF_BUTTON = registerItem("ARROW_RIGHT_OFF_BUTTON", "")
    val ENERGY_ON_BUTTON = registerItem("ENERGY_ON_BUTTON", "menu.nova.side_config.energy")
    val ENERGY_OFF_BUTTON = registerItem("ENERGY_OFF_BUTTON", "")
    val ENERGY_SELECTED_BUTTON = registerItem("ENERGY_SELECTED_BUTTON", "menu.nova.side_config.energy")
    val ITEM_ON_BUTTON = registerItem("ITEM_ON_BUTTON", "menu.nova.side_config.items")
    val ITEM_OFF_BUTTON = registerItem("ITEM_OFF_BUTTON", "")
    val ITEM_SELECTED_BUTTON = registerItem("ITEM_SELECTED_BUTTON", "menu.nova.side_config.items")
    val FLUID_ON_BUTTON = registerItem("FLUID_ON_BUTTON", "menu.nova.side_config.fluids")
    val FLUID_OFF_BUTTON = registerItem("FLUID_OFF_BUTTON", "")
    val FLUID_SELECTED_BUTTON = registerItem("FLUID_SELECTED_BUTTON", "menu.nova.side_config.fluids")
    val COBBLESTONE_MODE_BUTTON = registerItem("COBBLESTONE_MODE_BUTTON", "menu.nova.cobblestone_generator.mode.cobblestone")
    val STONE_MODE_BUTTON = registerItem("STONE_MODE_BUTTON", "menu.nova.cobblestone_generator.mode.stone")
    val OBSIDIAN_MODE_BUTTON = registerItem("OBSIDIAN_MODE_BUTTON", "menu.nova.cobblestone_generator.mode.obsidian")
    val FLUID_LEFT_RIGHT_BUTTON = registerItem("FLUID_LEFT_RIGHT_BUTTON", "menu.nova.fluid_infuser.mode.insert")
    val FLUID_RIGHT_LEFT_BUTTON = registerItem("FLUID_RIGHT_LEFT_BUTTON", "menu.nova.fluid_infuser.mode.extract")
    val ICE_MODE_BUTTON = registerItem("ICE_MODE_BUTTON", "menu.nova.freezer.mode.ice")
    val PACKED_ICE_MODE_BUTTON = registerItem("PACKED_ICE_MODE_BUTTON", "menu.nova.freezer.mode.packed_ice")
    val BLUE_ICE_MODE_BUTTON = registerItem("BLUE_ICE_MODE_BUTTON", "menu.nova.freezer.mode.blue_ice")
    val PUMP_PUMP_BUTTON = registerItem("PUMP_PUMP_ICON", "")
    val PUMP_REPLACE_BUTTON = registerItem("PUMP_REPLACE_ICON", "")
    val INVISIBLE_ITEM = registerItem("INVISIBLE", "")
    val STOPWATCH_ICON = registerItem("STOPWATCH_ICON", "")
    val SEARCH_ICON = registerItem("SEARCH_ICON", "")
    val NO_NUMBER = registerItem("NO_NUMBER", "")
    val PLUS_ICON = registerItem("PLUS_ICON", "")
    val MINUS_ICON = registerItem("MINUS_ICON", "")
    val COLOR_PICKER_ICON = registerItem("COLOR_PICKER_ICON", "menu.nova.color_picker")
    val HOE_PLACEHOLDER = registerItem("HOE_PLACEHOLDER", "")
    val AXE_PLACEHOLDER = registerItem("AXE_PLACEHOLDER", "")
    val SHEARS_PLACEHOLDER = registerItem("SHEARS_PLACEHOLDER", "")
    val ITEM_FILTER_PLACEHOLDER = registerItem("ITEM_FILTER_PLACEHOLDER", "")
    val MOB_CATCHER_PLACEHOLDER = registerItem("MOB_CATCHER_PLACEHOLDER", "")
    val FISHING_ROD_PLACEHOLDER = registerItem("FISHING_ROD_PLACEHOLDER", "")
    val SAPLING_PLACEHOLDER = registerItem("SAPLING_PLACEHOLDER", "")
    val TRASH_CAN_PLACEHOLDER = registerItem("TRASH_CAN_PLACEHOLDER", "")
    val BOTTLE_PLACEHOLDER = registerItem("BOTTLE_PLACEHOLDER", "")
    
    // Multi-Texture UI Elements
    val PROGRESS_ARROW = registerItem("PROGRESS_ARROW", "")
    val ENERGY_PROGRESS = registerItem("ENERGY_PROGRESS", "")
    val RED_BAR = registerItem("RED_BAR", "")
    val GREEN_BAR = registerItem("GREEN_BAR", "")
    val BLUE_BAR = registerItem("BLUE_BAR", "")
    val PRESS_PROGRESS = registerItem("PRESS_PROGRESS", "")
    val PULVERIZER_PROGRESS = registerItem("PULVERIZER_PROGRESS", "")
    val ORANGE_BAR = registerItem("ORANGE_BAR", "")
    val FLUID_PROGRESS_LEFT_RIGHT = registerItem("FLUID_PROGRESS_LEFT_RIGHT", "")
    val FLUID_PROGRESS_RIGHT_LEFT = registerItem("FLUID_PROGRESS_RIGHT_LEFT", "")
    val FLUID_PROGRESS_LEFT_RIGHT_TRANSPARENT = registerItem("FLUID_PROGRESS_LEFT_RIGHT_TRANSPARENT", "")
    val FLUID_PROGRESS_RIGHT_LEFT_TRANSPARENT = registerItem("FLUID_PROGRESS_RIGHT_LEFT_TRANSPARENT", "")
    val BLUE_BAR_TRANSPARENT = registerItem("BLUE_BAR_TRANSPARENT", "")
    val ORANGE_BAR_TRANSPARENT = registerItem("ORANGE_BAR_TRANSPARENT", "")
    val BREW_PROGRESS_TRANSPARENT = registerItem("BREW_PROGRESS_TRANSPARENT", "")
    
    // Numbers
    val NUMBER = registerItem("NUMBER", "")
    
    // Fluid Levels
    val TANK_LAVA_LEVELS = registerItem("TANK_LAVA_LEVELS", "")
    val TANK_WATER_LEVELS = registerItem("TANK_WATER_LEVELS", "")
    val COBBLESTONE_GENERATOR_LAVA_LEVELS = registerItem("COBBLESTONE_GENERATOR_LAVA_LEVELS", "")
    val COBBLESTONE_GENERATOR_WATER_LEVELS = registerItem("COBBLESTONE_GENERATOR_WATER_LEVELS", "")
    
    // Tree Miniatures
    val OAK_TREE_MINIATURE = registerItem("OAK_TREE_MINIATURE", "")
    val SPRUCE_TREE_MINIATURE = registerItem("SPRUCE_TREE_MINIATURE", "")
    val BIRCH_TREE_MINIATURE = registerItem("BIRCH_TREE_MINIATURE", "")
    val JUNGLE_TREE_MINIATURE = registerItem("JUNGLE_TREE_MINIATURE", "")
    val ACACIA_TREE_MINIATURE = registerItem("ACACIA_TREE_MINIATURE", "")
    val DARK_OAK_TREE_MINIATURE = registerItem("DARK_OAK_TREE_MINIATURE", "")
    val CRIMSON_TREE_MINIATURE = registerItem("CRIMSON_TREE_MINIATURE", "")
    val WARPED_TREE_MINIATURE = registerItem("WARPED_TREE_MINIATURE", "")
    val GIANT_RED_MUSHROOM_MINIATURE = registerItem("GIANT_RED_MUSHROOM_MINIATURE", "")
    val GIANT_BROWN_MUSHROOM_MINIATURE = registerItem("GIANT_BROWN_MUSHROOM_MINIATURE", "")
    
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
        return registerDefaultTileEntity(
            addon,
            name,
            tileEntityConstructor,
            hitboxType,
            listOf(EnergyHolder::modifyItemBuilder),
            placeCheck,
            isDirectional,
        )
    }
    
    internal fun registerEnergyTileEntity(
        id: String,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true
    ): NovaMaterial {
        return registerDefaultTileEntity(
            id,
            tileEntityConstructor,
            hitboxType,
            listOf(EnergyHolder::modifyItemBuilder),
            placeCheck,
            isDirectional
        )
    }
    
    internal fun registerDefaultTileEntity(
        id: String,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        val namespacedId = id.split(':')
        return registerTileEntity(
            id,
            if (namespacedId.size == 2) "block.${namespacedId[0]}.${namespacedId[1]}" else "block.nova.$id",
            itemBuilderModifiers,
            hitboxType,
            tileEntityConstructor,
            placeCheck,
            isDirectional
        )
    }
    
    fun registerDefaultTileEntity(
        addon: Addon,
        name: String,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
    ): NovaMaterial {
        return registerTileEntity(
            addon,
            name,
            tileEntityConstructor,
            hitboxType,
            itemBuilderModifiers,
            placeCheck,
            isDirectional
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
    
    internal fun registerDefaultItem(id: String, novaItem: NovaItem? = null) =
        registerItem(id, "item.nova.${id.lowercase()}", novaItem)
    
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