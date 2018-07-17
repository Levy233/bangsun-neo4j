package org.neo4j.kernel.network.handle;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.network.message.HeartBeatMessage;
import org.neo4j.kernel.network.message.TransactionStreamResponse;
import org.neo4j.logging.Log;

import java.util.Date;
import java.util.function.Supplier;

/**
 * Created by Think on 2018/5/24.
 */

@ChannelHandler.Sharable
public class ClientHandler extends ChannelInboundHandlerAdapter {

    private final int instanceId;

    private final HostnamePort hostnamePort;
    private Supplier<TransactionIdStore> storeSupplier;
    private TransactionIdStore transactionIdStore;
    private Log log;
    public ClientHandler(int instanceId, HostnamePort hostnamePort, Supplier<TransactionIdStore> storeSupplier,LogService logging) {
        this.instanceId = instanceId;
        this.hostnamePort = hostnamePort;
        this.storeSupplier = storeSupplier;
        this.log = logging.getInternalLog(getClass());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                if (transactionIdStore == null) {
                    transactionIdStore = storeSupplier.get();
                }
                HeartBeatMessage msg = new HeartBeatMessage(instanceId, hostnamePort, "I_am_alive");
                msg.setStoreId(transactionIdStore.getLastCommittedTransactionId());
                ctx.write(msg);
                ctx.flush();
                log.debug("Client: sended heartbeat msg... " + ctx.channel().id());
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.debug("Client: master is down");
        super.exceptionCaught(ctx, cause);
    }
}