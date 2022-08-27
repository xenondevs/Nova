package xyz.xenondevs.nova.loader.library;

import java.util.List;

public interface Dependency {
    
    String coords();
    
    record DefaultDependency(String coords) implements Dependency {
    }
    
    record ExclusionDependency(String coords, List<String> exclusions) implements Dependency {
    }
    
}