package org.neo4j.causalclustering.discovery;

import com.hazelcast.spi.properties.GroupProperty;
import org.neo4j.causalclustering.core.CausalClusteringSettings;
import org.neo4j.causalclustering.identity.MemberId;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.logging.LogProvider;
import org.neo4j.scheduler.JobScheduler;
import org.neo4j.ssl.SslPolicy;

public class SslHazelcastDiscoveryServiceFactory extends HazelcastDiscoveryServiceFactory {
    private SslPolicy sslPolicy;

    public SslHazelcastDiscoveryServiceFactory() {
    }

    public CoreTopologyService coreTopologyService(Config config, MemberId myself, JobScheduler jobScheduler, LogProvider logProvider, LogProvider userLogProvider, HostnameResolver hostnameResolver, TopologyServiceRetryStrategy topologyServiceRetryStrategy) {
        configureHazelcast(config);
        return new SslHazelcastCoreTopologyService(config, this.sslPolicy, myself, jobScheduler, logProvider, userLogProvider, hostnameResolver, topologyServiceRetryStrategy);
    }

    public TopologyService topologyService(Config config, LogProvider logProvider, JobScheduler jobScheduler, MemberId myself, HostnameResolver hostnameResolver, TopologyServiceRetryStrategy topologyServiceRetryStrategy) {
        configureHazelcast(config);
        return new HazelcastClient(new SslHazelcastClientConnector(config, logProvider, this.sslPolicy, hostnameResolver), jobScheduler, logProvider, config, myself, topologyServiceRetryStrategy);
    }

    private static void configureHazelcast(Config config) {
        System.setProperty("hazelcast.phone.home.enabled", "false");
        System.setProperty("hazelcast.socket.server.bind.any", "false");
        String licenseKey = (String)config.get(CausalClusteringSettings.hazelcast_license_key);
        if(licenseKey != null) {
            GroupProperty.ENTERPRISE_LICENSE_KEY.setSystemProperty(licenseKey);
        }

        if(((Boolean)config.get(CausalClusteringSettings.disable_middleware_logging)).booleanValue()) {
            System.setProperty("hazelcast.logging.type", "none");
        }

    }

    public void setSslPolicy(SslPolicy sslPolicy) {
        this.sslPolicy = sslPolicy;
    }
}
