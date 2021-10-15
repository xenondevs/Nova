package xyz.xenondevs.nova.util

import java.math.BigDecimal
import java.math.RoundingMode

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
    
    fun getEnergyString(energy: Int): String {
        val closest = MetricPrefix.findBestPrefix(BigDecimal(energy), *IGNORED_PREFIXES)
        val prefix = closest.second
        val resultNumber = closest.first.setScale(2, RoundingMode.HALF_UP)
            .let { if (prefix == MetricPrefix.NONE) it.stripTrailingZeros() else it }
            .toPlainString()
        return "$resultNumber ${prefix.prefixSymbol}"
    }
    
    fun getEnergyString(energy: Int, maxEnergy: Int): String {
        val bestMaxEnergy = MetricPrefix.findBestPrefix(BigDecimal(maxEnergy), *IGNORED_PREFIXES)
        val prefix = bestMaxEnergy.second
        
        val prefixedEnergy = if (energy == 0) "0" else
            BigDecimal(energy).divide(prefix.number)
                .setScale(2, RoundingMode.HALF_UP)
                .let { if (prefix == MetricPrefix.NONE) it.stripTrailingZeros() else it }
                .toPlainString()
        
        val prefixedMaxEnergy = bestMaxEnergy.first
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        
        val unit = "${prefix.prefixSymbol}J"
        return "§r$prefixedEnergy $unit / $prefixedMaxEnergy $unit"
    }
    
}