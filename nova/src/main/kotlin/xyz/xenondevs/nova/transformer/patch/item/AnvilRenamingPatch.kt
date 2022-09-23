package xyz.xenondevs.nova.transformer.patch.item

import org.apache.commons.lang3.StringUtils
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.i18n.LocaleManager
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.item.novaMaterial
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethodByName
import kotlin.reflect.jvm.javaMethod
import net.minecraft.world.item.ItemStack as MojangStack

@Suppress("unused")
internal object AnvilRenamingPatch : MethodTransformer(ReflectionRegistry.ANVIL_MENU_CREATE_RESULT_METHOD, computeFrames = true) {
    
    override fun transform() {
        methodNode.replaceFirst(0, 0, buildInsnList {
            aLoad(1)
            invokeStatic(getMethodByName(AnvilRenamingPatch::class.java, false, "isNotCustomName"))
        }) {
            it.opcode == Opcodes.INVOKESTATIC && (it as MethodInsnNode).calls(StringUtils::isBlank.javaMethod!!)
                && it.previous?.opcode == Opcodes.GETFIELD
        }
    }
    
    @JvmStatic
    fun isNotCustomName(name: String, itemStack: MojangStack): Boolean {
        val novaMaterial = itemStack.novaMaterial
        if (novaMaterial != null
            && !itemStack.hasCustomHoverName()
            && name in LocaleManager.getAllTranslations(novaMaterial.localizedName)
        ) return true
        
        return name.isBlank()
    }
    
}