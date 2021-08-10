package xyz.xenondevs.nova.upgrade

import de.studiocode.invui.virtualinventory.VirtualInventory
import de.studiocode.invui.virtualinventory.event.ItemUpdateEvent
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.serialization.cbf.element.CompoundElement
import xyz.xenondevs.nova.serialization.cbf.element.other.UpgradesElement
import xyz.xenondevs.nova.tileentity.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.TileEntityGUI
import xyz.xenondevs.nova.ui.UpgradesGUI
import xyz.xenondevs.nova.util.novaMaterial
import xyz.xenondevs.nova.util.runTaskLater
import java.util.*
import kotlin.math.pow

// TODO nerf
private val DEFAULT_MODIFIERS by lazy { (0..10).associateWith { if (it == 0) 1.0 else if (it == 1) it + 0.25 else it - 0.25 } }
private val SPEED_MODIFIERS by lazy { (0..10).associateWith { it.toDouble().pow(0.95) + 1 } }

private fun ItemStack.getUpgradeType(): UpgradeType? {
    val novaType = novaMaterial ?: return null
    return UpgradeType.values().firstOrNull { it.material == novaType }
}

class UpgradeHolder(val upgradeable: Upgradeable, data: CompoundElement, lazyGUI: () -> TileEntityGUI, vararg allowed: UpgradeType) {
    
    val input = VirtualInventory(null, 1).apply { setItemUpdateHandler(::handleNewInput) }
    val allowed = allowed.toList()
    val upgrades = EnumMap<UpgradeType, Int>(UpgradeType::class.java)
    
    val gui by lazy { UpgradesGUI(this) { lazyGUI().openWindow(it) } }
    
    constructor(upgradeable: Upgradeable, data: CompoundElement, lazyGUI: () -> TileEntityGUI) : this(upgradeable, data, lazyGUI, *UpgradeType.values())
    
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
        if (type !in allowed)
            return amount
        
        upgrades.putIfAbsent(type, 0)
        val current = upgrades[type]!!
        if (10 - current < amount) {
            upgrades[type] = 10
            return amount - (10 - current)
        } else {
            upgrades[type] = current + amount
            return 0
        }
    }
    
    fun removeUpgrade(type: UpgradeType): ItemStack? {
        if (type !in upgrades)
            return null
        val amount = upgrades[type]!!
        upgrades -= type
        handleUpgradeUpdates()
        return type.material.createItemStack(amount)
    }
    
    fun getModifier(type: UpgradeType): Double {
        val amount = upgrades[type] ?: 0
        if (type == UpgradeType.SPEED)
            return SPEED_MODIFIERS[amount]!!
        return DEFAULT_MODIFIERS[amount]!!
    }
    
    fun getSpeedModifier() = getModifier(UpgradeType.SPEED)
    
    fun getEfficiencyModifier() = getModifier(UpgradeType.EFFICIENCY)
    
    fun getEnergyModifier() = getModifier(UpgradeType.ENERGY)
    
    private fun handleNewInput(event: ItemUpdateEvent) {
        if (event.updateReason == SELF_UPDATE_REASON || event.isRemove || event.newItemStack == null)
            return
        val upgradeType = event.newItemStack.getUpgradeType()
        if (upgradeType == null) {
            event.isCancelled = true
            return
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