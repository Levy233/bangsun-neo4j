package org.neo4j.kernel.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.handle.HeartBeatSenderHandler;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Think on 2018/5/24.
 */
public class HeartBeatSender implements Lifecycle {

    public interface Configuration {
        List<HostnamePort> members();

        int instance(); // Name of this cluster instance. Null in most cases, but tools may use e.g. "Backup"

        HostnamePort me();
    }

    private HostnamePort master;

    private Configuration config;

    private EventLoopGroup group;

    public HeartBeatSender(Configuration config) {
        this.config = config;
    }

    @Override
    public void init() throws Throwable {
        master = this.config.members().get(0);
    }

    @Override
    public void start() throws Throwable {
        group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast("ping", new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS));
                            p.addLast("decoder", new ObjectDecoder(1024 * 1024,
                                    ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                            p.addLast("encoder", new ObjectDecoder(1024 * 1024,
                                    ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                            p.addLast(new HeartBeatSenderHandler(config.instance(),config.me()));
                        }
                    });

            ChannelFuture future = b.connect(master.getHost(), master.getPort()).sync();
            future.channel().closeFuture().sync();
        } finally {
            group.shutdownGracefully();
        }
    }

    @Override
    public void stop() throws Throwable {
        group.shutdownGracefully();
    }

    @Override
    public void shutdown() throws Throwable {

    }
}
