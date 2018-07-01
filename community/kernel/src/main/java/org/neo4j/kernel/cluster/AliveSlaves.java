package org.neo4j.kernel.cluster;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 2018/5/25.
 */
public class AliveSlaves {

    private volatile List<Slave> slaves = new ArrayList<>();

    public synchronized void add(Slave slave){
        if(!slaves.contains(slave)||this.search(slave.getCtx())!=null){
            slaves.add(slave);
        }else{

        }
    }

    public synchronized void remove(Slave slave){
        slaves.remove(slave);
    }

    public List<Slave> getSlaves(){
        return slaves;
    }
    public Slave search(ChannelHandlerContext ctx){
        for (Slave slave:slaves) {
            if (slave.getCtx()==ctx) return slave;
        }
        return null;
    }
}
