package org.neo4j.kernel.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.cluster.AliveSlaves;
import org.neo4j.kernel.cluster.Slave;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.handle.ServerHandler;
import org.neo4j.kernel.network.message.HeartBeatMessage;
import org.neo4j.kernel.network.message.RequestContext;
import org.neo4j.kernel.network.message.TransactionStreamResponse;
import org.neo4j.kernel.network.state.StateMachine;
import org.neo4j.logging.Log;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Think on 2018/5/24.
 */
public class Server implements Lifecycle {

    public interface Configuration {
        List<HostnamePort> members();

        int instance(); // Name of this cluster instance.

        HostnamePort me();

        int lost_time_out();
    }

    private Configuration config;
    private ServerBootstrap serverBootstrap;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Log logging;
    private AliveSlaves aliveSlaves = new AliveSlaves();
    private StateMachine stateMachine;
    private ResponsePacker responsePacker;

    public Server(Configuration config, LogService logging, StateMachine stateMachine, ResponsePacker responsePacker) {
        this.config = config;
        this.logging = logging.getInternalLogProvider().getLog(getClass());
        this.stateMachine = stateMachine;
        this.responsePacker = responsePacker;
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void start() throws Throwable {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        ServerHandler handler = new ServerHandler(aliveSlaves, config.lost_time_out(), stateMachine, logging, responsePacker);
        serverBootstrap = new ServerBootstrap().group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .localAddress(new InetSocketAddress(config.me().getPort()))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                        ch.pipeline().addLast(new ObjectDecoder(1024 * 1024,
                                ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                        ch.pipeline().addLast("encoder", new ObjectEncoder());
                        ch.pipeline().addLast(handler);
                    }
                }).option(ChannelOption.SO_BACKLOG, 128)
                .childOption(ChannelOption.SO_KEEPALIVE, true);
        doBind();
//            ChannelFuture future = serverBootstrap.bind(config.me().getPort()).sync();
//            System.out.println("Server start listen at " + config.me().getPort());
//            future.channel().closeFuture().sync();

    }

    private void doBind() {
        serverBootstrap.bind(config.me().getPort()).addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                System.out.println("Started Tcp Server: " + config.me().getPort());
            } else {
                System.out.println("Started Tcp Server Failed: " + config.me().getPort());
                f.channel().eventLoop().schedule(this::doBind, 5, TimeUnit.SECONDS);
            }
        });
    }

    @Override
    public void stop() throws Throwable {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void shutdown() throws Throwable {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
