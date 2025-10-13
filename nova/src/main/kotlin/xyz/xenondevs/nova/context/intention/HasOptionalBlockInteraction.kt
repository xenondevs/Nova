@file:Suppress("PropertyName")

package xyz.xenondevs.nova.context.intention

import org.bukkit.block.BlockFace
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.Nova
import xyz.xenondevs.nova.context.Autofiller
import xyz.xenondevs.nova.context.ContextParamType
import xyz.xenondevs.nova.context.DefaultingContextParamType
import xyz.xenondevs.nova.util.BlockFaceUtils
import xyz.xenondevs.nova.util.Key

/**
 * A context intention that has optional parameters about an interaction on or with a block.
 */
interface HasOptionalBlockInteraction<I : HasOptionalBlockInteraction<I>> : HasOptionalSource<I> {
    
    /**
     * The face of a block that was clicked.
     *
     * Autofilled by:
     * - [SOURCE_PLAYER]
     */
    val CLICKED_BLOCK_FACE: ContextParamType<BlockFace, I>
    
    /**
     * The hand that was used to interact.
     *
     * Autofilled by: none
     */
    val INTERACTION_HAND: ContextParamType<EquipmentSlot, I>
    
    /**
     * The item stack used to interact with a something.
     * Defaults to an empty item stack.
     *
     *  Autofilled by:
     * - [SOURCE_ENTITY] and [INTERACTION_HAND]
     */
    val INTERACTION_ITEM_STACK: DefaultingContextParamType<ItemStack, I>
    
    companion object {
        
        /**
         * Creates a param type for [CLICKED_BLOCK_FACE].
         */
        fun <I : HasOptionalBlockInteraction<I>> clickedBlockFace() =
            ContextParamType<BlockFace, I>(Key(Nova, "clicked_block_face"))
        
        /**
         * Creates a param type for [INTERACTION_HAND].
         */
        fun <I : HasOptionalBlockInteraction<I>> interactionHand() =
            ContextParamType<EquipmentSlot, I>(Key(Nova, "interaction_hand"), validate = EquipmentSlot::isHand)
        
        /**
         * Creates a param type for [INTERACTION_ITEM_STACK].
         */
        fun <I : HasOptionalBlockInteraction<I>> interactionItemStack() =
            DefaultingContextParamType<ItemStack, I>(Key(Nova, "interaction_item_stack"), default = ItemStack.empty(), copy = ItemStack::clone)
        
        fun <I : HasOptionalBlockInteraction<I>> applyDefaults(intention: HasOptionalBlockInteraction<I>) = intention.apply {
            addAutofiller(CLICKED_BLOCK_FACE, Autofiller.from(SOURCE_PLAYER) { BlockFaceUtils.determineBlockFaceLookingAt(it.eyeLocation) })
            addAutofiller(INTERACTION_ITEM_STACK, Autofiller.from(SOURCE_LIVING_ENTITY, INTERACTION_HAND) { entity, hand -> entity.equipment?.getItem(hand) })
        }
        
    }
    
}