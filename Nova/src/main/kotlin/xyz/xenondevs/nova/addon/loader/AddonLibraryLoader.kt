package xyz.xenondevs.nova.addon.loader

import org.apache.maven.repository.internal.MavenRepositorySystemUtils
import org.eclipse.aether.DefaultRepositorySystemSession
import org.eclipse.aether.RepositorySystem
import org.eclipse.aether.artifact.DefaultArtifact
import org.eclipse.aether.collection.CollectRequest
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory
import org.eclipse.aether.graph.Dependency
import org.eclipse.aether.repository.LocalRepository
import org.eclipse.aether.repository.RemoteRepository
import org.eclipse.aether.repository.RepositoryPolicy
import org.eclipse.aether.resolution.DependencyRequest
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory
import org.eclipse.aether.spi.connector.transport.TransporterFactory
import org.eclipse.aether.transfer.AbstractTransferListener
import org.eclipse.aether.transfer.TransferEvent
import org.eclipse.aether.transport.http.HttpTransporterFactory
import java.io.File
import java.net.URL
import java.net.URLClassLoader
import java.util.logging.Level

private const val CENTRAL_REPO = "https://repo1.maven.org/maven2/"
private val LIBRARIES_DIR = File("libraries")

internal class AddonLibraryLoader(private val loader: AddonLoader) {
    
    private val logger = loader.logger
    
    private lateinit var repoSystem: RepositorySystem
    private lateinit var session: DefaultRepositorySystemSession
    private lateinit var repositories: List<RemoteRepository>
    
    fun createClassLoader(): LibraryClassLoader? {
        if (loader.description.libraries.isEmpty())
            return null
        
        setupConnection()
        val urls = loadLibraries()
        
        return LibraryClassLoader(urls.toTypedArray(), null)
    }
    
    private fun setupConnection() {
        val locator = MavenRepositorySystemUtils.newServiceLocator()
        locator.addService(RepositoryConnectorFactory::class.java, BasicRepositoryConnectorFactory::class.java)
        locator.addService(TransporterFactory::class.java, HttpTransporterFactory::class.java)
        
        repoSystem = locator.getService(RepositorySystem::class.java)
        
        session = MavenRepositorySystemUtils.newSession()
        session.checksumPolicy = RepositoryPolicy.CHECKSUM_POLICY_FAIL
        session.localRepositoryManager = repoSystem.newLocalRepositoryManager(session, LocalRepository(LIBRARIES_DIR))
        session.transferListener = LoggingTransferListener()
        session.setReadOnly()
        
        repositories = repoSystem.newResolutionRepositories(
            session,
            (loader.description.repositories + CENTRAL_REPO)
                .mapIndexed { idx, url -> RemoteRepository.Builder(idx.toString(), "default", url).build() }
        )
    }
    
    private fun loadLibraries(): List<URL> {
        val libraries = loader.description.libraries
        logger.info("Loading ${libraries.size} libraries...")
        
        val request = CollectRequest(
            null as Dependency?,
            libraries.map { Dependency(DefaultArtifact(it), null) },
            repositories
        )
        
        val urls = ArrayList<URL>()
        repoSystem.resolveDependencies(session, DependencyRequest(request, null)).artifactResults.forEach {
            val file = it.artifact.file
            urls += file.toURI().toURL()
            
            logger.info("Loaded library ${file.absolutePath}")
        }
        
        return urls
    }
    
    private inner class LoggingTransferListener : AbstractTransferListener() {
        
        override fun transferStarted(event: TransferEvent) {
            logger.info("Downloading library ${event.resource.repositoryUrl + event.resource.resourceName}")
        }
        
        override fun transferCorrupted(event: TransferEvent) {
            logger.log(Level.SEVERE, "Invalid checksum for library ${event.resource.repositoryUrl + event.resource.resourceName}", event.exception)
        }
        
    }
    
}

internal class LibraryClassLoader(urls: Array<out URL>, parent: ClassLoader?) : URLClassLoader(urls, parent) {
    
    public override fun loadClass(name: String?, resolve: Boolean): Class<*> {
        return super.loadClass(name, resolve)
    }
    
}
