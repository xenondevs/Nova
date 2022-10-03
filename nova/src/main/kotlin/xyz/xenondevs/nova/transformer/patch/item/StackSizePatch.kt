package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.entity.item.ItemEntity
import org.bukkit.craftbukkit.v1_19_R1.inventory.CraftInventoryPlayer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.addItemCorrectly
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import net.minecraft.world.entity.player.Inventory as MojangInventory
import net.minecraft.world.item.ItemStack as MojangStack

internal object StackSizePatch : MultiTransformer(setOf(ItemEntity::class), computeFrames = true) {
    
    override fun transform() {
        VirtualClassPath[ReflectionRegistry.ITEM_ENTITY_PLAYER_TOUCH_METHOD]
            .replaceFirst(0, 0, buildInsnList { 
                invokeStatic(ReflectionUtils.getMethodByName(StackSizePatch::class.java, false, "addItemCorrectly"))
            }) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ReflectionRegistry.INVENTORY_ADD_METHOD) }
    }
    
    @JvmStatic
    fun addItemCorrectly(inventory: MojangInventory, itemStack: MojangStack): Boolean {
        val bukkitInventory = CraftInventoryPlayer(inventory)
        
        val count = itemStack.count
        itemStack.count = bukkitInventory.addItemCorrectly(itemStack.bukkitMirror)
        return count != itemStack.count
    }
    
}