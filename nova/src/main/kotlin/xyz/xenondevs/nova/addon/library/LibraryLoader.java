package xyz.xenondevs.nova.addon.library;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
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
import org.jetbrains.annotations.ApiStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApiStatus.Internal
@Deprecated
public class LibraryLoader {
    
    private static final File LIBRARIES_DIR = new File("libraries");
    
    public static List<File> downloadLibraries(List<String> repositories, List<Dependency> dependencies, Set<String> exclusions, Logger logger) throws DependencyResolutionException {
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
        var repoId = 0;
        for (var repoUrl : repositories) {
            remoteRepos.add(new RemoteRepository.Builder(String.valueOf(repoId), "default", repoUrl).build());
            repoId++;
        }
        
        remoteRepos = repoSystem.newResolutionRepositories(session, remoteRepos);
        
        // download libraries
        logger.info("Loading " + dependencies.size() + " libraries...");
        
        var request = new DependencyRequest(
            new CollectRequest(
                (Dependency) null,
                dependencies,
                remoteRepos
            ),
            new ExclusionsDependencyFilter(exclusions)
        );
        
        var files = new ArrayList<File>();
        var results = repoSystem.resolveDependencies(session, request).getArtifactResults();
        for (var result : results) {
            var file = result.getArtifact().getFile();
            logger.info("Loaded library " + file.getAbsolutePath());
            files.add(file);
        }
        
        return files;
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
