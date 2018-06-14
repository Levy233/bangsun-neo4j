package org.neo4j.kernel.network.handle;

/**
 * Created by Think on 2018/5/24.
 */
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.cluster.AliveSlaves;
import org.neo4j.kernel.cluster.Slave;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.network.HeartBeatReceiver;
import org.neo4j.kernel.network.message.HeartBeatMessage;
import org.neo4j.logging.Log;
import sun.plugin2.message.HeartbeatMessage;

import java.net.InetSocketAddress;

public class HeartBeatReceiverHandler extends ChannelInboundHandlerAdapter {

    private int loss_connect_time = 0;

    private Log logging;

    private volatile AliveSlaves aliveSlaves;

    public HeartBeatReceiverHandler(AliveSlaves aliveSlaves,Log logService){
        this.logging = logService;
        this.aliveSlaves = aliveSlaves;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                loss_connect_time++;
                logging.info("5 秒没有接收到客户端的信息了");
                if (loss_connect_time > 2) {
                    logging.info("关闭这个不活跃的channel");
                    ctx.channel().close();
                }
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        Slave slave = recognizeSlave(ctx,msg);
        if(aliveSlaves.getSlaves().contains(slave)){
            logging.debug("Master has received {} continuous heartbeat signal",slave);
        }else{
            aliveSlaves.getSlaves().add(slave);
            logging.info("Master has received {} first heartbeat signal",slave);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    private Slave recognizeSlave(ChannelHandlerContext ctx,Object msg){
        HeartBeatMessage msg1 = (HeartBeatMessage)msg;
        return new Slave(ctx,msg1.getInstanceId(),msg1.getSender());
    }
}
