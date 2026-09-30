import xyz.xenondevs.nova.annotations.gradle.AnnotationIndexExtension

plugins {
    id("xyz.xenondevs.nova.annotations")
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
