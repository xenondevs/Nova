@file:Suppress("UNCHECKED_CAST")

package xyz.xenondevs.nova.integration.worldedit.fawe

import com.sk89q.worldedit.event.extent.EditSessionEvent
import com.sk89q.worldedit.extent.AbstractDelegateExtent
import com.sk89q.worldedit.world.block.BlockStateHolder
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.LabelNode
import xyz.xenondevs.bytebase.ClassWrapperLoader
import xyz.xenondevs.bytebase.jvm.ClassWrapper
import xyz.xenondevs.bytebase.util.MethodNode
import xyz.xenondevs.nova.integration.worldedit.NovaBlockExtent
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import java.lang.reflect.Constructor

internal object FAWENovaBlockExtentFactory {
    
    private val SET_NOVA_BLOCK_METHOD = ReflectionUtils.getMethodByName(NovaBlockExtent::class.java, true, "setNovaBlock")
    private val SET_BLOCK_METHOD = ReflectionUtils.getMethod(AbstractDelegateExtent::class.java, true, "setBlock", Int::class.java, Int::class.java, Int::class.java, BlockStateHolder::class.java)
    
    private val BLOCK_EXTENT_CONSTRUCTOR: Constructor<NovaBlockExtent>
    
    init {
        val classWrapper = ClassWrapper("FAWENovaBlockExtent.class").apply {
            access = Opcodes.ACC_PUBLIC
            superName = "xyz/xenondevs/nova/integration/worldedit/NovaBlockExtent"
            
            val constructor = MethodNode(
                Opcodes.ACC_PUBLIC,
                "<init>",
                "(Lcom/sk89q/worldedit/event/extent/EditSessionEvent;)V"
            ) {
                aLoad(0)
                aLoad(1)
                invokeSpecial(superName, "<init>", "(Lcom/sk89q/worldedit/event/extent/EditSessionEvent;)V")
                _return()
            }
            
            val setBlock = MethodNode(
                Opcodes.ACC_PUBLIC,
                "setBlock",
                "(IIILcom/sk89q/worldedit/world/block/BlockStateHolder;)Z"
            ) {
                val startLabel = LabelNode()
                val retTrue = LabelNode()
                val callSuper = LabelNode()
    
                add(startLabel)
                aLoad(0)
                iLoad(1)
                iLoad(2)
                iLoad(3)
                aLoad(4)
                invokeVirtual(SET_NOVA_BLOCK_METHOD)
                ifeq(callSuper)
    
                add(retTrue)
                ldc(1)
                ireturn()
    
                add(callSuper)
                aLoad(0)
                iLoad(1)
                iLoad(2)
                iLoad(3)
                aLoad(4)
                invokeSpecial(SET_BLOCK_METHOD)
                ireturn()
            }
            
            methods.add(constructor)
            methods.add(setBlock)
        }
        
        BLOCK_EXTENT_CONSTRUCTOR = ClassWrapperLoader(javaClass.classLoader)
            .loadClass(classWrapper)
            .getConstructor(EditSessionEvent::class.java) as Constructor<NovaBlockExtent>
    }
    
    fun newInstance(event: EditSessionEvent): NovaBlockExtent {
        return BLOCK_EXTENT_CONSTRUCTOR.newInstance(event)
    }
    
}