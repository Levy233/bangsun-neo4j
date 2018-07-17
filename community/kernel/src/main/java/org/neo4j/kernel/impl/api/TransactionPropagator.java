//package org.neo4j.kernel.impl.api;
//
//import org.neo4j.kernel.cluster.AliveSlaves;
//import org.neo4j.kernel.cluster.Slave;
//import org.neo4j.kernel.impl.transaction.TransactionRepresentation;
//import org.neo4j.kernel.lifecycle.Lifecycle;
//import org.neo4j.kernel.network.HeartBeatReceiver;
//
//import java.util.List;
//
///**
// * Created by Think on 2018/5/28.
// */
//public class TransactionPropagator implements Lifecycle {
//
//    private AliveSlaves slaves;
//
//    public TransactionPropagator(HeartBeatReceiver receiver) {
//
//        this.slaves = receiver.getAliveSlaves();
//    }
//
//    public int committed(AliveSlaves slaves, TransactionRepresentation authorId) {
//        try {
//            for (Slave slave : slaves.getSlaves()) {
//                slave.commit(authorId);
//            }
//        }catch (Throwable e){
//            return 1;
//        }
//        return 0;
//    }
//
//    @Override
//    public void init() throws Throwable {
//
//    }
//
//    @Override
//    public void start() throws Throwable {
//
//    }
//
//    @Override
//    public void stop() throws Throwable {
//
//    }
//
//    @Override
//    public void shutdown() throws Throwable {
//
//    }
//}
