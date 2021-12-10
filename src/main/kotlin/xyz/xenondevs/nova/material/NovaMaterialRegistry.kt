package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.Material.*
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.impl.FilterItem
import xyz.xenondevs.nova.item.impl.JetpackItem
import xyz.xenondevs.nova.item.impl.MobCatcherItem
import xyz.xenondevs.nova.tileentity.impl.agriculture.*
import xyz.xenondevs.nova.tileentity.impl.energy.*
import xyz.xenondevs.nova.tileentity.impl.fluid.*
import xyz.xenondevs.nova.tileentity.impl.mob.Breeder
import xyz.xenondevs.nova.tileentity.impl.mob.MobDuplicator
import xyz.xenondevs.nova.tileentity.impl.mob.MobKiller
import xyz.xenondevs.nova.tileentity.impl.processing.ElectricalFurnace
import xyz.xenondevs.nova.tileentity.impl.processing.MechanicalPress
import xyz.xenondevs.nova.tileentity.impl.processing.Pulverizer
import xyz.xenondevs.nova.tileentity.impl.storage.StorageUnit
import xyz.xenondevs.nova.tileentity.impl.storage.TrashCan
import xyz.xenondevs.nova.tileentity.impl.storage.VacuumChest
import xyz.xenondevs.nova.tileentity.impl.world.*
import xyz.xenondevs.nova.tileentity.network.energy.holder.EnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.util.toIntArray

private fun blockOf(data: IntArray) = ModelData(BARRIER, data)
private fun blockOf(data: Int) = ModelData(BARRIER, intArrayOf(data))
private fun structureBlockOf(data: IntArray) = ModelData(STRUCTURE_VOID, data)
private fun structureBlockOf(data: Int) = ModelData(STRUCTURE_VOID, intArrayOf(data))
private fun itemOf(data: IntArray) = ModelData(SHULKER_SHELL, data)
private fun itemOf(data: Int) = ModelData(SHULKER_SHELL, intArrayOf(data))

// Refers to models on the "complete" branch of the resource pack that slow down loading times
private fun bulkItemOf(data: Int) = ModelData(NAUTILUS_SHELL, intArrayOf(data))
private fun bulkItemOf(data: IntArray) = ModelData(NAUTILUS_SHELL, data)

