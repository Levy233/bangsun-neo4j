package org.neo4j.kernel.cluster;

import io.netty.channel.ChannelHandlerContext;
import org.neo4j.helpers.HostnamePort;
import org.neo4j.kernel.impl.transaction.TransactionRepresentation;

import java.util.HashMap;

/**
 * Created by Think on 2018/5/25.
 */
public class Slave implements AutoCloseable{

    private int instanceId;

    private HostnamePort hostnamePort;

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    private ChannelHandlerContext ctx;

    private int time_out = 0;

    public Slave(ChannelHandlerContext ctx,int instanceId, HostnamePort hostnamePort){
        this.instanceId = instanceId;
        this.hostnamePort = hostnamePort;
        this.ctx =ctx;
    }

    public int getInstanceId(){
        return instanceId;
    }

    public HostnamePort getHostnamePort(){
        return hostnamePort;
    }

    @Override
    public boolean equals(Object obj) {
        return this.instanceId == ((Slave) obj).instanceId
                && this.hostnamePort.equals(((Slave) obj).hostnamePort);
    }

    @Override
    public String toString(){
        return instanceId+" "+hostnamePort.toString();
    }

    public Boolean commit(TransactionRepresentation representation){
        ctx.writeAndFlush(representation);
        return true;
    }
    @Override
    public void close() throws Exception {

    }

    public int addTimeOut(){
        return time_out++;
    }

    public boolean resetTimeOut(){
        time_out=0;
        return true;
    }
}
