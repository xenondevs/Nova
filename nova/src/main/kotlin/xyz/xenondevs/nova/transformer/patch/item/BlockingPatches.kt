package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResultHolder
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.UseAnim
import net.minecraft.world.level.Level
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.MethodInsnNode
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.bytebase.jvm.VirtualClassPath
import xyz.xenondevs.bytebase.util.calls
import xyz.xenondevs.bytebase.util.replaceFirst
import xyz.xenondevs.nova.item.behavior.Blocking
import xyz.xenondevs.nova.transformer.MultiTransformer
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry

@Suppress("unused")
internal object BlockingPatches : MultiTransformer(Item::class, Player::class) {
    
    override fun transform() {
        patchUse()
        patchUseDuration()
        patchUseAnimation()
        patchHurtCurrentlyUsedShield()
    }
    
    private fun patchUse() {
        VirtualClassPath[Item::use]
            .instructions = buildInsnList {
            aLoad(0)
            aLoad(1)
            aLoad(2)
            aLoad(3)
            invokeStatic(::use)
            areturn()
        }
    }
    
    @JvmStatic
    fun use(item: Item, world: Level, user: Player, hand: InteractionHand): InteractionResultHolder<ItemStack> {
        val itemStack: ItemStack = user.getItemInHand(hand)
        
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            if (novaItem.hasBehavior(Blocking::class)) {
                user.startUsingItem(hand)
                return InteractionResultHolder.consume(itemStack)
            }
        }
        
        // -- nms logic --
        return if (item.isEdible) {
            if (user.canEat(item.foodProperties!!.canAlwaysEat())) {
                user.startUsingItem(hand)
                InteractionResultHolder.consume(itemStack)
            } else {
                InteractionResultHolder.fail(itemStack)
            }
        } else {
            InteractionResultHolder.pass(user.getItemInHand(hand))
        }
        // ----
    }
    
    private fun patchUseDuration() {
        VirtualClassPath[Item::getUseDuration]
            .instructions = buildInsnList {
            aLoad(1)
            aLoad(0)
            invokeStatic(::getUseDuration)
            ireturn()
        }
    }
    
    @JvmStatic
    fun getUseDuration(stack: ItemStack, item: Item): Int {
        val novaItem = stack.novaItem
        if (novaItem != null) {
            if (novaItem.hasBehavior(Blocking::class)) {
                return 72000
            }
        }
        
        // -- nms logic --
        return if (stack.item.isEdible) {
            if (item.foodProperties!!.isFastFood) 16 else 32
        } else {
            0
        }
        // ----
    }
    
    private fun patchUseAnimation() {
        VirtualClassPath[Item::getUseAnimation]
            .instructions = buildInsnList {
            aLoad(1)
            aLoad(0)
            invokeStatic(::getUseAnimation)
            areturn()
        }
    }
    
    @JvmStatic
    fun getUseAnimation(stack: ItemStack, item: Item): UseAnim {
        val novaItem = stack.novaItem
        if (novaItem != null) {
            if (novaItem.hasBehavior(Blocking::class)) {
                return UseAnim.BLOCK
            }
        }
        
        return if (stack.item.isEdible) UseAnim.EAT else UseAnim.NONE  // nms logic
    }
    
    private fun patchHurtCurrentlyUsedShield() {
        VirtualClassPath[ReflectionRegistry.PLAYER_HURT_CURRENTLY_USED_SHIELD_METHOD].replaceFirst(
            2, 0,
            buildInsnList {
                invokeStatic(BlockingPatches::isShieldItem)
            }
        ) { it.opcode == Opcodes.INVOKEVIRTUAL && (it as MethodInsnNode).calls(ReflectionRegistry.ITEM_STACK_IS_METHOD) }
    }
    
    @JvmStatic
    fun isShieldItem(player: Player): Boolean {
        return player.useItem.`is`(Items.SHIELD) || (player.useItem.novaItem?.hasBehavior(Blocking::class) ?: false)
    }
    
}