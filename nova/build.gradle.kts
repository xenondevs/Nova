group = "xyz.xenondevs.nova"

val mojangMapped = project.hasProperty("mojang-mapped") ||  System.getProperty("mojang-mapped") != null

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    kotlin("jvm") version libs.versions.kotlin
    id("org.jetbrains.dokka") version libs.versions.dokka
    id("xyz.xenondevs.jar-loader-gradle-plugin")
    id("xyz.xenondevs.specialsource-gradle-plugin") version "1.0.0"
    id("xyz.xenondevs.string-remapper-gradle-plugin") version "1.0"
    `maven-publish`
}

dependencies {
    // api dependencies
    api(project(":nova-api"))
    spigotLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi(libs.bundles.xenondevs.commons)
    novaLoaderApi(libs.bundles.kyori.adventure)
    novaLoaderApi("xyz.xenondevs.invui:invui:1.0-SNAPSHOT") { for (i in 1..13) exclude("xyz.xenondevs.invui", "inventory-access-r$i") }
    novaLoaderApi("xyz.xenondevs.invui:inventory-access-r13:1.0-SNAPSHOT:remapped-mojang")
    novaLoaderApi("xyz.xenondevs.invui:invui-kotlin:1.0-SNAPSHOT")
    novaLoaderApi("xyz.xenondevs:nms-utilities:0.8:remapped-mojang")
    
    // internal dependencies
    compileOnly(project(":nova-loader"))
    novaLoader(libs.bundles.ktor)
    novaLoader(libs.bundles.minecraft.assets)
    novaLoader("xyz.xenondevs.invui:invui-resourcepack:1.0-SNAPSHOT") { exclude("xyz.xenondevs.invui", "invui") }
    novaLoader("xyz.xenondevs.bstats:bstats-bukkit:3.0.1")
    novaLoader("xyz.xenondevs.bytebase:ByteBase-Runtime:0.4.4")
    novaLoader("me.xdrop:fuzzywuzzy:1.4.0")
    novaLoader("software.amazon.awssdk:s3:2.18.35")
    novaLoader("com.google.jimfs:jimfs:1.2")
    
    // spigot runtime dependencies
    spigotRuntime(libs.bundles.maven.resolver)
    spigotRuntime(variantOf(libs.spigot.server) { classifier("remapped-mojang") })
    
    // plugin dependencies
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.6")
    compileOnly("com.github.TechFortress:GriefPrevention:16.17.1") { isTransitive = false }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Core:6.10.5") { isTransitive = false }
    compileOnly("com.plotsquared:PlotSquared-Bukkit:6.10.5") { isTransitive = false }
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.2.4") { isTransitive = false }
    compileOnly("com.github.TownyAdvanced:Towny:0.97.2.0") { isTransitive = false }
    compileOnly("io.th0rgal:oraxen:1.151.0") { isTransitive = false }
    compileOnly("io.lumine:MythicLib-dist:1.3") { isTransitive = false }
    compileOnly("net.Indyuce:MMOItems:6.7") { isTransitive = false }
    compileOnly("dev.espi:protectionstones:2.10.2") { isTransitive = false }
    compileOnly("org.maxgamer:QuickShop:5.1.0.7") { isTransitive = false }
    compileOnly("com.bekvon:Residence:5.0.1.6") { isTransitive = false }
    
    // test dependencies
    testImplementation(libs.bundles.test)
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src/main/kotlin/"))
        }
    }
}

tasks {
    register("finalJar") {
        group = "build"
        dependsOn(if (mojangMapped) "jar" else "remapObfToSpigot")
    }
    
    register<Jar>("sources") {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        from("src/main/kotlin")
        archiveClassifier.set("sources")
    }
}

spigotRemap {
    spigotVersion.set(libs.versions.spigot.get().substringBefore('-'))
    sourceJarTask.set(tasks.jar)
    spigotJarClassifier.set("")
}

remapStrings {
    remapGoal.set(if (mojangMapped) "mojang" else "spigot")
    spigotVersion.set(libs.versions.spigot.get())
    classes.addAll(
        "xyz.xenondevs.nova.util.reflection.ReflectionRegistry",
        "xyz.xenondevs.nova.util.NMSUtils",
        "xyz.xenondevs.nova.transformer.patch.noteblock.NoteBlockPatch",
        "xyz.xenondevs.nova.world.generation.WorldGenerationManager",
        "xyz.xenondevs.nova.transformer.patch.worldgen.FeatureSorterPatch",
        "xyz.xenondevs.nova.transformer.patch.worldgen.registry.RegistryCodecPatch",
        "xyz.xenondevs.nova.transformer.patch.worldgen.registry.MappedRegistryPatch",
        "xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.ChunkAccessSectionsPatch",
        "xyz.xenondevs.nova.transformer.patch.worldgen.chunksection.LevelChunkSectionPatch",
        "xyz.xenondevs.nova.transformer.patch.item.ToolPatches",
        "xyz.xenondevs.nova.transformer.patch.item.DamageablePatches"
    )
}

publishing {
    repositories {
        maven {
            credentials {
                name = "xenondevs"
                url = uri { "https://repo.xenondevs.xyz/releases/" }
                credentials(PasswordCredentials::class)
            }
        }
    }
    
    publications {
        create<MavenPublication>("nova") {
            from(components.getByName("kotlin"))
            artifact(tasks.getByName("sources"))
        }
    }
}