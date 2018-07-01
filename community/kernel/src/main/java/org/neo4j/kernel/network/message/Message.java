package org.neo4j.kernel.network.message;

import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;

import java.io.IOException;

/**
 * Created by Think on 2018/6/15.
 */
public interface Message{
    void handle(Handler handler) throws Exception;
    public interface Handler
    {
        void obligation( long txId ) throws IOException;

        /**
         * @return a {@link Visitor} which will {@link Visitor#visit(Object) receive} calls about transactions.
         */
        Visitor<CommittedTransactionRepresentation,Exception> transactions();
    }
}
