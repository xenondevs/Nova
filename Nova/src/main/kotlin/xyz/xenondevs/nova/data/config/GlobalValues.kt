package xyz.xenondevs.nova.data.config

object GlobalValues  {
    
    val USE_METRIC_PREFIXES by configReloadable { DEFAULT_CONFIG.getBoolean("use_metric_prefixes") }
    val DROP_EXCESS_ON_GROUND by configReloadable { DEFAULT_CONFIG.getBoolean("performance.drop_excess_on_ground") }
    val BLOCK_BREAK_EFFECTS by configReloadable { DEFAULT_CONFIG.getBoolean("performance.block_break_effects") }
    
}