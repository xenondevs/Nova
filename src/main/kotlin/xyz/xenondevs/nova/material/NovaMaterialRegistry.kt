package xyz.xenondevs.nova.material

import org.bukkit.Material
import org.bukkit.Material.*
import xyz.xenondevs.nova.item.NovaItem
import xyz.xenondevs.nova.item.impl.BottledMobItem
import xyz.xenondevs.nova.item.impl.FilterItem
import xyz.xenondevs.nova.item.impl.JetpackItem
import xyz.xenondevs.nova.tileentity.EnergyTileEntity
import xyz.xenondevs.nova.tileentity.impl.agriculture.AutoFisher
import xyz.xenondevs.nova.tileentity.impl.agriculture.Fertilizer
import xyz.xenondevs.nova.tileentity.impl.agriculture.Harvester
import xyz.xenondevs.nova.tileentity.impl.agriculture.Planter
import xyz.xenondevs.nova.tileentity.impl.energy.*
import xyz.xenondevs.nova.tileentity.impl.mob.Breeder
import xyz.xenondevs.nova.tileentity.impl.mob.MobDuplicator
import xyz.xenondevs.nova.tileentity.impl.mob.MobKiller
import xyz.xenondevs.nova.tileentity.impl.processing.ElectricalFurnace
import xyz.xenondevs.nova.tileentity.impl.processing.MechanicalPress
import xyz.xenondevs.nova.tileentity.impl.processing.Pulverizer
import xyz.xenondevs.nova.tileentity.impl.storage.StorageUnit
import xyz.xenondevs.nova.tileentity.impl.storage.VacuumChest
import xyz.xenondevs.nova.tileentity.impl.world.BlockBreaker
import xyz.xenondevs.nova.tileentity.impl.world.BlockPlacer
import xyz.xenondevs.nova.tileentity.impl.world.ChunkLoader
import xyz.xenondevs.nova.tileentity.impl.world.Quarry
import xyz.xenondevs.nova.util.toIntArray

