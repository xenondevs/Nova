plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.paperweight)
}

dependencies {
    paperweight.paperDevBundle(libs.versions.paper)
    implementation(project(":nova"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Libs-Core:2.11.0") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.9.1") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.9.2") { isTransitive = false }
}