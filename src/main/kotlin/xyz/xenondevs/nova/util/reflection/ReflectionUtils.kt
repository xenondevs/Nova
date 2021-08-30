package xyz.xenondevs.nova.util.reflection

import org.bukkit.Bukkit
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CALLABLE_REFERENCE_RECEIVER_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CB_PACKAGE_PATH
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.K_PROPERTY_1_GET_DELEGATE_METHOD
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KProperty0
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("MemberVisibilityCanBePrivate", "UNCHECKED_CAST")
object ReflectionUtils {
    
    fun getCB(): String {
        val path = Bukkit.getServer().javaClass.getPackage().name
        val version = path.substring(path.lastIndexOf(".") + 1)
        return "org.bukkit.craftbukkit.$version."
    }
    
    fun getCB(name: String): String {
        return CB_PACKAGE_PATH + name
    }
    
    fun getCBClass(name: String): Class<*> {
        return Class.forName(getCB(name))
    }
    
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String, vararg args: Class<*>): Method {
        val method = if (declared) clazz.getDeclaredMethod(methodName, *args) else clazz.getMethod(methodName, *args)
        if (declared) method.isAccessible = true
        return method
    }
    
    fun getConstructor(clazz: Class<*>, declared: Boolean, vararg args: Class<*>): Constructor<*> {
        return if (declared) clazz.getDeclaredConstructor(*args) else clazz.getConstructor(*args)
    }
    
    fun getField(clazz: Class<*>, declared: Boolean, name: String): Field {
        val field = if (declared) clazz.getDeclaredField(name) else clazz.getField(name)
        if (declared) field.isAccessible = true
        return field
    }
    
    val KProperty0<*>.isLazyInitialized: Boolean
        get() {
            val delegate = actualDelegate
            return if (delegate is Lazy<*>) delegate.isInitialized() else throw IllegalStateException("Property doesn't delegate to Lazy")
        }
    
    val KProperty0<*>.actualDelegate: Any?
        get() {
            val receiver = CALLABLE_REFERENCE_RECEIVER_FIELD.get(this)
            if (receiver == CallableReference.NO_RECEIVER) {
                isAccessible = true
                return this.getDelegate()
            }
            
            val property = receiver::class.memberProperties.first { it.name == name }
            property.isAccessible = true
            return K_PROPERTY_1_GET_DELEGATE_METHOD.invoke(property, receiver)
        }
    
}