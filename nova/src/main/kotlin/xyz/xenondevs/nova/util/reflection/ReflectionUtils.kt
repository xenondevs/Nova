@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package xyz.xenondevs.nova.util.reflection

import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.commons.collections.mapToArray
import xyz.xenondevs.nova.util.ServerSoftware
import xyz.xenondevs.nova.util.ServerUtils
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CLASS_LOADER_DEFINE_CLASS_METHOD
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.security.ProtectionDomain
import kotlin.reflect.KClass

internal fun ClassLoader.defineClass(name: String, bytecode: ByteArray, protectionDomain: ProtectionDomain?) =
    CLASS_LOADER_DEFINE_CLASS_METHOD.invoke(this, name, bytecode, 0, bytecode.size, protectionDomain) as Class<*>

internal fun ClassLoader.defineClass(clazz: Class<*>) =
    defineClass(clazz.name, VirtualClassPath[clazz].assemble(true), clazz.protectionDomain)

internal fun ClassLoader.defineClass(clazz: KClass<*>) =
    defineClass(clazz.java)

internal fun ClassLoader.defineClass(classWrapper: ClassWrapper) =
    defineClass(classWrapper.name.replace('/', '.'), classWrapper.assemble(true), null)

internal object ReflectionUtils {
    
    @JvmStatic
    fun getClass(name: String): Class<*> {
        return Class.forName(name)
    }
    
    @JvmStatic
    fun getMethod(clazz: KClass<*>, declared: Boolean, methodName: String, vararg args: KClass<*>): Method =
        getMethod(clazz.java, declared, methodName, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String, vararg args: KClass<*>): Method =
        getMethod(clazz, declared, methodName, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String): Method =
        getMethod(clazz, declared, methodName, *arrayOf<Class<*>>())
    
    @JvmStatic
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String, vararg args: Class<*>): Method {
        val method = if (declared) clazz.getDeclaredMethod(methodName, *args) else clazz.getMethod(methodName, *args)
        if (declared) method.isAccessible = true
        return method
    }
    
    @JvmStatic
    fun getMethod(clazz: KClass<*>, methodName: String, vararg args: KClass<*>): Method =
        getMethod(clazz.java, methodName, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun getMethod(clazz: Class<*>, methodName: String, vararg args: KClass<*>): Method =
        getMethod(clazz, methodName, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun getMethod(clazz: Class<*>, methodName: String): Method =
        getMethod(clazz, methodName, *arrayOf<Class<*>>())
    
    @JvmStatic
    fun getMethod(clazz: Class<*>, methodName: String, vararg args: Class<*>): Method {
        val method = clazz.getDeclaredMethod(methodName, *args)
        method.isAccessible = true
        return method
    }
    
    @JvmStatic
    fun getMethodByName(clazz: KClass<*>, methodName: String): Method =
        getMethodByName(clazz.java, methodName)
    
    @JvmStatic
    fun getMethodByName(clazz: Class<*>, methodName: String): Method {
        val method = clazz.declaredMethods.first { it.name == methodName }
        method.isAccessible = true
        return method
    }
    
    @JvmStatic
    fun getMethodHandle(clazz: KClass<*>, methodName: String, vararg args: KClass<*>): MethodHandle {
        val method = getMethod(clazz, methodName, *args)
        return MethodHandles.privateLookupIn(clazz.java, MethodHandles.lookup()).unreflect(method)
    }
    
    @JvmStatic
    fun getGetterHandle(clazz: KClass<*>, fieldName: String): MethodHandle {
        val field = getField(clazz, fieldName)
        return MethodHandles.privateLookupIn(clazz.java, MethodHandles.lookup()).unreflectGetter(field)
    }
    
    @JvmStatic
    fun getSetterHandle(clazz: KClass<*>, fieldName: String): MethodHandle {
        val field = getField(clazz, fieldName)
        return MethodHandles.privateLookupIn(clazz.java, MethodHandles.lookup()).unreflectSetter(field)
    }
    
    @JvmStatic
    fun <C : Any> getConstructor(clazz: KClass<C>, declared: Boolean, vararg args: KClass<*>): Constructor<C> =
        getConstructor(clazz.java, declared, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun <C : Any> getConstructor(clazz: Class<C>, declared: Boolean, vararg args: KClass<*>): Constructor<C> =
        getConstructor(clazz, declared, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun <C> getConstructor(clazz: Class<C>, declared: Boolean, vararg args: Class<*>): Constructor<C> {
        val constructor = if (declared) clazz.getDeclaredConstructor(*args) else clazz.getConstructor(*args)
        if (declared) constructor.isAccessible = true
        return constructor
    }
    
    @JvmStatic
    fun <C : Any> getConstructor(clazz: KClass<C>, vararg args: KClass<*>): Constructor<C> =
        getConstructor(clazz.java, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun <C : Any> getConstructor(clazz: Class<C>, vararg args: KClass<*>): Constructor<C> =
        getConstructor(clazz, *args.mapToArray(KClass<*>::java))
    
    @JvmStatic
    fun <C> getConstructor(clazz: Class<C>, vararg args: Class<*>): Constructor<C> {
        val constructor = clazz.getDeclaredConstructor(*args)
        constructor.isAccessible = true
        return constructor
    }
    
    @JvmStatic
    fun getConstructorMethodHandle(clazz: KClass<*>, vararg args: KClass<*>): MethodHandle {
        val constructor = getConstructor(clazz, *args)
        return MethodHandles.privateLookupIn(clazz.java, MethodHandles.lookup()).unreflectConstructor(constructor)
    }
    
    @JvmStatic
    fun getField(clazz: KClass<*>, declared: Boolean, name: String): Field =
        getField(clazz.java, declared, name)
    
    @JvmStatic
    fun getField(clazz: Class<*>, declared: Boolean, name: String): Field {
        val field = if (declared) clazz.getDeclaredField(name) else clazz.getField(name)
        if (declared) field.isAccessible = true
        return field
    }
    
    @JvmStatic
    fun getField(clazz: KClass<*>, name: String): Field =
        getField(clazz.java, name)
    
    @JvmStatic
    fun getField(clazz: Class<*>, name: String): Field {
        val field = clazz.getDeclaredField(name)
        field.isAccessible = true
        return field
    }
    
    @JvmStatic
    fun getFieldOrNull(clazz: Class<*>, name: String): Field? {
        try {
            return getField(clazz, name)
        } catch (_: NoSuchFieldException) {
            return null
        }
    }
    
    @JvmStatic
    fun getServerSoftwareField(clazz: KClass<*>, name: String, serverSoftware: ServerSoftware): Field? {
        if (serverSoftware !in ServerUtils.SERVER_SOFTWARE.superSoftwares)
            return null
        return getField(clazz, name)
    }
    
    @JvmStatic
    fun getServerSoftwareField(clazz: Class<*>, name: String, serverSoftware: ServerSoftware): Field? {
        if (serverSoftware !in ServerUtils.SERVER_SOFTWARE.superSoftwares)
            return null
        return getField(clazz, name)
    }
    
}