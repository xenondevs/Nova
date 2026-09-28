package xyz.xenondevs.nova.ui.menu

import xyz.xenondevs.commons.provider.Provider
import xyz.xenondevs.invui.dsl.WindowDsl
import xyz.xenondevs.nova.i18n.LocaleManager
import java.util.*

val WindowDsl.locale: Provider<Locale>
    get() = LocaleManager.getLocaleProvider(viewer)