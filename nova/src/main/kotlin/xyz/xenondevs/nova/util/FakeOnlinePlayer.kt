package xyz.xenondevs.nova.util

import org.bukkit.Location
import org.bukkit.OfflinePlayer
import org.bukkit.World
import org.bukkit.entity.Player
import org.bukkit.permissions.Permission
import org.objectweb.asm.Opcodes
import xyz.xenondevs.bytebase.ClassWrapperLoader
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.MethodNode
import xyz.xenondevs.bytebase.util.internalName
import xyz.xenondevs.nova.NOVA
import xyz.xenondevs.nova.integration.permission.PermissionManager
import xyz.xenondevs.nova.util.data.AsmUtils
import java.io.File
import java.lang.reflect.Constructor
import java.util.*

/**
 * A [Player] which will throw an [UnsupportedOperationException]
 * when methods which aren't supported by [OfflinePlayer] are called.
 *
 * This [Player] is also granted access to the [hasPermission]
 * method via permission integrations.
 */
abstract class FakeOnlinePlayer(
    private val offlinePlayer: OfflinePlayer,
    private val location: Location
) : Player, OfflinePlayer by offlinePlayer {
    
    override fun hasPermission(name: String): Boolean {
        return PermissionManager.hasPermission(world, uniqueId, name).get()
    }
    
    override fun hasPermission(perm: Permission): Boolean {
        return PermissionManager.hasPermission(world, uniqueId, perm.name).get()
    }
    
    override fun isPermissionSet(name: String): Boolean {
        return true
    }
    
    override fun isPermissionSet(perm: Permission): Boolean {
        return true
    }
    
    override fun getWorld(): World {
        return location.world!!
    }
    
    @Deprecated("Deprecated in Java", ReplaceWith("locale()"))
    override fun getLocale(): String {
        return "en_us"
    }
    
    override fun locale(): Locale {
        return Locale.US
    }
    
    override fun getLocation(): Location {
        return location
    }
    
    override fun isOnline(): Boolean {
        return true
    }
    
    override fun getName(): String {
        return offlinePlayer.name ?: "OfflinePlayer"
    }
    
    companion object {
        
        private val ctor: Constructor<FakeOnlinePlayer> by lazy(::buildImpl)
        
        fun create(offlinePlayer: OfflinePlayer, location: Location): FakeOnlinePlayer {
            return ctor.newInstance(offlinePlayer, location)
        }
        
        @Suppress("UNCHECKED_CAST")
        private fun buildImpl(): Constructor<FakeOnlinePlayer> {
            val classWrapper = ClassWrapper("xyz/xenondevs/nova/util/FakeOnlinePlayerImpl.class").apply {
                access = Opcodes.ACC_PUBLIC
                superName = "xyz/xenondevs/nova/util/FakeOnlinePlayer"
                
                val constructor = MethodNode(
                    Opcodes.ACC_PUBLIC,
                    "<init>",
                    "(Lorg/bukkit/OfflinePlayer;Lorg/bukkit/Location;)V"
                ) {
                    aLoad(0)
                    aLoad(1)
                    aLoad(2)
                    invokeSpecial(superName, "<init>", "(Lorg/bukkit/OfflinePlayer;Lorg/bukkit/Location;)V")
                    _return()
                }
                methods.add(constructor)
                
                AsmUtils.listNonOverriddenMethods(
                    VirtualClassPath[FakeOnlinePlayer::class],
                    OfflinePlayer::class, Any::class
                ).forEach {
                    val methodNode = MethodNode(
                        Opcodes.ACC_PUBLIC,
                        it.name,
                        it.desc
                    ) {
                        new(UnsupportedOperationException::class)
                        dup()
                        ldc("Player is not online")
                        invokeSpecial(UnsupportedOperationException::class.internalName, "<init>", "(Ljava/lang/String;)V")
                        aThrow()
                    }
                    
                    methods.add(methodNode)
                }
            }
            
            File("FakeOnlinePlayer.class").writeBytes(classWrapper.assemble(false))
            
            return ClassWrapperLoader(NOVA::class.java.classLoader)
                .loadClass(classWrapper)
                .getConstructor(OfflinePlayer::class.java, Location::class.java) as Constructor<FakeOnlinePlayer>
        }
        
    }
    
}