package xyz.xenondevs.nova.world.block.logic.tileentity

import net.minecraft.advancements.CriteriaTriggers
import net.minecraft.core.BlockPos
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.stats.Stats
import net.minecraft.util.RandomSource
import net.minecraft.util.random.WeightedEntry
import net.minecraft.util.random.WeightedRandom
import net.minecraft.world.Container
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.DataSlot
import net.minecraft.world.inventory.EnchantmentMenu
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.EnchantmentTableBlock
import org.bukkit.enchantments.EnchantmentOffer
import org.bukkit.event.enchantment.EnchantItemEvent
import org.bukkit.event.enchantment.PrepareItemEnchantEvent
import xyz.xenondevs.commons.collections.takeUnlessEmpty
import xyz.xenondevs.nova.data.resources.lookup.ResourceLookups
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.item.enchantment.Enchantment
import xyz.xenondevs.nova.item.enchantment.NovaEnchantment
import xyz.xenondevs.nova.item.enchantment.VanillaEnchantment
import xyz.xenondevs.nova.registry.NovaRegistries
import xyz.xenondevs.nova.util.bukkitMirror
import xyz.xenondevs.nova.util.callEvent
import xyz.xenondevs.nova.util.getOrThrow
import xyz.xenondevs.nova.util.item.novaItem
import xyz.xenondevs.nova.util.namespacedKey
import xyz.xenondevs.nova.util.reflection.ReflectionUtils
import kotlin.math.max
import kotlin.math.roundToInt
import net.minecraft.world.item.enchantment.Enchantment as MojangEnchantment
import org.bukkit.enchantments.Enchantment as BukkitEnchantment
import org.bukkit.entity.Player as BukkitPlayer

private val ENCHANTMENT_MENU_RANDOM_FIELD = ReflectionUtils.getField(EnchantmentMenu::class, true, "SRF(net.minecraft.world.inventory.EnchantmentMenu random)")
private val ENCHANTMENT_MENU_ENCHANTMENT_SEED_FIELD = ReflectionUtils.getField(EnchantmentMenu::class, true, "SRF(net.minecraft.world.inventory.EnchantmentMenu enchantmentSeed)")
private val ENCHANTMENT_MENU_ENCHANT_SLOTS_FIELD = ReflectionUtils.getField(EnchantmentMenu::class, true, "SRF(net.minecraft.world.inventory.EnchantmentMenu enchantSlots)")
private val ENCHANTMENT_MENU_PLAYER_FIELD = ReflectionUtils.getField(EnchantmentMenu::class, true, "player")

private data class EnchantmentInstance(
    val enchantment: Enchantment,
    val level: Int
) : WeightedEntry.IntrusiveBase(enchantment.rarity)

/**
 * Replaces the enchantment table logic.
 */
internal object EnchantmentTableLogic {
    
    private val EnchantmentMenu.bukkitPlayer: BukkitPlayer
        get() = ENCHANTMENT_MENU_PLAYER_FIELD.get(this) as BukkitPlayer
    
    private val EnchantmentMenu.random: RandomSource
        get() = ENCHANTMENT_MENU_RANDOM_FIELD.get(this) as RandomSource
    
    private val EnchantmentMenu.enchantSlots: Container
        get() = ENCHANTMENT_MENU_ENCHANT_SLOTS_FIELD.get(this) as Container
    
    private val EnchantmentMenu.enchantmentSeedSlot: DataSlot
        get() = ENCHANTMENT_MENU_ENCHANTMENT_SEED_FIELD.get(this) as DataSlot
    
    @JvmStatic
    fun enchantmentMenuPrepareClues(menu: EnchantmentMenu, itemStack: ItemStack, level: Level, pos: BlockPos) {
        val player = menu.bukkitPlayer
        val random = menu.random
        random.setSeed(menu.enchantmentSeed.toLong())
        
        // count valid bookshelves
        val bookshelves = EnchantmentTableBlock.BOOKSHELF_OFFSETS.count { EnchantmentTableBlock.isValidBookShelf(level, pos, it) }
        
        // reset table slots and calculate new clues
        val offers = arrayOfNulls<EnchantmentOffer>(3)
        for (slot in 0..2) {
            var levelRequirement = getEnchantmentCost(random, itemStack, slot, bookshelves)
            if (levelRequirement < slot + 1)
                levelRequirement = 0
            
            menu.costs[slot] = levelRequirement
            menu.enchantClue[slot] = -1
            menu.levelClue[slot] = -1
            
            if (levelRequirement <= 0)
                continue
            
            random.setSeed(menu.enchantmentSeed.toLong() + slot)
            val enchantments = selectTableEnchantments(random, itemStack, levelRequirement, Enchantment::isTableDiscoverable)
            if (enchantments.isNotEmpty()) {
                val clue = enchantments[random.nextInt(enchantments.size)]
                offers[slot] = EnchantmentOffer(Enchantment.asBukkitEnchantment(clue.enchantment), clue.level, levelRequirement)
            }
        }
        
        // call PrepareItemEnchantEvent
        val event = PrepareItemEnchantEvent(player, menu.bukkitView, level.world.getBlockAt(pos.x, pos.y, pos.z), itemStack.bukkitMirror, offers, bookshelves)
        event.isCancelled = !Enchantable.isEnchantable(itemStack)
        callEvent(event)

        // apply enchantment clues to slots
        if (!event.isCancelled) {
            for (slot in 0..2) {
                val offer = event.offers[slot]
                if (offer != null) {
                    menu.costs[slot] = offer.cost
                    val (clueEnch, clueLevel) = getEnchantmentClueData(player, Enchantment.of(offer.enchantment), offer.enchantmentLevel)
                    menu.enchantClue[slot] = clueEnch
                    menu.levelClue[slot] = clueLevel
                } else {
                    menu.costs[slot] = 0
                }
            }
        } else {
            // disable slots
            for (slot in 0..2) {
                menu.costs[slot] = 0
            }
        }
        
        menu.broadcastChanges()
    }
    
