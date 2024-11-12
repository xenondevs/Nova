package xyz.xenondevs.nova.util.data

import org.slf4j.Logger

internal fun Logger.logExceptionMessages(log: Logger.(String) -> Unit, message: String, throwable: Throwable) {
    log(message)
    
    var t: Throwable? = throwable
    var depth = 1
    
    while (t != null) {
        log("  ".repeat(depth) + t::class.simpleName + ": " + t.message)
        
        t = t.cause
        depth++
    }
}