package xyz.xenondevs.nova.util

import xyz.xenondevs.commons.collections.treeMapOf
import xyz.xenondevs.nova.config.MAIN_CONFIG
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.*

private val USE_METRIC_PREFIXES by MAIN_CONFIG.entry<Boolean>("use_metric_prefixes")

object NumberFormatUtils {
    
    private val IGNORED_ENERGY_PREFIXES = MetricPrefix.generateIgnoredArray(
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
    
    private val IGNORED_FLUID_PREFIXES = MetricPrefix.generateIgnoredArray(
        MetricPrefix.YOCTO,
        MetricPrefix.ZEPTO,
        MetricPrefix.ATTO,
        MetricPrefix.FEMTO,
        MetricPrefix.PICO,
        MetricPrefix.NANO,
        MetricPrefix.MICRO,
        MetricPrefix.CENTI,
        MetricPrefix.DECI,
        MetricPrefix.DEKA,
        MetricPrefix.HECTO,
    )
    
    private val NUMBER_FORMAT = NumberFormat.getInstance(Locale.US).apply { isGroupingUsed = true }
    
    private val ROMAN_NUMERALS = treeMapOf(
        1 to "I",
        4 to "IV",
        5 to "V",
        9 to "IX",
        10 to "X",
        40 to "XL",
        50 to "L",
        90 to "XC",
        100 to "C",
        400 to "CD",
        500 to "D",
        900 to "CM",
        1000 to "M"
    )
    
    fun getEnergyString(energy: Long): String =
        if (USE_METRIC_PREFIXES)
            getSoleString(IGNORED_ENERGY_PREFIXES, BigDecimal(energy), "J")
        else "${NUMBER_FORMAT.format(energy)} J"
    
    fun getEnergyString(energy: Long, maxEnergy: Long): String =
        if (USE_METRIC_PREFIXES)
            getOutOfString(IGNORED_ENERGY_PREFIXES, BigDecimal(energy), BigDecimal(maxEnergy), "J")
        else "${NUMBER_FORMAT.format(energy)} J / ${NUMBER_FORMAT.format(maxEnergy)} J"
    
    fun getFluidString(fluid: Long): String =
        if (USE_METRIC_PREFIXES)
            getSoleString(IGNORED_FLUID_PREFIXES, BigDecimal(fluid).divide(1000.toBigDecimal()), "B")
        else "${NUMBER_FORMAT.format(fluid)} mB"
    
    fun getFluidString(fluid: Long, maxFluid: Long): String =
        if (USE_METRIC_PREFIXES)
            getOutOfString(IGNORED_FLUID_PREFIXES, BigDecimal(fluid).divide(1000.toBigDecimal()), BigDecimal(maxFluid).divide(1000.toBigDecimal()), "B")
        else "${NUMBER_FORMAT.format(fluid)} mB / ${NUMBER_FORMAT.format(maxFluid)} mB"
    
    private fun getSoleString(ignoredPrefixes: BooleanArray, number: BigDecimal, unit: String): String {
        val closest = MetricPrefix.findBestPrefix(number, ignoredPrefixes)
        val prefix = closest.second
        val resultNumber = closest.first.setScale(2, RoundingMode.HALF_UP)
            .let { if (prefix == MetricPrefix.NONE) it.stripTrailingZeros() else it }
            .toPlainString()
        return "$resultNumber ${prefix.prefixSymbol}$unit"
    }
    
    private fun getOutOfString(ignoredPrefixes: BooleanArray, number: BigDecimal, maxNumber: BigDecimal, unit: String): String {
        val bestMaxNumber = MetricPrefix.findBestPrefix(maxNumber, ignoredPrefixes)
        val prefix = bestMaxNumber.second
        
        val prefixedNumber = if (number == BigDecimal.ZERO) "0" else
            number.divide(prefix.number)
                .setScale(2, RoundingMode.HALF_UP)
                .let { if (prefix == MetricPrefix.NONE) it.stripTrailingZeros() else it }
                .toPlainString()
        
        val prefixedMaxNumber = bestMaxNumber.first
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
        
        val prefixedUnit = "${prefix.prefixSymbol}$unit"
        return "§r$prefixedNumber $prefixedUnit / $prefixedMaxNumber $prefixedUnit"
    }
    
    fun getRomanNumeral(number: Int): String {
        val l = ROMAN_NUMERALS.floorKey(number)
        if (number == l)
            return ROMAN_NUMERALS[number]!!
        return ROMAN_NUMERALS[l]!! + getRomanNumeral(number - l)
    }
    
}