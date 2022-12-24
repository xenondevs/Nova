group = "xyz.xenondevs.nova"

val mojangMapped = System.getProperty("mojang-mapped") != null

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    java
    kotlin("jvm") version libs.versions.kotlin
    id("xyz.xenondevs.jar-loader-gradle-plugin")
    id("xyz.xenondevs.specialsource-gradle-plugin") version "1.0.0"
    id("xyz.xenondevs.string-remapper-gradle-plugin") version "1.0.0"
    `maven-publish`
}

dependencies {
    // api dependencies
    api(project(":nova-api"))
    spigotLoaderApi(libs.bundles.kotlin)
    novaLoaderApi(libs.bundles.cbf)
    novaLoaderApi("de.studiocode.invui:InvUI:0.10") { for (i in 1..12) exclude("de.studiocode.invui", "IA-R$i") }
    novaLoaderApi("de.studiocode.invui:ResourcePack:0.10") { exclude("de.studiocode.invui", "InvUI") }
    novaLoaderApi("de.studiocode.invui:IA-R12:0.10:remapped-mojang")
    novaLoaderApi("xyz.xenondevs:nms-utilities:0.6:remapped-mojang")
    
    // internal dependencies
    compileOnly(project(":nova-loader"))
    novaLoader(libs.bundles.ktor)
    novaLoader(libs.bundles.minecraft.assets)
    novaLoader(libs.zip4j)
    novaLoader("xyz.xenondevs.bstats:bstats-bukkit:3.0.1")
    novaLoader("xyz.xenondevs.bytebase:ByteBase-Runtime:0.4.2")
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
    compileOnly("com.griefdefender:api:2.0.0-SNAPSHOT") { isTransitive = false }
    compileOnly("com.github.LoneDev6:API-ItemsAdder:3.2.4") { isTransitive = false }
    compileOnly("com.github.TownyAdvanced:Towny:0.97.2.0") { isTransitive = false }
    compileOnly("com.google.code.gson:gson:2.8.9") // The Oraxen artifact is a fat jar with an outdated gson version, this fixes compilation issues
    compileOnly("com.github.Th0rgal:Oraxen:ebd90cfbb2") { isTransitive = false }
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
    named<Jar>("jar") {
        dependsOn("remapStrings")
    }
    
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
    classes.set(listOf(
        "xyz.xenondevs.nova.util.reflection.ReflectionRegistry",
        "xyz.xenondevs.nova.util.NMSUtils",
        "xyz.xenondevs.nova.transformer.patch.noteblock.NoteBlockPatch",
        "xyz.xenondevs.nova.transformer.patch.item.ToolPatches",
        "xyz.xenondevs.nova.transformer.patch.item.DamageablePatches"
    ))
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