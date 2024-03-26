package xyz.xenondevs.nova.tileentity.network.energy.holder

// TODO

//
//@Suppress("UNCHECKED_CAST")
//class ConsumerEnergyHolder internal constructor(
//    endPoint: NetworkedTileEntity,
//    defaultMaxEnergy: Provider<Long>,
//    defaultEnergyConsumption: Provider<Long>?,
//    defaultSpecialEnergyConsumption: Provider<Long>?,
//    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
//) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, energyUpgradeType, lazyDefaultConfig) {
//    
//    override val allowedConnectionType = NetworkConnectionType.INSERT
//    
//    private val defaultEnergyConsumption by (defaultEnergyConsumption ?: NullProvider) as Provider<Long?>
//    private val defaultSpecialEnergyConsumption by (defaultSpecialEnergyConsumption ?: NullProvider) as Provider<Long?>
//    
//    constructor(
//        endPoint: NetworkedTileEntity,
//        defaultMaxEnergy: Provider<Long>,
//        defaultEnergyConsumption: Provider<Long>? = null,
//        defaultSpecialEnergyConsumption: Provider<Long>? = null,
//        lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
//    ) : this(
//        endPoint,
//        defaultMaxEnergy,
//        defaultEnergyConsumption,
//        defaultSpecialEnergyConsumption,
//        null, null, null, null,
//        lazyDefaultConfig
//    )
//    
//    var energyConsumption = calculateEnergyConsumption(this.defaultEnergyConsumption)
//        private set
//    var specialEnergyConsumption = calculateEnergyConsumption(this.defaultSpecialEnergyConsumption)
//        private set
//    
//    private fun calculateEnergyConsumption(default: Long?): Long {
//        if (default == null)
//            return 0L
//        
//        if (upgradeHolder != null && speedUpgradeType != null && efficiencyUpgradeType != null) {
//            return (default * (upgradeHolder.getValue(speedUpgradeType) / upgradeHolder.getValue(efficiencyUpgradeType))).toLong()
//        }
//        
//        return default
//    }
//    
//    override fun reload() {
//        energyConsumption = calculateEnergyConsumption(defaultEnergyConsumption)
//        specialEnergyConsumption = calculateEnergyConsumption(defaultSpecialEnergyConsumption)
//        
//        super.reload()
//    }
//    
//}