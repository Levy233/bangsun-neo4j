package org.neo4j.causalclustering.readreplica;

import org.neo4j.causalclustering.core.CausalClusteringSettings;
import org.neo4j.causalclustering.discovery.DiscoveryServiceFactory;
import org.neo4j.causalclustering.discovery.SslHazelcastDiscoveryServiceFactory;
import org.neo4j.causalclustering.handlers.PipelineHandlerAppenderFactory;
import org.neo4j.causalclustering.handlers.SslPipelineHandlerAppenderFactory;
import org.neo4j.causalclustering.identity.MemberId;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.configuration.ssl.SslPolicyLoader;
import org.neo4j.kernel.impl.factory.PlatformModule;
import org.neo4j.kernel.impl.util.Dependencies;
import org.neo4j.logging.LogProvider;
import org.neo4j.ssl.SslPolicy;

public class CommercialReadReplicaEditionModule extends EnterpriseReadReplicaEditionModule {
    CommercialReadReplicaEditionModule(PlatformModule platformModule, DiscoveryServiceFactory discoveryServiceFactory, MemberId myself) {
        super(platformModule, discoveryServiceFactory, myself);
    }

    protected void configureDiscoveryService(DiscoveryServiceFactory discoveryServiceFactory, Dependencies dependencies, Config config, LogProvider logProvider) {
        SslPolicyLoader sslPolicyFactory = (SslPolicyLoader)dependencies.satisfyDependency(SslPolicyLoader.create(config, logProvider));
        SslPolicy clusterSslPolicy = sslPolicyFactory.getPolicy((String)config.get(CausalClusteringSettings.ssl_policy));
        if(discoveryServiceFactory instanceof SslHazelcastDiscoveryServiceFactory) {
            ((SslHazelcastDiscoveryServiceFactory)discoveryServiceFactory).setSslPolicy(clusterSslPolicy);
        }

    }

    protected PipelineHandlerAppenderFactory appenderFactory() {
        return new SslPipelineHandlerAppenderFactory();
    }
}
