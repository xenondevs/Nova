package xyz.xenondevs.nova.tileentity.upgrade

import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.cbf.Compound
import xyz.xenondevs.commons.collections.mapKeysNotNullTo
import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.virtualinventory.VirtualInventory
import xyz.xenondevs.invui.virtualinventory.event.InventoryUpdatedEvent
import xyz.xenondevs.invui.virtualinventory.event.ItemUpdateEvent
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.TileEntity.Companion.SELF_UPDATE_REASON
import xyz.xenondevs.nova.tileentity.menu.MenuContainer
import xyz.xenondevs.nova.ui.UpgradesGui
import xyz.xenondevs.nova.util.item.novaItem
import kotlin.math.min

private fun ItemStack.getUpgradeType(): UpgradeType<*>? {
    val novaMaterial = novaItem ?: return null
    return UpgradeType.of<UpgradeType<*>>(novaMaterial)
}

class UpgradeHolder internal constructor(
    tileEntity: TileEntity,
    internal val menuContainer: MenuContainer,
    private val updateHandler: (() -> Unit)?,
    internal val allowed: Set<UpgradeType<*>>
) {
    
    private val material = tileEntity.block
    private val valueProviders: Map<UpgradeType<*>, ModifierProvider<*>> = allowed.associateWithTo(HashMap()) { ModifierProvider(it) }
    
    internal val input = VirtualInventory(null, 1).apply { setItemUpdateHandler(::handlePreInvUpdate); setInventoryUpdatedHandler(::handlePostInvUpdate) }
    internal val upgrades: HashMap<UpgradeType<*>, Int> =
        tileEntity.retrieveData<Map<ResourceLocation, Int>>("upgrades", ::HashMap)
            .mapKeysNotNullTo(HashMap()) { NovaRegistries.UPGRADE_TYPE[it.key] }
    
    val gui by lazy { UpgradesGui(this) { menuContainer.openWindow(it) } }
    
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
    
    @Suppress("UNCHECKED_CAST")
    fun <T> getValueProvider(type: UpgradeType<T>): Provider<T> {
        return valueProviders[type] as Provider<T>
    }
    
    fun getLevel(type: UpgradeType<*>): Int {
        return upgrades[type] ?: 0
    }
    
    fun hasUpgrade(type: UpgradeType<*>): Boolean {
        return type in upgrades
    }
    
    fun getLimit(type: UpgradeType<*>): Int {
        return min(type.getValueList(material).size - 1, 999)
    }
    
    fun getUpgradeItems(): List<ItemStack> {
        return upgrades.map { (type, amount) -> type.item.createItemStack(amount) }
    }
    
    internal fun save(compound: Compound) {
        compound["upgrades"] = upgrades.mapKeys { it.key.id }
    }
    
    internal fun handleRemoved() {
        valueProviders.values.forEach(ModifierProvider<*>::handleRemoved)
    }
    
    private fun handleUpgradeUpdates() {
        gui.updateUpgrades()
        updateHandler?.invoke()
        valueProviders.values.forEach(ModifierProvider<*>::update)
    }
    
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
    
    private inner class ModifierProvider<T>(private val type: UpgradeType<T>) : Provider<T>() {
        
        private val parent = type.getValueListProvider(material)
        
        init {
            parent.addChild(this)
        }
        
        fun handleRemoved() {
            parent.removeChild(this)
        }
        
        override fun loadValue(): T {
            return getValue(type)
        }
        
    }
    
}