package org.neo4j.server.enterprise;

import org.neo4j.kernel.GraphDatabaseDependencies;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.LogProvider;
import org.neo4j.server.NeoServer;

public class CommercialBootstrapper extends OpenEnterpriseBootstrapper {
    public CommercialBootstrapper() {
    }

    protected NeoServer createNeoServer(Config configurator, GraphDatabaseDependencies dependencies, LogProvider userLogProvider) {
        return new CommercialNeoServer(configurator, dependencies, userLogProvider);
    }
}
