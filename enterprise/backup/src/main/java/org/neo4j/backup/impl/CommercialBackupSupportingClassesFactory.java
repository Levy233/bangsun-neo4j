package org.neo4j.backup.impl;

import org.neo4j.causalclustering.handlers.PipelineHandlerAppender;
import org.neo4j.causalclustering.handlers.SslPipelineHandlerAppenderFactory;
import org.neo4j.kernel.configuration.Config;
import org.neo4j.kernel.configuration.ssl.SslPolicyLoader;
import org.neo4j.kernel.impl.util.Dependencies;

public class CommercialBackupSupportingClassesFactory extends BackupSupportingClassesFactory {
    CommercialBackupSupportingClassesFactory(BackupModule backupModule) {
        super(backupModule);
    }

    protected PipelineHandlerAppender createPipelineHandlerAppender(Config config) {
        SslPipelineHandlerAppenderFactory factory = new SslPipelineHandlerAppenderFactory();
        Dependencies deps = new Dependencies();
        deps.satisfyDependencies(new Object[]{SslPolicyLoader.create(config, this.logProvider)});
        return factory.create(config, deps, this.logProvider);
    }
}