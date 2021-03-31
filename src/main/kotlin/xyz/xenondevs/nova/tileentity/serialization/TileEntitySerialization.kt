package xyz.xenondevs.nova.tileentity.serialization

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.bukkit.entity.ArmorStand
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.tileentity.TileEntity
import java.util.*

object TileEntitySerialization {
    
    fun serialize(tileEntity: TileEntity): String {
        val jsonObject = JsonObject()
        jsonObject.addProperty("material", tileEntity.material.name)
        jsonObject.addProperty("uuid", tileEntity.uuid.toString())
        
        return jsonObject.toString()
    }
    
    fun deserialize(armorStand: ArmorStand, string: String): TileEntity {
        val jsonObject = JsonParser().parse(string) as JsonObject
        val material = NovaMaterial.valueOf(jsonObject["material"].asString)
        val uuid = UUID.fromString(jsonObject["uuid"].asString)
        
        return material.tileEntityConstructor!!(material, uuid, armorStand)
    }
    
}