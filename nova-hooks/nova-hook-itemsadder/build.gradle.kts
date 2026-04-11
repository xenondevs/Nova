plugins {
    id("nova.hook-conventions")
}

repositories {
    maven("https://maven.devs.beer/") { content { includeGroupAndSubgroups("dev.lone") }}
}

dependencies {
    compileOnly("dev.lone:api-itemsadder:4.0.10")
}