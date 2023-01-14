package xyz.xenondevs.nova.util.reflection

import com.mojang.brigadier.tree.CommandNode
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.protocol.Packet
import net.minecraft.resources.ResourceKey
import net.minecraft.server.players.PlayerList
import net.minecraft.util.RandomSource
import net.minecraft.world.Container
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.ExperienceOrb
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.inventory.AnvilMenu
import net.minecraft.world.inventory.CraftingContainer
import net.minecraft.world.inventory.ItemCombinerMenu
import net.minecraft.world.item.Item
import net.minecraft.world.item.crafting.BannerDuplicateRecipe
import net.minecraft.world.item.crafting.BookCloningRecipe
import net.minecraft.world.item.crafting.Recipe
import net.minecraft.world.item.enchantment.EnchantmentCategory
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.level.ItemLike
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.chunk.HashMapPalette
import net.minecraft.world.level.chunk.LinearPalette
import net.minecraft.world.level.chunk.PalettedContainer
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.craftbukkit.v1_19_R2.block.CraftBlock
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockPhysicsEvent
import org.bukkit.event.inventory.PrepareItemCraftEvent
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCB
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getCBClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getClass
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getConstructor
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getField
import xyz.xenondevs.nova.util.reflection.ReflectionUtils.getMethod
import java.util.*
import java.util.function.Consumer
import kotlin.jvm.internal.CallableReference
import kotlin.reflect.KProperty1
import net.minecraft.world.entity.Entity as MojangEntity
import net.minecraft.world.entity.EquipmentSlot as MojangEquipmentSlot
import net.minecraft.world.entity.LivingEntity as MojangLivingEntity
import net.minecraft.world.entity.player.Player as MojangPlayer
import net.minecraft.world.item.Item as MojangItem
import net.minecraft.world.item.ItemStack as MojangStack
import net.minecraft.world.level.block.Block as MojangBlock

@Suppress("MemberVisibilityCanBePrivate")
internal object ReflectionRegistry {
    
    // Paths
    val CB_PACKAGE_PATH = getCB()
    
    // Classes
    val CB_CRAFT_META_ITEM_CLASS = getCBClass("inventory.CraftMetaItem")
    val SECTION_PATH_DATA_CLASS = getClass("org.bukkit.configuration.SectionPathData")
    val PALETTED_CONTAINER_DATA_CLASS = getClass("SRC(net.minecraft.world.level.chunk.PalettedContainer\$Data)")
    
    // Constructors
    val ENUM_MAP_CONSTRUCTOR = getConstructor(EnumMap::class, false, Class::class)
    val MEMORY_SECTION_CONSTRUCTOR = getConstructor(MemorySection::class, true, ConfigurationSection::class, String::class)
    val SECTION_PATH_DATA_CONSTRUCTOR = getConstructor(SECTION_PATH_DATA_CLASS, true, Any::class)
    val ITEM_STACK_CONSTRUCTOR = getConstructor(MojangStack::class, false, ItemLike::class)
    
