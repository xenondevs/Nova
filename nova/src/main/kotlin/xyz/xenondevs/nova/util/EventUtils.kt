@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.util

import org.bukkit.Bukkit
import org.bukkit.craftbukkit.CraftServer
import org.bukkit.event.Event
import org.bukkit.event.Event.Result
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.RegisteredListener
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.patch.impl.misc.EventPreventionPatch
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HANDLER_LIST_HANDLERS_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.HANDLER_LIST_HANDLER_SLOTS_FIELD
import java.util.*
import kotlin.reflect.KClass

fun Action.isClickBlock() = this == Action.LEFT_CLICK_BLOCK || this == Action.RIGHT_CLICK_BLOCK

fun Action.isClickAir() = this == Action.LEFT_CLICK_AIR || this == Action.RIGHT_CLICK_AIR

fun PlayerInteractEvent.isCompletelyDenied() = useInteractedBlock() == Result.DENY && useItemInHand() == Result.DENY

val PlayerInteractEvent.handItems: Array<ItemStack>
    get() = arrayOf(player.inventory.itemInMainHand, player.inventory.itemInOffHand)

val PlayerInteractEvent.hands: Array<Pair<EquipmentSlot, ItemStack>>
    get() = arrayOf(EquipmentSlot.HAND to player.inventory.itemInMainHand, EquipmentSlot.OFF_HAND to player.inventory.itemInOffHand)

fun callEvent(event: Event) = Bukkit.getPluginManager().callEvent(event)

fun Listener.registerEvents() {
    Bukkit.getPluginManager().registerEvents(this, NOVA)
}

fun Listener.unregisterEvents() {
    HandlerList.unregisterAll(this)
}

fun Listener.registerEventsExcept(vararg eventClasses: KClass<out Event>) {
    registerEventsExcept(*eventClasses.mapToArray(KClass<out Event>::java))
}

fun Listener.registerEventsExcept(vararg eventClasses: Class<out Event>) {
    registerEventsExcept(this, *eventClasses)
}

fun Listener.registerEvent(eventClass: KClass<out Event>) {
    registerEvent(eventClass.java)
}

fun Listener.registerEvent(eventClass: Class<out Event>) {
    registerEvent(this, eventClass)
}

fun Listener.registerEventFirst(eventClass: KClass<out Event>) {
    registerEventFirst(eventClass.java)
}

fun Listener.registerEventFirst(eventClass: Class<out Event>) {
    registerEventFirst(this, eventClass)
}

fun Listener.registerEventsFirst() {
    registerEventsFirst(this)
}

@JvmName("registerEvents1")
fun registerEvents(listener: Listener) {
    Bukkit.getPluginManager().registerEvents(listener, NOVA)
}

@JvmName("registerEventFirst1")
fun registerEventFirst(listener: Listener, event: Class<out Event>) {
    val registeredListeners = createRegisteredListener(listener, NOVA)[event]!!
    registerRegisteredListenerFirst(registeredListeners, event)
}

@JvmName("registerEventsFirst1")
fun registerEventsFirst(listener: Listener) {
    val registeredListeners = createRegisteredListener(listener, NOVA)
    registeredListeners.forEach { registerRegisteredListenerFirst(it.value, it.key) }
}

private fun createRegisteredListener(listener: Listener, plugin: Plugin): Map<Class<out Event>, Set<RegisteredListener>> {
    val paperPluginManager = (Bukkit.getServer() as CraftServer).paperPluginManager
    val paperEventManager = ReflectionRegistry.PAPER_PLUGIN_MANAGER_IMPL_PAPER_EVENT_MANAGER_FIELD.get(paperPluginManager)
    return ReflectionRegistry.PAPER_EVENT_MANAGER_CREATE_REGISTERED_LISTENERS_METHOD.invoke(paperEventManager, listener, plugin) as Map<Class<out Event>, Set<RegisteredListener>>
}

private fun registerRegisteredListenerFirst(registeredListeners: Set<RegisteredListener>, event: Class<out Event>) {
    val handlerList = event.getMethod("getHandlerList").invoke(null) as HandlerList
    val handlerSlots = HANDLER_LIST_HANDLER_SLOTS_FIELD.get(handlerList) as EnumMap<EventPriority, ArrayList<RegisteredListener>>
    
    val listeners = ArrayList(registeredListeners)
    handlerSlots[EventPriority.LOWEST]?.let(listeners::addAll)
    handlerSlots[EventPriority.LOWEST] = listeners
    
    HANDLER_LIST_HANDLERS_FIELD.set(handlerList, null)
}

@JvmName("registerEvent1")
fun registerEvent(listener: Listener, event: Class<out Event>) {
    val registeredListeners = createRegisteredListener(listener, NOVA)[event]!!
    val handlerList = event.getMethod("getHandlerList").invoke(null) as HandlerList
    handlerList.registerAll(registeredListeners)
}

@JvmName("registerEventsExcept1")
fun registerEventsExcept(listener: Listener, vararg eventClasses: Class<out Event>) {
    val registeredListeners = createRegisteredListener(listener, NOVA)
    
    registeredListeners.forEach { (clazz, listeners) ->
        if (clazz in eventClasses)
            return@forEach
        
        val handlerList = clazz.getMethod("getHandlerList").invoke(null) as HandlerList
        handlerList.registerAll(listeners)
    }
}

inline fun preventEvents(run: () -> Unit) {
    EventPreventionPatch.dropAll = true
    try {
        run()
    } finally {
        EventPreventionPatch.dropAll = false
    }
}