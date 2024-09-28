@file:Suppress("MemberVisibilityCanBePrivate")

package xyz.xenondevs.nova.ui.overlay.bossbar.positioning

import xyz.xenondevs.commons.collections.poll
import xyz.xenondevs.nova.ui.overlay.bossbar.BossBarOverlayCompound

abstract class BarPositioning(
    val info: BarMatchInfo,
    val matchAbove: BarMatcher,
    val matchBelow: BarMatcher
) {
    
    open fun compareTo(other: BarPositioning): Int {
        val matchAbove = matchAbove
        val otherMatchAbove = other.matchAbove
        val matchBelow = matchBelow
        val otherMatchBelow = other.matchBelow
        
        val info = info
        val otherInfo = other.info
        
        // whether other should be above this bar
        val otherAboveThis = matchAbove.test(otherInfo)
        // whether other should be below this bar
        val otherBelowThis = matchBelow.test(otherInfo)
        // whether this bar should be above other
        val thisAboveOther = otherMatchAbove.test(info)
        // whether this bar should be below other
        val thisBelowOther = otherMatchBelow.test(info)
        
        // invalid scenario: both bars want the other bar to be above or below them
        if (otherAboveThis == thisAboveOther && otherBelowThis == thisBelowOther)
            return 0
        
        // invalid scenario: a bar wants the other bar to be above and below it
        if ((otherAboveThis && otherBelowThis) || (thisAboveOther && thisBelowOther))
            return 0
        
        // if this bar wants the other bar to be above it, or the other bar wants this bar to be below it (or both)
        if (otherAboveThis || thisBelowOther)
            return 1
        
        // if this bar wants the other bar to be below it, or the other bar wants this bar to be above it (or both)
        // implied: otherBelowThis || thisAboveOther
        return -1
    }
    
    
    /**
     * A fixed bar positioning which places the overlay at a fixed offset.
     *
     * Overlays with a fixed offset will be drawn over by dynamic or other fixed overlays.
     *
     * @param offset The offset of the bar.
     * @param info The [BarMatchInfo] of the bar.
     * @param matchAbove The [BarMatcher] which should be matched by the bar on top of this one.
     * @param matchBelow The [BarMatcher] which should be matched by the bar below this one.
     */
    class Fixed(
        val offset: Int,
        info: BarMatchInfo,
        matchAbove: BarMatcher,
        matchBelow: BarMatcher
    ) : BarPositioning(info, matchAbove, matchBelow)
    
    /**
     * A dynamic bar positioning which allows the overlay to change position based on the surrounding overlays.
     *
     * @param marginTop The minimum margin between this bar and the bar above it.
     * @param marginBottom The minimum margin between this bar and the bar below it.
     * @param info The [BarMatchInfo] of the bar.
     * @param matchAbove The [BarMatcher] which should be matched by the bar above this one (vertically).
     * @param matchBelow The [BarMatcher] which should be matched by the bar below this one (vertically).
     */
    class Dynamic(
        val marginTop: Int,
        val marginBottom: Int,
        info: BarMatchInfo,
        matchAbove: BarMatcher,
        matchBelow: BarMatcher
    ) : BarPositioning(info, matchAbove, matchBelow)
    
    companion object {
        
        fun sort(overlays: List<BossBarOverlayCompound>): List<BossBarOverlayCompound> {
            val inp = overlays.toMutableList()
            val out = ArrayList<BossBarOverlayCompound>()
            
            outer@ while (inp.isNotEmpty()) {
                val newEntry = inp.poll()!!
                var self = -1
                
                var i = 0
                while (i < out.size) {
                    val oldEntry = out[i]
                    val res = newEntry.positioning.compareTo(oldEntry.positioning)
                    if (self == -1) {
                        // newEntry has not been added to out yet
                        if (res == -1) {
                            // place newEntry directly below oldEntry
                            out.add(i, newEntry)
                            self = i
                            ++i
                        } else if (res == 1) {
                            // place oldEntry directly below newEntry
                            out.add(i + 1, newEntry)
                            self = i + 1
                            ++i
                        }
                    } else {
                        // newEntry has been added to out, now we rearrange other entries that are below but should be above
                        if (res == 1) {
                            // move oldEntry directly above self
                            out.removeAt(i)
                            out.add(self, oldEntry)
                            ++self
                        }
                    }
                    
                    ++i
                }
                
                if (self == -1)
                    out.add(newEntry)
            }
            
            return out
        }
        
    }
    
}