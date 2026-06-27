@file:Suppress("DEPRECATION")

package xyz.xenondevs.nova.world.fakeentity.metadata.impl

import xyz.xenondevs.nova.world.fakeentity.FAKE_ENTITY_DEPRECATION

@Deprecated(FAKE_ENTITY_DEPRECATION)
open class MobMetadata internal constructor() : LivingEntityMetadata() {
    
    private val sharedFlags = sharedFlags(15)
    
    var hasNoAI: Boolean by sharedFlags[0]
    var isLeftHanded: Boolean by sharedFlags[1]
    var isAggressive: Boolean by sharedFlags[3]
    
}