package xyz.xenondevs.nova.transformer.patch.item

import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.item.EnchantedBookItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.enchantment.Enchantment.Rarity.*
import net.minecraft.world.item.enchantment.EnchantmentHelper
import org.bukkit.event.inventory.PrepareAnvilEvent
import org.bukkit.inventory.InventoryView
import xyz.xenondevs.bytebase.asm.buildInsnList
import xyz.xenondevs.nova.item.behavior.Damageable.Companion.getDamage
import xyz.xenondevs.nova.item.behavior.Damageable.Companion.getMaxDurability
import xyz.xenondevs.nova.item.behavior.Damageable.Companion.isDamageable
import xyz.xenondevs.nova.item.behavior.Damageable.Companion.isValidRepairItem
import xyz.xenondevs.nova.item.behavior.Damageable.Companion.setDamage
import xyz.xenondevs.nova.transformer.MethodTransformer
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.component.adventure.toPlainText
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.reflection.ReflectionRegistry
import kotlin.math.max
import kotlin.math.min

@Suppress("unused")
internal object AnvilResultPatch : MethodTransformer(AnvilMenu::createResult) {
    
    override fun transform() {
        methodNode.instructions = buildInsnList {
            aLoad(0)
            dup()
            getField(ReflectionRegistry.ITEM_COMBINER_MENU_INPUT_SLOTS_FIELD)
            aLoad(0)
            getField(ReflectionRegistry.ITEM_COMBINER_MENU_PLAYER_FIELD)
            invokeStatic(::createResult)
            _return()
        }
        methodNode.localVariables.clear()
    }
    
