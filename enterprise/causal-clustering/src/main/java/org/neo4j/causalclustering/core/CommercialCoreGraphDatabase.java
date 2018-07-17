package org.neo4j.causalclustering.core;

import java.io.File;
import java.util.function.Function;
import org.neo4j.causalclustering.discovery.DiscoveryServiceFactory;
import org.neo4j.causalclustering.discovery.SslHazelcastDiscoveryServiceFactory;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.kernel.impl.factory.EditionModule;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacadeFactory;
import org.neo4j.kernel.impl.factory.PlatformModule;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacadeFactory.Dependencies;

public class CommercialCoreGraphDatabase extends CoreGraphDatabase {
    public CommercialCoreGraphDatabase(File storeDir, Config config, Dependencies dependencies) {
        this(storeDir, config, dependencies, new SslHazelcastDiscoveryServiceFactory());
    }

    public CommercialCoreGraphDatabase(File storeDir, Config config, Dependencies dependencies, DiscoveryServiceFactory discoveryServiceFactory) {
        Function<PlatformModule, EditionModule> factory = (platformModule) -> {
            return new CommercialCoreEditionModule(platformModule, discoveryServiceFactory);
        };
        (new GraphDatabaseFacadeFactory(DatabaseInfo.CORE, factory)).initFacade(storeDir, config, dependencies, this);
    }
}
