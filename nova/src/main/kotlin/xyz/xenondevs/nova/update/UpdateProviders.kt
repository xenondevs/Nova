package xyz.xenondevs.nova.update

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.nova.HTTP_CLIENT
import xyz.xenondevs.nova.util.data.Version

interface ProjectDistributor {
    
    /**
     * The url to the project page.
     */
    val projectUrl: String
    
    /**
     * Returns the latest version of the plugin.
     */
    fun getLatestVersion(onlyRelease: Boolean): Version
    
    companion object {
        
        /**
         * An update provider for [GitHub](https://github.com).
         *
         * @param repo The repository in the format `owner/repo`.
         * @param onlyRelease Whether to only check for releases and not for pre-releases.
         */
        fun github(repo: String): ProjectDistributor = GitHub(repo)
        
        /**
         * An update provider for [SpigotMC](https://spigotmc.org).
         *
         * @param id The resource id of the plugin.
         */
        fun spigotmc(id: Int): ProjectDistributor = SpigotMC(id)
        
        /**
         * An update provider for [Hangar](https://hangar.papermc.io).
         *
         * @param id The id of the project in the format `owner/repo`.
         * @param onlyRelease Whether to only check for updates in the release channel.
         */
        fun hangar(id: String): ProjectDistributor = Hangar(id)
        
        /**
         * An update provider for [Modrinth](https://modrinth.com).
         *
         * @param id The id of the project.
         * @param onlyRelease Whether to only check for updates in the release channel.
         */
        fun modrinth(id: String): ProjectDistributor = Modrinth(id)
        
    }
    
    private class GitHub(repo: String) : ProjectDistributor {
        
        private val latestVersionUrl = "https://api.github.com/repos/$repo/releases?per_page=1"
        private val releaseVersionUrl = "https://api.github.com/repos/$repo/releases/latest"
        
        override val projectUrl = "https://github.com/$repo"
        
        override fun getLatestVersion(onlyRelease: Boolean): Version = runBlocking {
            val versionJson = if(onlyRelease)
                HTTP_CLIENT.get(releaseVersionUrl).body<JsonObject>()
            else HTTP_CLIENT.get(latestVersionUrl).body<JsonArray>().first().asJsonObject
            
            return@runBlocking Version(versionJson.getString("tag_name").removePrefix("v"))
        }
        
    }
    
    private class SpigotMC(id: Int) : ProjectDistributor {
        
        private val apiUrl = "https://api.spigotmc.org/legacy/update.php?resource=$id"
        
        override val projectUrl = "https://spigotmc.org/resources/$id"
        
        override fun getLatestVersion(onlyRelease: Boolean): Version = runBlocking {
            return@runBlocking Version(HTTP_CLIENT.get(apiUrl).bodyAsText())
        }
        
    }
    
    private class Hangar(id: String) : ProjectDistributor {
        
        private val allVersionsUrl = "https://hangar.papermc.io/api/v1/projects/$id/versions"
        private val releaseVersionUrl = "https://hangar.papermc.io/api/v1/projects/$id/latestrelease"
        
        override val projectUrl = "https://hangar.papermc.io/$id"
        
        override fun getLatestVersion(onlyRelease: Boolean): Version =
            runBlocking {
                val version: String
                if (onlyRelease) {
                    version = HTTP_CLIENT.get(releaseVersionUrl) { accept(ContentType.Text.Plain) }.bodyAsText()
                } else {
                    val json = HTTP_CLIENT.get(allVersionsUrl).body<JsonObject>()
                    version = json.getArray("result").first().asJsonObject.getString("name")
                }
                
                return@runBlocking Version(version)
            }
        
    }
    
    private class Modrinth(id: String) : ProjectDistributor {
        
        private val versionsUrl = "https://api.modrinth.com/v2/project/$id/version"
        override val projectUrl = "https://modrinth.com/plugin/$id"
        
        override fun getLatestVersion(onlyRelease: Boolean): Version =
            runBlocking {
                val versions = HTTP_CLIENT.get(versionsUrl).body<JsonArray>()
                val latestVersion = if (onlyRelease)
                    versions.first { it.asJsonObject.getString("version_type") == "release" }.asJsonObject
                else versions.first().asJsonObject
                
                return@runBlocking Version(latestVersion.getString("version_number"))
            }
        
    }
    
}