    @JvmStatic
    fun createResult(menu: AnvilMenu, inputSlots: Container, player: Player) {
        val inputStack = inputSlots.getItem(0)
        
        if (inputStack.isEmpty) {
            callPrepareAnvilEvent(menu.bukkitView, ItemStack.EMPTY)
            menu.cost.set(-1)
            return
        }
        
        menu.repairItemCountCost = 0
        
        var resultStack = inputStack.copy()
        val secondStack = inputSlots.getItem(1)
        var extraCost = 0
        
        if (!secondStack.isEmpty) {
            if (isDamageable(inputStack) && isValidRepairItem(inputStack, secondStack)) {
                //<editor-fold desc="repairing with repair items", defaultstate="collapsed">
                val maxDurability = getMaxDurability(inputStack)
                var damageValue = getDamage(inputStack)
                var repairValue = min(damageValue, maxDurability / 4)
                
                if (repairValue <= 0) {
                    callPrepareAnvilEvent(menu.bukkitView, ItemStack.EMPTY)
                    menu.cost.set(-1)
                    return
                }
                
                var itemsUsed = 0
                while (repairValue > 0 && itemsUsed < secondStack.count) {
                    itemsUsed++
                    extraCost++
                    
                    damageValue -= repairValue
                    repairValue = min(damageValue, maxDurability / 4)
                }
                
                setDamage(resultStack, damageValue)
                menu.repairItemCountCost = itemsUsed
                //</editor-fold>
            } else {
                val isEnchantedBook = secondStack.item == Items.ENCHANTED_BOOK
                    && EnchantedBookItem.getEnchantments(secondStack).isNotEmpty()
                
                if (!isEnchantedBook && (!isSameItemType(inputStack, secondStack) || !isDamageable(inputStack))) {
                    callPrepareAnvilEvent(menu.bukkitView, ItemStack.EMPTY)
                    menu.cost.set(-1)
                    return
                }
                
                //<editor-fold desc="repairing with same item", defaultstate="collapsed">
                if (!isEnchantedBook && isDamageable(inputStack)) {
                    val inputStackMaxDurability = getMaxDurability(inputStack)
                    val inputStackDamage = getDamage(inputStack)
                    
                    val firstDurability = inputStackMaxDurability - inputStackDamage
                    val secondDurability = getMaxDurability(secondStack) - getDamage(secondStack)
                    
                    val resultDurability = firstDurability + secondDurability + (inputStackMaxDurability * 12 / 100)
                    val resultDamage = inputStackMaxDurability - max(0, resultDurability)
                    
                    if (resultDamage < inputStackDamage) {
                        setDamage(resultStack, resultDamage)
                        extraCost += 2
                    }
                }
                //</editor-fold>
                //<editor-fold desc="enchantments", defaultstate="collapsed">
                var incompatibleEnchantments = false
                var hasChanged = false
                
                val enchantments = EnchantmentHelper.getEnchantments(inputStack)
                val extraEnchantments = EnchantmentHelper.getEnchantments(secondStack)
                
                for ((enchantment, level) in extraEnchantments) {
                    var isCompatible = enchantment.canEnchant(inputStack) || player.abilities.instabuild || inputStack.item == Items.ENCHANTED_BOOK
                    for (previousEnchantment in enchantments.keys) {
                        if (previousEnchantment != enchantment && !enchantment.isCompatibleWith(previousEnchantment)) {
                            isCompatible = false
                            extraCost++
                        }
                    }
                    
                    if (isCompatible) {
                        val currentLevel = enchantments.getOrDefault(enchantment, 0)
                        val newLevel = (if (currentLevel == level) level + 1 else max(currentLevel, level)).coerceAtMost(enchantment.maxLevel)
                        
                        enchantments[enchantment] = newLevel
                        
                        val costMultiplier = when (enchantment.rarity) {
                            COMMON -> 1
                            UNCOMMON -> 2
                            RARE -> 4
                            VERY_RARE -> 8
                        }.let { if (isEnchantedBook) max(1, it / 2) else it }
                        
                        extraCost += costMultiplier * level
                        
                        if (inputStack.count > 1) {
                            extraCost = menu.maximumRepairCost
                        }
                        
                        hasChanged = true
                    } else incompatibleEnchantments = true
                }
                
                if (incompatibleEnchantments && !hasChanged) {
                    callPrepareAnvilEvent(menu.bukkitView, ItemStack.EMPTY)
                    menu.cost.set(-1)
                    return
                }
                
                EnchantmentHelper.setEnchantments(enchantments, resultStack)
                //</editor-fold>
            }
        }
        
        //<editor-fold desc="renaming", defaultstate="collapsed">
        var renamed = false
        if (menu.itemName.isNullOrBlank()) {
            if (inputStack.hasCustomHoverName()) {
                renamed = true
                extraCost += 1
                resultStack.resetHoverName()
            }
        } else if (menu.itemName != getHoverName(player, inputStack)) {
            renamed = true
            extraCost += 1
            resultStack.hoverName = menu.itemName?.let(Component::literal)
        }
        //</editor-fold>
        //<editor-fold desc="cost calculations", defaultstate="collapsed">
        var totalCost = inputStack.baseRepairCost + secondStack.baseRepairCost + extraCost
        
        if (extraCost == 0) {
            resultStack = ItemStack.EMPTY
        } else {
            // always allow item renaming
            if (renamed && extraCost == 1 && totalCost >= menu.maximumRepairCost) {
                totalCost = menu.maximumRepairCost - 1
            }
            
            if (totalCost >= menu.maximumRepairCost && !player.abilities.instabuild) {
                // too expensive to craft
                resultStack = ItemStack.EMPTY
            } else {
                // calculate base repair cost for new item
                var resultRepairCost = inputStack.baseRepairCost
                
                if (!secondStack.isEmpty && resultRepairCost < secondStack.baseRepairCost) {
                    resultRepairCost = secondStack.baseRepairCost
                }
                
                if (!renamed || extraCost > 1) {
                    resultRepairCost = AnvilMenu.calculateIncreasedRepairCost(resultRepairCost)
                }
                
                resultStack.setRepairCost(resultRepairCost)
            }
        }
        
        menu.cost.set(totalCost)
        //</editor-fold>
        
        callPrepareAnvilEvent(menu.bukkitView, resultStack)
        menu.sendAllDataToRemote()
        menu.broadcastChanges()
    }
    
    private fun getHoverName(player: Player, itemStack: ItemStack): String {
        return ItemUtils.getName(itemStack).toPlainText((player as? ServerPlayer)?.clientInformation()?.language ?: "en_us")
    }
    
    private fun isSameItemType(first: ItemStack, second: ItemStack): Boolean {
        val novaItem = first.novaItem
        if (novaItem != null) {
            return novaItem == second.novaItem
        }
        
        return first.item == second.item
    }
    
    private fun callPrepareAnvilEvent(view: InventoryView, item: ItemStack) {
        val event = PrepareAnvilEvent(view, item.bukkitMirror)
            .also(::callEvent)
        
        event.inventory.setItem(2, event.result)
    }
    
}