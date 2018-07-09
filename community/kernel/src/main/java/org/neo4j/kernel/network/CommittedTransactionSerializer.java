package org.neo4j.kernel.network;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.FlushableChannel;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntryCommit;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntryStart;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntryWriter;

import java.io.IOException;

/**
 * Created by Think on 2018/7/3.
 */
public class CommittedTransactionSerializer implements Visitor<CommittedTransactionRepresentation,Exception>
{
    private final LogEntryWriter writer;

    public CommittedTransactionSerializer( FlushableChannel networkFlushableChannel )
    {
        this.writer = new LogEntryWriter( networkFlushableChannel );
    }

    @Override
    public boolean visit( CommittedTransactionRepresentation tx ) throws IOException
    {
        LogEntryStart startEntry = tx.getStartEntry();
        writer.writeStartEntry( startEntry.getMasterId(), startEntry.getLocalId(),
                startEntry.getTimeWritten(), startEntry.getLastCommittedTxWhenTransactionStarted(),
                startEntry.getAdditionalHeader() );
        writer.serialize( tx.getTransactionRepresentation() );
        LogEntryCommit commitEntry = tx.getCommitEntry();
        writer.writeCommitEntry( commitEntry.getTxId(), commitEntry.getTimeWritten() );
        return false;
    }
}
