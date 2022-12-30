package xyz.xenondevs.nova.util.data

import java.util.logging.Level
import java.util.logging.Logger

fun Logger.logExceptionMessages(level: Level, message: String, throwable: Throwable) {
    log(level, message)
    
    var t: Throwable? = throwable
    var depth = 1
    
    while (t != null) {
        log(level, "  ".repeat(depth) + t::class.simpleName + ": " + t.message)
        
        t = t.cause
        depth++
    }
}