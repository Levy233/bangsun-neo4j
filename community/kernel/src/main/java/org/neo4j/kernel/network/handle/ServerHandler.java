package org.neo4j.kernel.network.handle;

/**
 * Created by Think on 2018/5/24.
 */
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.neo4j.kernel.cluster.AliveSlaves;
import org.neo4j.kernel.cluster.Slave;
import org.neo4j.kernel.network.ResponsePacker;
import org.neo4j.kernel.network.message.HeartBeatMessage;
import org.neo4j.kernel.network.message.RequestContext;
import org.neo4j.kernel.network.message.TransactionStreamResponse;
import org.neo4j.kernel.network.state.StateMachine;
import org.neo4j.logging.Log;

@ChannelHandler.Sharable
public class ServerHandler extends ChannelInboundHandlerAdapter {
    private Log logging;

    private volatile AliveSlaves aliveSlaves;

    //未收到信号间隔次数
    private int outTime;

    private StateMachine stateMachine;
    private ResponsePacker responsePacker;

    public ServerHandler(AliveSlaves aliveSlaves, int outTime, StateMachine stateMachine, Log logService,ResponsePacker responsePacker){
        this.logging = logService;
        this.outTime = outTime;
        this.aliveSlaves = aliveSlaves;
        this.stateMachine = stateMachine;
        this.responsePacker = responsePacker;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                Slave slave = aliveSlaves.search(ctx);
                int time_out = slave.addTimeOut();
                System.out.println("5 秒没有接收到客户端的信息了");
                if (time_out > outTime) {
                    System.out.println("关闭这个不活跃的channel");
                    ctx.channel().close();
                    aliveSlaves.remove(slave);
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Server: channelActive: "+ctx.channel().id());
        System.out.println(aliveSlaves.getSlaves().size());
        try (Slave slave = aliveSlaves.search(ctx)) {
            if (slave != null) {
                System.out.println("Server: "+slave.getCtx().channel().remoteAddress());
            } else {
                aliveSlaves.add(slave);
            }
        }
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Server: channelInactive: "+ctx.channel().id());
        ctx.fireChannelInactive();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("111");
        System.out.println("Server: channelRead "+msg.toString());
        if (msg instanceof HeartBeatMessage) {
            stateMachine.handle((HeartBeatMessage) msg);
        } else if (msg instanceof RequestContext) {
            TransactionStreamResponse response = responsePacker.packTransactionStreamResponse((RequestContext) msg);
            ctx.channel().writeAndFlush(response);
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        System.out.println("Server: channelReadComplete "+ctx.channel().id());
        ctx.fireChannelReadComplete();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        try (Slave slave = aliveSlaves.search(ctx)) {
            if (slave != null) {
                aliveSlaves.remove(slave);
                System.out.println(slave.getCtx().channel().remoteAddress() + "closed due to exception" + cause.getMessage());
            }
        }
        ctx.close();
    }

//    private Slave recognizeSlave(ChannelHandlerContext ctx,Object msg){
//        HeartBeatMessage msg1 = (HeartBeatMessage)msg;
//        return new Slave(ctx,msg1.getInstanceId(),msg1.getSender());
//    }
}
