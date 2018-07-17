package org.neo4j.causalclustering.discovery;

import com.hazelcast.config.NetworkConfig;
import org.neo4j.causalclustering.identity.MemberId;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.LogProvider;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.ssl.SslPolicy;

class SslHazelcastCoreTopologyService extends HazelcastCoreTopologyService {
    private final SslPolicy sslPolicy;

    SslHazelcastCoreTopologyService(Config config, SslPolicy sslPolicy, MemberId myself, JobScheduler jobScheduler, LogProvider logProvider, LogProvider userLogProvider, HostnameResolver hostnameResolver, TopologyServiceRetryStrategy topologyServiceRetryStrategy) {
        super(config, myself, jobScheduler, logProvider, userLogProvider, hostnameResolver, topologyServiceRetryStrategy);
        this.sslPolicy = sslPolicy;
    }

    protected void additionalConfig(NetworkConfig networkConfig, LogProvider logProvider) {
        HazelcastSslConfiguration.configureSsl(networkConfig, this.sslPolicy, logProvider);
    }
}
