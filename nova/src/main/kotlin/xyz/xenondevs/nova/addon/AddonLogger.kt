package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.LOGGER
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

internal class AddonLogger(name: String) : Logger("Nova", null) {
    
    private val prefix = "[$name] "
    
    init {
        parent = LOGGER
        level = Level.ALL
    }
    
    override fun log(record: LogRecord) {
        record.message = prefix + record.message
        super.log(record)
    }
    
}