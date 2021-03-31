package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.serialization.TileEntitySerialization
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.fromJson
import java.io.File
import java.util.*

abstract class TileEntity(val material: NovaMaterial, val uuid: UUID, val armorStand: ArmorStand) {
    
    protected val saveFile = File("plugins/Nova/TileEntity/$uuid.json").also { it.parentFile.mkdirs() }
    protected val jsonObject: JsonObject = if (saveFile.exists()) JsonParser().parse(saveFile.readText()) as JsonObject else JsonObject()
    open val drops = mutableListOf(material.createItemStack())
    
    abstract fun handleRightClick(event: PlayerInteractEvent)
    
    abstract fun handleDisable()
    
    abstract fun handleRemove()
    
    abstract fun handleTick()
    
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
