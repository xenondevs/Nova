package xyz.xenondevs.nova.tileentity

import org.bukkit.Chunk
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.serialization.UUIDDataType
import xyz.xenondevs.nova.util.EntityUtils
import java.util.*

private val MULTI_MODEL_KEY = NamespacedKey(NOVA, "multiModel")

class MultiModel(
    private val uuid: UUID,
    private var _chunks: MutableSet<Chunk> = mutableSetOf()
) {
    
    private val currentModels = HashMap<ArmorStand, Model>()
    private var chunksInvalid = false
    
    val chunks: Set<Chunk>
        get() {
            if (chunksInvalid) {
                _chunks = currentModels.keys.mapTo(HashSet()) { it.location.chunk }
                chunksInvalid = false
            }
            
            return _chunks
        }
    
    val armorStands: Set<ArmorStand>
        get() = currentModels.keys.toSet()
    
    init {
        currentModels += findModelArmorStands(chunks)
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
    
    fun addModels(models: List<Model>) {
        chunksInvalid = true
        models.forEach {
            val location = it.location
            val armorStand = EntityUtils.spawnArmorStandSilently(location, it.itemStack) {
                val dataContainer = persistentDataContainer
                dataContainer.set(MULTI_MODEL_KEY, UUIDDataType, uuid)
            }
            currentModels[armorStand] = it
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
