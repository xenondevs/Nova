package xyz.xenondevs.nova

import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.internal.LazilyParsedNumber
import org.bukkit.Bukkit
import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.BlockFace
import org.bukkit.block.TileState
import org.bukkit.entity.ArmorStand
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.config.PermanentStorage
import xyz.xenondevs.nova.data.serialization.cbf.BackedElement
import xyz.xenondevs.nova.data.serialization.cbf.Element
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.ListElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.toElement
import xyz.xenondevs.nova.data.serialization.persistentdata.CompoundElementDataType
import xyz.xenondevs.nova.data.serialization.persistentdata.JsonElementDataType
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TILE_ENTITY_KEY
import xyz.xenondevs.nova.tileentity.TileEntityManager
import xyz.xenondevs.nova.tileentity.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.tileentity.network.item.ItemConnectionType
import xyz.xenondevs.nova.util.blockLocation
import xyz.xenondevs.nova.util.data.GSON
import xyz.xenondevs.nova.util.data.fromJson
import xyz.xenondevs.nova.util.hasNovaData
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.world.armorstand.AsyncChunkPos
import xyz.xenondevs.nova.world.armorstand.pos
import java.util.*

private fun ArmorStand.getTileEntityData() =
    persistentDataContainer.get(TILE_ENTITY_KEY, JsonElementDataType) as JsonObject

private fun ArmorStand.hasTileEntityData(): Boolean =
    persistentDataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)

private fun TileState.hasLegacyData(): Boolean =
    persistentDataContainer.has(TILE_ENTITY_KEY, JsonElementDataType)

private fun TileState.getLegacyData(): JsonObject =
    persistentDataContainer.get(TILE_ENTITY_KEY, JsonElementDataType) as JsonObject

private fun TileState.setVanillaTileEntityData(compound: CompoundElement) {
    persistentDataContainer.set(TILE_ENTITY_KEY, CompoundElementDataType, compound)
    update()
}

object NovaLegacyDataConverter : Listener {
    
    private val IGNORED_MANUAL_KEYS = hashSetOf("global", "material", "owner", "itemConfig", "inventories", "energyConfig", "bridgeFaces")
    
    private val convertedEntityChunks = PermanentStorage.retrieve("convertedEntityChunks") { HashSet<AsyncChunkPos>() }
    private val convertedTileChunks = PermanentStorage.retrieve("convertedTileChunks") { HashSet<AsyncChunkPos>() }
    
    fun init() {
        Bukkit.getPluginManager().registerEvents(this, NOVA)
        Bukkit.getWorlds().flatMap { it.loadedChunks.asList() }.forEach(::convertChunk)
        
        NOVA.disableHandlers += {
            PermanentStorage.store("convertedEntityChunks", convertedEntityChunks)
            PermanentStorage.store("convertedTileChunks", convertedTileChunks)
        }
    }
    
    @EventHandler(priority = EventPriority.LOWEST) // we want to be called first
    fun handleChunkLoad(event: ChunkLoadEvent) {
        convertChunk(event.chunk)
    }
    
