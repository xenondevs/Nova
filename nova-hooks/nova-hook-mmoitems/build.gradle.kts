plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")
}

dependencies {
    compileOnly("net.Indyuce:MMOItems-API:6.10-SNAPSHOT") { isTransitive = false }
    compileOnly("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT") { isTransitive = false }
}