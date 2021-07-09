package xyz.xenondevs.nova.tileentity

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.serialization.persistentdata.UUIDDataType
import xyz.xenondevs.nova.util.EntityUtils
import xyz.xenondevs.nova.util.runTaskLater
import java.util.*

private val MULTI_MODEL_KEY = NamespacedKey(NOVA, "multiModel")

fun ArmorStand.isMultiModel() = persistentDataContainer.has(MULTI_MODEL_KEY, UUIDDataType)

fun ArmorStand.getMultiModelParent(): TileEntity? {
    val uuid = persistentDataContainer.get(MULTI_MODEL_KEY, UUIDDataType)
    return if (uuid != null) TileEntityManager.tileEntities.firstOrNull { tileEntity ->
        tileEntity.multiModels.values.any { multiModel ->
            multiModel.uuid == uuid
        }
    } else null
}

class MultiModel(
    val uuid: UUID,
    chunks: Set<Chunk> = emptySet()
) {
    
    private val currentModels = HashMap<ArmorStand, Model>()
    private var chunksInvalid = false
    
    val chunks: Set<Chunk>
        get() = currentModels.keys.mapTo(HashSet()) { it.location.chunk }
    
    val armorStands: Set<ArmorStand>
        get() = currentModels.keys.toSet()
    
    init {
        // https://hub.spigotmc.org/jira/browse/SPIGOT-6547
        // workaround because of async entity loading:
        // check for entities every 10 ticks for the next 15 seconds (300 ticks)
        for (delay in 0..300 step 10) {
            runTaskLater(delay.toLong()) { currentModels += findModelArmorStands(chunks) }
        }
    }
    
    fun useArmorStands(run: (ArmorStand) -> Unit) {
        chunksInvalid = true
        currentModels.keys.forEach(run::invoke)
    }
    
    fun replaceModels(models: List<Model>) {
        removeModels(currentModels.filterNot { models.contains(it.value) }.keys.toList())
        addModels(models.filterNot { currentModels.containsValue(it) })
    }
    
    fun addModels(vararg models: Model) = addModels(models.toList())
    
    fun addModels(models: List<Model>): List<ArmorStand> {
        chunksInvalid = true
        
        return models.map {
            val location = it.location
            val armorStand = EntityUtils.spawnArmorStandSilently(location, it.itemStack, false) {
                val dataContainer = persistentDataContainer
                dataContainer.set(MULTI_MODEL_KEY, UUIDDataType, uuid)
            }
            currentModels[armorStand] = it
            
            return@map armorStand
        }
    }
    
    fun hasModelLocation(location: Location) =
        currentModels.any { it.key.location == location }
    
    fun removeIf(predicate: (ArmorStand, Model) -> Boolean) {
        removeModels(currentModels.filter { predicate(it.key, it.value) }.keys.toList())
    }
    
    fun removeAllModels() {
        currentModels.forEach { it.key.remove() }
        currentModels.clear()
    }
    
    private fun removeModels(models: List<ArmorStand>) {
        models.forEach {
            it.remove()
            currentModels.remove(it)
        }
    }
    
    private fun findModelArmorStands(chunks: Set<Chunk>) =
        chunks
            .flatMap { it.entities.toList() }
            .filterIsInstance<ArmorStand>()
            .mapNotNull {
                val model = it.model
                if (model != null) it to model else null
            }.toMap()
    
    private val ArmorStand.model: Model?
        get() {
            return if (persistentDataContainer.get(MULTI_MODEL_KEY, UUIDDataType) == uuid)
                Model(equipment!!.helmet!!, location)
            else null
        }
    
}

data class Model(val itemStack: ItemStack, val location: Location)
