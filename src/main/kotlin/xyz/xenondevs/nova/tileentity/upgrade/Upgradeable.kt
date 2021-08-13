package xyz.xenondevs.nova.tileentity.upgrade

interface Upgradeable {
    
    val upgradeHolder: UpgradeHolder
    
    fun handleUpgradeUpdates()
    
}