    @JvmStatic
    fun enchantmentMenuEnchant(menu: EnchantmentMenu, itemStack: ItemStack, slot: Int, player: Player, expLevels: Int, lapis: ItemStack, level: Level, pos: BlockPos) {
        val random = menu.random
        val enchantSlots = menu.enchantSlots
        random.setSeed(menu.enchantmentSeed.toLong() + slot)
        
        var levelRequirement = menu.costs[slot]
        var enchantments = selectTableEnchantments(random, itemStack, levelRequirement, Enchantment::isTableDiscoverable)
            .associateTo(HashMap()) { it.enchantment to it.level }
        val isBook = itemStack.item == Items.BOOK
        
        //<editor-fold desc="EnchantItemEvent", defaultstate="collapsed">
        val bukkitEnchantments = enchantments.mapKeysTo(HashMap()) { Enchantment.asBukkitEnchantment(it.key) }
        val hintedBukkitEnchantment = BukkitEnchantment.getByKey(BuiltInRegistries.ENCHANTMENT.getKey(BuiltInRegistries.ENCHANTMENT.byId(menu.enchantClue[slot])!!)!!.namespacedKey)!!
        val hintedLevel = menu.levelClue[slot]
        
        val event = EnchantItemEvent(
            player.bukkitEntity as BukkitPlayer,
            menu.bukkitView,
            level.world.getBlockAt(pos.x, pos.y, pos.z),
            itemStack.bukkitMirror,
            levelRequirement, bukkitEnchantments,
            hintedBukkitEnchantment, hintedLevel,
            slot
        ).also(::callEvent)
        
        // load modified event values
        levelRequirement = event.expLevelCost
        enchantments = event.enchantsToAdd.mapKeysTo(HashMap()) { Enchantment.of(it.key) }
        
        // event cancelled / enchantments removed
        if (event.isCancelled || event.enchantsToAdd.isEmpty())
            return
        //</editor-fold>
        
        // too expensive and not in creative
        if (levelRequirement > player.experienceLevel && !player.abilities.instabuild)
            return
        
        if (isBook) {
            val enchantedBook = ItemStack(Items.ENCHANTED_BOOK)
            enchantedBook.tag = itemStack.tag?.copy() 
            Enchantable.setStoredEnchantments(enchantedBook, enchantments)
            enchantSlots.setItem(0, enchantedBook)
        } else {
            Enchantable.setEnchantments(itemStack, enchantments)
        }
        
        // take exp, generate new enchantment seed
        player.onEnchantmentPerformed(itemStack, expLevels)
        menu.enchantmentSeedSlot.set(player.enchantmentSeed)
        
        if (!player.abilities.instabuild) {
            lapis.shrink(expLevels)
            if (lapis.isEmpty)
                enchantSlots.setItem(1, ItemStack.EMPTY)
        }
        
        menu.slotsChanged(enchantSlots)
        player.awardStat(Stats.ENCHANT_ITEM)
        CriteriaTriggers.ENCHANTED_ITEM.trigger(player as ServerPlayer, itemStack, expLevels)
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1f, level.random.nextFloat() * 0.1F + 0.9F)
    }
    
    private fun getEnchantmentClueData(player: BukkitPlayer, enchantment: Enchantment, level: Int): Pair<Int, Int> {
        val vanillaEnch: MojangEnchantment
        val clueLevel: Int
        
        if (enchantment is NovaEnchantment) {
            val localeLookup = ResourceLookups.ENCHANTMENT_DATA[player.locale]
                ?: throw IllegalStateException("Missing enchantment lookup data for lang ${player.locale}")
            vanillaEnch = BuiltInRegistries.ENCHANTMENT.getOrThrow(localeLookup.vanillaEnchantment)
            clueLevel = localeLookup.enchantments[enchantment.id]?.get(level -1)
                ?: throw IllegalStateException("Missing enchantment lookup data for ${enchantment.id} at level $level")
        } else {
            vanillaEnch = (enchantment as VanillaEnchantment).enchantment
            clueLevel = level
        }
        
        return BuiltInRegistries.ENCHANTMENT.getId(vanillaEnch) to clueLevel
    }
    
    private fun getEnchantmentCost(random: RandomSource, itemStack: ItemStack, slot: Int, bookshelves: Int): Int {
        // see: EnchantmentHelper#getEnchantmentCost
        
        val value = getEnchantmentValue(itemStack)
        if (value <= 0)
            return 0
        
        val clampedBookshelves = bookshelves.coerceAtMost(15)
        val i = random.nextInt(8) + 1 + (clampedBookshelves shr 1) + random.nextInt(clampedBookshelves + 1)
        return when (slot) {
            0 -> max(i / 3, 1)
            1 -> i * 2 / 3 + 1
            else -> max(i, clampedBookshelves * 2)
        }
    }
    
    private fun selectTableEnchantments(random: RandomSource, itemStack: ItemStack, levelCost: Int, verify: (Enchantment) -> Boolean): List<EnchantmentInstance> {
        // see: EnchantmentHelper#selectEnchantment
        
        val enchantmentValue = getEnchantmentValue(itemStack)
        if (enchantmentValue <= 0)
            return emptyList()
        
        val possibleEnchantments = getPossibleTableEnchantments(itemStack, levelCost, verify)
        if (possibleEnchantments.isEmpty())
            return emptyList()
        
        val selected = ArrayList<EnchantmentInstance>()
        
        // I have no idea either
        var i = levelCost
        i += 1 + random.nextInt(enchantmentValue / 4 + 1) + random.nextInt(enchantmentValue / 4 + 1)
        val f = (random.nextFloat() + random.nextFloat() - 1f) * 0.15f // float between -0.15, 0.15
        i = (i + i * f).roundToInt().coerceAtLeast(1)
        
        WeightedRandom.getRandomItem(random, possibleEnchantments).ifPresent(selected::add)
        while (possibleEnchantments.isNotEmpty() && random.nextInt(50) <= i) {
            // remove incompatible enchantments from the list of possible enchantments
            if (selected.isNotEmpty()) {
                possibleEnchantments.removeIf { possible ->
                    selected.any { selected -> !selected.enchantment.isCompatibleWith(possible.enchantment) }
                }
            }
            
            WeightedRandom.getRandomItem(random, possibleEnchantments).ifPresent(selected::add)
            i /= 2
        }
        
        // remove one enchantment if the item is a book with multiple selected enchantments
        // see: EnchantmentMenu#getEnchantmentList
        if (selected.size > 1 && itemStack.novaItem == null && itemStack.item == Items.BOOK) {
            selected.removeAt(random.nextInt(selected.size))
        }
        
        return selected
    }
    
    private fun getPossibleTableEnchantments(itemStack: ItemStack, levelCost: Int, verify: (Enchantment) -> Boolean): MutableList<EnchantmentInstance> {
        // see: EnchantmentHelper#getAvailableEnchantmentResults
        
        // build a sequence of all possible enchantments for this item under the given levelCost
        val enchantments: Sequence<Enchantment>
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            val categories = novaItem.getBehaviorOrNull<Enchantable>()?.enchantmentCategories?.takeUnlessEmpty()
                ?: return ArrayList()
            
            enchantments = categories.asSequence()
                .filter { it.canEnchant(novaItem) }
                .flatMap { it.enchantments }
        } else {
            val item = itemStack.item
            if (item == Items.BOOK) {
                enchantments = NovaRegistries.ENCHANTMENT_CATEGORY.asSequence()
                    .flatMap { it.enchantments }
            } else {
                enchantments = NovaRegistries.ENCHANTMENT_CATEGORY.asSequence()
                    .filter { it.canEnchant(item) }
                    .flatMap { it.enchantments }
            }
        }
        
        // map all enchantments to an EnchantmentInstance with the highest possible level
        val enchantmentInstances = ArrayList<EnchantmentInstance>()
        for (enchantment in enchantments) {
            if (!verify(enchantment))
                continue
            
            for (level in enchantment.maxLevel downTo enchantment.minLevel) {
                if (levelCost in enchantment.getTableLevelRequirement(level)) {
                    enchantmentInstances += EnchantmentInstance(enchantment, level)
                    break
                }
            }
        }
        
        return enchantmentInstances
    }
    
    private fun getEnchantmentValue(itemStack: ItemStack): Int {
        val novaItem = itemStack.novaItem
        if (novaItem != null) {
            return novaItem.getBehaviorOrNull<Enchantable>()?.enchantmentValue ?: 0
        } else {
            return itemStack.item.enchantmentValue
        }
    }
    
}