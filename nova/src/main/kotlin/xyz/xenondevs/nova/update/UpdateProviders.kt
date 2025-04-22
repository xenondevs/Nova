package xyz.xenondevs.nova.update

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.commons.version.Version
import xyz.xenondevs.nova.HTTP_CLIENT

interface ProjectDistributor {
    
    /**
     * The url to the project page.
     */
    val projectUrl: String
    
    /**
     * Returns the latest version of the plugin.
     */
    suspend fun getLatestVersion(onlyRelease: Boolean): Version
    
    companion object {
        
        /**
         * An update provider for [GitHub](https://github.com).
         *
         * @param repo The repository in the format `owner/repo`.
         */
        fun github(repo: String): ProjectDistributor = GitHub(repo)
        
        /**
         * An update provider for [Hangar](https://hangar.papermc.io).
         *
         * @param id The id of the project in the format `owner/repo`.
         */
        fun hangar(id: String): ProjectDistributor = Hangar(id)
        
        /**
         * An update provider for [Modrinth](https://modrinth.com).
         *
         * @param id The id of the project.
         */
        fun modrinth(id: String): ProjectDistributor = Modrinth(id)
        
    }
    
    private class GitHub(repo: String) : ProjectDistributor {
        
        private val latestVersionUrl = "https://api.github.com/repos/$repo/releases?per_page=1"
        private val releaseVersionUrl = "https://api.github.com/repos/$repo/releases/latest"
        
        override val projectUrl = "https://github.com/$repo"
        
        override suspend fun getLatestVersion(onlyRelease: Boolean): Version {
            val versionJson = if (onlyRelease)
                HTTP_CLIENT.get(releaseVersionUrl).body<JsonObject>()
            else HTTP_CLIENT.get(latestVersionUrl).body<JsonArray>().first().asJsonObject
            
            return Version(versionJson.getString("tag_name").removePrefix("v"))
        }
        
    }
    
    private class Hangar(id: String) : ProjectDistributor {
        
        private val allVersionsUrl = "https://hangar.papermc.io/api/v1/projects/$id/versions"
        private val releaseVersionUrl = "https://hangar.papermc.io/api/v1/projects/$id/latestrelease"
        
        override val projectUrl = "https://hangar.papermc.io/$id"
        
        override suspend fun getLatestVersion(onlyRelease: Boolean): Version {
            val version: String
            if (onlyRelease) {
                version = HTTP_CLIENT.get(releaseVersionUrl) { accept(ContentType.Text.Plain) }.bodyAsText()
            } else {
                val json = HTTP_CLIENT.get(allVersionsUrl).body<JsonObject>()
                version = json.getArray("result").first().asJsonObject.getString("name")
            }
            
            return Version(version)
        }
        
    }
    
    private class Modrinth(id: String) : ProjectDistributor {
        
        private val versionsUrl = "https://api.modrinth.com/v2/project/$id/version"
        override val projectUrl = "https://modrinth.com/plugin/$id"
        
        override suspend fun getLatestVersion(onlyRelease: Boolean): Version {
            val versions = HTTP_CLIENT.get(versionsUrl).body<JsonArray>()
            val latestVersion = if (onlyRelease)
                versions.first { it.asJsonObject.getString("version_type") == "release" }.asJsonObject
            else versions.first().asJsonObject
            
            return Version(latestVersion.getString("version_number"))
        }
        
    }
    
}