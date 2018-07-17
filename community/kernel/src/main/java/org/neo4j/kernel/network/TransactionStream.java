package org.neo4j.kernel.network;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;

/**
 * Created by Think on 2018/6/26.
 */
public interface TransactionStream {
    void accept( Visitor<CommittedTransactionRepresentation,Exception> visitor ) throws Exception;

    TransactionStream EMPTY = visitor ->
    {
    };
}
