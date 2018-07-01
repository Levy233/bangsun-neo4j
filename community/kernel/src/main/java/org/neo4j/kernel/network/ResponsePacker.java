package org.neo4j.kernel.network;

import org.neo4j.cursor.IOCursor;
import org.neo4j.helpers.collection.Visitor;
import org.neo4j.kernel.impl.store.StoreId;
import org.neo4j.kernel.impl.transaction.CommittedTransactionRepresentation;
import org.neo4j.kernel.impl.transaction.log.LogicalTransactionStore;
import org.neo4j.kernel.impl.transaction.log.TransactionIdStore;
import org.neo4j.kernel.network.message.RequestContext;
import org.neo4j.kernel.network.message.TransactionStreamResponse;

import java.io.IOException;
import java.util.function.Supplier;

import static org.neo4j.kernel.impl.transaction.log.TransactionIdStore.BASE_TX_ID;

/**
 * Created by Think on 2018/6/26.
 */
public class ResponsePacker {

    protected final Supplier<LogicalTransactionStore> transactionStoreSupplier;
    protected final Supplier<StoreId> storeId; // for lazy storeId getter
    private final Supplier<TransactionIdStore> transactionIdStoreSupplier;
    public ResponsePacker( Supplier<LogicalTransactionStore> transactionStoreSupplier, Supplier<TransactionIdStore> transactionIdStoreSupplier,
                           Supplier<StoreId> storeId )
    {
        this.transactionStoreSupplier = transactionStoreSupplier;
        this.transactionIdStoreSupplier = transactionIdStoreSupplier;
        this.storeId = storeId;
    }
    public TransactionStreamResponse packTransactionStreamResponse(RequestContext context) throws IOException {
        final long toStartFrom = context.lastAppliedTransaction() + 1;
        final long toEndAt = transactionIdStoreSupplier.get().getLastCommittedTransactionId();
        IOCursor<CommittedTransactionRepresentation> cursor = transactionStoreSupplier.get()
                .getTransactions( toStartFrom );
        TransactionStreamList list = new TransactionStreamList();
        while (cursor.next()){
            CommittedTransactionRepresentation transactionRepresentation = cursor.get();
            if(transactionRepresentation.getCommitEntry().getTxId()>toEndAt) break;
            list.add(transactionRepresentation);
        }
        return new TransactionStreamResponse(storeId.get(),list);
//        TransactionStream transactions = visitor ->
//        {
//            // Check so that it's even worth thinking about extracting any transactions at all
//            if ( toStartFrom > BASE_TX_ID && toStartFrom <= toEndAt )
//            {
//                extractTransactions( toStartFrom, filterVisitor( visitor, toEndAt ) );
//            }
//        };
//        return new TransactionStreamResponse(storeId.get(), transactions);
    }

    protected Visitor<CommittedTransactionRepresentation,Exception> filterVisitor(
            final Visitor<CommittedTransactionRepresentation,Exception> delegate, final long txToEndAt )
    {
        return new Visitor<CommittedTransactionRepresentation, Exception>() {
            @Override
            public boolean visit(CommittedTransactionRepresentation element) throws Exception {
                if (element.getCommitEntry().getTxId() > txToEndAt) {
                    return false;
                }
                return delegate.visit(element);
            }
        };
    }

    protected void extractTransactions( long startingAtTransactionId,
                                        Visitor<CommittedTransactionRepresentation,Exception> visitor )
            throws Exception
    {
        try ( IOCursor<CommittedTransactionRepresentation> cursor = transactionStoreSupplier.get()
                .getTransactions( startingAtTransactionId ) )
        {
            while ( cursor.next() && !visitor.visit( cursor.get() ) )
            {
            }
        }
    }
}
