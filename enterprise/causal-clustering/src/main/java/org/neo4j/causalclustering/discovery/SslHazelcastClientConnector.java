package org.neo4j.causalclustering.discovery;

import com.hazelcast.client.config.ClientNetworkConfig;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.LogProvider;
import org.neo4j.ssl.SslPolicy;

public class SslHazelcastClientConnector extends HazelcastClientConnector {
    private final SslPolicy sslPolicy;

    SslHazelcastClientConnector(Config config, LogProvider logProvider, SslPolicy sslPolicy, HostnameResolver hostnameResolver) {
        super(config, logProvider, hostnameResolver);
        this.sslPolicy = sslPolicy;
    }

    protected void additionalConfig(ClientNetworkConfig networkConfig, LogProvider logProvider) {
        HazelcastSslConfiguration.configureSsl(networkConfig, this.sslPolicy, logProvider);
    }
}
