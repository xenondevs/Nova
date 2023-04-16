package xyz.xenondevs.nmsutils.internal.util

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method

internal object ReflectionUtils {
    
    fun getClass(name: String): Class<*> {
        return Class.forName(name)
    }
    
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String, vararg args: Class<*>): Method {
        val method = if (declared) clazz.getDeclaredMethod(methodName, *args) else clazz.getMethod(methodName, *args)
        if (declared) method.isAccessible = true
        return method
    }
    
    fun <C> getConstructor(clazz: Class<C>, declared: Boolean, vararg args: Class<*>): Constructor<C> {
        val constructor = if (declared) clazz.getDeclaredConstructor(*args) else clazz.getConstructor(*args)
        if (declared) constructor.isAccessible = true
        return constructor
    }
    
    fun getField(clazz: Class<*>, declared: Boolean, name: String): Field {
        val field = if (declared) clazz.getDeclaredField(name) else clazz.getField(name)
        if (declared) field.isAccessible = true
        return field
    }
    
    fun getFieldOrNull(clazz: Class<*>, declared: Boolean, name: String): Field? {
        return runCatching { 
            val field = if (declared) clazz.getDeclaredField(name) else clazz.getField(name)
            if (declared) field.isAccessible = true
            field
        }.getOrNull()
    }
    
}