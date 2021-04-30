package xyz.xenondevs.nova.tileentity

import com.google.common.base.Preconditions
import com.google.gson.JsonObject
import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.VirtualInventoryManager
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.block.BlockFace
import org.bukkit.entity.ArmorStand
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.material.NovaMaterial
import xyz.xenondevs.nova.network.energy.EnergyConnectionType
import xyz.xenondevs.nova.network.item.ItemConnectionType
import xyz.xenondevs.nova.util.*
import java.util.*

abstract class TileEntity(
    val material: NovaMaterial,
    val armorStand: ArmorStand,
) {
    
    val mainDataObject: JsonObject = if (armorStand.hasTileEntityData()) armorStand.getTileEntityData() else JsonObject()
    val globalDataObject: JsonObject = mainDataObject.get("global")?.let { it as JsonObject }
        ?: JsonObject().also { mainDataObject.add("global", it) }
    
    val uuid: UUID = armorStand.uniqueId
    val location = armorStand.location.blockLocation
    val world = location.world!!
    val chunk = location.chunk
    val facing = armorStand.facing
    
    private val inventories = ArrayList<VirtualInventory>()
    private val multiModels = HashMap<String, MultiModel>()
    
    init {
        if (mainDataObject.size() <= 1) {
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
            val item = material.createItemBuilder(this).build()
            item.setTileEntityData(globalDataObject)
            drops += item
        }
        
        // inventory drops ignore the dropItems parameter
        inventories.forEach {
            drops += it.items.filterNotNull()
            VirtualInventoryManager.getInstance().remove(it)
        }
        
        multiModels.values.forEach { it.removeAllModels() }
        
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
     * Called to save all data using the [storeData] method.
     */
    open fun saveData() {
        multiModels.forEach { (name, multiModel) -> 
            storeData("multiModel_$name", multiModel.chunks)
        }
    }
    
    /**
     * Called every tick for every TileEntity that is in a loaded chunk.
     */
    abstract fun handleTick()
    
    /**
     * Called after the TileEntity has been initialized and added to the
     * TileEntity map in the TileEntityManager.
     *
     * The [first] parameter specifies if it is the first time this
     * [TileEntity] got initialized, meaning it has just been placed.
     */
    abstract fun handleInitialized(first: Boolean)
    
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
     * Gets a [VirtualInventory] for this [TileEntity].
     * When [dropItems] is true, the [VirtualInventory] will automatically be
     * deleted and its contents dropped when the [TileEntity] is destroyed.
     */
    fun getInventory(seed: String, size: Int, dropItems: Boolean, itemHandler: (ItemUpdateEvent) -> Unit): VirtualInventory {
        val inventory = VirtualInventoryManager.getInstance().getOrCreate(uuid.seed(seed), size)
        inventory.setItemUpdateHandler(itemHandler)
        if (dropItems) inventories += inventory
        return inventory
    }
    
    /**
     * Gets a [MultiModel] for this [TileEntity].
     * The [MultiModel] will use the storage of this [TileEntity]
     * to store data regarding the location of [Model]s.
     * When the [TileEntity] is destroyed, all [Model]s belonging
     * to this [MultiModel] will be removed.
     */
    fun getMultiModel(name: String): MultiModel {
        val uuid = this.uuid.seed(name)
        val multiModel = MultiModel(uuid, retrieveData("multiModel_$name") { mutableSetOf(chunk) })
        multiModels[name] = multiModel
        return multiModel
    }
    
    /**
     * Gets the correct direction a block side.
     */
    fun getFace(blockSide: BlockSide) = blockSide.getBlockFace(armorStand.location.yaw)
    
    /**
     * Creates an energy side config
     */
    fun createEnergySideConfig(
        default: EnergyConnectionType,
        vararg blocked: BlockSide
    ): EnumMap<BlockFace, EnergyConnectionType> {
        
        val sideConfig = EnumMap<BlockFace, EnergyConnectionType>(BlockFace::class.java)
        val blockedFaces = blocked.map { getFace(it) }
        CUBE_FACES.forEach { sideConfig[it] = if (blockedFaces.contains(it)) EnergyConnectionType.NONE else default }
        
        return sideConfig
    }
    
    /**
     * Creates an item side config
     */
    fun createItemSideConfig(
        default: ItemConnectionType,
        vararg blocked: BlockSide
    ): EnumMap<BlockFace, ItemConnectionType> {
        
        val sideConfig = EnumMap<BlockFace, ItemConnectionType>(BlockFace::class.java)
        val blockedFaces = blocked.map { getFace(it) }
        CUBE_FACES.forEach { sideConfig[it] = if (blockedFaces.contains(it)) ItemConnectionType.NONE else default }
        
        return sideConfig
    }
    
    /**
     * Retrieves data using GSON deserialization from the
     * ArmorStand of this TileEntity.
     * If it can't find anything under the given key, the
     * result of the [getAlternative] lambda is returned.
     */
    inline fun <reified T> retrieveData(key: String, getAlternative: () -> T): T {
        return retrieveOrNull(key) ?: getAlternative()
    }
    
    /**
     * Retrieves data using GSON deserialization from the
     * ArmorStand of this TileEntity.
     */
    inline fun <reified T> retrieveOrNull(key: String): T? {
        return GSON.fromJson<T>(mainDataObject.get(key) ?: globalDataObject.get(key))
    }
    
    /**
     * Serializes objects using GSON and stores them under the given key in
     * the ArmorStand of this TileEntity.
     *
     * @param global If the data should also be stored in the [ItemStack]
     * of this [TileEntity].
     */
    fun storeData(key: String, value: Any?, global: Boolean = false) {
        if (global) {
            Preconditions.checkArgument(!mainDataObject.has(key), "$key is already a non-global value")
            if (value != null) globalDataObject.add(key, GSON.toJsonTree(value))
            else globalDataObject.remove(key)
        } else {
            Preconditions.checkArgument(!globalDataObject.has(key), "$key is already a global value")
            if (value != null) mainDataObject.add(key, GSON.toJsonTree(value))
            else mainDataObject.remove(key)
        }
        armorStand.setTileEntityData(mainDataObject)
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
            
            return material.createTileEntity!!(material, armorStand)
        }
        
    }
    
}
