package xyz.xenondevs.nova.world.block.behavior.impl

import xyz.xenondevs.nova.data.resources.model.blockstate.BrownMushroomBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.MushroomStemBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.RedMushroomBlockStateConfig
import xyz.xenondevs.nova.world.block.behavior.BackingState

internal object RedMushroomBackingState : BackingState(RedMushroomBlockStateConfig, true)
internal object BrownMushroomBackingState : BackingState(BrownMushroomBlockStateConfig, true)
internal object MushroomStemBackingState : BackingState(MushroomStemBlockStateConfig, true)