package xyz.xenondevs.nova.data.config

object GlobalValues {
    
    val USE_METRIC_PREFIXES = DEFAULT_CONFIG.getBoolean("use_metric_prefixes")
    val DROP_EXCESS_ON_GROUND = DEFAULT_CONFIG.getBoolean("performance.drop_excess_on_ground")
    val BLOCK_BREAK_EFFECTS = DEFAULT_CONFIG.getBoolean("performance.block_break_effects")
    
}