    // Methods
    val CB_CRAFT_META_APPLY_TO_METHOD = getMethod(CB_CRAFT_META_ITEM_CLASS, true, "applyToItem", CompoundTag::class)
    val K_PROPERTY_1_GET_DELEGATE_METHOD = getMethod(KProperty1::class, false, "getDelegate", Any::class)
    val CRAFT_BLOCK_IS_PREFERRED_TOOL_METHOD = getMethod(CraftBlock::class, true, "isPreferredTool", BlockState::class, MojangStack::class)
    val ITEM_STACK_GET_ATTRIBUTE_MODIFIERS_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack getAttributeModifiers)", MojangEquipmentSlot::class)
    val ITEM_STACK_HURT_AND_BREAK_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack hurtAndBreak)", Int::class, MojangLivingEntity::class, Consumer::class)
    val ITEM_STACK_HURT_ENTITY_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack hurtEnemy)", MojangLivingEntity::class, MojangPlayer::class)
    val PLAYER_ATTACK_METHOD = getMethod(MojangPlayer::class, false, "SRM(net.minecraft.world.entity.player.Player attack)", MojangEntity::class)
    val ANVIL_MENU_CREATE_RESULT_METHOD = getMethod(AnvilMenu::class, false, "SRM(net.minecraft.world.inventory.AnvilMenu createResult)")
    val ENCHANTMENT_HELPER_GET_AVAILABLE_ENCHANTMENT_RESULTS_METHOD = getMethod(EnchantmentHelper::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper getAvailableEnchantmentResults)", Int::class, MojangStack::class, Boolean::class)
    val ENCHANTMENT_HELPER_GET_ENCHANTMENT_COST_METHOD = getMethod(EnchantmentHelper::class, true, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper getEnchantmentCost)", RandomSource::class, Int::class, Int::class, MojangStack::class)
    val ENCHANTMENT_HELPER_SELECT_ENCHANTMENT_METHOD = getMethod(EnchantmentHelper::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper selectEnchantment)", RandomSource::class, MojangStack::class, Int::class, Boolean::class)
    val ENCHANTMENT_HELPER_GET_KNOCKBACK_BONUS_METHOD = getMethod(EnchantmentHelper::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentHelper getKnockbackBonus)", MojangLivingEntity::class)
    val ENCHANTMENT_CATEGORY_CAN_ENCHANT_METHOD = getMethod(EnchantmentCategory::class, false, "SRM(net.minecraft.world.item.enchantment.EnchantmentCategory canEnchant)", MojangItem::class)
    val ITEM_IS_ENCHANTABLE_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item isEnchantable)", MojangStack::class)
    val ITEM_GET_ENCHANTMENT_VALUE_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item getEnchantmentValue)")
    val EXPERIENCE_ORB_REPAIR_PLAYER_ITEMS_METHOD = getMethod(ExperienceOrb::class, true, "SRM(net.minecraft.world.entity.ExperienceOrb repairPlayerItems)", MojangPlayer::class, Int::class)
    val ENTITY_PLAY_STEP_SOUND_METHOD = getMethod(MojangEntity::class, true, "SRM(net.minecraft.world.entity.Entity playStepSound)", BlockPos::class, BlockState::class)
    val LIVING_ENTITY_PLAY_BLOCK_FALL_SOUND_METHOD = getMethod(LivingEntity::class, true, "SRM(net.minecraft.world.entity.LivingEntity playBlockFallSound)")
    val ITEM_STACK_GET_MAX_STACK_SIZE_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack getMaxStackSize)")
    val PLAYER_LIST_BROADCAST_METHOD = getMethod(PlayerList::class, false, "SRM(net.minecraft.server.players.PlayerList broadcast)", MojangPlayer::class, Double::class, Double::class, Double::class, Double::class, ResourceKey::class, Packet::class)
    val BLOCK_PLAYER_WILL_DESTROY_METHOD = getMethod(MojangBlock::class, false, "SRM(net.minecraft.world.level.block.Block playerWillDestroy)", Level::class, BlockPos::class, BlockState::class, MojangPlayer::class)
    val INVENTORY_HURT_ARMOR_METHOD = getMethod(Inventory::class, false, "SRM(net.minecraft.world.entity.player.Inventory hurtArmor)", DamageSource::class, Float::class, IntArray::class)
    val ITEM_GET_DEFAULT_ATTRIBUTE_MODIFIERS_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item getDefaultAttributeModifiers)", MojangEquipmentSlot::class)
    val ABSTRACT_FURNACE_BLOCK_ENTITY_IS_FUEL_METHOD = getMethod(AbstractFurnaceBlockEntity::class, false, "SRM(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity isFuel)", MojangStack::class)
    val ABSTRACT_FURNACE_BLOCK_ENTITY_GET_BURN_DURATION_METHOD = getMethod(AbstractFurnaceBlockEntity::class, true, "SRM(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity getBurnDuration)", MojangStack::class)
    val ITEM_GET_CRAFTING_REMAINING_ITEM_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item getCraftingRemainingItem)")
    val ITEM_STACK_GET_ITEM_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack getItem)")
    val RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(Recipe::class, false, "SRM(net.minecraft.world.item.crafting.Recipe getRemainingItems)", Container::class)
    val BANNER_DUPLICATE_RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(BannerDuplicateRecipe::class, false, "SRM(net.minecraft.world.item.crafting.BannerDuplicateRecipe getRemainingItems)", CraftingContainer::class)
    val BOOK_CLONING_RECIPE_GET_REMAINING_ITEMS_METHOD = getMethod(BookCloningRecipe::class, false, "SRM(net.minecraft.world.item.crafting.BookCloningRecipe getRemainingItems)", CraftingContainer::class)
    val ABSTRACT_FURNACE_BLOCK_ENTITY_SERVER_TICK_METHOD = getMethod(AbstractFurnaceBlockEntity::class, false, "SRM(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity serverTick)", Level::class, BlockPos::class, BlockState::class, AbstractFurnaceBlockEntity::class)
    val NON_NULL_LIST_SET_METHOD = getMethod(NonNullList::class, false , "SRM(net.minecraft.core.NonNullList set)", Int::class, Any::class)
    val ITEM_HAS_CRAFTING_REMAINING_ITEM_METHOD = getMethod(Item::class, false, "SRM(net.minecraft.world.item.Item hasCraftingRemainingItem)")
    val BREWING_STAND_BLOCK_ENTITY_DO_BREW_METHOD = getMethod(BrewingStandBlockEntity::class, true, "SRM(net.minecraft.world.level.block.entity.BrewingStandBlockEntity doBrew)", Level::class, BlockPos::class, NonNullList::class, BrewingStandBlockEntity::class)
    val ITEM_ENTITY_FIRE_IMMUNE_METHOD = getMethod(ItemEntity::class, false, "SRM(net.minecraft.world.entity.item.ItemEntity fireImmune)")
    val ITEM_IS_FIRE_RESISTANT_METHOD = getMethod(MojangItem::class, false, "SRM(net.minecraft.world.item.Item isFireResistant)")
    val ITEM_STACK_GET_EQUIP_SOUND_METHOD = getMethod(MojangStack::class, false, "SRM(net.minecraft.world.item.ItemStack getEquipSound)")
    val LIVING_ENTITY_PLAY_EQUIP_SOUND_METHOD = getMethod(LivingEntity::class, true, "SRM(net.minecraft.world.entity.LivingEntity playEquipSound)", MojangStack::class)
    
    // Fields
    val CRAFT_META_ITEM_UNHANDLED_TAGS_FIELD = getField(CB_CRAFT_META_ITEM_CLASS, true, "unhandledTags")
    val COMMAND_NODE_CHILDREN_FIELD = getField(CommandNode::class, true, "children")
    val COMMAND_NODE_LITERALS_FIELD = getField(CommandNode::class, true, "literals")
    val COMMAND_NODE_ARGUMENTS_FIELD = getField(CommandNode::class, true, "arguments")
    val CALLABLE_REFERENCE_RECEIVER_FIELD = getField(CallableReference::class, true, "receiver")
    val PREPARE_ITEM_CRAFT_EVENT_MATRIX_FIELD = getField(PrepareItemCraftEvent::class, true, "matrix")
    val MEMORY_SECTION_MAP_FIELD = getField(MemorySection::class, true, "map")
    val HANDLER_LIST_HANDLERS_FIELD = getField(HandlerList::class, true, "handlers")
    val HANDLER_LIST_HANDLER_SLOTS_FIELD = getField(HandlerList::class, true, "handlerslots")
    val BLOCK_PHYSICS_EVENT_CHANGED_FIELD = getField(BlockPhysicsEvent::class, true, "changed")
    val SECTION_PATH_DATA_DATA_FIELD = getField(SECTION_PATH_DATA_CLASS, true, "data")
    val PALETTED_CONTAINER_DATA_FIELD = getField(PalettedContainer::class, true, "SRF(net.minecraft.world.level.chunk.PalettedContainer data)")
    val PALETTED_CONTAINER_DATA_PALETTE_FIELD = getField(PALETTED_CONTAINER_DATA_CLASS, true, "SRF(net.minecraft.world.level.chunk.PalettedContainer\$Data palette)")
    val PALETTED_CONTAINER_DATA_STORAGE_FIELD = getField(PALETTED_CONTAINER_DATA_CLASS, true, "SRF(net.minecraft.world.level.chunk.PalettedContainer\$Data storage)")
    val LINEAR_PALETTE_VALUES_FIELD = getField(LinearPalette::class, true, "SRF(net.minecraft.world.level.chunk.LinearPalette values)")
    val HASH_MAP_PALETTE_VALUES_FIELD = getField(HashMapPalette::class, true, "SRF(net.minecraft.world.level.chunk.HashMapPalette values)")
    val ITEM_COMBINER_MENU_INPUT_SLOTS_FIELD = getField(ItemCombinerMenu::class, true, "SRF(net.minecraft.world.inventory.ItemCombinerMenu inputSlots)")
    val ITEM_COMBINER_MENU_PLAYER_FIELD = getField(ItemCombinerMenu::class, true, "SRF(net.minecraft.world.inventory.ItemCombinerMenu player)")
    val ABSTRACT_FURNACE_BLOCK_ENTITY_ITEMS_FIELD = getField(AbstractFurnaceBlockEntity::class, true, "SRF(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity items)")
    
}
