package org.neo4j.causalclustering.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelPipeline;
import org.neo4j.ssl.SslPolicy;

public class SslPipelineHandlerAppender implements PipelineHandlerAppender {
    private final SslPolicy sslPolicy;

    public SslPipelineHandlerAppender(SslPolicy sslPolicy) {
        this.sslPolicy = sslPolicy;
    }

    public void addPipelineHandlerForServer(ChannelPipeline pipeline, Channel ch) throws Exception {
        if(this.sslPolicy != null) {
            pipeline.addLast(new ChannelHandler[]{this.sslPolicy.nettyServerHandler(ch)});
        }

    }

    public void addPipelineHandlerForClient(ChannelPipeline pipeline, Channel ch) throws Exception {
        if(this.sslPolicy != null) {
            pipeline.addLast(new ChannelHandler[]{this.sslPolicy.nettyClientHandler(ch)});
        }

    }
}
