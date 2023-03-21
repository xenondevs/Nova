package xyz.xenondevs.nova.initialize

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
internal annotation class InternalInit(
    val stage: InitializationStage,
    val dependsOn: Array<KClass<*>> = []
)
