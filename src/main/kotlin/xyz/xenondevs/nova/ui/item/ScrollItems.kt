package xyz.xenondevs.nova.ui.item

import de.studiocode.invui.item.impl.controlitem.ScrollItem
import de.studiocode.invui.resourcepack.Icon

class ScrollUpItem : ScrollItem(-1, {
    if (it.canScroll(-1)) Icon.ARROW_1_UP.itemBuilder else Icon.LIGHT_ARROW_1_UP.itemBuilder
})

class ScrollDownItem : ScrollItem(1, {
    if (it.canScroll(1)) Icon.ARROW_1_DOWN.itemBuilder else Icon.LIGHT_ARROW_1_DOWN.itemBuilder
})