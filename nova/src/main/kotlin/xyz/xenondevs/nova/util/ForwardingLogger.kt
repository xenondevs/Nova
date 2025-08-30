package xyz.xenondevs.nova.util

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.event.Level
import org.slf4j.helpers.MessageFormatter

internal class ForwardingLogger(private val delegate: Logger, private val audience: Audience) : Logger by delegate {
    
    private val prefix = "[${delegate.name}] "
    
    private fun sendToAudience(level: Level, formatted: String, t: Throwable?) {
        val color = when (level) {
            Level.TRACE, Level.DEBUG -> NamedTextColor.WHITE
            Level.INFO -> NamedTextColor.GRAY
            Level.WARN -> NamedTextColor.YELLOW
            Level.ERROR -> NamedTextColor.RED
        }
        
        audience.sendMessage(Component.text(prefix + formatted, color))
        if (t != null) {
            audience.sendMessage(Component.text(prefix + t.toString(), color))
            for (line in t.stackTrace) {
                audience.sendMessage(Component.text(prefix + line.toString(), color))
            }
        }
    }
    
    private fun log(level: Level, format: String, vararg args: Any?) {
        when (level) {
            Level.TRACE -> delegate.trace(format, *args)
            Level.DEBUG -> delegate.debug(format, *args)
            Level.INFO  -> delegate.info(format, *args)
            Level.WARN  -> delegate.warn(format, *args)
            Level.ERROR -> delegate.error(format, *args)
        }
        val ft = MessageFormatter.arrayFormat(format, args)
        sendToAudience(level, ft.message, ft.throwable)
    }
    
    private fun log(level: Level, msg: String) {
        when (level) {
            Level.TRACE -> delegate.trace(msg)
            Level.DEBUG -> delegate.debug(msg)
            Level.INFO  -> delegate.info(msg)
            Level.WARN  -> delegate.warn(msg)
            Level.ERROR -> delegate.error(msg)
        }
        sendToAudience(level, msg, null)
    }
    
    private fun log(level: Level, msg: String, t: Throwable?) {
        when (level) {
            Level.TRACE -> delegate.trace(msg, t)
            Level.DEBUG -> delegate.debug(msg, t)
            Level.INFO  -> delegate.info(msg, t)
            Level.WARN  -> delegate.warn(msg, t)
            Level.ERROR -> delegate.error(msg, t)
        }
        sendToAudience(level, msg, t)
    }
    
    private fun log(level: Level, marker: Marker, format: String, vararg args: Any?) {
        when (level) {
            Level.TRACE -> delegate.trace(marker, format, *args)
            Level.DEBUG -> delegate.debug(marker, format, *args)
            Level.INFO  -> delegate.info(marker, format, *args)
            Level.WARN  -> delegate.warn(marker, format, *args)
            Level.ERROR -> delegate.error(marker, format, *args)
        }
        val ft = MessageFormatter.arrayFormat(format, args)
        sendToAudience(level, ft.message, ft.throwable)
    }
    
    private fun log(level: Level, marker: Marker, msg: String) {
        when (level) {
            Level.TRACE -> delegate.trace(marker, msg)
            Level.DEBUG -> delegate.debug(marker, msg)
            Level.INFO  -> delegate.info(marker, msg)
            Level.WARN  -> delegate.warn(marker, msg)
            Level.ERROR -> delegate.error(marker, msg)
        }
        sendToAudience(level, msg, null)
    }
    
    private fun log(level: Level, marker: Marker, msg: String, t: Throwable?) {
        when (level) {
            Level.TRACE -> delegate.trace(marker, msg, t)
            Level.DEBUG -> delegate.debug(marker, msg, t)
            Level.INFO  -> delegate.info(marker, msg, t)
            Level.WARN  -> delegate.warn(marker, msg, t)
            Level.ERROR -> delegate.error(marker, msg, t)
        }
        sendToAudience(level, msg, t)
    }
    
    override fun trace(msg: String) = log(Level.TRACE, msg)
    override fun trace(format: String, arg: Any?) = log(Level.TRACE, format, arg)
    override fun trace(format: String, arg1: Any?, arg2: Any?) = log(Level.TRACE, format, arg1, arg2)
    override fun trace(format: String, vararg arguments: Any?) = log(Level.TRACE, format, *arguments)
    override fun trace(msg: String, t: Throwable?) = log(Level.TRACE, msg, t)
    override fun trace(marker: Marker, msg: String) = log(Level.TRACE, marker, msg)
    override fun trace(marker: Marker, format: String, arg: Any?) = log(Level.TRACE, marker, format, arg)
    override fun trace(marker: Marker, format: String, arg1: Any?, arg2: Any?) = log(Level.TRACE, marker, format, arg1, arg2)
    override fun trace(marker: Marker, format: String, vararg argArray: Any?) = log(Level.TRACE, marker, format, *argArray)
    override fun trace(marker: Marker, msg: String, t: Throwable?) = log(Level.TRACE, marker, msg, t)
    
