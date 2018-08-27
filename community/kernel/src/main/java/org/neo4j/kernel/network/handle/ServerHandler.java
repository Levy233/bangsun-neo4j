package org.neo4j.kernel.network.handle;

import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.cluster.AliveSlaves;
import org.neo4j.kernel.cluster.Slave;
import org.neo4j.kernel.network.ResponsePacker;
import org.neo4j.kernel.network.Server;
import org.neo4j.kernel.network.message.HeartBeatMessage;
import org.neo4j.kernel.network.message.HeartBeatResponse;
import org.neo4j.kernel.network.state.StateMachine;
import org.neo4j.logging.Log;
import org.neo4j.logging.LogProvider;

import java.time.Clock;

@ChannelHandler.Sharable
public class ServerHandler extends SimpleChannelHandler{

    private Log logging;
    private volatile AliveSlaves aliveSlaves;
    //未收到信号间隔次数
    private int outTime;
    private StateMachine stateMachine;

    public ServerHandler(AliveSlaves aliveSlaves, Server.Configuration config, LogProvider logProvider, ChannelGroup channelGroup, Clock clock, StateMachine stateMachine, ResponsePacker responsePacker) {
        this.logging = logProvider.getLog(getClass());
        this.outTime = config.lost_time_out();
        this.aliveSlaves = aliveSlaves;
        this.stateMachine = stateMachine;
    }

    @Override
    public void handleUpstream(final ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
        if (e instanceof org.jboss.netty.handler.timeout.IdleStateEvent) {
            if (((org.jboss.netty.handler.timeout.IdleStateEvent) e).getState() == org.jboss.netty.handler.timeout.IdleState.ALL_IDLE) {
//                System.out.println("需要提醒玩家下线");
                //关闭会话,踢玩家下线
                ChannelFuture write = ctx.getChannel().write("hi  time out, you will close");
                write.addListener(future -> ctx.getChannel().close());
            }
        } else {
            super.handleUpstream(ctx, e);
        }
    }

    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        logging.info("Server: channelActive: Local: " + ctx.getChannel().getLocalAddress()+" from: "+ctx.getChannel().getRemoteAddress());
        logging.info("HeartBeat started");
        try (Slave slave = aliveSlaves.search(ctx)) {
            if (slave != null) {
//                System.out.println("Server: " + slave.getCtx().getChannel().getRemoteAddress());
            } else {
                HostnamePort hostnamePort = new HostnamePort(ctx.getChannel().getRemoteAddress().toString().replace("/", ""));
                Slave slave1 = new Slave(ctx, hostnamePort);
                aliveSlaves.add(slave1);
            }
        }
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent event) throws Exception {
        Object msg = event.getMessage();
        logging.debug("Server: channelRead " + msg.toString());
        if (msg instanceof HeartBeatMessage) {
            stateMachine.handle((HeartBeatMessage) msg);
//            ctx.getChannel().write(new HeartBeatResponse("receive heartbeat msg"));
        }
    }
}
