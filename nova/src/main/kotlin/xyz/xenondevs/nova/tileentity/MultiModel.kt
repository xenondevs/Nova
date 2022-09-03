package xyz.xenondevs.nova.tileentity

import net.minecraft.core.Rotations
import net.minecraft.world.entity.EquipmentSlot
import org.bukkit.Location
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.world.fakeentity.impl.FakeArmorStand
import java.util.logging.Level

class MultiModel {
    
    val currentModels = HashMap<FakeArmorStand, Model>()
    private var closed = false
    
    @Synchronized
    fun removeDuplicates() {
        val models = HashSet<Model>()
        currentModels.forEach { (armorStand, model) ->
            if (models.contains(model)) {
                armorStand.remove()
            } else models += model
        }
    }
    
    @Synchronized
    fun useArmorStands(run: (FakeArmorStand) -> Unit) {
        currentModels.keys.forEach(run::invoke)
    }
    
    @Synchronized
    fun replaceModels(models: List<Model>) {
        removeModels(currentModels.filterNot { models.contains(it.value) }.keys.toList())
        addModels(models.filterNot { currentModels.containsValue(it) })
    }
    
    @Synchronized
    fun addModels(vararg models: Model) = addModels(models.asList())
    
    @Synchronized
    fun addModels(models: Iterable<Model>): List<FakeArmorStand> {
        if (closed) {
            LOGGER.log(Level.INFO, "MultiModel is closed", Exception())
            return emptyList()
        }
        
        return models.map { model ->
            val location = model.location
            val armorStand = FakeArmorStand(location) { ast, data ->
                ast.setEquipment(EquipmentSlot.HEAD, model.itemStack, false)
                data.isInvisible = true
                data.isMarker = true
                data.headRotation = model.headPose
            }
            
            currentModels[armorStand] = model
            
            return@map armorStand
        }
    }
    
    @Synchronized
    fun hasModelLocation(location: Location) =
        currentModels.any { it.key.location == location }
    
    @Synchronized
    fun removeIf(predicate: (FakeArmorStand, Model) -> Boolean) {
        removeModels(currentModels.filter { predicate(it.key, it.value) }.keys.toList())
    }
    
    @Synchronized
    fun removeAllModels() {
        currentModels.forEach { it.key.remove() }
        currentModels.clear()
    }
    
    @Synchronized
    fun close() {
        removeAllModels()
        closed = true
    }
    
    @Synchronized
    private fun removeModels(models: List<FakeArmorStand>) {
        models.forEach {
            it.remove()
            currentModels.remove(it)
        }
    }
    
}

data class Model(val itemStack: ItemStack, val location: Location, val headPose: Rotations = Rotations(0f, 0f, 0f))
