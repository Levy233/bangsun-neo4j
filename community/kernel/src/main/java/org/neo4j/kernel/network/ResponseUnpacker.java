package org.neo4j.kernel.network;

import org.neo4j.kernel.impl.api.*;
import org.neo4j.kernel.impl.logging.LogService;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.Commitment;
import org.neo4j.kernel.impl.transaction.log.TransactionAppender;
import org.neo4j.kernel.impl.util.Dependencies;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.kernel.network.message.TransactionStreamResponse;
import org.neo4j.storageengine.api.StorageEngine;

import javax.xml.ws.Response;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Think on 2018/6/27.
 */
public class ResponseUnpacker extends LifecycleAdapter {
    private final Dependencies dependencies;

    private TransactionQueue queue;

    private TransactionBatchCommitter batchCommitter;

    private final long idReuseSafeZoneTime;

    private TransactionCommitProcess commitProcess;
    private LogService log;
    private KernelTransactions kernelTransactions;
    private int maxBatchSize;

    public ResponseUnpacker(Dependencies dependencies, long idReuseSafeZoneTime) {
        this.dependencies = dependencies;
        this.idReuseSafeZoneTime = idReuseSafeZoneTime;
    }

    public void unpackResponse(TransactionStreamResponse response) throws Exception {
        try{
            List<CommittedTransactionRepresentation> txList = ((TransactionStreamList) response.getTransactions()).get();
            for (CommittedTransactionRepresentation tx : txList) {
                setQueue(tx);
            }
        }finally {
            applyQueuedTransactions();
        }


//        if ( stopped )
//        {
//            throw new IllegalStateException( "Component is currently stopped" );
//        }
//
//        BatchingResponseHandler responseHandler = new BatchingResponseHandler( maxBatchSize,
//                batchCommitter, obligationFulfiller, txHandler, log );
//        try
//        {
//            response.accept( responseHandler );
//        }
//        finally
//        {
//            responseHandler.applyQueuedTransactions();
//        }
    }

    private void setQueue(CommittedTransactionRepresentation transaction) throws Exception {
        queue.queue(new TransactionToApply(
                transaction.getTransactionRepresentation(),
                transaction.getCommitEntry().getTxId()) {
            @Override
            public void commitment(Commitment commitment, long transactionId) {
                // TODO Perhaps odd to override this method here just to be able to call txHandler?
                super.commitment(commitment, transactionId);
            }
        });
    }

    @Override
    public void init() throws Throwable {
        super.init();
    }

    @Override
    public void start() throws Throwable {
        this.commitProcess = new TransactionRepresentationCommitProcess(
                dependencies.resolveDependency(TransactionAppender.class),
                dependencies.resolveDependency(StorageEngine.class));
        this.log = dependencies.resolveDependency(LogService.class);
        this.kernelTransactions = dependencies.resolveDependency(KernelTransactions.class);
        this.batchCommitter = new TransactionBatchCommitter(kernelTransactions, idReuseSafeZoneTime, commitProcess, log.getInternalLog(ResponseUnpacker.class));
        this.queue = new TransactionQueue(maxBatchSize, batchCommitter);
    }

    @Override
    public void stop() throws Throwable {
        super.stop();
    }

    @Override
    public void shutdown() throws Throwable {
        super.shutdown();
    }

    void applyQueuedTransactions() throws Exception {
        queue.empty();
    }
}
