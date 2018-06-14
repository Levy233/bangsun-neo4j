package org.neo4j.causalclustering.core;

import org.neo4j.causalclustering.core.state.ClusterStateDirectory;
import org.neo4j.causalclustering.core.state.ClusteringModule;
import org.neo4j.causalclustering.discovery.DiscoveryServiceFactory;
import org.neo4j.causalclustering.discovery.SslHazelcastDiscoveryServiceFactory;
import org.neo4j.causalclustering.handlers.PipelineHandlerAppenderFactory;
import org.neo4j.causalclustering.handlers.SslPipelineHandlerAppenderFactory;
import org.neo4j.kernel.api.bolt.BoltConnectionTracker;
import org.neo4j.kernel.configuration.ssl.SslPolicyLoader;
import org.neo4j.kernel.impl.enterprise.EnterpriseEditionModule;
import org.neo4j.kernel.impl.enterprise.StandardBoltConnectionTracker;
import org.neo4j.kernel.impl.factory.PlatformModule;
import org.neo4j.kernel.impl.proc.Procedures;
import org.neo4j.kernel.impl.util.Dependencies;
import org.neo4j.ssl.SslPolicy;

public class CommercialCoreEditionModule extends EnterpriseCoreEditionModule {
    private SslPolicy clusterSslPolicy;

    CommercialCoreEditionModule(PlatformModule platformModule, DiscoveryServiceFactory discoveryServiceFactory) {
        super(platformModule, discoveryServiceFactory);
    }

    protected BoltConnectionTracker createSessionTracker() {
        return new StandardBoltConnectionTracker();
    }

    public void setupSecurityModule(PlatformModule platformModule, Procedures procedures) {
        EnterpriseEditionModule.setupEnterpriseSecurityModule(platformModule, procedures);
    }

    protected ClusteringModule getClusteringModule(PlatformModule platformModule, DiscoveryServiceFactory discoveryServiceFactory, ClusterStateDirectory clusterStateDirectory, IdentityModule identityModule, Dependencies dependencies) {
        SslPolicyLoader sslPolicyFactory = dependencies.satisfyDependency(SslPolicyLoader.create(this.config, this.logProvider));
        this.clusterSslPolicy = sslPolicyFactory.getPolicy((String)this.config.get(CausalClusteringSettings.ssl_policy));
        if(discoveryServiceFactory instanceof SslHazelcastDiscoveryServiceFactory) {
            ((SslHazelcastDiscoveryServiceFactory)discoveryServiceFactory).setSslPolicy(this.clusterSslPolicy);
        }

        return new ClusteringModule(discoveryServiceFactory, identityModule.myself(), platformModule, clusterStateDirectory.get());
    }

    protected PipelineHandlerAppenderFactory appenderFactory() {
        return new SslPipelineHandlerAppenderFactory();
    }
}