    override fun debug(msg: String) = log(Level.DEBUG, msg)
    override fun debug(format: String, arg: Any?) = log(Level.DEBUG, format, arg)
    override fun debug(format: String, arg1: Any?, arg2: Any?) = log(Level.DEBUG, format, arg1, arg2)
    override fun debug(format: String, vararg arguments: Any?) = log(Level.DEBUG, format, *arguments)
    override fun debug(msg: String, t: Throwable?) = log(Level.DEBUG, msg, t)
    override fun debug(marker: Marker, msg: String) = log(Level.DEBUG, marker, msg)
    override fun debug(marker: Marker, format: String, arg: Any?) = log(Level.DEBUG, marker, format, arg)
    override fun debug(marker: Marker, format: String, arg1: Any?, arg2: Any?) = log(Level.DEBUG, marker, format, arg1, arg2)
    override fun debug(marker: Marker, format: String, vararg arguments: Any?) = log(Level.DEBUG, marker, format, *arguments)
    override fun debug(marker: Marker, msg: String, t: Throwable?) = log(Level.DEBUG, marker, msg, t)
    
    override fun info(msg: String) = log(Level.INFO, msg)
    override fun info(format: String, arg: Any?) = log(Level.INFO, format, arg)
    override fun info(format: String, arg1: Any?, arg2: Any?) = log(Level.INFO, format, arg1, arg2)
    override fun info(format: String, vararg arguments: Any?) = log(Level.INFO, format, *arguments)
    override fun info(msg: String, t: Throwable?) = log(Level.INFO, msg, t)
    override fun info(marker: Marker, msg: String) = log(Level.INFO, marker, msg)
    override fun info(marker: Marker, format: String, arg: Any?) = log(Level.INFO, marker, format, arg)
    override fun info(marker: Marker, format: String, arg1: Any?, arg2: Any?) = log(Level.INFO, marker, format, arg1, arg2)
    override fun info(marker: Marker, format: String, vararg arguments: Any?) = log(Level.INFO, marker, format, *arguments)
    override fun info(marker: Marker, msg: String, t: Throwable?) = log(Level.INFO, marker, msg, t)
    
    override fun warn(msg: String) = log(Level.WARN, msg)
    override fun warn(format: String, arg: Any?) = log(Level.WARN, format, arg)
    override fun warn(format: String, arg1: Any?, arg2: Any?) = log(Level.WARN, format, arg1, arg2)
    override fun warn(format: String, vararg arguments: Any?) = log(Level.WARN, format, *arguments)
    override fun warn(msg: String, t: Throwable?) = log(Level.WARN, msg, t)
    override fun warn(marker: Marker, msg: String) = log(Level.WARN, marker, msg)
    override fun warn(marker: Marker, format: String, arg: Any?) = log(Level.WARN, marker, format, arg)
    override fun warn(marker: Marker, format: String, arg1: Any?, arg2: Any?) = log(Level.WARN, marker, format, arg1, arg2)
    override fun warn(marker: Marker, format: String, vararg arguments: Any?) = log(Level.WARN, marker, format, *arguments)
    override fun warn(marker: Marker, msg: String, t: Throwable?) = log(Level.WARN, marker, msg, t)
    
    override fun error(msg: String) = log(Level.ERROR, msg)
    override fun error(format: String, arg: Any?) = log(Level.ERROR, format, arg)
    override fun error(format: String, arg1: Any?, arg2: Any?) = log(Level.ERROR, format, arg1, arg2)
    override fun error(format: String, vararg arguments: Any?) = log(Level.ERROR, format, *arguments)
    override fun error(msg: String, t: Throwable?) = log(Level.ERROR, msg, t)
    override fun error(marker: Marker, msg: String) = log(Level.ERROR, marker, msg)
    override fun error(marker: Marker, format: String, arg: Any?) = log(Level.ERROR, marker, format, arg)
    override fun error(marker: Marker, format: String, arg1: Any?, arg2: Any?) = log(Level.ERROR, marker, format, arg1, arg2)
    override fun error(marker: Marker, format: String, vararg arguments: Any?) = log(Level.ERROR, marker, format, *arguments)
    override fun error(marker: Marker, msg: String, t: Throwable?) = log(Level.ERROR, marker, msg, t)
    
}
