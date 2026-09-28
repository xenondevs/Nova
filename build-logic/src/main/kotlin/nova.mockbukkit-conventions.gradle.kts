import org.gradle.accessors.dm.LibrariesForLibs
import java.util.jar.JarFile

val libs = the<LibrariesForLibs>()

fun resolveMockBukkitPaperVersion(): String {
    val mockBukkitJar = configurations.detachedConfiguration(dependencies.create(libs.mockbukkit.get()))
        .apply { isTransitive = false }
        .resolve()
        .single { it.name.startsWith("mockbukkit-") }
    
    return JarFile(mockBukkitJar).use { jar ->
        jar.manifest.mainAttributes.getValue("Paper-Version")
            ?: error("MockBukkit manifest does not declare Paper-Version")
    }
}

val mockBukkitPaperApiVersion = resolveMockBukkitPaperVersion()

configurations.matching { it.name == "testCompileClasspath" || it.name == "testRuntimeClasspath" }.configureEach {
    resolutionStrategy.force("io.papermc.paper:paper-api:$mockBukkitPaperApiVersion")
}

dependencies {
    add("testImplementation", "io.papermc.paper:paper-api:$mockBukkitPaperApiVersion")
    add("testImplementation", libs.mockbukkit)
}
