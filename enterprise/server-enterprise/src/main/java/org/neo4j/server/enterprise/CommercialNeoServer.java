package org.neo4j.server.enterprise;

import org.neo4j.causalclustering.core.CommercialCoreGraphDatabase;
import org.neo4j.causalclustering.readreplica.CommercialReadReplicaGraphDatabase;
import org.neo4j.graphdb.factory.GraphDatabaseSettings;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.impl.enterprise.configuration.EnterpriseEditionSettings;
import org.neo4j.kernel.impl.enterprise.configuration.EnterpriseEditionSettings.Mode;
import org.neo4j.kernel.impl.factory.GraphDatabaseFacadeFactory.Dependencies;
import org.neo4j.logging.LogProvider;
import org.neo4j.server.database.Database.Factory;
import org.neo4j.server.database.LifecycleManagingDatabase;
import org.neo4j.server.database.LifecycleManagingDatabase.GraphFactory;

import java.io.File;

public class CommercialNeoServer extends OpenEnterpriseNeoServer {
    private static final GraphFactory CORE_FACTORY = (config, dependencies) -> {
        File storeDir = config.get(GraphDatabaseSettings.database_path);
        return new CommercialCoreGraphDatabase(storeDir, config, dependencies);
    };
    private static final GraphFactory READ_REPLICA_FACTORY = (config, dependencies) -> {
        File storeDir = config.get(GraphDatabaseSettings.database_path);
        return new CommercialReadReplicaGraphDatabase(storeDir, config, dependencies);
    };

    public CommercialNeoServer(Config config, Dependencies dependencies, LogProvider logProvider) {
        super(config, createDbFactory(config), dependencies, logProvider);
    }

    protected static Factory createDbFactory(Config config) {
        Mode mode = config.get(EnterpriseEditionSettings.mode);
        switch(mode) {
            case CORE:
                return LifecycleManagingDatabase.lifecycleManagingDatabase(CORE_FACTORY);
            case READ_REPLICA:
                return LifecycleManagingDatabase.lifecycleManagingDatabase(READ_REPLICA_FACTORY);
            default:
                return OpenEnterpriseNeoServer.createDbFactory(config);
        }
    }
}
