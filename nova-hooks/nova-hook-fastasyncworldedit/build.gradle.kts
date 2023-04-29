plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(project(":nova"))
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Libs-Core:2.6.0") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core:2.6.0") { isTransitive = false }
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.6.0") { isTransitive = false }
}