@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package xyz.xenondevs.nova.util.reflection

import com.google.gson.reflect.TypeToken
import jdk.internal.misc.Unsafe
import org.bukkit.Bukkit
import org.checkerframework.checker.units.qual.C
import xyz.xenondevs.nova.util.mapToArray
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CALLABLE_REFERENCE_RECEIVER_FIELD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CB_PACKAGE_PATH
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.CLASS_LOADER_DEFINE_CLASS_METHOD
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry.K_PROPERTY_1_GET_DELEGATE_METHOD
import java.lang.reflect.Array
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.GenericArrayType
import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import java.security.ProtectionDomain
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KClass
import kotlin.reflect.KProperty0
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

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

inline val <reified K, V> Map<K, V>.keyType: Type
    get() = type<K>()

inline val <K, reified V> Map<K, V>.valueType: Type
    get() = type<V>()

@Suppress("UNCHECKED_CAST")
inline val <reified K, V> Map<K, V>.keyClass: Class<K>
    get() = Class.forName(keyType.typeName) as Class<K>

@Suppress("UNCHECKED_CAST")
inline val <K, reified V> Map<K, V>.valueClass: Class<V>
    get() = Class.forName(valueType.typeName) as Class<V>

inline fun <reified T> type(): Type = object : TypeToken<T>() {}.type

val Type.representedClass: Class<*>
    get() = when (this) {
        is ParameterizedType -> rawType as Class<*>
        is WildcardType -> upperBounds[0] as Class<*>
        is GenericArrayType -> Array.newInstance(genericComponentType.representedClass, 0)::class.java
        is Class<*> -> this
        else -> throw IllegalStateException("Type $this is not a class")
    }

val Type.representedKClass: KClass<*>
    get() = representedClass.kotlin

fun <T : Enum<*>> enumValueOf(enumClass: Class<T>, name: String): T =
    enumClass.enumConstants.first { it.name == name }

fun Type.tryTakeUpperBound(): Type {
    return if (this is WildcardType) this.upperBounds[0] else this
}

internal fun ClassLoader.defineClass(name: String, bytecode: ByteArray, protectionDomain: ProtectionDomain) =
    CLASS_LOADER_DEFINE_CLASS_METHOD.invoke(this, name, bytecode, 0, bytecode.size, protectionDomain) as Class<*>

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
    
    fun getClass(name: String): Class<*> {
        return Class.forName(name)
    }
    
    fun getCBClass(name: String): Class<*> {
        return Class.forName(getCB(name))
    }
    
    fun getMethod(clazz: KClass<*>, declared: Boolean, methodName: String, vararg args: KClass<*>): Method =
        getMethod(clazz.java, declared, methodName, *args.mapToArray(KClass<*>::java))
    
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String, vararg args: KClass<*>): Method =
        getMethod(clazz, declared, methodName, *args.mapToArray(KClass<*>::java))
    
    fun getMethod(clazz: Class<*>, declared: Boolean, methodName: String, vararg args: Class<*>): Method {
        val method = if (declared) clazz.getDeclaredMethod(methodName, *args) else clazz.getMethod(methodName, *args)
        if (declared) method.isAccessible = true
        return method
    }
    
    fun getMethodByName(clazz: KClass<*>, declared: Boolean, methodName: String): Method =
        getMethodByName(clazz.java, declared, methodName)
    
    fun getMethodByName(clazz: Class<*>, declared: Boolean, methodName: String): Method {
        val method = if (declared) clazz.declaredMethods.first { it.name == methodName } else clazz.methods.first { it.name == methodName }
        if (declared) method.isAccessible = true
        return method
    }
    
    fun <C : Any> getConstructor(clazz: KClass<C>, declared: Boolean, vararg args: KClass<*>): Constructor<C> =
        getConstructor(clazz.java, declared, *args.mapToArray(KClass<*>::java))
    
    fun <C : Any> getConstructor(clazz: Class<C>, declared: Boolean, vararg args: KClass<*>): Constructor<C> =
        getConstructor(clazz, declared, *args.mapToArray(KClass<*>::java))
    
    fun <C> getConstructor(clazz: Class<C>, declared: Boolean, vararg args: Class<*>): Constructor<C> {
        val constructor = if (declared) clazz.getDeclaredConstructor(*args) else clazz.getConstructor(*args)
        if (declared) constructor.isAccessible = true
        return constructor
    }
    
    fun getField(clazz: KClass<*>, declared: Boolean, name: String): Field =
        getField(clazz.java, declared, name)
    
    fun getField(clazz: Class<*>, declared: Boolean, name: String): Field {
        val field = if (declared) clazz.getDeclaredField(name) else clazz.getField(name)
        if (declared) field.isAccessible = true
        return field
    }
    
    internal fun setFinalField(field: Field, obj: Any, value: Any?) {
        val unsafe = Unsafe.getUnsafe()
        val offset = unsafe.objectFieldOffset(field)
        unsafe.putReference(obj, offset, value)
    }
    
    internal fun setStaticFinalField(field: Field, value: Any?) {
        val unsafe = Unsafe.getUnsafe()
        val base = unsafe.staticFieldBase(field)
        val offset = unsafe.staticFieldOffset(field)
        unsafe.putReference(base, offset, value)
    }
    
}