package xyz.xenondevs.nova.tileentity.upgrade

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.UpgradesElement
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.UpgradesGUI
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.runTaskLater
import java.lang.Integer.min
import java.util.*

private fun ItemStack.getUpgradeType(): UpgradeType? {
    val novaType = novaMaterial ?: return null
    return UpgradeType.values().firstOrNull { it.material == novaType }
}

class UpgradeHolder(
    tileEntity: TileEntity,
    val lazyGUI: Lazy<TileEntityGUI>,
    private val defaultUpdateHandler: (() -> Unit)?,
    vararg allowed: UpgradeType
) {
    
    val data = tileEntity.data
    val material = tileEntity.material
    val input = VirtualInventory(null, 1).apply { setItemUpdateHandler(::handleNewInput) }
    val allowed = allowed.toList()
    val upgrades = EnumMap<UpgradeType, Int>(UpgradeType::class.java)
    
    val gui by lazy { UpgradesGUI(this) { lazyGUI.value.openWindow(it) } }
    
    val upgradeUpdateHandlers = ArrayList<() -> Unit>()
    
    constructor(tileEntity: TileEntity, lazyGUI: Lazy<TileEntityGUI>, vararg allowed: UpgradeType) :
        this(tileEntity, lazyGUI, null, allowed = allowed)
    
    constructor(tileEntity: TileEntity, lazyGUI: Lazy<TileEntityGUI>, defaultUpdateHandler: (() -> Unit)? = null) :
        this(tileEntity, lazyGUI, defaultUpdateHandler, allowed = UpgradeType.values())
    
    init {
        val savedUpgrades = data.get<EnumMap<UpgradeType, Int>>("upgrades")
        if (savedUpgrades != null)
            upgrades.putAll(savedUpgrades)
    }
    
    /**
     * Tries adding the given amount of upgrades and
     * returns the amount it wasn't able to add
     */
    @Suppress("LiftReturnOrAssignment")
    fun addUpgrade(type: UpgradeType, amount: Int): Int {
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
    
    fun removeUpgrade(type: UpgradeType, all: Boolean): ItemStack? {
        val amount = upgrades[type] ?: return null
        
        if (all) {
            upgrades.remove(type)
        } else {
            if (amount - 1 == 0) upgrades.remove(type)
            else upgrades[type] = amount - 1
        }
        
        handleUpgradeUpdates()
        return type.material.createItemStack(if (all) amount else 1)
    }
    
    fun getModifier(type: UpgradeType): Double {
        val amount = upgrades[type] ?: 0
        return type[material][amount]
    }
    
    fun getLimit(type: UpgradeType): Int = min(type[material].size - 1, 999)
    
    fun calculateEnergyUsage(baseUsage: Int) =
        (baseUsage * getSpeedModifier() / getEfficiencyModifier()).toInt()
    
    fun getSpeedModifier() = getModifier(UpgradeType.SPEED)
    
    fun getEfficiencyModifier() = getModifier(UpgradeType.EFFICIENCY)
    
    fun getEnergyModifier() = getModifier(UpgradeType.ENERGY)
    
    fun getRangeModifier() = getModifier(UpgradeType.RANGE).toInt()
    
    private fun handleNewInput(event: ItemUpdateEvent) {
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
        
        runTaskLater(1) {
            val vi = event.virtualInventory
            var item = vi.items[0]
            if (item != null) {
                val amountLeft = addUpgrade(upgradeType, item.amount)
                if (amountLeft == 0) item = null
                else item.amount = amountLeft
                vi.setItemStack(SELF_UPDATE_REASON, 0, item)
            }
            handleUpgradeUpdates()
        }
    }
    
    private fun handleUpgradeUpdates() {
        gui.updateUpgrades()
        upgradeUpdateHandlers.forEach { it() }
        defaultUpdateHandler?.invoke() // This is separate from the arraylist, so it is always called last.
    }
    
    fun dropUpgrades() = upgrades.map { (type, amount) -> type.material.createItemStack(amount) }
    
    fun save(compound: CompoundElement) {
        compound.putElement("upgrades", UpgradesElement(upgrades))
    }
    
}