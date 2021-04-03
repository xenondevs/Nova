package xyz.xenondevs.nova.tileentity

import com.google.gson.JsonObject
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.util.GSON
import xyz.xenondevs.nova.util.blockLocation
import xyz.xenondevs.nova.util.fromJson
import java.util.*

abstract class TileEntity(
    val material: NovaMaterial,
    val armorStand: ArmorStand,
    private val keepData: Boolean
) {
    
    protected val data: JsonObject = if (armorStand.hasTileEntityData()) armorStand.getTileEntityData() else JsonObject()
    val uuid: UUID = armorStand.uniqueId
    val location = armorStand.location.blockLocation
    val chunk = location.chunk
    
    init {
        if (data.size() == 0) {
            storeData("material", material)
        }
    }
    
    /**
     * Called when the TileEntity is being broken.
     *
     * @param dropItems If items should be dropped. Can be ignored for reasons like
     * dropping the contents of a chest event when the player is in creative mode.
     *
     * @return A list of [ItemStack]s that should be dropped.
     */
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
    
    /**
     * Called after the TileEntity has been removed from the TileEntityManager's
     * TileEntity map because it got unloaded.
     */
    open fun handleDisabled() {
        saveData()
    }
    
    /**
     * Called to create the [ItemStack] to this TileEntity.
     */
    open fun createItem() = material.createItemStack()
    
    /**
     * Called to save all data using the [storeData] method.
     */
    abstract fun saveData()
    
    /**
     * Called every tick for every TileEntity that is in a loaded chunk.
     */
    abstract fun handleTick()
    
    /**
     * Called after the TileEntity has been initialized and added to the
     * TileEntity map in the TileEntityManager.
     */
    abstract fun handleInitialized()
    
    /**
     * Called after the TileEntity has been removed from the
     * TileEntityManager's TileEntity map because it either got
     * unloaded or destroyed.
     *
     * @param unload If the [TileEntity] was removed because the chunk got unloaded.
     */
    abstract fun handleRemoved(unload: Boolean)
    
    /**
     * Called when a player right-clicks the TileEntity.
     * The event has should probably be cancelled if any action
     * is performed in that method.
     */
    abstract fun handleRightClick(event: PlayerInteractEvent)
    
    /**
     * Retrieves data using GSON deserialization from the
     * ArmorStand of this TileEntity.
     * If it can't find anything under the given key, the parameter
     * [alternative] is returned.
     */
    protected inline fun <reified T> retrieveData(alternative: T, key: String): T {
        return retrieveOrNull(key) ?: alternative
    }
    
    /**
     * Retrieves data using GSON deserialization from the
     * ArmorStand of this TileEntity.
     */
    protected inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(data.get(key))
    }
    
    /**
     * Serializes objects using GSON and stores them under the given key in
     * the ArmorStand of this TileEntity.
     */
    fun storeData(key: String, value: Any) {
        data.add(key, GSON.toJsonTree(value))
        armorStand.setTileEntityData(data)
    }
    
    override fun equals(other: Any?): Boolean {
        return if (other is TileEntity) other.uuid == uuid else other === this
    }
    
    override fun hashCode(): Int {
        return uuid.hashCode()
    }
    
    override fun toString(): String {
        return "${javaClass.name}(Material: $material, Location: ${armorStand.location.blockLocation}, UUID: $uuid)"
    }
    
    companion object {
        
        fun newInstance(armorStand: ArmorStand): TileEntity {
            val data = armorStand.getTileEntityData()
            val material: NovaMaterial = GSON.fromJson(data.get("material"))!!
            
            return material.tileEntityConstructor!!(material, armorStand)
        }
        
    }
    
}
