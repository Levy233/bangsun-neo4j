package org.neo4j.kernel.network.handle;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.network.message.HeartBeatMessage;

import java.util.Date;

/**
 * Created by Think on 2018/5/24.
 */

@ChannelHandler.Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final int instanceId;

    private final HostnamePort hostnamePort;

    public ClientHandler(int instanceId, HostnamePort hostnamePort){
        this.instanceId = instanceId;
        this.hostnamePort = hostnamePort;
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client: active channel id "+ctx.channel().id());
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Client: 停止时间是：" + new Date());
        System.out.println("Client: HeartBeatClientHandler channelInactive");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                ctx.write(new HeartBeatMessage(instanceId,hostnamePort,"I_am_alive"));
                ctx.flush();
                System.out.println("Client: sended heartbeat msg... "+ctx.channel().id());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.out.println("Client: master is down");
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String message = (String) msg;
        System.out.println(message);
        if (message.equals("Heartbeat")) {
            ctx.write("has read message from server");
            ctx.flush();
        }
        ReferenceCountUtil.release(msg);
    }
}