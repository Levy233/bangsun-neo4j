package org.neo4j.kernel.network.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.neo4j.helpers.HostnamePort;

import java.io.Serializable;

/**
 * Created by Think on 2018/5/25.
 */
public class HeartBeatMessage implements Serializable {
    private static final long nSerialVerUID = 1L;

    private  int instanceId;

    private String alive;

    public int getInstanceId() {
        return instanceId;
    }

    public String getAlive() {
        return alive;
    }

    public HostnamePort getSender() {
        return sender;
    }

    private HostnamePort sender;

    public HeartBeatMessage(int instanceId,HostnamePort sender,String alive){
        this.instanceId = instanceId;
        this.alive = alive;
        this.sender = sender;
    }

    @Override
    public String toString(){
        return "Id is "+instanceId +" and is " + alive;
    }
}
