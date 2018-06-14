package org.neo4j.kernel.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.cluster.AliveSlaves;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.handle.HeartBeatReceiverHandler;
import org.neo4j.logging.Log;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Think on 2018/5/24.
 */
public class HeartBeatReceiver implements Lifecycle {

    public interface Configuration {
        List<HostnamePort> members();

        int instance(); // Name of this cluster instance. Null in most cases, but tools may use e.g. "Backup"

        HostnamePort me();
    }

    private Configuration config;

    private ServerBootstrap serverBootstrap;

    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;

    private Log logging;

    private AliveSlaves aliveSlaves = new AliveSlaves();

    public HeartBeatReceiver(Configuration config, LogService logging) {
        this.config = config;
        this.logging = logging.getInternalLogProvider().getLog(getClass());
    }

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void start() throws Throwable {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            HeartBeatReceiverHandler handler = new HeartBeatReceiverHandler(aliveSlaves,logging);
            serverBootstrap = new ServerBootstrap().group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).handler(new LoggingHandler(LogLevel.INFO)).localAddress(new InetSocketAddress(config.me().getPort()))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                            ch.pipeline().addLast("decoder", new ObjectDecoder(1024 * 1024,
                                    ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                            ch.pipeline().addLast("encoder", new ObjectDecoder(1024 * 1024,
                                    ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                            ch.pipeline().addLast(handler);
                        }

                        ;

                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
            // 绑定端口，开始接收进来的连接
            ChannelFuture future = serverBootstrap.bind(config.me().getPort()).sync();
            logging.info("Server start listen at " + config.me().getPort());
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }

    @Override
    public void stop() throws Throwable {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

    @Override
    public void shutdown() throws Throwable {

    }

    public AliveSlaves getAliveSlaves() {
        return this.aliveSlaves;
    }
}
