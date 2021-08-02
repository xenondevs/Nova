package xyz.xenondevs.nova.upgrade

interface Upgradeable {
    
    val upgradeHolder: UpgradeHolder
    
    fun handleUpgradeUpdates()
    
}