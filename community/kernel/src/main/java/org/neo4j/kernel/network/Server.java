package org.neo4j.kernel.network;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.handler.timeout.IdleStateHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.cluster.AliveSlaves;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.handle.ServerHandler;
import org.neo4j.kernel.network.state.StateMachine;

import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;
import org.neo4j.time.Clocks;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.neo4j.helpers.NamedThreadFactory.daemon;

/**
 * Created by Think on 2018/5/24.
 */
public class Server implements Lifecycle {

    public interface Configuration {
        List<HostnamePort> members();

        InstanceId instance(); // Name of this cluster instance.

        HostnamePort me();

        int lost_time_out();
    }

    private Configuration config;
    private LogProvider logging;
    private AliveSlaves aliveSlaves = new AliveSlaves();
    private StateMachine stateMachine;
    private ResponsePacker responsePacker;
    private ChannelGroup channelGroup  = new DefaultChannelGroup();
    private ServerBootstrap bootstrap;
    private Log log;

    public Server(Configuration config, LogService logging, StateMachine stateMachine, ResponsePacker responsePacker) {
        this.config = config;
        this.logging = logging.getInternalLogProvider();
        this.stateMachine = stateMachine;
        this.responsePacker = responsePacker;
        this.log = logging.getInternalLog(getClass());
    }

    @Override
    public void init() {
//        this.log = logging.getLog(getClass());
    }

    @Override
    public void stop() {
        bootstrap.releaseExternalResources();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void start() throws Throwable {
        String className = getClass().getSimpleName();
        ExecutorService boss = Executors.newCachedThreadPool(daemon( "Boss-" + className ));
        ExecutorService worker = Executors.newCachedThreadPool(daemon( "Worker-" + className ));
        bootstrap = new ServerBootstrap();
        bootstrap.setFactory(new NioServerSocketChannelFactory(boss, worker));
        ServerHandler handler = new ServerHandler(aliveSlaves, config, logging,channelGroup, Clocks.systemClock(),stateMachine,responsePacker);
        final HashedWheelTimer hashedWheelTimer = new HashedWheelTimer();
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {

                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("idle", new IdleStateHandler(hashedWheelTimer, 5, 0, 0));
                pipeline.addLast("decoder",new ObjectDecoder(1024 * 1024,
                                ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                pipeline.addLast("encoder", new ObjectEncoder());
                pipeline.addLast("ServerHandler",handler);
                return pipeline;
            }
        });
        Channel channel = bootstrap.bind(new InetSocketAddress(config.me().getPort()));
        log.info("HeartBeat server started and binded to port: "+ config.me().getPort());
        channelGroup.add( channel );
    }

}
