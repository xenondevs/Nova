package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.NamespacedKey
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.serialization.TileEntitySerialization
import xyz.xenondevs.nova.tileentity.serialization.UUIDDataType
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.fromJson
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

abstract class TileEntity(
    val material: NovaMaterial,
    val uuid: UUID,
    val armorStand: ArmorStand,
) {
    
    protected val saveFile = File("plugins/Nova/TileEntity/$uuid.json").also { it.parentFile.mkdirs() }
    protected val jsonObject: JsonObject = if (saveFile.exists()) JsonParser().parse(saveFile.readText()) as JsonObject else JsonObject()
    
    open fun handleDisable() {
        saveData()
    }
    
    open fun destroy(dropItems: Boolean): ArrayList<ItemStack> {
        val drops = java.util.ArrayList<ItemStack>()
        if (dropItems) {
            val item = createItem()
            drops += item
            if (item.hasUUID()) handleDisable() // item will be dropped and references the TileEntity, data is needed
            else saveFile.delete() // item won't reference the TileEntity, data is not needed
        } else saveFile.delete() // no item will be dropped, save data is not needed
        
        return drops
    }
    
    abstract fun saveData()
    
    abstract fun handleTick()
    
    abstract fun handleRightClick(event: PlayerInteractEvent)
    
    abstract fun createItem(): ItemStack
    
    protected inline fun <reified T> retrieveData(alternative: T, key: String): T {
        return retrieveOrNull(key) ?: alternative
    }
    
    protected inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(jsonObject.get(key))
    }
    
    fun storeData(key: String, value: Any) {
        jsonObject.add(key, GSON.toJsonTree(value))
        saveFile.writeText(GSON.toJson(jsonObject))
    }
    
    override fun toString(): String {
        return TileEntitySerialization.serialize(this)
    }
    
}
