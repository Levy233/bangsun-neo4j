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

    public Server(Configuration config, LogService logging, StateMachine stateMachine, ResponsePacker responsePacker) {
        this.config = config;
        this.logging = logging.getInternalLogProvider();
        this.stateMachine = stateMachine;
        this.responsePacker = responsePacker;
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

        channelGroup.add( channel );
//        bossGroup = new NioEventLoopGroup(1);
//        workerGroup = new NioEventLoopGroup();
//        ServerHandler handler = new ServerHandler(aliveSlaves, config.lost_time_out(), stateMachine, logging, responsePacker);
//        serverBootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
//                .channel(NioServerSocketChannel.class)
//                .handler(new LoggingHandler(LogLevel.INFO))
//                .localAddress(new InetSocketAddress(config.me().getPort()))
//                .childHandler(new ChannelInitializer<SocketChannel>() {
//                    protected void initChannel(SocketChannel ch) throws Exception {
//                        ch.pipeline().addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
//                        ch.pipeline().addLast(new ObjectDecoder(1024 * 1024,
//                                ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
//                        ch.pipeline().addLast("encoder", new ObjectEncoder());
////                        ch.pipeline().addLast(new Decoder());
////                        ch.pipeline().addLast(new Encoder());
////                        ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingEncoder());
////                        ch.pipeline().addLast(MarshallingCodeCFactory.buildMarshallingDecoder());
//                        ch.pipeline().addLast(handler);
//                    }
//                }).option(ChannelOption.SO_BACKLOG, 128)
//                .childOption(ChannelOption.SO_KEEPALIVE, true);
//        doBind();
//            ChannelFuture future = serverBootstrap.bind(config.me().getPort()).sync();
//            System.out.println("Server start listen at " + config.me().getPort());
//            future.channel().closeFuture().sync();

    }

//    private void doBind() {
//        serverBootstrap.bind(config.me().getPort()).addListener((ChannelFutureListener) f -> {
//            if (f.isSuccess()) {
//                System.out.println("Started Tcp Server: " + config.me().getPort());
//            } else {
//                System.out.println("Started Tcp Server Failed: " + config.me().getPort());
//                f.channel().eventLoop().schedule(this::doBind, 5, TimeUnit.SECONDS);
//            }
//        });
//    }

//    public static void main(String[] args) throws Throwable {
//        new Server().start();
//    }
}
