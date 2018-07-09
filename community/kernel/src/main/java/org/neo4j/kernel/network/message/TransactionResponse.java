package org.neo4j.kernel.network.message;

import org.neo4j.helpers.HostnamePort;

import java.io.Serializable;

/**
 * Created by Think on 2018/7/2.
 */
public class TransactionResponse implements Serializable{
    private static final long nSerialVerUID = 89098410L;

    private int instanceId;

    private String alive;

    public long getStoreId() {
        return storeId;
    }
    public void setStoreId(long storeId) {
        this.storeId = storeId;
    }

    private long storeId;

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

    public TransactionResponse(int instanceId,HostnamePort sender, String alive){
        this.instanceId = instanceId;
        this.alive = alive;
        this.sender = sender;
    }

    @Override
    public String toString(){
        return "Id is "+instanceId +" and is " + alive;
    }
}
