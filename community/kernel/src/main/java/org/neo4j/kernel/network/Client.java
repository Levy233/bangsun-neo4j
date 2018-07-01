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
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.exception.ComException;
import org.neo4j.kernel.network.handle.ClientHandler;
import org.neo4j.kernel.network.message.HeartBeatMessage;
import org.neo4j.kernel.network.message.TransactionStreamResponse;
import org.neo4j.logging.Log;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by Think on 2018/5/24.
 */
public class Client implements Lifecycle {

    public interface Configuration {
        List<HostnamePort> members();

        int instance(); // Name of this cluster instance. Null in most cases, but tools may use e.g. "Backup"

        HostnamePort me();

        int connectTimeOut();
    }

    private HostnamePort another;
    private Configuration config;
    private EventLoopGroup group;
    private Log logging;
    private Bootstrap b = new Bootstrap();

    public Channel getChannel() {
        return channel;
    }

    private Channel channel;
    private ResponseUnpacker unpacker;

    public Client(Configuration config, ResponseUnpacker unpacker, LogService logging) {
        this.config = config;
        this.logging = logging.getInternalLogProvider().getLog(getClass());
        this.unpacker = unpacker;
    }

    @Override
    public void init() throws Throwable {
        another = getAnother(this.config.members(), config.instance());
    }

    private HostnamePort getAnother(List<HostnamePort> members, int me) throws Exception {
        if (members.size() != 2) {
            throw new Exception("This version cannot support more than 2 instances");
        }
        return members.get(2 - me);
    }

    @Override
    public void start() throws Throwable {
        group = new NioEventLoopGroup();
        ClientHandler handler = new ClientHandler(config.instance(), config.me());
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
                                ctx.channel().eventLoop().schedule(() -> doConnect(), 5, TimeUnit.SECONDS);
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
//            new Thread(new ConnectThread()).start();
    }

    private void doConnect() {
        ChannelFuture future = b.connect(another.getHost(), another.getPort());
        future.addListener((ChannelFutureListener) f -> {
            if (f.isSuccess()) {
                channel = future.channel();
                System.out.println("Started Tcp Client: " + another);
            } else {
                System.out.println("Started Tcp Client Failed: " + another + " Due to " + f.cause());
                f.channel().eventLoop().schedule(this::doConnect, 5, TimeUnit.SECONDS);
            }
        });
    }

    @Override
    public void stop() throws Throwable {
        group.shutdownGracefully();
    }

    @Override
    public void shutdown() throws Throwable {

    }

    private ComException traceComException(ComException exception, String tracePoint) {
        return exception.traceComException(logging, tracePoint);
    }

    class ConnectThread implements Runnable {
        @Override
        public void run() {
            ChannelFuture future = null;
            try {
//                future = b.connect(another.getHost(), another.getPort()).sync();
//                future.awaitUninterruptibly(5, TimeUnit.SECONDS);
                int connectRetry = 1;
                while (future == null || !future.isSuccess()) {
                    System.out.println("Trying to connect to " + another);
                    try {
                        future = b.connect(another.getHost(), another.getPort()).sync();
                    } catch (Exception e) {
                        throw e;
                    }
                    future.awaitUninterruptibly(5, TimeUnit.SECONDS);
                    connectRetry++;
                    Thread.sleep(5000);
                    if (connectRetry > config.connectTimeOut()) {
                        break;
                    }
                }
                if (future.isSuccess()) {
                    channel = future.channel();
                    System.out.println("Client connected to " + channel.remoteAddress());
                } else {
                    Throwable cause = future.cause();
                    String msg = Client.this.getClass().getSimpleName() + " could not connect to " +
                            another.getHost() + ":" + another.getPort();
                    throw Client.this.traceComException(new ComException(msg, cause), "Client.start");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}