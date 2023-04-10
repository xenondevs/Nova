package xyz.xenondevs.nova.world.block.backingstate.impl

import xyz.xenondevs.nova.data.resources.model.blockstate.BrownMushroomBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.MushroomStemBlockStateConfig
import xyz.xenondevs.nova.data.resources.model.blockstate.RedMushroomBlockStateConfig
import xyz.xenondevs.nova.world.block.backingstate.BackingState

internal object RedMushroomBlockBackingState : BackingState(RedMushroomBlockStateConfig, true)
internal object BrownMushroomBlockBackingState : BackingState(BrownMushroomBlockStateConfig, true)
internal object MushroomStemBackingState : BackingState(MushroomStemBlockStateConfig, true)