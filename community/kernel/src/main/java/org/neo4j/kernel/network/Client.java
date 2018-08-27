package org.neo4j.kernel.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.handle.ClientHandler;
import org.neo4j.logging.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by Think on 2018/5/24.
 */
public class Client implements Lifecycle{
    public interface Configuration {
        List<HostnamePort> members();

        InstanceId instance(); // Name of this cluster instance. Null in most cases, but tools may use e.g. "Backup"

        HostnamePort me();

        int connectTimeOut();
    }
    private HostnamePort another;
    private Configuration config;
    private Log log;
    private EventLoopGroup group;
    private Bootstrap b = new Bootstrap();
    private LogService logging;

    public Channel getChannel() {
        return channel;
    }

    private Channel channel;
    private Supplier<TransactionIdStore> storeSupplier;
    static final byte INTERNAL_PROTOCOL_VERSION = 2;
    static final byte applicationProtocolVersion = 3;

    public Client(Configuration config,LogService logging, Supplier<TransactionIdStore> storeSupplier) {
        this.config = config;
        this.logging = logging;
        this.log = logging.getInternalLogProvider().getLog(getClass());
        this.storeSupplier = storeSupplier;
    }

    @Override
    public void init() throws Exception {
        another = getAnother(this.config.members(), config.instance().toIntegerIndex());
    }

    @Override
    public void stop(){
        group.shutdownGracefully();
    }

    @Override
    public void shutdown(){

    }

    protected static HostnamePort getAnother(List<HostnamePort> members, int me) {

        if (members.size() != 2) {
            return null;
        }
        return members.get(2 - me);
    }

    @Override
    public void start() throws Throwable {
        group = new NioEventLoopGroup();
        ClientHandler handler = new ClientHandler(config.instance().toIntegerIndex(),config.me(),storeSupplier,logging);
        b.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) throws Exception {
                                ctx.channel().eventLoop().schedule(Client.this::doConnect, 5, TimeUnit.SECONDS);
                                ctx.fireChannelInactive();
                            }
                        });
                        p.addLast("ping", new IdleStateHandler(0, 4, 0, TimeUnit.SECONDS));
                        p.addLast(new ObjectDecoder(1024 * 1024,
                                ClassResolvers.weakCachingConcurrentResolver(this.getClass().getClassLoader())));
                        p.addLast("encoder", new ObjectEncoder());
                        p.addLast(handler);
                    }
                });
        doConnect();
    }

    private void doConnect() {
        ChannelFuture future = b.connect(another.getHost(), another.getPort());
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = future.channel();
                log.info("Started Tcp Client: " + another.getPort());
            } else {
                log.warn("Started Tcp Client Failed: " + another.getPort() + " Due to " + f.cause());
                f.channel().eventLoop().schedule(this::doConnect, 5, TimeUnit.SECONDS);
            }
        });
    }
}