private fun blockOf(data: IntArray) = ModelData(BARRIER, data)
private fun blockOf(data: Int) = ModelData(BARRIER, intArrayOf(data))
private fun structureBlockOf(data: IntArray) = ModelData(STRUCTURE_VOID, data)
private fun structureBlockOf(data: Int) = ModelData(STRUCTURE_VOID, intArrayOf(data))
private fun itemOf(data: IntArray) = ModelData(SHULKER_SHELL, data)
private fun itemOf(data: Int) = ModelData(SHULKER_SHELL, intArrayOf(data))

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
    val MECHANICAL_PRESS = registerDefaultTileEntity("MECHANICAL_PRESS", "block.nova.mechanical_press", 2, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::MechanicalPress)
    val BASIC_POWER_CELL = registerDefaultTileEntity("BASIC_POWER_CELL", "block.nova.basic_power_cell", 3, EnergyTileEntity::createItemBuilder, IRON_BLOCK, ::BasicPowerCell)
    val ADVANCED_POWER_CELL = registerDefaultTileEntity("ADVANCED_POWER_CELL", "block.nova.advanced_power_cell", 4, EnergyTileEntity::createItemBuilder, IRON_BLOCK, ::AdvancedPowerCell)
    val ELITE_POWER_CELL = registerDefaultTileEntity("ELITE_POWER_CELL", "block.nova.elite_power_cell", 5, EnergyTileEntity::createItemBuilder, IRON_BLOCK, ::ElitePowerCell)
    val ULTIMATE_POWER_CELL = registerDefaultTileEntity("ULTIMATE_POWER_CELL", "block.nova.ultimate_power_cell", 6, EnergyTileEntity::createItemBuilder, IRON_BLOCK, ::UltimatePowerCell)
    val CREATIVE_POWER_CELL = registerDefaultTileEntity("CREATIVE_POWER_CELL", "block.nova.creative_power_cell", 7, EnergyTileEntity::createItemBuilder, IRON_BLOCK, ::CreativePowerCell)
    val PULVERIZER = registerDefaultTileEntity("PULVERIZER", "block.nova.pulverizer", 8, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::Pulverizer)
    val SOLAR_PANEL = registerDefaultTileEntity("SOLAR_PANEL", "block.nova.solar_panel", 9, EnergyTileEntity::createItemBuilder, BARRIER, ::SolarPanel)
    val QUARRY = registerDefaultTileEntity("QUARRY", "block.nova.quarry", 10, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::Quarry, Quarry::canPlace)
    // 11: Reserved for legacy electrical furnace
    val CHUNK_LOADER = registerDefaultTileEntity("CHUNK_LOADER", "block.nova.chunk_loader", 12, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::ChunkLoader)
    val BLOCK_BREAKER = registerDefaultTileEntity("BLOCK_BREAKER", "block.nova.block_breaker", 13, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::BlockBreaker)
    val BLOCK_PLACER = registerDefaultTileEntity("BLOCK_PLACER", "block.nova.block_placer", 14, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::BlockPlacer)
    val STORAGE_UNIT = registerDefaultTileEntity("STORAGE_UNIT", "block.nova.storage_unit", 15, null, BARRIER, ::StorageUnit)
    val CHARGER = registerDefaultTileEntity("CHARGER", "block.nova.charger", 16, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::Charger)
    val MOB_KILLER = registerDefaultTileEntity("MOB_KILLER", "block.nova.mob_killer", 17, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::MobKiller)
    val VACUUM_CHEST = registerDefaultTileEntity("VACUUM_CHEST", "block.nova.vacuum_chest", 18, null, BARRIER, ::VacuumChest)
    val BREEDER = registerDefaultTileEntity("BREEDER", "block.nova.breeder", 19, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::Breeder)
    val MOB_DUPLICATOR = registerDefaultTileEntity("MOB_DUPLICATOR", "block.nova.mob_duplicator", 20, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::MobDuplicator)
    val PLANTER = registerDefaultTileEntity("PLANTER", "block.nova.planter", 21, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::Planter)
    val HARVESTER = registerDefaultTileEntity("HARVESTER", "block.nova.harvester", 22, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::Harvester)
    val FERTILIZER = registerDefaultTileEntity("FERTILIZER", "block.nova.fertilizer", 23, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::Fertilizer)
    val WIRELESS_CHARGER = registerDefaultTileEntity("WIRELESS_CHARGER", "block.nova.wireless_charger", 24, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::WirelessCharger)
    val AUTO_FISHER = registerDefaultTileEntity("AUTO_FISHER", "block.nova.auto_fisher", 25, EnergyTileEntity::createItemBuilder, COBBLESTONE, ::AutoFisher)
    val LIGHTNING_EXCHANGER = registerDefaultTileEntity("LIGHTNING_EXCHANGER", "block.nova.lightning_exchanger", 26, EnergyTileEntity::createItemBuilder, BARRIER, ::LightningExchanger)
    
    // 1000 - 2000: Crafting Items
    // Plates
    val IRON_PLATE = registerItem("IRON_PLATE", "item.nova.iron_plate", 1000)
    val GOLD_PLATE = registerItem("GOLD_PLATE", "item.nova.gold_plate", 1001)
    val DIAMOND_PLATE = registerItem("DIAMOND_PLATE", "item.nova.diamond_plate", 1002)
    val NETHERITE_PLATE = registerItem("NETHERITE_PLATE", "item.nova.netherite_plate", 1003)
    val EMERALD_PLATE = registerItem("EMERALD_PLATE", "item.nova.emerald_plate", 1004)
    val REDSTONE_PLATE = registerItem("REDSTONE_PLATE", "item.nova.redstone_plate", 1005)
    val LAPIS_PLATE = registerItem("LAPIS_PLATE", "item.nova.lapis_plate", 1006)
    val COPPER_PLATE = registerItem("COPPER_PLATE", "item.nova.copper_plate", 1007)
    
    // Gears
    val IRON_GEAR = registerItem("IRON_GEAR", "item.nova.iron_gear", 1010)
    val GOLD_GEAR = registerItem("GOLD_GEAR", "item.nova.gold_gear", 1011)
    val DIAMOND_GEAR = registerItem("DIAMOND_GEAR", "item.nova.diamond_gear", 1012)
    val NETHERITE_GEAR = registerItem("NETHERITE_GEAR", "item.nova.netherite_gear", 1013)
    val EMERALD_GEAR = registerItem("EMERALD_GEAR", "item.nova.emerald_gear", 1014)
    val REDSTONE_GEAR = registerItem("REDSTONE_GEAR", "item.nova.redstone_gear", 1015)
    val LAPIS_GEAR = registerItem("LAPIS_GEAR", "item.nova.lapis_gear", 1016)
    val COPPER_GEAR = registerItem("COPPER_GEAR", "item.nova.copper_gear", 1017)
    
    // Dust
    val IRON_DUST = registerItem("IRON_DUST", "item.nova.iron_dust", 1020)
    val GOLD_DUST = registerItem("GOLD_DUST", "item.nova.gold_dust", 1021)
    val DIAMOND_DUST = registerItem("DIAMOND_DUST", "item.nova.diamond_dust", 1022)
    val NETHERITE_DUST = registerItem("NETHERITE_DUST", "item.nova.netherite_dust", 1023)
    val EMERALD_DUST = registerItem("EMERALD_DUST", "item.nova.emerald_dust", 1024)
    val LAPIS_DUST = registerItem("LAPIS_DUST", "item.nova.lapis_dust", 1025)
    val COAL_DUST = registerItem("COAL_DUST", "item.nova.coal_dust", 1026)
    val COPPER_DUST = registerItem("COPPER_DUST", "item.nova.copper_dust", 1027)
    val STAR_DUST = registerItem("STAR_DUST", "item.nova.star_dust", 1028)
    
    // Other
    val NETHERITE_DRILL = registerItem("NETHERITE_DRILL", "item.nova.netherite_drill", 1030)
    val SOLAR_CELL = registerItem("SOLAR_CELL", "item.nova.solar_cell", 1031)
    val BOTTLED_MOB = registerItem("BOTTLED_MOB", "item.nova.bottled_mob", 1032, BottledMobItem)
    val STAR_SHARDS = registerItem("STAR_SHARDS", "item.nova.star_shards", 1033)
    val BASIC_MACHINE_FRAME = registerItem("BASIC_MACHINE_FRAME", "block.nova.basic_machine_frame", 1034)
    val ADVANCED_MACHINE_FRAME = registerItem("ADVANCED_MACHINE_FRAME", "block.nova.advanced_machine_frame", 1035)
    val ELITE_MACHINE_FRAME = registerItem("ELITE_MACHINE_FRAME", "block.nova.elite_machine_frame", 1036)
    val ULTIMATE_MACHINE_FRAME = registerItem("ULTIMATE_MACHINE_FRAME", "block.nova.ultimate_machine_frame", 1037)
    val CREATIVE_MACHINE_FRAME = registerItem("CREATIVE_MACHINE_FRAME", "block.nova.creative_machine_frame", 1038)
    
    // 2000 - 3000: Upgrades and similar
    val WRENCH = registerItem("WRENCH", "item.nova.wrench", 2000)
    val ITEM_FILTER = registerItem("ITEM_FILTER", "item.nova.item_filter", 2001, FilterItem)
    val SPEED_UPGRADE = registerItem("SPEED_UPGRADE", "item.nova.speed_upgrade", 2002)
    val EFFICIENCY_UPGRADE = registerItem("EFFICIENCY_UPGRADE", "item.nova.efficiency_upgrade", 2003)
    val ENERGY_UPGRADE = registerItem("ENERGY_UPGRADE", "item.nova.energy_upgrade", 2004)
    val RANGE_UPGRADE = registerItem("RANGE_UPGRADE", "item.nova.range_upgrade", 2005)
    
    // 3000 - 4000: Equipment, Attachments
    val JETPACK = registerItem("JETPACK", "item.nova.jetpack", ModelData(IRON_CHESTPLATE, intArrayOf(3000)), JetpackItem)
    
    // 5000 - 9.000 MultiModel Blocks
    // 5000 - 5100: Reserved for legacy cables
    val BASIC_CABLE = registerTileEntity("BASIC_CABLE", "block.nova.basic_cable", structureBlockOf(5100), null, structureBlockOf((5101..5164).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::BasicCable, isDirectional = false, legacyItemIds = intArrayOf(5004))
    val ADVANCED_CABLE = registerTileEntity("ADVANCED_CABLE", "block.nova.advanced_cable", structureBlockOf(5165), null, structureBlockOf((5166..5229).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::AdvancedCable, isDirectional = false, legacyItemIds = intArrayOf(5009))
    val ELITE_CABLE = registerTileEntity("ELITE_CABLE", "block.nova.elite_cable", structureBlockOf(5230), null, structureBlockOf((5231..5294).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::EliteCable, isDirectional = false, legacyItemIds = intArrayOf(5014))
    val ULTIMATE_CABLE = registerTileEntity("ULTIMATE_CABLE", "block.nova.ultimate_cable", structureBlockOf(5295), null, structureBlockOf((5296..5359).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::UltimateCable, isDirectional = false, legacyItemIds = intArrayOf(5019))
    val CREATIVE_CABLE = registerTileEntity("CREATIVE_CABLE", "block.nova.creative_cable", structureBlockOf(5360), null, structureBlockOf((5361..5424).toIntArray() + (5025..5033).toIntArray()), CHAIN, ::CreativeCable, isDirectional = false, legacyItemIds = intArrayOf(5024))
    val SCAFFOLDING = register(NovaMaterial("SCAFFOLDING", "item.nova.scaffolding", itemOf(5040), null, null, itemOf((5041..5046).toIntArray())))
    val WIND_TURBINE = registerTileEntity("WIND_TURBINE", "block.nova.wind_turbine", blockOf(5050), EnergyTileEntity::createItemBuilder, blockOf((5051..5054).toIntArray()), BARRIER, ::WindTurbine, WindTurbine::canPlace)
    val FURNACE_GENERATOR = registerTileEntity("FURNACE_GENERATOR", "block.nova.furnace_generator", blockOf(5060), EnergyTileEntity::createItemBuilder, blockOf(intArrayOf(5060, 5061)), COBBLESTONE, ::FurnaceGenerator, null, true, intArrayOf(1))
    val ELECTRICAL_FURNACE = registerTileEntity("ELECTRICAL_FURNACE", "block.nova.electrical_furnace", blockOf(5070), EnergyTileEntity::createItemBuilder, blockOf(intArrayOf(5070, 5071)), COBBLESTONE, ::ElectricalFurnace, null, true, intArrayOf(11))
    
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
    val ENERGY_ON_BUTTON = registerItem("ENERGY_ON_BUTTON", "", 9105)
    val ENERGY_OFF_BUTTON = registerItem("ENERGY_OFF_BUTTON", "", 9106)
    val ITEM_ON_BUTTON = registerItem("ITEM_ON_BUTTON", "", 9107)
    val ITEM_OFF_BUTTON = registerItem("ITEM_OFF_BUTTON", "", 9108)
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
    val INVISIBLE_ITEM = registerItem("INVISIBLE", "", 9399)
    val STOPWATCH_ICON = registerItem("STOPWATCH_ICON", "", 9400)
    val SEARCH_ICON = registerItem("SEARCH_ICON", "", 9401)
    val SPEED_UPGRADE_ICON = registerItem("SPEED_UPGRADE_ICON", "", 9402)
    val EFFICIENCY_UPGRADE_ICON = registerItem("EFFICIENCY_UPGRADE_ICON", "", 9403)
    val ENERGY_UPGRADE_ICON = registerItem("ENERGY_UPGRADE_ICON", "", 9404)
    val RANGE_UPGRADE_ICON = registerItem("RANGE_UPGRADE_ICON", "", 9405)
    val HOE_PLACEHOLDER = registerItem("HOE_PLACEHOLDER", "", 9500)
    val AXE_PLACEHOLDER = registerItem("AXE_PLACEHOLDER", "", 9501)
    val SHEARS_PLACEHOLDER = registerItem("SHEARS_PLACEHOLDER", "", 9502)
    val ITEM_FILTER_PLACEHOLDER = registerItem("ITEM_FILTER_PLACEHOLDER", "", 9503)
    val BOTTLED_MOB_PLACEHOLDER = registerItem("BOTTLED_MOB_PLACEHOLDER", "", 9504)
    val FISHING_ROD_PLACEHOLDER = registerItem("FISHING_ROD_PLACEHOLDER", "", 9505)
    
    // 10.000 - ? Multi-Texture UI Elements
    val PROGRESS_ARROW = registerItem("PROGRESS_ARROW", "", itemOf((10_000..10_016).toIntArray()))
    val ENERGY_PROGRESS = registerItem("ENERGY_PROGRESS", "", itemOf((10_100..10_116).toIntArray()))
    val RED_BAR = registerItem("RED_BAR", "", itemOf((10_200..10_216).toIntArray()))
    val GREEN_BAR = registerItem("GREEN_BAR", "", itemOf((10_300..10_316).toIntArray()))
    val BLUE_BAR = registerItem("BLUE_BAR", "", itemOf((10_400..10_416).toIntArray()))
    val PRESS_PROGRESS = registerItem("PRESS_PROGRESS", "", itemOf((10_500..10_508).toIntArray()))
    val PULVERIZER_PROGRESS = registerItem("PULVERIZER_PROGRESS", "", itemOf((10_600..10_614).toIntArray()))
    
    // 100.000 - ? Numbers
    val NUMBER = registerItem("NUMBER", "", itemOf((100_000..100_999).toIntArray()))
    
    fun get(typeName: String): NovaMaterial = materialsByTypeName[typeName]!!
    fun get(modelId: Int): NovaMaterial = materialsByModelId[modelId]!!
    fun getOrNull(typeName: String): NovaMaterial? = materialsByTypeName[typeName]
    fun getOrNull(modelId: Int): NovaMaterial? = materialsByModelId[modelId]
    
    fun registerDefaultTileEntity(
        typeName: String,
        name: String,
        id: Int,
        itemBuilderCreator: ItemBuilderCreatorFun?,
        hitboxType: Material,
        tileEntityConstructor: TileEntityConstructor,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        val modelData = blockOf(id)
        return registerTileEntity(typeName, name, modelData, itemBuilderCreator, modelData,
            hitboxType, tileEntityConstructor, placeCheck, isDirectional, legacyItemIds)
    }
    
    fun registerTileEntity(
        typeName: String,
        name: String,
        item: ModelData,
        itemBuilderCreator: ItemBuilderCreatorFun?,
        block: ModelData,
        hitboxType: Material,
        tileEntityConstructor: TileEntityConstructor,
        placeCheck: PlaceCheckFun? = null,
        isDirectional: Boolean = true,
        legacyItemIds: IntArray? = null,
    ): NovaMaterial {
        require(item.dataArray.size == 1) { "Item ModelData of $typeName cannot be bigger than 1 (is ${item.dataArray.size})" }
        
        val material = NovaMaterial(typeName, name, item, null, itemBuilderCreator, block,
            hitboxType, tileEntityConstructor, placeCheck, isDirectional, legacyItemIds)
        
        return register(material)
    }
    
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