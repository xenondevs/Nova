package xyz.xenondevs.nova.world.block.behavior.impl

import xyz.xenondevs.nova.data.resources.model.config.BrownMushroomBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.config.RedMushroomBlockStateConfig
import xyz.xenondevs.nova.world.block.behavior.BlockBehavior

internal object RedMushroomBlockBehavior : BlockBehavior(RedMushroomBlockStateConfig.of(63), true)
internal object BrownMushroomBlockBehavior : BlockBehavior(BrownMushroomBlockStateConfig.of(63), true)
internal object MushroomStemBlockBehavior : BlockBehavior(BrownMushroomBlockStateConfig.of(63), true)