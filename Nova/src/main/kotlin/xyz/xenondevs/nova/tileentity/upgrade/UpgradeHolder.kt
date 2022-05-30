package xyz.xenondevs.nova.tileentity.upgrade

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.InventoryUpdatedEvent
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.Compound
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntity.TileEntityGUI
import xyz.xenondevs.nova.ui.UpgradesGUI
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.mapKeysNotNullTo
import kotlin.math.min

private fun ItemStack.getUpgradeType(): UpgradeType<*>? {
    val novaMaterial = novaMaterial ?: return null
    return UpgradeTypeRegistry.of(novaMaterial)
}

class UpgradeHolder internal constructor(
    tileEntity: TileEntity,
    val lazyGUI: Lazy<TileEntityGUI>,
    private val updateHandler: (() -> Unit)?,
    vararg allowed: UpgradeType<*>
) {
    
    val material = tileEntity.material
    val input = VirtualInventory(null, 1).apply { setItemUpdateHandler(::handlePreInvUpdate); setInventoryUpdatedHandler(::handlePostInvUpdate) }
    val allowed: Set<UpgradeType<*>> = allowed.toSet()
    val upgrades: HashMap<UpgradeType<*>, Int> =
        tileEntity.retrieveData<Map<String, Int>>("upgrades", ::HashMap)
            .mapKeysNotNullTo(HashMap()) { UpgradeTypeRegistry.of(it.key) }
    
    val gui by lazy { UpgradesGUI(this) { lazyGUI.value.openWindow(it) } }
    
    /**
     * Tries adding the given amount of upgrades and
     * returns the amount it wasn't able to add
     */
    @Suppress("LiftReturnOrAssignment")
    fun addUpgrade(type: UpgradeType<*>, amount: Int): Int {
        if (type !in allowed || amount == 0)
            return amount
        
        val limit = getLimit(type)
        
        val current = upgrades[type] ?: 0
        if (limit - current < amount) {
            upgrades[type] = limit
            return amount - (limit - current)
        } else {
            upgrades[type] = current + amount
            return 0
        }
    }
    
    fun removeUpgrade(type: UpgradeType<*>, all: Boolean): ItemStack? {
        val amount = upgrades[type] ?: return null
        
        if (all) {
            upgrades.remove(type)
        } else {
            if (amount - 1 == 0) upgrades.remove(type)
            else upgrades[type] = amount - 1
        }
        
        handleUpgradeUpdates()
        return type.item.createItemStack(if (all) amount else 1)
    }
    
    fun <T> getValue(type: UpgradeType<T>): T {
        val amount = upgrades[type] ?: 0
        return type.getValue(material, amount)
    }
    
    fun getLevel(type: UpgradeType<*>): Int {
        return upgrades[type] ?: 0
    }
    
    fun hasUpgrade(type: UpgradeType<*>): Boolean {
        return type in upgrades
    }
    
    fun getLimit(type: UpgradeType<*>): Int = min(type.getUpgradeValues(material).size - 1, 999)
    
    private fun handlePreInvUpdate(event: ItemUpdateEvent) {
        if (event.updateReason == SELF_UPDATE_REASON || event.isRemove || event.newItemStack == null)
            return
        
        val upgradeType = event.newItemStack.getUpgradeType()
        if (upgradeType == null || upgradeType !in allowed) {
            event.isCancelled = true
            return
        }
        
        val limit = getLimit(upgradeType)
        
        val currentAmount = upgrades[upgradeType] ?: 0
        if (currentAmount == limit) {
            event.isCancelled = true
            return
        }
        
        var addedAmount = event.addedAmount
        if (addedAmount + currentAmount > limit) {
            addedAmount = limit - currentAmount
            event.newItemStack.amount = addedAmount
        }
    }
    
    private fun handlePostInvUpdate(event: InventoryUpdatedEvent) {
        var item = event.newItemStack?.clone()
        if (item != null) {
            val upgradeType = item.getUpgradeType()!!
            
            val amountLeft = addUpgrade(upgradeType, item.amount)
            if (amountLeft == 0) item = null
            else item.amount = amountLeft
            input.setItemStack(SELF_UPDATE_REASON, 0, item)
            
            handleUpgradeUpdates()
        }
    }
    
    private fun handleUpgradeUpdates() {
        gui.updateUpgrades()
        updateHandler?.invoke()
    }
    
    fun dropUpgrades() = upgrades.map { (type, amount) -> type.item.createItemStack(amount) }
    
    fun save(compound: Compound) {
        compound["upgrades"] = upgrades.mapKeys { it.key.id }
    }
    
}