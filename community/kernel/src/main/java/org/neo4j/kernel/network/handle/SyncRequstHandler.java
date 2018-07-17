package org.neo4j.kernel.network.handle;

/**
 * Created by Think on 2018/7/2.
 */

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.jboss.netty.buffer.ChannelBuffer;
import org.neo4j.kernel.network.message.HeartBeatResponse;
import org.neo4j.kernel.network.message.TransactionResponse;
import org.neo4j.kernel.network.message.TransactionStreamResponse;

import java.util.concurrent.CountDownLatch;

@ChannelHandler.Sharable
public class SyncRequstHandler extends ChannelInboundHandlerAdapter {

    private CountDownLatch lathc;

    public SyncRequstHandler(CountDownLatch lathc) {
        this.lathc = lathc;
    }

    private Object result;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ChannelBuffer) {
            result = msg;
            System.out.println(result.toString());
            lathc.countDown();
        } else if (msg instanceof HeartBeatResponse) {
            System.out.println("Client: " + msg);
        }else {
            System.out.println("Client: received msg cannot be recegnized");
        }
        ctx.fireChannelRead(msg);
    }

    public void resetLatch(CountDownLatch initLathc) {
        this.lathc = initLathc;
    }

    public Object getResult() {
        return result;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        // IdleStateHandler 所产生的 IdleStateEvent 的处理逻辑.
//        if (evt instanceof IdleStateEvent) {
//            IdleStateEvent e = (IdleStateEvent) evt;
//            switch (e.state()) {
//                case ALL_IDLE:
//                    handleAllIdle(ctx);
//                    break;
//                default:
//                    break;
//            }
//        }
    }

//    protected void handleAllIdle(ChannelHandlerContext ctx) {
//        ctx.channel().writeAndFlush("1" + "\r\n");
//    }

}
