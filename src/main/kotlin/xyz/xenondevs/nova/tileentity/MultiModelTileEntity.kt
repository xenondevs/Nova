package xyz.xenondevs.nova.tileentity

import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.serialization.UUIDDataType
import xyz.xenondevs.nova.util.EntityUtils

private val MULTI_MODEL_KEY = NamespacedKey(NOVA, "multiModel")

abstract class MultiModelTileEntity(
    material: NovaMaterial,
    armorStand: ArmorStand,
) : TileEntity(material, armorStand) {
    
    private var currentModels: HashSet<Pair<ItemStack, Float>>? = null
    
    override fun handleDisabled() {
        super.handleDisabled()
        removeModels(null)
    }
    
    fun replaceModels(models: List<Pair<ItemStack, Float>>) {
        val modelsToRemove = if (currentModels != null) currentModels!!.filterNot { models.contains(it) } else models
        
        removeModels(modelsToRemove)
        spawnModels(models.filterNot { currentModels?.contains(it) ?: false })
        
        currentModels = HashSet(models)
    }
    
    private fun spawnModels(models: List<Pair<ItemStack, Float>>) {
        models.forEach { (itemStack, yaw) ->
            val location = armorStand.location.clone()
            location.yaw = yaw
            EntityUtils.spawnArmorStandSilently(location, itemStack) {
                val dataContainer = persistentDataContainer
                dataContainer.set(MULTI_MODEL_KEY, UUIDDataType, uuid)
            }
        }
    }
    
    private fun removeModels(models: List<Pair<ItemStack, Float>>?) {
        if (models != null && models.isEmpty()) return
        armorStand.location.chunk.entities
            .filterIsInstance<ArmorStand>()
            .filter { armorStand ->
                val dataContainer = armorStand.persistentDataContainer
                dataContainer.has(MULTI_MODEL_KEY, UUIDDataType)
                    && dataContainer.get(MULTI_MODEL_KEY, UUIDDataType) == uuid
                    && models?.any { armorStand.hasModel(it) } ?: true
            }
            .forEach(ArmorStand::remove)
    }
    
    private fun ArmorStand.hasModel(model: Pair<ItemStack, Float>) =
        equipment!!.helmet == model.first && location.yaw == model.second
    
    override fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        removeModels(null)
        return super.destroy(dropItems)
    }
    
}