    private fun convertChunk(chunk: Chunk) {
        val chunkPos = chunk.pos
        if (convertedEntityChunks.contains(chunkPos)) return
        val world = chunk.world
        
        // tile entities are not loaded async, we don't need to wait here
        chunk.tileEntities
            .filterIsInstance<TileState>()
            .filter { it.hasLegacyData() }
            .forEach {
                try {
                    convertLegacyVanillaTileEntity(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        
        convertedTileChunks += chunkPos
        
        // async entity loading is still not supported by spigot,
        // so we wait 10s and hope that all entities are loaded by then
        runTaskLater(20 * 10) {
            // do not convert when the chunk has already been converted or is no longer loaded
            if (convertedEntityChunks.contains(chunkPos) || !world.isChunkLoaded(chunkPos.x, chunkPos.z))
                return@runTaskLater
            
            chunk.entities
                .filterIsInstance<ArmorStand>()
                .filter { it.persistentDataContainer.hasNovaData() }
                .forEach {
                    try {
                        convertLegacyArmorStand(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            
            convertedEntityChunks += chunkPos
        }
        
    }
    
    private fun convertLegacyVanillaTileEntity(tile: TileState) {
        println("[NovaLegacyDataConverter] Converting Vanilla-TileEntity at ${tile.location}")
        
        val data = tile.getLegacyData()
        println("[NovaLegacyDataConverter] Converting JSON: $data")
        
        val itemConfig: EnumMap<BlockFace, ItemConnectionType>? = GSON.fromJson(data.get("itemConfig"))
        
        val convertedData = CompoundElement()
        if (itemConfig != null) convertedData.putElement("itemConfig", itemConfig.toElement(ItemConnectionType::class))
        
        println("[NovaLegacyDataConverter] Converted to: $convertedData")
        
        tile.setVanillaTileEntityData(convertedData)
    }
    
    private fun convertLegacyArmorStand(armorStand: ArmorStand) {
        if (armorStand.hasTileEntityData()) {
            println("[NovaLegacyDataConverter] Converting Nova-TileEntity at ${armorStand.location}")
            val tileEntityData = armorStand.getTileEntityData()
            val (material, ownerUUID, convertedData) = convertTileEntityData(tileEntityData)
            
            TileEntityManager.placeTileEntity(
                ownerUUID,
                armorStand.location.blockLocation,
                armorStand.location.yaw - 180f,
                material,
                convertedData,
                armorStand.uniqueId
            )
        }
        
        armorStand.remove()
    }
    
    private fun convertTileEntityData(tileEntityData: JsonObject): Triple<NovaMaterial, UUID, CompoundElement> {
        println("[NovaLegacyDataConverter] Converting JSON: $tileEntityData")
        val material: NovaMaterial = GSON.fromJson(tileEntityData.get("material"))!!
        val ownerUUID: UUID = GSON.fromJson(tileEntityData.get("owner"))!!
        
        val convertedData = tileEntityData.convertToCompoundElement().apply {
            putElement("global", tileEntityData.get("global").asJsonObject.convertToCompoundElement())
            put("owner", ownerUUID)
        }
        
        val itemConfig: EnumMap<BlockFace, ItemConnectionType>? = GSON.fromJson(tileEntityData.get("itemConfig"))
        val inventories: EnumMap<BlockFace, UUID>? = GSON.fromJson(tileEntityData.get("inventories"))
        val energyConfig: EnumMap<BlockFace, EnergyConnectionType>? = GSON.fromJson(tileEntityData.get("energyConfig"))
        val bridgeFaces: List<BlockFace>? = GSON.fromJson(tileEntityData.get("bridgeFaces"))
        
        if (itemConfig != null) convertedData.putElement("itemConfig", itemConfig.toElement(ItemConnectionType::class))
        if (inventories != null) convertedData.putElement("inventories", inventories.toElement(UUID::class))
        if (energyConfig != null) convertedData.putElement("energyConfig", energyConfig.toElement(EnergyConnectionType::class))
        if (bridgeFaces != null) {
            val listElement = ListElement<Element>()
            bridgeFaces.forEach { listElement.add(BackedElement.createElement(it)) }
            convertedData.putElement("bridgeFaces", listElement)
        }
        
        val triple = Triple(material, ownerUUID, convertedData)
        println("[NovaLegacyDataConverter] Converted to: $triple")
        return triple
    }
    
    private fun JsonObject.convertToCompoundElement(): CompoundElement {
        val convertedData = CompoundElement()
        for ((key, jsonElement) in entrySet()) {
            if (IGNORED_MANUAL_KEYS.contains(key)) continue
            
            if (key.endsWith("location", true) || key.endsWith("destination", true)) {
                val location: Location = GSON.fromJson(jsonElement)!!
                convertedData.put(key, location)
            } else if (key == "type" || key.endsWith("item", true)) {
                val itemStack: ItemStack = GSON.fromJson(jsonElement)!!
                convertedData.put(key, itemStack)
            } else if (key.endsWith("recipe", true)) {
                val recipeKey: NamespacedKey = GSON.fromJson(jsonElement)!!
                convertedData.put(key, recipeKey)
            } else if (jsonElement is JsonPrimitive) {
                convertedData.putJsonPrimitive(key, jsonElement)
            } else {
                println("[NovaLegacyDataConverter] Could not convert $key ($jsonElement)")
            }
        }
        
        return convertedData
    }
    
    private fun CompoundElement.putJsonPrimitive(key: String, primitive: JsonPrimitive) {
        when {
            primitive.isBoolean -> put(key, primitive.asBoolean)
            primitive.isString -> put(key, primitive.asString)
            primitive.isNumber -> {
                val number = primitive.asNumber as LazilyParsedNumber
                val numberString = number.toString()
                
                // this assumes that only Ints and Doubles are used
                if (numberString.contains('.')) {
                    put(key, number.toDouble())
                } else {
                    put(key, number.toInt())
                }
            }
        }
    }
    
}
