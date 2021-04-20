package xyz.xenondevs.nova.util

import java.math.BigDecimal

object EnergyUtils {
    
    private val IGNORED_PREFIXES = arrayOf(
        MetricPrefix.YOCTO,
        MetricPrefix.ZEPTO,
        MetricPrefix.ATTO,
        MetricPrefix.FEMTO,
        MetricPrefix.PICO,
        MetricPrefix.NANO,
        MetricPrefix.MICRO,
        MetricPrefix.MILLI,
        MetricPrefix.CENTI,
        MetricPrefix.DECI,
        MetricPrefix.DEKA,
        MetricPrefix.HECTO,
    )
    
    fun getEnergyString(energy: Int) = MetricPrefix.getMetricString(BigDecimal(energy), "J", true, *IGNORED_PREFIXES)
    
    fun getEnergyString(energy: Int, maxEnergy: Int): String {
        val bestMaxEnergy = MetricPrefix.findBestPrefix(BigDecimal(maxEnergy), *IGNORED_PREFIXES)
        val prefix = bestMaxEnergy.second
        
        val prefixedEnergy = BigDecimal(energy).divide(prefix.number).stripTrailingZeros().toPlainString()
        val prefixedMaxEnergy = bestMaxEnergy.first.stripTrailingZeros().toPlainString()
        
        val unit = "${prefix.prefixSymbol}J"
        return "§r$prefixedEnergy$unit / $prefixedMaxEnergy$unit"
    }
    
}