package org.neo4j.kernel.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.neo4j.kernel.lifecycle.Lifecycle;
import org.neo4j.kernel.network.handle.UpdateSenderHandler;

public class UpdateSender implements Lifecycle {
    static UpdateSenderHandler client = new UpdateSenderHandler();

    EventLoopGroup workerGroup;

    @Override
    public void init() throws Throwable {

    }

    @Override
    public void start() {
        String host = "127.0.0.1";
        int port = 9090;
        workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ch.pipeline().addLast(client);
                }
            });
            ChannelFuture f = b.connect(host, port).sync();
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public void commit(){

    }

    @Override
    public void stop() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {

    }
}