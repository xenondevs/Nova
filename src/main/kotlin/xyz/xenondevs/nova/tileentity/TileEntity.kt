package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.fromJson
import java.util.*

abstract class TileEntity(
    val material: NovaMaterial,
    val armorStand: ArmorStand,
    private val keepData: Boolean
) {
    
    protected val data: JsonObject = if (armorStand.hasTileEntityData()) armorStand.getTileEntityData() else JsonObject()
    protected val uuid: UUID = armorStand.uniqueId
    
    init {
        if (data.size() == 0) {
            storeData("material", material)
        }
    }
    
    open fun handleDisable() {
        saveData()
    }
    
    open fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        val drops = ArrayList<ItemStack>()
        if (dropItems) {
            saveData()
            val item = createItem()
            if (keepData) item.setTileEntityData(data)
            drops += item
        }
        
        return drops
    }
    
    open fun createItem() = material.createItemStack()
    
    abstract fun saveData()
    
    abstract fun handleTick()
    
    abstract fun handleRightClick(event: PlayerInteractEvent)
    
    protected inline fun <reified T> retrieveData(alternative: T, key: String): T {
        return retrieveOrNull(key) ?: alternative
    }
    
    protected inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(data.get(key))
    }
    
    fun storeData(key: String, value: Any) {
        data.add(key, GSON.toJsonTree(value))
        armorStand.setTileEntityData(data)
    }
    
    override fun toString(): String {
        return "${javaClass.name}(Material: $material, ArmorStand UUID: $uuid)"
    }
    
    companion object {
        
        fun newInstance(armorStand: ArmorStand): TileEntity {
            val data = armorStand.getTileEntityData()
            val material: NovaMaterial = GSON.fromJson(data.get("material"))!!
            
            return material.tileEntityConstructor!!(material, armorStand)
        }
        
    }
    
}
