package xyz.xenondevs.nova.tileentity.network.energy.holder

// TODO

//fun ProviderEnergyHolder(
//    endPoint: NetworkedTileEntity,
//    defaultMaxEnergy: Provider<Long>,
//    defaultEnergyGeneration: Provider<Long>?,
//    upgradeHolder: UpgradeHolder,
//    generationUpgradeType: UpgradeType<Double>,
//    energyUpgradeType: UpgradeType<Double>,
//    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
//) = ProviderEnergyHolder(
//    endPoint,
//    defaultMaxEnergy,
//    defaultEnergyGeneration,
//    upgradeHolder as UpgradeHolder?,
//    generationUpgradeType as UpgradeType<Double>?,
//    energyUpgradeType as UpgradeType<Double>?,
//    lazyDefaultConfig
//)
//
//@Suppress("UNCHECKED_CAST")
//class ProviderEnergyHolder internal constructor(
//    endPoint: NetworkedTileEntity,
//    defaultMaxEnergy: Provider<Long>,
//    defaultEnergyGeneration: Provider<Long>?,
//    upgradeHolder: UpgradeHolder?,
//    private val generationUpgradeType: UpgradeType<Double>?,
//    energyUpgradeType: UpgradeType<Double>?,
//    lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
//) : NovaEnergyHolder(endPoint, defaultMaxEnergy, upgradeHolder, energyUpgradeType, lazyDefaultConfig) {
//    
//    override val allowedConnectionType = NetworkConnectionType.EXTRACT
//    private val defaultEnergyGeneration by (defaultEnergyGeneration ?: NullProvider) as Provider<Long?>
//    var energyGeneration = calculateEnergyGeneration()
//        private set
//    
//    constructor(
//        endPoint: NetworkedTileEntity,
//        defaultMaxEnergy: Provider<Long>,
//        defaultEnergyGeneration: Provider<Long>? = null,
//        lazyDefaultConfig: () -> EnumMap<BlockFace, NetworkConnectionType>
//    ) : this(
//        endPoint,
//        defaultMaxEnergy,
//        defaultEnergyGeneration,
//        null, null, null,
//        lazyDefaultConfig
//    )
//    
//    private fun calculateEnergyGeneration(): Long {
//        val default = defaultEnergyGeneration ?: return 0L
//        
//        if (upgradeHolder != null && generationUpgradeType != null) {
//            return (default * upgradeHolder.getValue(generationUpgradeType)).toLong()
//        }
//        
//        return default
//    }
//    
//    override fun reload() {
//        energyGeneration = calculateEnergyGeneration()
//        super.reload()
//    }
//    
//}