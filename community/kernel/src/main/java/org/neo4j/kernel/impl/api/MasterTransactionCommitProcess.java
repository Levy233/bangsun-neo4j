/*
 * Copyright (c) 2002-2018 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.api;

import org.neo4j.kernel.api.exceptions.TransactionFailureException;
import org.neo4j.kernel.cluster.AliveSlaves;
import org.neo4j.kernel.cluster.Slave;
import org.neo4j.kernel.impl.transaction.state.IntegrityValidator;
import org.neo4j.kernel.impl.transaction.tracing.CommitEvent;
import org.neo4j.kernel.network.HeartBeatReceiver;
import org.neo4j.kernel.network.handle.HeartBeatReceiverHandler;
import org.neo4j.storageengine.api.StorageEngine;
import org.neo4j.storageengine.api.TransactionApplicationMode;

import java.util.List;

/**
 * Commit process on the master side in HA, where transactions either comes in from slaves committing,
 * or gets created and committed directly on the master.
 */
public class MasterTransactionCommitProcess implements TransactionCommitProcess {

    private final TransactionCommitProcess inner;
    private final TransactionPropagator txPropagator;
    private final HeartBeatReceiver receiver;
//    private final IntegrityValidator validator;
//    private final Monitor monitor;

//    public interface Monitor
//    {
//        void missedReplicas(int number);
//    }

    public MasterTransactionCommitProcess(TransactionCommitProcess commitProcess,HeartBeatReceiver receiver,
                                          TransactionPropagator txPropagator) {
        this.inner = commitProcess;
        this.receiver = receiver;
        this.txPropagator = txPropagator;
//        this.validator = validator;
//        this.monitor = monitor;
    }

    @Override
    public long commit(TransactionToApply batch, CommitEvent commitEvent, TransactionApplicationMode mode)
            throws TransactionFailureException {
//        validate( batch );
        long result = inner.commit(batch, commitEvent, mode);
        AliveSlaves slaves = receiver.getAliveSlaves();

        // Assuming all the transactions come from the same author
        int missedReplicas = txPropagator.committed(slaves, batch.transactionRepresentation());

        if (missedReplicas > 0) {
//            monitor.missedReplicas( missedReplicas );
        }

        return result;
    }

//    private void validate( TransactionToApply batch ) throws TransactionFailureException
//    {
//        while ( batch != null )
//        {
//            validator.validateTransactionStartKnowledge(
//                    batch.transactionRepresentation().getLatestCommittedTxWhenStarted() );
//            batch = batch.next();
//        }
//    }

}
