package xyz.xenondevs.nova.addon

import xyz.xenondevs.nova.LOGGER
import xyz.xenondevs.nova.util.ServerSoftware
import xyz.xenondevs.nova.util.ServerUtils
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class AddonLogger(name: String) : Logger("Nova", null) {
    
    // On Paper, [logger.name] is appended in front of each log message
    private val prefix = if (ServerSoftware.PAPER in ServerUtils.SERVER_SOFTWARE.tree) "[$name] " else "[Nova] [$name] "
    
    init {
        parent = LOGGER
        level = Level.ALL
    }
    
    override fun log(record: LogRecord) {
        record.message = prefix + record.message
        super.log(record)
    }
    
}