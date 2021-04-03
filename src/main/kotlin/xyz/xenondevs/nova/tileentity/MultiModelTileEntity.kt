package xyz.xenondevs.nova.tileentity

import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.serialization.UUIDDataType
import xyz.xenondevs.nova.util.EntityUtils
import java.util.*

private val MULTI_MODEL_KEY = NamespacedKey(NOVA, "multiModel")

abstract class MultiModelTileEntity(
    material: NovaMaterial,
    armorStand: ArmorStand,
    keepData: Boolean
) : TileEntity(material, armorStand, keepData) {
    
    override fun handleDisabled() {
        super.handleDisabled()
        removeModels()
    }
    
    fun replaceModels(models: List<Pair<ItemStack, Float>>) {
        removeModels()
        setModels(models)
    }
    
    open fun setModels(models: List<Pair<ItemStack, Float>>) {
        models.forEach { (itemStack, yaw) ->
            val location = armorStand.location.clone()
            location.yaw = yaw
            EntityUtils.spawnArmorStandSilently(location, itemStack) {
                val dataContainer = persistentDataContainer
                dataContainer.set(MULTI_MODEL_KEY, UUIDDataType, uuid)
            }
        }
    }
    
    fun removeModels() {
        armorStand.location.chunk.entities
            .filterIsInstance<ArmorStand>()
            .filter {
                val dataContainer = it.persistentDataContainer
                dataContainer.has(MULTI_MODEL_KEY, UUIDDataType)
                    && dataContainer.get(MULTI_MODEL_KEY, UUIDDataType) == uuid
            }
            .forEach(ArmorStand::remove)
    }
    
    override fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        removeModels()
        return super.destroy(dropItems)
    }
    
}