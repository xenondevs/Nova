package xyz.xenondevs.nova.loader.library;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.filter.ExclusionsDependencyFilter;
import org.eclipse.aether.util.repository.SimpleResolutionErrorPolicy;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LibraryLoader {
    
    private static final String CENTRAL_REPO = "https://repo1.maven.org/maven2/";
    private static final File LIBRARIES_DIR;
    
    static {
        var repoProperty = System.getProperty("repo");
        LIBRARIES_DIR = repoProperty != null ? new File(repoProperty) : new File("libraries");
    }
    
    public static List<URL> downloadLibraries(List<String> repositories, List<Dependency> libraries, Logger logger) throws DependencyResolutionException, MalformedURLException {
        // setup connection
        var locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        
        var repoSystem = locator.getService(RepositorySystem.class);
        
        var session = MavenRepositorySystemUtils.newSession();
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_NEVER);
        session.setResolutionErrorPolicy(new SimpleResolutionErrorPolicy(true, false));
        session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, new LocalRepository(LIBRARIES_DIR)));
        session.setTransferListener(new LoggingTransferListener(logger));
        session.setReadOnly();
        
        List<RemoteRepository> remoteRepos = new ArrayList<>();
        remoteRepos.add(new RemoteRepository.Builder("central", "default", CENTRAL_REPO).build());
        
        var repoId = 0;
        for (var repoUrl : repositories) {
            remoteRepos.add(new RemoteRepository.Builder(String.valueOf(repoId), "default", repoUrl).build());
            repoId++;
        }
        
        remoteRepos = repoSystem.newResolutionRepositories(session, remoteRepos);
        
        // download libraries
        logger.info("Loading " + libraries.size() + " libraries...");
        
        var urls = new ArrayList<URL>();
        for (var dep : libraries) {
            List<String> exclusions = dep instanceof Dependency.ExclusionDependency eDep ? eDep.exclusions() : Collections.emptyList();
            
            var request = new DependencyRequest(
                new CollectRequest(
                    new org.eclipse.aether.graph.Dependency(new DefaultArtifact(dep.coords()), null),
                    remoteRepos
                ),
                new ExclusionsDependencyFilter(exclusions)
            );
            
            var results = repoSystem.resolveDependencies(session, request).getArtifactResults();
            for (var result : results) {
                var file = result.getArtifact().getFile();
                logger.info("Loaded library " + file.getAbsolutePath());
                urls.add(file.toURI().toURL());
            }
        }
        
        return urls;
    }
    
    private static class LoggingTransferListener extends AbstractTransferListener {
        
        private final Logger logger;
        
        public LoggingTransferListener(Logger logger) {
            this.logger = logger;
        }
        
        @Override
        public void transferStarted(TransferEvent event) {
            logger.info("Downloading " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
        }
        
        @Override
        public void transferCorrupted(TransferEvent event) {
            logger.log(
                Level.SEVERE,
                "Invalid checksum for library " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName(),
                event.getException()
            );
        }
    }
    
}
