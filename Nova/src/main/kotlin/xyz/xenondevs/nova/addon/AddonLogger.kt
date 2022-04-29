package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.LOGGER
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class AddonLogger(name: String) : Logger(name, null) {
    
    private val prefix = "[Nova] [$name] "
    
    init {
        parent = LOGGER
        level = Level.ALL
    }
    
    override fun log(record: LogRecord) {
        record.message = prefix + record.message
        super.log(record)
    }
    
}