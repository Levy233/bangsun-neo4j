package org.neo4j.kernel.cluster;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Think on 2018/5/25.
 */
public class AliveSlaves {

    private volatile List<Slave> slaves = new ArrayList<>();

    public synchronized void add(Slave slave){
        if(!slaves.contains(slave)){
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
}
