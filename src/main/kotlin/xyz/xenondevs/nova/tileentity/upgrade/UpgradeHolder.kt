package xyz.xenondevs.nova.tileentity.upgrade

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.data.serialization.cbf.element.other.UpgradesElement
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.UpgradesGUI
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.runTaskLater
import java.util.*

private fun ItemStack.getUpgradeType(): UpgradeType? {
    val novaType = novaMaterial ?: return null
    return UpgradeType.values().firstOrNull { it.material == novaType }
}

class UpgradeHolder(
    private val upgradeable: Upgradeable,
    data: CompoundElement,
    vararg allowed: UpgradeType,
    lazyGUI: () -> TileEntityGUI
) {
    
    val input = VirtualInventory(null, 1, arrayOfNulls(1), intArrayOf(10)).apply { setItemUpdateHandler(::handleNewInput) }
    val allowed = allowed.toList()
    val upgrades = EnumMap<UpgradeType, Int>(UpgradeType::class.java)
    
    val gui by lazy { UpgradesGUI(this) { lazyGUI().openWindow(it) } }
    
    constructor(upgradeable: Upgradeable, data: CompoundElement, lazyGUI: () -> TileEntityGUI) : this(upgradeable, data, allowed = UpgradeType.values(), lazyGUI)
    
    init {
        val savedUpgrades = data.get<EnumMap<UpgradeType, Int>>("upgrades")
        if(savedUpgrades != null)
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
    
        val current = upgrades[type] ?: 0
        if (10 - current < amount) {
            upgrades[type] = 10
            return amount - (10 - current)
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
        return type.modifiers[amount]
    }
    
    fun getSpeedModifier() = getModifier(UpgradeType.SPEED)
    
    fun getEfficiencyModifier() = getModifier(UpgradeType.EFFICIENCY)
    
    fun getEnergyModifier() = getModifier(UpgradeType.ENERGY)
    
    fun getRangeModifier() = getModifier(UpgradeType.RANGE)
    
    private fun handleNewInput(event: ItemUpdateEvent) {
        if (event.updateReason == SELF_UPDATE_REASON || event.isRemove || event.newItemStack == null)
            return
        
        val upgradeType = event.newItemStack.getUpgradeType()
        if (upgradeType == null || upgradeType !in allowed) {
            event.isCancelled = true
            return
        }
        
        val currentAmount = upgrades[upgradeType] ?: 0
        if (currentAmount == 10) {
            event.isCancelled = true
            return
        }
        
        var addedAmount = event.addedAmount
        if (addedAmount + currentAmount > 10) {
            addedAmount = 10 - currentAmount
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
        upgradeable.handleUpgradeUpdates()
    }
    
    fun dropUpgrades() = upgrades.map { (type, amount) -> type.material.createItemStack(amount) }
    
    fun save(compound: CompoundElement) {
        compound.putElement("upgrades", UpgradesElement(upgrades))
    }
    
}