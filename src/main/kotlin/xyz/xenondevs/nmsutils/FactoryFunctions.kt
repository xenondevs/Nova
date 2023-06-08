package xyz.xenondevs.nmsutils

import net.minecraft.advancements.critereon.EntityPredicate
import net.minecraft.advancements.critereon.LightPredicate
import net.minecraft.advancements.critereon.LighthingBoltPredicate
import net.minecraft.advancements.critereon.LocationPredicate
import net.minecraft.advancements.critereon.MinMaxBounds
import net.minecraft.advancements.critereon.StatePropertiesPredicate
import net.minecraft.core.BlockPos
import net.minecraft.core.NonNullList
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.storage.loot.predicates.LocationCheck
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
import xyz.xenondevs.nmsutils.internal.util.ReflectionRegistry

fun <E> NonNullList(list: List<E>, default: E? = null): NonNullList<E> {
    val nonNullList: NonNullList<E>
    if (default == null) {
        nonNullList = NonNullList.createWithCapacity(list.size)
        nonNullList.addAll(list)
    } else {
        nonNullList = NonNullList.withSize(list.size, default)
        list.forEachIndexed { index, e -> nonNullList[index] = e }
    }
    
    return nonNullList
}

@Suppress("FunctionName") // Light thing
fun LightningBoltPredicate(blocksOnFire: MinMaxBounds.Ints, entityStruck: EntityPredicate): LighthingBoltPredicate =
    ReflectionRegistry.LIGHTNING_BOLT_PREDICATE_CONSTRUCTOR.newInstance(blocksOnFire, entityStruck)

fun LightPredicate(light: MinMaxBounds.Ints): LightPredicate =
    LightPredicate.Builder.light().setComposite(light).build()

fun LocationCheck(predicate: LocationPredicate, pos: BlockPos = BlockPos.ZERO): LocationCheck =
    ReflectionRegistry.LOCATION_CHECK_CONSTRUCTOR.newInstance(predicate, pos)

fun LootItemBlockStatePropertyCondition(block: Block, predicate: StatePropertiesPredicate): LootItemBlockStatePropertyCondition =
    ReflectionRegistry.LOOT_ITEM_BLOCK_STATE_PROPERTY_CONDITION_CONSTRUCTOR.newInstance(block, predicate)