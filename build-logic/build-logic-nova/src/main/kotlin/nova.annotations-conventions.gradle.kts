import xyz.xenondevs.nova.annotations.gradle.AnnotationIndexExtension
import xyz.xenondevs.nova.annotations.gradle.GenerateAnnotationIndex

plugins {
    id("xyz.xenondevs.nova.annotations")
}

plugins.withId("java") {
    configurations.named("runtimeElements") {
        outgoing.variants.named("resources") {
            artifact(tasks.named<GenerateAnnotationIndex>("generateAnnotationIndex").flatMap { it.outputFile })
        }
    }
}

extensions.configure<AnnotationIndexExtension> {
    classAnnotations.addAll(
        "xyz.xenondevs.nova.initialize.InternalInit",
        "xyz.xenondevs.nova.initialize.Init",
        "xyz.xenondevs.nova.integration.Hook"
    )
    methodAnnotations.addAll(
        "xyz.xenondevs.nova.initialize.InitFun",
        "xyz.xenondevs.nova.initialize.DisableFun"
    )
}