@Suppress("unused", "MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
object NovaMaterialRegistry {
    
    private val materialsByModelId = HashMap<Int, NovaMaterial>()
    private val materialsByTypeName = HashMap<String, NovaMaterial>()
    
    val values: Collection<NovaMaterial>
        get() = materialsByTypeName.values
    
    val sortedValues: Set<NovaMaterial> by lazy { materialsByTypeName.values.toSortedSet() }
    val sortedObtainables: Set<NovaMaterial> by lazy { sortedValues.filterTo(LinkedHashSet()) { it.item.data < 9000 } }
    
    // 1 - 1000: Blocks
    // 1: Reserved for legacy furnace generator
    val MECHANICAL_PRESS = registerEnergyTileEntity("MECHANICAL_PRESS", 2, ::MechanicalPress, COBBLESTONE)
    val BASIC_POWER_CELL = registerEnergyTileEntity("BASIC_POWER_CELL", 3, ::BasicPowerCell, IRON_BLOCK)
    val ADVANCED_POWER_CELL = registerEnergyTileEntity("ADVANCED_POWER_CELL", 4, ::AdvancedPowerCell, IRON_BLOCK)
    val ELITE_POWER_CELL = registerEnergyTileEntity("ELITE_POWER_CELL", 5, ::ElitePowerCell, IRON_BLOCK)
    val ULTIMATE_POWER_CELL = registerEnergyTileEntity("ULTIMATE_POWER_CELL", 6, ::UltimatePowerCell, IRON_BLOCK)
    val CREATIVE_POWER_CELL = registerEnergyTileEntity("CREATIVE_POWER_CELL", 7, ::CreativePowerCell, IRON_BLOCK)
    val PULVERIZER = registerEnergyTileEntity("PULVERIZER", 8, ::Pulverizer, COBBLESTONE)
    val SOLAR_PANEL = registerEnergyTileEntity("SOLAR_PANEL", 9, ::SolarPanel, BARRIER)
    val QUARRY = registerEnergyTileEntity("QUARRY", 10, ::Quarry, COBBLESTONE, Quarry::canPlace)
    // 11: Reserved for legacy electrical furnace
    val CHUNK_LOADER = registerEnergyTileEntity("CHUNK_LOADER", 12, ::ChunkLoader, COBBLESTONE)
    val BLOCK_BREAKER = registerEnergyTileEntity("BLOCK_BREAKER", 13, ::BlockBreaker, COBBLESTONE)
    val BLOCK_PLACER = registerEnergyTileEntity("BLOCK_PLACER", 14, ::BlockPlacer, COBBLESTONE)
    val STORAGE_UNIT = registerDefaultTileEntity("STORAGE_UNIT", 15, ::StorageUnit, BARRIER)
    val CHARGER = registerEnergyTileEntity("CHARGER", 16, ::Charger, COBBLESTONE)
    val MOB_KILLER = registerEnergyTileEntity("MOB_KILLER", 17, ::MobKiller, COBBLESTONE)
    val VACUUM_CHEST = registerDefaultTileEntity("VACUUM_CHEST", 18, ::VacuumChest, BARRIER)
    val BREEDER = registerEnergyTileEntity("BREEDER", 19, ::Breeder, COBBLESTONE)
    val MOB_DUPLICATOR = registerEnergyTileEntity("MOB_DUPLICATOR", 20, ::MobDuplicator, COBBLESTONE)
    val PLANTER = registerEnergyTileEntity("PLANTER", 21, ::Planter, COBBLESTONE)
    val HARVESTER = registerEnergyTileEntity("HARVESTER", 22, ::Harvester, COBBLESTONE)
    val FERTILIZER = registerEnergyTileEntity("FERTILIZER", 23, ::Fertilizer, COBBLESTONE)
    val WIRELESS_CHARGER = registerEnergyTileEntity("WIRELESS_CHARGER", 24, ::WirelessCharger, COBBLESTONE)
    val AUTO_FISHER = registerEnergyTileEntity("AUTO_FISHER", 25, ::AutoFisher, COBBLESTONE)
    val LIGHTNING_EXCHANGER = registerEnergyTileEntity("LIGHTNING_EXCHANGER", 26, ::LightningExchanger, BARRIER)
    val TREE_FACTORY = registerEnergyTileEntity("TREE_FACTORY", 27, ::TreeFactory, BARRIER)
    val TRASH_CAN = registerDefaultTileEntity("TRASH_CAN", 28, ::TrashCan, BARRIER)
    val BASIC_FLUID_TANK = registerDefaultTileEntity("BASIC_FLUID_TANK", 29, ::BasicFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val ADVANCED_FLUID_TANK = registerDefaultTileEntity("ADVANCED_FLUID_TANK", 30, ::AdvancedFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val ELITE_FLUID_TANK = registerDefaultTileEntity("ELITE_FLUID_TANK", 31, ::EliteFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val ULTIMATE_FLUID_TANK = registerDefaultTileEntity("ULTIMATE_FLUID_TANK", 32, ::UltimateFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    val CREATIVE_FLUID_TANK = registerDefaultTileEntity("CREATIVE_FLUID_TANK", 33, ::CreativeFluidTank, BARRIER, listOf(NovaFluidHolder::modifyItemBuilder))
    
    // 1000 - 2000: Crafting Items
    // Plates
    val IRON_PLATE = registerDefaultItem("IRON_PLATE", 1000)
    val GOLD_PLATE = registerDefaultItem("GOLD_PLATE", 1001)
    val DIAMOND_PLATE = registerDefaultItem("DIAMOND_PLATE", 1002)
    val NETHERITE_PLATE = registerDefaultItem("NETHERITE_PLATE", 1003)
    val EMERALD_PLATE = registerDefaultItem("EMERALD_PLATE", 1004)
    val REDSTONE_PLATE = registerDefaultItem("REDSTONE_PLATE", 1005)
    val LAPIS_PLATE = registerDefaultItem("LAPIS_PLATE", 1006)
    val COPPER_PLATE = registerDefaultItem("COPPER_PLATE", 1007)
    
    // Gears
    val IRON_GEAR = registerDefaultItem("IRON_GEAR", 1010)
    val GOLD_GEAR = registerDefaultItem("GOLD_GEAR", 1011)
    val DIAMOND_GEAR = registerDefaultItem("DIAMOND_GEAR", 1012)
    val NETHERITE_GEAR = registerDefaultItem("NETHERITE_GEAR", 1013)
    val EMERALD_GEAR = registerDefaultItem("EMERALD_GEAR", 1014)
    val REDSTONE_GEAR = registerDefaultItem("REDSTONE_GEAR", 1015)
    val LAPIS_GEAR = registerDefaultItem("LAPIS_GEAR", 1016)
    val COPPER_GEAR = registerDefaultItem("COPPER_GEAR", 1017)
    
    // Dust
    val IRON_DUST = registerDefaultItem("IRON_DUST", 1020)
    val GOLD_DUST = registerDefaultItem("GOLD_DUST", 1021)
    val DIAMOND_DUST = registerDefaultItem("DIAMOND_DUST", 1022)
    val NETHERITE_DUST = registerDefaultItem("NETHERITE_DUST", 1023)
    val EMERALD_DUST = registerDefaultItem("EMERALD_DUST", 1024)
    val LAPIS_DUST = registerDefaultItem("LAPIS_DUST", 1025)
    val COAL_DUST = registerDefaultItem("COAL_DUST", 1026)
    val COPPER_DUST = registerDefaultItem("COPPER_DUST", 1027)
    val STAR_DUST = registerDefaultItem("STAR_DUST", 1028)
    
    // Other
    val NETHERITE_DRILL = registerDefaultItem("NETHERITE_DRILL", 1030)
    val SOLAR_CELL = registerDefaultItem("SOLAR_CELL", 1031)
    val MOB_CATCHER = registerDefaultItem("MOB_CATCHER", 1032, MobCatcherItem)
    val STAR_SHARDS = registerDefaultItem("STAR_SHARDS", 1033)
    val BASIC_MACHINE_FRAME = registerDefaultItem("BASIC_MACHINE_FRAME", 1034)
    val ADVANCED_MACHINE_FRAME = registerDefaultItem("ADVANCED_MACHINE_FRAME", 1035)
    val ELITE_MACHINE_FRAME = registerDefaultItem("ELITE_MACHINE_FRAME", 1036)
    val ULTIMATE_MACHINE_FRAME = registerDefaultItem("ULTIMATE_MACHINE_FRAME", 1037)
    val CREATIVE_MACHINE_FRAME = registerDefaultItem("CREATIVE_MACHINE_FRAME", 1038)
    
    // 2000 - 3000: Upgrades and similar
    val WRENCH = registerDefaultItem("WRENCH", 2000)
    val ITEM_FILTER = registerDefaultItem("ITEM_FILTER", 2001, FilterItem)
    val SPEED_UPGRADE = registerDefaultItem("SPEED_UPGRADE", 2002)
    val EFFICIENCY_UPGRADE = registerDefaultItem("EFFICIENCY_UPGRADE", 2003)
    val ENERGY_UPGRADE = registerDefaultItem("ENERGY_UPGRADE", 2004)
    val RANGE_UPGRADE = registerDefaultItem("RANGE_UPGRADE", 2005)
    
    // 3000 - 4000: Equipment, Attachments
    val JETPACK = registerItem("JETPACK", "item.nova.jetpack", ModelData(IRON_CHESTPLATE, intArrayOf(3000)), JetpackItem)
    
    // 5000 - 9.000 MultiModel Blocks
    // 5000 - 5100: Reserved for legacy cables
    val BASIC_CABLE = registerTileEntity("BASIC_CABLE", "block.nova.basic_cable", structureBlockOf(5100), null, structureBlockOf((5101..5164).toIntArray() + (5000..5047).toIntArray()), STRUCTURE_VOID, ::BasicCable, isDirectional = false, legacyItemIds = intArrayOf(5004))
    val ADVANCED_CABLE = registerTileEntity("ADVANCED_CABLE", "block.nova.advanced_cable", structureBlockOf(5165), null, structureBlockOf((5166..5229).toIntArray() + (5000..5047).toIntArray()), STRUCTURE_VOID, ::AdvancedCable, isDirectional = false, legacyItemIds = intArrayOf(5009))
    val ELITE_CABLE = registerTileEntity("ELITE_CABLE", "block.nova.elite_cable", structureBlockOf(5230), null, structureBlockOf((5231..5294).toIntArray() + (5000..5047).toIntArray()), STRUCTURE_VOID, ::EliteCable, isDirectional = false, legacyItemIds = intArrayOf(5014))
    val ULTIMATE_CABLE = registerTileEntity("ULTIMATE_CABLE", "block.nova.ultimate_cable", structureBlockOf(5295), null, structureBlockOf((5296..5359).toIntArray() + (5000..5047).toIntArray()), STRUCTURE_VOID, ::UltimateCable, isDirectional = false, legacyItemIds = intArrayOf(5019))
    val CREATIVE_CABLE = registerTileEntity("CREATIVE_CABLE", "block.nova.creative_cable", structureBlockOf(5360), null, structureBlockOf((5361..5424).toIntArray() + (5000..5047).toIntArray()), STRUCTURE_VOID, ::CreativeCable, isDirectional = false, legacyItemIds = intArrayOf(5024))
    val SCAFFOLDING = register(NovaMaterial("SCAFFOLDING", "item.nova.scaffolding", itemOf(5040), null, null, itemOf((5041..5046).toIntArray())))
    val WIND_TURBINE = registerTileEntity("WIND_TURBINE", "block.nova.wind_turbine", blockOf(5050), listOf(EnergyHolder::modifyItemBuilder), blockOf((5051..5054).toIntArray()), BARRIER, ::WindTurbine, WindTurbine::canPlace)
    val FURNACE_GENERATOR = registerTileEntity("FURNACE_GENERATOR", "block.nova.furnace_generator", blockOf(5060), listOf(EnergyHolder::modifyItemBuilder), blockOf(intArrayOf(5060, 5061)), COBBLESTONE, ::FurnaceGenerator, legacyItemIds = intArrayOf(1))
    val ELECTRICAL_FURNACE = registerTileEntity("ELECTRICAL_FURNACE", "block.nova.electrical_furnace", blockOf(5070), listOf(EnergyHolder::modifyItemBuilder), blockOf(intArrayOf(5070, 5071)), COBBLESTONE, ::ElectricalFurnace, legacyItemIds = intArrayOf(11))
    val STAR_COLLECTOR = registerTileEntity("STAR_COLLECTOR", "block.nova.star_collector", blockOf(5080), listOf(EnergyHolder::modifyItemBuilder), blockOf(intArrayOf(5080, 5081, 5082)), BARRIER, ::StarCollector)
    
    // 9.000 - 10.000 UI Elements
    val GRAY_BUTTON = registerItem("GRAY_BUTTON", "", 9001)
    val ORANGE_BUTTON = registerItem("ORANGE_BUTTON", "", 9002)
    val BLUE_BUTTON = registerItem("BLUE_BUTTON", "", 9003)
    val GREEN_BUTTON = registerItem("GREEN_BUTTON", "", 9004)
    val WHITE_BUTTON = registerItem("WHITE_BUTTON", "", 9005)
    val RED_BUTTON = registerItem("RED_BUTTON", "", 9006)
    val YELLOW_BUTTON = registerItem("YELLOW_BUTTON", "", 9007)
    val PINK_BUTTON = registerItem("PINK_BUTTON", "", 9008)
    val SIDE_CONFIG_BUTTON = registerItem("SIDE_CONFIG_BUTTON", "", 9100)
    val PLATE_ON_BUTTON = registerItem("PLATE_ON_BUTTON", "", 9101)
    val PLATE_OFF_BUTTON = registerItem("PLATE_OFF_BUTTON", "", 9102)
    val GEAR_ON_BUTTON = registerItem("GEAR_ON_BUTTON", "", 9103)
    val GEAR_OFF_BUTTON = registerItem("GEAR_OFF_BUTTON", "", 9104)
    val WHITELIST_BUTTON = registerItem("WHITELIST_BUTTON", "", 9109)
    val BLACKLIST_BUTTON = registerItem("BLACKLIST_BUTTON", "", 9110)
    val PLUS_ON_BUTTON = registerItem("PLUS_ON_BUTTON", "", 9111)
    val PLUS_OFF_BUTTON = registerItem("PLUS_OFF_BUTTON", "", 9112)
    val MINUS_ON_BUTTON = registerItem("MINUS_ON_BUTTON", "", 9113)
    val MINUS_OFF_BUTTON = registerItem("MINUS_OFF_BUTTON", "", 9114)
    val AREA_ON_BUTTON = registerItem("AREA_ON_BUTTON", "", 9115)
    val AREA_OFF_BUTTON = registerItem("AREA_OFF_BUTTON", "", 9116)
    val NBT_ON_BUTTON = registerItem("NBT_ON_BUTTON", "", 9117)
    val NBT_OFF_BUTTON = registerItem("NBT_OFF_BUTTON", "", 9118)
    val HOE_ON_BUTTON = registerItem("HOE_ON_BUTTON", "", 9119)
    val HOE_OFF_BUTTON = registerItem("HOE_OFF_BUTTON", "", 9120)
    val UPGRADES_BUTTON = registerItem("UPGRADES_BUTTON", "menu.nova.upgrades", 9121)
    val ARROW_LEFT_ON_BUTTON = registerItem("ARROW_LEFT_ON_BUTTON", "", 9122)
    val ARROW_LEFT_OFF_BUTTON = registerItem("ARROW_LEFT_OFF_BUTTON", "", 9123)
    val ARROW_RIGHT_ON_BUTTON = registerItem("ARROW_RIGHT_ON_BUTTON", "", 9124)
    val ARROW_RIGHT_OFF_BUTTON = registerItem("ARROW_RIGHT_OFF_BUTTON", "", 9125)
    val ENERGY_ON_BUTTON = registerItem("ENERGY_ON_BUTTON", "menu.nova.side_config.energy", 9126)
    val ENERGY_OFF_BUTTON = registerItem("ENERGY_OFF_BUTTON", "", 9127)
    val ENERGY_SELECTED_BUTTON = registerItem("ENERGY_SELECTED_BUTTON", "menu.nova.side_config.energy", 9128)
    val ITEM_ON_BUTTON = registerItem("ITEM_ON_BUTTON", "menu.nova.side_config.items", 9129)
    val ITEM_OFF_BUTTON = registerItem("ITEM_OFF_BUTTON", "", 9130)
    val ITEM_SELECTED_BUTTON = registerItem("ITEM_SELECTED_BUTTON", "menu.nova.side_config.items", 9131)
    val FLUID_ON_BUTTON = registerItem("FLUID_ON_BUTTON", "menu.nova.side_config.fluids", 9132)
    val FLUID_OFF_BUTTON = registerItem("FLUID_OFF_BUTTON", "", 9133)
    val FLUID_SELECTED_BUTTON = registerItem("FLUID_SELECTED_BUTTON", "menu.nova.side_config.fluids", 9134)
    val INVISIBLE_ITEM = registerItem("INVISIBLE", "", 9300)
    val STOPWATCH_ICON = registerItem("STOPWATCH_ICON", "", 9301)
    val SEARCH_ICON = registerItem("SEARCH_ICON", "", 9302)
    val NO_NUMBER = registerItem("NO_NUMBER", "", 9303)
    val SPEED_UPGRADE_ICON = registerItem("SPEED_UPGRADE_ICON", "", 9402)
    val TRANSLUCENT_SPEED_UPGRADE_ICON = registerItem("TRANSLUCENT_SPEED_UPGRADE_ICON", "", 9403)
    val EFFICIENCY_UPGRADE_ICON = registerItem("EFFICIENCY_UPGRADE_ICON", "", 9404)
    val TRANSLUCENT_EFFICIENCY_UPGRADE_ICON = registerItem("TRANSLUCENT_EFFICIENCY_UPGRADE_ICON", "", 9405)
    val ENERGY_UPGRADE_ICON = registerItem("ENERGY_UPGRADE_ICON", "", 9406)
    val TRANSLUCENT_ENERGY_UPGRADE_ICON = registerItem("TRANSLUCENT_ENERGY_UPGRADE_ICON", "", 9407)
    val RANGE_UPGRADE_ICON = registerItem("RANGE_UPGRADE_ICON", "", 9408)
    val TRANSLUCENT_RANGE_UPGRADE_ICON = registerItem("TRANSLUCENT_RANGE_UPGRADE_ICON", "", 9409)
    val HOE_PLACEHOLDER = registerItem("HOE_PLACEHOLDER", "", 9500)
    val AXE_PLACEHOLDER = registerItem("AXE_PLACEHOLDER", "", 9501)
    val SHEARS_PLACEHOLDER = registerItem("SHEARS_PLACEHOLDER", "", 9502)
    val ITEM_FILTER_PLACEHOLDER = registerItem("ITEM_FILTER_PLACEHOLDER", "", 9503)
    val MOB_CATCHER_PLACEHOLDER = registerItem("MOB_CATCHER_PLACEHOLDER", "", 9504)
    val FISHING_ROD_PLACEHOLDER = registerItem("FISHING_ROD_PLACEHOLDER", "", 9505)
    val SAPLING_PLACEHOLDER = registerItem("SAPLING_PLACEHOLDER", "", 9506)
    val TRASH_CAN_PLACEHOLDER = registerItem("TRASH_CAN_PLACEHOLDER", "", 9507)
    
    // 10.000 - ? Multi-Texture UI Elements
    val PROGRESS_ARROW = registerItem("PROGRESS_ARROW", "", itemOf((10_000..10_016).toIntArray()))
    val ENERGY_PROGRESS = registerItem("ENERGY_PROGRESS", "", itemOf((10_100..10_116).toIntArray()))
    val RED_BAR = registerItem("RED_BAR", "", itemOf((10_200..10_216).toIntArray()))
    val GREEN_BAR = registerItem("GREEN_BAR", "", itemOf((10_300..10_316).toIntArray()))
    val BLUE_BAR = registerItem("BLUE_BAR", "", itemOf((10_400..10_416).toIntArray()))
    val PRESS_PROGRESS = registerItem("PRESS_PROGRESS", "", itemOf((10_500..10_508).toIntArray()))
    val PULVERIZER_PROGRESS = registerItem("PULVERIZER_PROGRESS", "", itemOf((10_600..10_614).toIntArray()))
    val ORANGE_BAR = registerItem("ORANGE_BAR", "", itemOf((10_700..10_716).toIntArray()))
    
    // 100.000 - ? Numbers
    val NUMBER = registerItem("NUMBER", "", itemOf((100_000..100_999).toIntArray()))
    
    // 190.000 Fluid Levels
    val LAVA_LEVELS = registerItem("LAVA_LEVELS", "", ModelData(BLUE_STAINED_GLASS, (190_000 until 190_100).toIntArray()))
    val WATER_LEVELS = registerItem("WATER_LEVELS", "", ModelData(BLUE_STAINED_GLASS, (191_000 until 191_100).toIntArray()))
    
    // 200.000 - ? Tree Miniatures
    val OAK_TREE_MINIATURE = registerItem("OAK_TREE_MINIATURE", "", bulkItemOf((200_000 until 200_500).toIntArray()))
    val SPRUCE_TREE_MINIATURE = registerItem("SPRUCE_TREE_MINIATURE", "", bulkItemOf((200_500 until 201_000).toIntArray()))
    val BIRCH_TREE_MINIATURE = registerItem("BIRCH_TREE_MINIATURE", "", bulkItemOf((201_000 until 201_500).toIntArray()))
    val JUNGLE_TREE_MINIATURE = registerItem("JUNGLE_TREE_MINIATURE", "", bulkItemOf((201_500 until 202_000).toIntArray()))
    val ACACIA_TREE_MINIATURE = registerItem("ACACIA_TREE_MINIATURE", "", bulkItemOf((202_000 until 202_500).toIntArray()))
    val DARK_OAK_TREE_MINIATURE = registerItem("DARK_OAK_TREE_MINIATURE", "", bulkItemOf((202_500 until 203_000).toIntArray()))
    val CRIMSON_TREE_MINIATURE = registerItem("CRIMSON_TREE_MINIATURE", "", bulkItemOf((203_000 until 203_500).toIntArray()))
    val WARPED_TREE_MINIATURE = registerItem("WARPED_TREE_MINIATURE", "", bulkItemOf((203_500 until 204_000).toIntArray()))
    val GIANT_RED_MUSHROOM_MINIATURE = registerItem("GIANT_RED_MUSHROOM_MINIATURE", "", bulkItemOf((204_000..204_500).toIntArray()))
    val GIANT_BROWN_MUSHROOM_MINIATURE = registerItem("GIANT_BROWN_MUSHROOM_MINIATURE", "", bulkItemOf((204_500..205_000).toIntArray()))
    
    fun get(typeName: String): NovaMaterial = materialsByTypeName[typeName]!!
    fun get(modelId: Int): NovaMaterial = materialsByModelId[modelId]!!
    fun getOrNull(typeName: String): NovaMaterial? = materialsByTypeName[typeName]
    fun getOrNull(modelId: Int): NovaMaterial? = materialsByModelId[modelId]
    
    fun registerDefaultTileEntity(
        typeName: String,
        id: Int,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        itemBuilderModifiers: List<ItemBuilderModifierFun>? = null,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        val modelData = blockOf(id)
        return registerTileEntity(typeName, "block.nova.${typeName.lowercase()}", modelData, itemBuilderModifiers, modelData,
            hitboxType, tileEntityConstructor, placeCheck, isDirectional, legacyItemIds)
    }
    
    fun registerEnergyTileEntity(
        typeName: String,
        id: Int,
        tileEntityConstructor: TileEntityConstructor?,
        hitboxType: Material,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        return registerDefaultTileEntity(typeName,
            id,
            tileEntityConstructor,
            hitboxType,
            listOf(EnergyHolder::modifyItemBuilder),
            placeCheck,
            isDirectional,
            legacyItemIds)
    }
    
    fun registerTileEntity(
        typeName: String,
        name: String,
        item: ModelData,
        itemBuilderModifiers: List<ItemBuilderModifierFun>?,
        block: ModelData,
        hitboxType: Material,
        tileEntityConstructor: TileEntityConstructor?,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        require(item.dataArray.size == 1) { "Item ModelData of $typeName cannot be bigger than 1 (is ${item.dataArray.size})" }
        
        val material = NovaMaterial(typeName, name, item, null, itemBuilderModifiers, block,
            hitboxType, tileEntityConstructor, placeCheck, isDirectional, legacyItemIds)
        
        return register(material)
    }
    
    fun registerDefaultItem(typeName: String, id: Int, novaItem: NovaItem? = null, legacyItemIds: IntArray? = null) =
        registerItem(typeName, "item.nova.${typeName.lowercase()}", id, novaItem, legacyItemIds)
    
    fun registerItem(typeName: String, name: String, id: Int, novaItem: NovaItem? = null, legacyItemIds: IntArray? = null) =
        registerItem(typeName, name, itemOf(id), novaItem, legacyItemIds)
    
    fun registerItem(typeName: String, name: String, item: ModelData, novaItem: NovaItem? = null, legacyItemIds: IntArray? = null): NovaMaterial {
        require(item.dataArray.isNotEmpty()) { "Item ModelData of $typeName cannot be empty" }
        
        return register(NovaMaterial(typeName, name, item, novaItem))
    }
    
    private fun register(material: NovaMaterial): NovaMaterial {
        val typeName = material.typeName
        require(material.item.dataArray.isNotEmpty()) { "Item ModelData of $typeName cannot be empty" }
        
        val id = material.item.data
        val legacyIds = material.legacyItemIds
        require(!materialsByModelId.containsKey(id) && legacyIds?.none { materialsByModelId.containsKey(it) } ?: true) { "Duplicate id: $id" }
        require(!materialsByTypeName.containsKey(typeName)) { "Duplicate type name: $typeName" }
        
        materialsByModelId[id] = material
        materialsByTypeName[typeName] = material
        
        legacyIds?.forEach {
            materialsByModelId[it] = material
        }
        
        return material
    }
    
}